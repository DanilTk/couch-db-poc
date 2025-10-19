package pl.home.couchdbpoc.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

	private String id;
	private Integer index;
	private String firstName;
	private String lastName;
	private String company;
	private String city;
	private String country;
	private String phone1;
	private String phone2;
	private String email;
	private LocalDate subscriptionDate;
	private String website;

}
