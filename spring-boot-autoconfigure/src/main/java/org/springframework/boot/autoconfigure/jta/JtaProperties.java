package org.springframework.boot.autoconfigure.jta;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * External configuration properties for a {@link JtaTransactionManager} created by
 * Spring. All {@literal spring.jta.} properties are also applied to the appropriate
 * vendor specific configuration.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = JtaProperties.PREFIX, ignoreUnknownFields = true)
public class JtaProperties {

	public static final String PREFIX = "spring.jta";

	private String logDir;

	public void setLogDir(String logDir) {
		this.logDir = logDir;
	}

	public String getLogDir() {
		return this.logDir;
	}

}
