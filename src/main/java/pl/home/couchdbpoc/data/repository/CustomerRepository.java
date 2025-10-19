package pl.home.couchdbpoc.data.repository;

import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.web.dto.CustomerRequest;
import pl.home.couchdbpoc.web.dto.CustomerResponse;

import java.util.List;

public interface CustomerRepository {

    CustomerResponse saveCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(String id, CustomerRequest request);

    void deleteCustomer(String id);

    CustomerResponse getCustomerById(String id);

    List<String> findDistinctCountries();

    List<CustomerEntity> findAll();

}
