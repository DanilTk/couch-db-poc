package pl.home.couchdbpoc.web.mapper;

import org.springframework.stereotype.Component;
import pl.home.couchdbpoc.data.CustomerEntity;
import pl.home.couchdbpoc.web.dto.CustomerResponse;

@Component
public class CustomerResponseMapper {

    public CustomerResponse toResponse(CustomerEntity entity) {
        if (entity == null) {
            return null;
        }

        return CustomerResponse.builder()
                .id(entity.getId())
                .index(entity.getIndex())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .company(entity.getCompany())
                .city(entity.getCity())
                .country(entity.getCountry())
                .phone1(entity.getPhone1())
                .phone2(entity.getPhone2())
                .email(entity.getEmail())
                .subscriptionDate(entity.getSubscriptionDate())
                .website(entity.getWebsite())
                .build();
    }

}
