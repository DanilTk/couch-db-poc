package pl.home.couchdbpoc.sync;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.data.SyncStatusDocument;
import pl.home.couchdbpoc.data.repository.CustomerRepositoryCouchDb;
import pl.home.couchdbpoc.data.repository.CustomerRepository;
import pl.home.couchdbpoc.mapper.CustomerDocumentMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DbSyncHandler {
	private static final String DB_SYNC_MARKER = "sync-check";
	private static final int PAGE_SIZE = 2500;

	private final CustomerRepositoryCouchDb customerRepositoryCouchDb;
	private final CustomerRepository customerRepository;
	private final CustomerDocumentMapper customerDocumentMapper;

	@EventListener(value = ApplicationReadyEvent.class)
	public void synchronizeDbCopies() {
		List<String> countries = customerRepository.findDistinctCountries();
		for (String country : countries) {
			try {
				customerRepositoryCouchDb.prepareReplica(country);
				log.info("✔ Finished setup for {}", country);
			} catch (Exception e) {
				log.error("❌ Failed to replicate country '{}'", country, e);
			}
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void onApplicationReady() {
		if (customerRepositoryCouchDb.existsByMarkerId(DB_SYNC_MARKER)) {
			return;
		}

		long totalRecords = customerRepository.findAll().size();
		AtomicInteger processed = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		List<CustomerEntity> allEntities = customerRepository.findAll();
		
		// Process in chunks for better memory management
		for (int i = 0; i < allEntities.size(); i += PAGE_SIZE) {
			int endIndex = Math.min(i + PAGE_SIZE, allEntities.size());
			List<CustomerEntity> chunk = allEntities.subList(i, endIndex);
			
			List<CustomerDocument> documents = chunk.parallelStream()
				.map(customerDocumentMapper::map)
				.toList();

			/* save asynchronously and update counter when done */
			executor.submit(() -> {
				customerRepositoryCouchDb.saveAll(documents);
				int done = processed.addAndGet(documents.size());
				log.info("✔ Saved {} docs |  progress: {}/{}", documents.size(), done, totalRecords);
			});

			log.info("→ Queued chunk {}/{}", (i / PAGE_SIZE) + 1, (allEntities.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		}

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		customerRepositoryCouchDb.save(new SyncStatusDocument());
	}

}
