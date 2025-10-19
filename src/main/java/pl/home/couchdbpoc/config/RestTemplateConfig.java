package pl.home.couchdbpoc.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {
	private final CouchDatabaseProperties couchDbProperties;

	@Bean
	public RestTemplate couchRestTemplate(RestTemplateBuilder builder) {
		return builder
			.basicAuthentication(couchDbProperties.getUsername(), couchDbProperties.getPassword())
			.build();
	}

}
