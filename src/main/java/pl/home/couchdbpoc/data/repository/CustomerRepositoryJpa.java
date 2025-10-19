package pl.home.couchdbpoc.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.home.couchdbpoc.data.CustomerEntity;

import java.util.List;

public interface CustomerRepositoryJpa extends JpaRepository<CustomerEntity, String> {

	@Query("SELECT DISTINCT e.country FROM CustomerEntity e")
	List<String> findDistinctCountries();

}
