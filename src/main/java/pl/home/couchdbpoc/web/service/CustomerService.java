package pl.home.couchdbpoc.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.repository.CustomerRepository;
import pl.home.couchdbpoc.data.repository.CustomerRepositoryCouchDb;
import pl.home.couchdbpoc.mapper.CustomerDocumentMapper;
import pl.home.couchdbpoc.web.dto.CustomerRequest;
import pl.home.couchdbpoc.web.dto.CustomerResponse;

@Service
@RequiredArgsConstructor
public class CustomerService {
	private final CustomerDocumentMapper customerDocumentMapper;
	private final CustomerRepositoryCouchDb customerRepositoryCouchDb;
	private final CustomerRepository customerRepository;

	public CustomerResponse saveCustomer(CustomerRequest request) {
		CustomerResponse customerResponse = customerRepository.saveCustomer(request);
		CustomerDocument document = customerDocumentMapper.map(customerResponse);
		customerRepositoryCouchDb.save(document);
		return customerResponse;
	}

	public CustomerResponse updateCustomer(String id, CustomerRequest request) {
		CustomerResponse customerResponse = customerRepository.updateCustomer(id, request);
		CustomerDocument document = customerDocumentMapper.map(customerResponse);
		customerRepositoryCouchDb.update(document);
		return customerResponse;
	}

	public void deleteCustomer(String id) {
		customerRepository.deleteCustomer(id);
		customerRepositoryCouchDb.deleteById(id);
	}

	public CustomerResponse getCustomerById(String id) {
		return customerRepository.getCustomerById(id);
	}

}
