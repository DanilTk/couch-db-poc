package pl.home.couchdbpoc.data.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.lightcouch.CouchDbClient;
import org.lightcouch.NoDocumentException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.home.couchdbpoc.config.CouchDatabaseProperties;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.SyncStatusDocument;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRepositoryCouchDb {
	private final CouchDatabaseProperties couchDbProperties;
	private final RestTemplate restTemplate;
	private final CouchDbClient couchDbClient;

	public void saveAll(List<CustomerDocument> collection) {
		couchDbClient.bulk(collection, true);
	}

	public boolean existsByMarkerId(String id) {
		try {
			couchDbClient.find(SyncStatusDocument.class, id);
			return true;
		} catch (NoDocumentException e) {
			return false;
		}
	}

	public void save(SyncStatusDocument syncStatusDocument) {
		couchDbClient.save(syncStatusDocument);
	}

	public void prepareReplica(String country) {
		String dbName = "customers_" + hash8(country);
		String targetDbUrl = couchDbProperties.getUrl() + "/" + dbName;

		try {
			restTemplate.put(targetDbUrl, null);
		} catch (Exception ignored) {
			//ignore
		}

		ensureCountryUserAndSecurity(country);

		String replicationId = "rep-" + country.toLowerCase().replaceAll("\\s+", "-");
		String source = couchDbProperties.getUrlWithAuth() + "/customers";
		String target = couchDbProperties.getUrlWithAuth() + "/" + dbName;

		Map<String, Object> replicationBody = Map.of(
			"_id", replicationId,
			"source", source,
			"target", target,
			"selector", Map.of("country", country),
			"continuous", true,
			"create_target", false
		);

		String replicatorUrl = couchDbProperties.getUrl() + "/_replicator";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(replicationBody, headers);

		try {
			restTemplate.postForEntity(replicatorUrl, request, String.class);
			log.info("✅ Replica ready for '{}' -> {}", country, dbName);
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode() == HttpStatus.CONFLICT) {
				log.warn("⚠ Replication already exists for '{}'", country);
			} else {
				throw e;
			}
		}
	}

	private HttpHeaders jsonHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	public void waitForDatabaseToBeReady(String dbName) {
		String dbUrl = couchDbProperties.getUrl() + "/" + dbName;      // http://…/customers_<hash>

		for (int i = 0; i < 50; i++) {        // максимум ~5 сек (50 × 100 мс)
			try {
				restTemplate.headForHeaders(dbUrl); // HEAD быстрее, чем GET
				return;                             // 200 → база готова
			} catch (HttpClientErrorException.NotFound ex) {
				// ещё не создана
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		throw new IllegalStateException("Timeout: database " + dbName + " was not created.");
	}

	public String replicateCountry(String countryName) {
		String targetDb = "customers_" + hash8(countryName);          // customers_5dbddf91

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("source", couchDbProperties.getUrl() + "/customers"); // http://…/customers
		body.put("target", couchDbProperties.getUrl() + "/" + targetDb);
		body.put("selector", Map.of("country", countryName));          // только selector
		body.put("create_target", true);
		body.put("continuous", true);                                  // live-репликация

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		restTemplate.postForEntity(
			couchDbProperties.getUrl() + "/_replicate",
			new HttpEntity<>(body, headers),
			String.class);

		log.info("✅ Replicated '{}' → {}", countryName, targetDb);
		return targetDb;                                               // <- вернули имя БД
	}

	private String hash8(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] digest = md.digest(input.getBytes(UTF_8));
			return Hex.encodeHexString(digest).substring(0, 8);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Hash error", e);
		}
	}

	/**
	 * Ensure a reader user exists for this country and that the replica DB
	 * accepts only that reader role.
	 */
	public void ensureCountryUserAndSecurity(String countryName) {
		String hash = hash8(countryName);           // same helper you already have
		String userName = "reader_" + hash;             // e.g. reader_f1a28e3b
		String userDocId = "org.couchdb.user:" + userName;
		String password = new StringBuilder(hash).reverse().toString(); // reversed hash

		// ---- 1. create user in /_users if missing -------------------------
		String usersUrl = couchDbProperties.getUrl() + "/_users/" + userDocId;

		try {
			restTemplate.getForEntity(usersUrl, String.class);  // 200 → already exists
		} catch (HttpClientErrorException.NotFound nf) {
			Map<String, Object> userDoc = Map.of(
				"_id", userDocId,
				"name", userName,
				"roles", List.of(userName),
				"type", "user",
				"password", password
			);
			restTemplate.put(usersUrl, userDoc);                // creates or replaces
			log.info("👤 Created CouchDB user {}", userName);
		}

		// ---- 2. write _security doc into replica DB -----------------------
		String dbName = "customers_" + hash;
		String securityUrl = couchDbProperties.getUrl() + "/" + dbName + "/_security";

		Map<String, Object> secDoc = Map.of(
			"members", Map.of("roles", List.of(userName))
		);
		restTemplate.put(securityUrl, secDoc);
		log.info("🔐 Applied _security to {}", dbName);
	}
}
