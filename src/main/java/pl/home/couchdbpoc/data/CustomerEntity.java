package pl.home.couchdbpoc.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
@AllArgsConstructor
public class CustomerEntity {

	@Id
	@Column(name = "customer_id", nullable = false)
	private String id;

	@Column(name = "idx")
	private Integer index;

	@Column(name = "first_name")
	private String firstName;

	@Column(name = "last_name")
	private String lastName;

	@Column(name = "company")
	private String company;

	@Column(name = "city")
	private String city;

	@Column(name = "country")
	private String country;

	@Column(name = "phone1")
	private String phone1;

	@Column(name = "phone2")
	private String phone2;

	@Column(name = "email")
	private String email;

	@Column(name = "subscription_date")
	private LocalDate subscriptionDate;

	@Column(name = "website")
	private String website;
}
