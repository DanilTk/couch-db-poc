package pl.home.couchdbpoc.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.home.couchdbpoc.data.repository.CustomerRepository;
import pl.home.couchdbpoc.web.dto.CustomerRequest;
import pl.home.couchdbpoc.web.dto.CustomerResponse;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerResponse saveCustomer(CustomerRequest request) {
        return customerRepository.saveCustomer(request);
    }

    public CustomerResponse updateCustomer(String id, CustomerRequest request) {
        return customerRepository.updateCustomer(id, request);
    }

    public void deleteCustomer(String id) {
        customerRepository.deleteCustomer(id);
    }

    public CustomerResponse getCustomerById(String id) {
        return customerRepository.getCustomerById(id);
    }

}
