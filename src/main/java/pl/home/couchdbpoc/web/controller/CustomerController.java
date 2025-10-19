package pl.home.couchdbpoc.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.home.couchdbpoc.web.dto.CustomerRequest;
import pl.home.couchdbpoc.web.dto.CustomerResponse;
import pl.home.couchdbpoc.web.service.CustomerService;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing customers")
public class CustomerController {

	private final CustomerService customerService;

	@PostMapping
	@Operation(summary = "Create a new customer")
	public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerRequest request) {
		CustomerResponse response = customerService.saveCustomer(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing customer")
	public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable String id,
														   @Valid @RequestBody CustomerRequest request) {
		CustomerResponse response = customerService.updateCustomer(id, request);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a customer by ID")
	public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
		customerService.deleteCustomer(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a customer by ID")
	public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable String id) {
		CustomerResponse response = customerService.getCustomerById(id);
		return ResponseEntity.ok(response);
	}

}
