package pl.home.couchdbpoc.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.home.couchdbpoc.data.CustomerDocument;
import pl.home.couchdbpoc.data.CustomerEntity;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.mapstruct.ReportingPolicy.IGNORE;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = IGNORE, injectionStrategy = CONSTRUCTOR)
public interface CustomerDocumentMapper {

	@Mapping(target = "id", source = "entity.id")
	@Mapping(target = "customerId", source = "entity.id")
	CustomerDocument map(CustomerEntity entity);

}
