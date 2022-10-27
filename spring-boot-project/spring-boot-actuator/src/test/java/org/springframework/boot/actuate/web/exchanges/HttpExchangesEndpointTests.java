/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.security.Principal;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangesEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class HttpExchangesEndpointTests {

	private static final Supplier<Principal> NO_PRINCIPAL = () -> null;

	private static final Supplier<String> NO_SESSION_ID = () -> null;

	@Test
	void httpExchanges() {
		HttpExchangesRepository repository = new InMemoryHttpExchangesRepository();
		repository.add(HttpExchange.start(createRequest("GET")).finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID));
		List<HttpExchange> httpExchanges = new HttpExchangesEndpoint(repository).httpExchanges().getTraces();
		assertThat(httpExchanges).hasSize(1);
		HttpExchange trace = httpExchanges.get(0);
		assertThat(trace.getRequest().getMethod()).isEqualTo("GET");
	}

	private SourceHttpRequest createRequest(String method) {
		SourceHttpRequest request = mock(SourceHttpRequest.class);
		given(request.getMethod()).willReturn(method);
		return request;
	}

	private SourceHttpResponse createResponse() {
		return mock(SourceHttpResponse.class);
	}

}
