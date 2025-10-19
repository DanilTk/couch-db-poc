package pl.home.couchdbpoc.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import lombok.RequiredArgsConstructor;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CouchDbConfig {
	private final CouchDatabaseProperties couchDbProperties;

	@Bean
	public CouchDbClient customersCouchDbClient() {
		return createCouchDbClient("customers");
	}

	private GsonBuilder getGsonBuilder() {
		return new GsonBuilder()
			.registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
			.registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
			.registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
			.registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, typeOfT, context) -> LocalDate.parse(json.getAsString()));
	}

	private CouchDbProperties getDbProperties() {
		return new CouchDbProperties()
			.setCreateDbIfNotExist(true)
			.setProtocol(couchDbProperties.getProtocol())
			.setHost(couchDbProperties.getHost())
			.setPort(couchDbProperties.getPort())
			.setUsername(couchDbProperties.getUsername())
			.setPassword(couchDbProperties.getPassword());

	}

	private CouchDbClient createCouchDbClient(String dbName) {
		CouchDbProperties properties = getDbProperties().setDbName(dbName.toLowerCase());
		CouchDbClient client = new CouchDbClient(properties);
		client.setGsonBuilder(getGsonBuilder());
		return client;
	}

}
