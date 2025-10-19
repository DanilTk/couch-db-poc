package pl.home.couchdbpoc.sync;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.data.SyncStatusDocument;
import pl.home.couchdbpoc.data.repository.CustomerRepositoryCouchDb;
import pl.home.couchdbpoc.data.repository.CustomerRepositoryJpa;
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

	private final CustomerRepositoryCouchDb customerRepository;
	private final CustomerRepositoryJpa customerRepositoryJpa;
	private final CustomerDocumentMapper customerDocumentMapper;

	@EventListener(value = ApplicationReadyEvent.class)
	public void synchronizeDbCopies() {
		List<String> countries = customerRepositoryJpa.findDistinctCountries();
		for (String country : countries) {
			try {
				customerRepository.prepareReplica(country);
				log.info("✔ Finished setup for {}", country);
			} catch (Exception e) {
				log.error("❌ Failed to replicate country '{}'", country, e);
			}
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void onApplicationReady() {
		if (customerRepository.existsByMarkerId(DB_SYNC_MARKER)) {
			return;
		}

		long totalRecords = customerRepositoryJpa.count();
		AtomicInteger processed = new AtomicInteger(0);

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		int page = 0;
		Page<CustomerEntity> entitiesPage;

		do {
			Pageable pageable = PageRequest.of(page, PAGE_SIZE);
			entitiesPage = customerRepositoryJpa.findAll(pageable);

			List<CustomerDocument> documents = entitiesPage.getContent().parallelStream()
				.map(customerDocumentMapper::map)
				.toList();

			/* save asynchronously and update counter when done */
			executor.submit(() -> {
				customerRepository.saveAll(documents);
				int done = processed.addAndGet(documents.size());
				log.info("✔ Saved {} docs |  progress: {}/{}", documents.size(), done, totalRecords);
			});

			log.info("→ Queued page {}/{}", page + 1, entitiesPage.getTotalPages());
			page++;

		} while (!entitiesPage.isLast());

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		customerRepository.save(new SyncStatusDocument());
	}

}
