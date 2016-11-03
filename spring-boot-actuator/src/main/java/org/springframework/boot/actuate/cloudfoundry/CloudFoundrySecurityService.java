/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.cloudfoundry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.actuate.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Madhura Bhave
 */
public class CloudFoundrySecurityService {

	private final RestTemplate restTemplate;

	private final String cloudControllerUrl;

	private String uaaUrl;

	public CloudFoundrySecurityService(RestTemplateBuilder restTemplateBuilder,
			String cloudControllerUrl) {
		Assert.notNull(restTemplateBuilder, "RestTemplateBuilder must not be null");
		Assert.notNull(cloudControllerUrl, "CloudControllerUrl must not be null");
		this.restTemplate = restTemplateBuilder.build();
		this.cloudControllerUrl = cloudControllerUrl;
	}

	public AccessLevel getAccessLevel(String token, String applicationId)
			throws Exception {
		try {
			URI uri = new URI(this.cloudControllerUrl + "/v2/apps/" + applicationId
					+ "/permissions");
			RequestEntity<?> request = RequestEntity.get(uri)
					.header("Authorization", "bearer " + token).build();
			Map<?, ?> body = this.restTemplate.exchange(request, Map.class).getBody();
			if (Boolean.TRUE.equals(body.get("read_sensitive_data"))) {
				return AccessLevel.FULL;
			}
			return AccessLevel.RESTRICTED;
		}
		catch (HttpClientErrorException ex) {
			if (ex.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
				throw new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED,
						"Access denied");
			}
			throw new CloudFoundryAuthorizationException(Reason.INVALID_TOKEN,
					"Invalid token", ex);
		}
		catch (HttpServerErrorException ex) {
			throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"Cloud controller not reachable");
		}
	}

	public List<String> fetchTokenKeys() {
		try {
			return extractTokenKeys(this.restTemplate
					.getForObject(getUaaUrl() + "/token_keys", Map.class));
		}
		catch (HttpStatusCodeException e) {
			throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
					"UAA not reachable");
		}
	}

	private List<String> extractTokenKeys(Map<?, ?> response) {
		List<String> tokenKeys = new ArrayList<String>();
		List<?> keys = (List<?>) response.get("keys");
		for (Object key : keys) {
			tokenKeys.add((String) ((Map<?, ?>) key).get("value"));
		}
		return tokenKeys;
	}

	public String getUaaUrl() {
		if (this.uaaUrl == null) {
			try {
				Map<?, ?> response = this.restTemplate
						.getForObject(this.cloudControllerUrl + "/info", Map.class);
				this.uaaUrl = (String) response.get("token_endpoint");
			}
			catch (HttpStatusCodeException ex) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Unable to fetch token keys from UAA");
			}
		}
		return this.uaaUrl;
	}

}
