package pl.home.couchdbpoc.data.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.web.dto.CustomerRequest;
import pl.home.couchdbpoc.web.dto.CustomerResponse;
import pl.home.couchdbpoc.web.mapper.CustomerResponseMapper;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryDb implements CustomerRepository {

    private final CustomerRepositoryJpa customerRepositoryJpa;
    private final CustomerResponseMapper customerResponseMapper;

    @Override
    @Transactional
    public CustomerResponse saveCustomer(CustomerRequest request) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setCompany(request.getCompany());
        entity.setCity(request.getCity());
        entity.setCountry(request.getCountry());
        entity.setPhone1(request.getPhone1());
        entity.setPhone2(request.getPhone2());
        entity.setEmail(request.getEmail());
        entity.setSubscriptionDate(request.getSubscriptionDate());
        entity.setWebsite(request.getWebsite());
        
        CustomerEntity savedEntity = customerRepositoryJpa.save(entity);
        return customerResponseMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(String id, CustomerRequest request) {
        CustomerEntity existingEntity = customerRepositoryJpa.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
        
        existingEntity.setFirstName(request.getFirstName());
        existingEntity.setLastName(request.getLastName());
        existingEntity.setCompany(request.getCompany());
        existingEntity.setCity(request.getCity());
        existingEntity.setCountry(request.getCountry());
        existingEntity.setPhone1(request.getPhone1());
        existingEntity.setPhone2(request.getPhone2());
        existingEntity.setEmail(request.getEmail());
        existingEntity.setSubscriptionDate(request.getSubscriptionDate());
        existingEntity.setWebsite(request.getWebsite());
        
        CustomerEntity updatedEntity = customerRepositoryJpa.save(existingEntity);
        return customerResponseMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteCustomer(String id) {
        if (!customerRepositoryJpa.existsById(id)) {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
        
        customerRepositoryJpa.deleteById(id);
    }

    @Override
    public CustomerResponse getCustomerById(String id) {
        CustomerEntity entity = customerRepositoryJpa.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
        
        return customerResponseMapper.toResponse(entity);
    }

    @Override
    public List<String> findDistinctCountries() {
        return customerRepositoryJpa.findDistinctCountries();
    }

    @Override
    public List<CustomerEntity> findAll() {
        return customerRepositoryJpa.findAll();
    }
}
