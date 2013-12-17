/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.properties;

import java.net.InetAddress;

import javax.validation.constraints.NotNull;

import org.springframework.boot.autoconfigure.security.SecurityPrequisite;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Properties for the management server (e.g. port and path settings).
 * 
 * @author Dave Syer
 * @see ServerProperties
 */
@ConfigurationProperties(name = "management", ignoreUnknownFields = false)
public class ManagementServerProperties implements SecurityPrequisite {

	private static final boolean SPRING_SECURITY_PRESENT = ClassUtils.isPresent(
			"org.springframework.security.core.Authentication", null);

	private Integer port;

	private InetAddress address;

	@NotNull
	private String contextPath = "";

	private Security security;

	public ManagementServerProperties() {
		if (SPRING_SECURITY_PRESENT) {
			this.security = new Security();
		}
	}

	/**
	 * Returns the management port or {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used.
	 * @see #setPort(Integer)
	 */
	public Integer getPort() {
		return this.port;
	}

	/**
	 * Sets the port of the management server, use {@code null} if the
	 * {@link ServerProperties#getPort() server port} should be used. To disable use 0.
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

	/**
	 * Return the {@link InetAddress} of the management server. This address is only used
	 * when running on a different port to the standard server.
	 */
	public InetAddress getAddress() {
		return this.address;
	}

	/**
	 * Set the {@link InetAddress} of the management server. This address is only used
	 * when running on a different port to the standard server.
	 */
	public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
	 * Return the context path for the management server. When running on the same port as
	 * the standard server the context path is relative to the {@link DispatcherServlet}
	 * mapping.
	 */
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * Set the context path for the management server. When running on the same port as
	 * the standard server the context path is relative to the {@link DispatcherServlet}
	 * mapping.
	 */
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Return the security configuration.
	 */
	public Security getSecurity() {
		Assert.state(this.security != null, "Security Security is required to configure"
				+ " management security settings");
		return this.security;
	}

	/**
	 * Management server security configuration.
	 */
	public static class Security {

		private boolean enabled = true;

		private String role = "ADMIN";

		private SessionCreationPolicy sessions = SessionCreationPolicy.STATELESS;

		/**
		 * Return the {@link SessionCreationPolicy} for the management server.
		 */
		public SessionCreationPolicy getSessions() {
			return this.sessions;
		}

		/**
		 * Set the {@link SessionCreationPolicy} for the management server.
		 */
		public void setSessions(SessionCreationPolicy sessions) {
			this.sessions = sessions;
		}

		/**
		 * Return the security role required to access secure URLs on the management
		 * server.
		 */
		public String getRole() {
			return this.role;
		}

		/**
		 * Set the security role required to access secure URLs on the management server.
		 */
		public void setRole(String role) {
			this.role = role;
		}

		/**
		 * Return if the security is enable for the management server.
		 */
		public boolean isEnabled() {
			return this.enabled;
		}

		/**
		 * Set if the security is enable for the management server.
		 */
		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}
