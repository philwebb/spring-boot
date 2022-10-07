/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AuthorizationAuditListener}.
 */
class AuthorizationAuditListenerTests {

	private final AuthorizationAuditListener listener = new AuthorizationAuditListener();

	private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

	@BeforeEach
	void init() {
		this.listener.setApplicationEventPublisher(this.publisher);
	}

	@Test
	void authorizationDeniedEvent() {
		AuthorizationDecision decision = new AuthorizationDecision(false);
		AuthorizationDeniedEvent<?> authorizationEvent = new AuthorizationDeniedEvent<>(
				() -> new UsernamePasswordAuthenticationToken("spring", "password"), "", decision);
		AuditEvent auditEvent = handleAuthorizationEvent(authorizationEvent).getAuditEvent();
		assertThat(auditEvent.getPrincipal()).isEqualTo("spring");
		assertThat(auditEvent.getType()).isEqualTo(AuthorizationAuditListener.AUTHORIZATION_FAILURE);
		assertThat(auditEvent.getData()).containsEntry("decision", decision.toString());
	}

	private AuditApplicationEvent handleAuthorizationEvent(ApplicationEvent event) {
		ArgumentCaptor<AuditApplicationEvent> eventCaptor = ArgumentCaptor.forClass(AuditApplicationEvent.class);
		this.listener.onApplicationEvent(event);
		then(this.publisher).should().publishEvent(eventCaptor.capture());
		return eventCaptor.getValue();
	}

}
