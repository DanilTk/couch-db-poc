package pl.home.couchdbpoc.data;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CustomerDocument {

	@SerializedName("_id")
	private String id;

	@SerializedName("_rev")
	private String rev;

	private String customerId;
	private Integer idx;
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
