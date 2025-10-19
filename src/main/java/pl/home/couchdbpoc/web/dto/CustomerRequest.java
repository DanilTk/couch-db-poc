package pl.home.couchdbpoc.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String company;

    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String country;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone1;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone2;

    @Email(message = "Email should be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    private LocalDate subscriptionDate;

    @Size(max = 255, message = "Website must not exceed 255 characters")
    private String website;

}
