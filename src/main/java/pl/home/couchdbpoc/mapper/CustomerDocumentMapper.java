package pl.home.couchdbpoc.mapper;

import org.springframework.stereotype.Component;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.web.dto.CustomerResponse;

@Component
public class CustomerDocumentMapper {

	public CustomerDocument map(CustomerEntity entity) {
		if (entity == null) {
			return null;
		}

		CustomerDocument document = new CustomerDocument();
		document.setId(entity.getId());
		document.setCustomerId(entity.getId());
		document.setIdx(entity.getIndex());
		document.setFirstName(entity.getFirstName());
		document.setLastName(entity.getLastName());
		document.setCompany(entity.getCompany());
		document.setCity(entity.getCity());
		document.setCountry(entity.getCountry());
		document.setPhone1(entity.getPhone1());
		document.setPhone2(entity.getPhone2());
		document.setEmail(entity.getEmail());
		document.setSubscriptionDate(entity.getSubscriptionDate());
		document.setWebsite(entity.getWebsite());

		return document;
	}

	public CustomerDocument map(CustomerResponse response) {
		if (response == null) {
			return null;
		}

		CustomerDocument document = new CustomerDocument();
		document.setId(response.getId());
		document.setCustomerId(response.getId());
		document.setIdx(response.getIndex());
		document.setFirstName(response.getFirstName());
		document.setLastName(response.getLastName());
		document.setCompany(response.getCompany());
		document.setCity(response.getCity());
		document.setCountry(response.getCountry());
		document.setPhone1(response.getPhone1());
		document.setPhone2(response.getPhone2());
		document.setEmail(response.getEmail());
		document.setSubscriptionDate(response.getSubscriptionDate());
		document.setWebsite(response.getWebsite());

		return document;
	}

}