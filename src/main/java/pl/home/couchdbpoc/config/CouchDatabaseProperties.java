package pl.home.couchdbpoc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@Getter
@Setter
@ConfigurationProperties(prefix = "couchdb")
public class CouchDatabaseProperties {
	private String protocol;
	private String host;
	private int port;
	private String username;
	private String password;
	private  String url;

	public String getUrlWithAuth() {
		URI uri = URI.create(url);
		String auth = username + ":" + password;
		return uri.getScheme() + "://" + auth + "@" + uri.getHost() + ":" + uri.getPort();
	}
}
