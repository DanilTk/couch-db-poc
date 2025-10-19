package pl.home.couchdbpoc.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.home.couchdbpoc.data.Country;
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
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader("X-Country") Country country) {
        CustomerResponse response = customerService.saveCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing customer")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerRequest request,
            @RequestHeader("X-Country") Country country) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer by ID")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable String id,
            @RequestHeader("X-Country") Country country) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer by ID")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable String id,
            @RequestHeader("X-Country") Country country) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

}
