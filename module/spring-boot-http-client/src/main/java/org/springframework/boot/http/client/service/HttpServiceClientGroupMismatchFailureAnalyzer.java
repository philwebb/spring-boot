/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client.service;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link HttpServiceClientGroupMismatchException}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientGroupMismatchFailureAnalyzer
		extends AbstractFailureAnalyzer<HttpServiceClientGroupMismatchException> {

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, HttpServiceClientGroupMismatchException cause) {
		return new FailureAnalysis(getMessage(cause), getAction(cause), cause);
	}

	private String getMessage(HttpServiceClientGroupMismatchException cause) {
		StringBuilder message = new StringBuilder();
		message
			.append(String.format("The @HttpServiceClient annotated interface '%s'", cause.getServiceType().getName()));
		message.append(String.format("%nhas been registered to an incorrect group:"));
		message.append(String.format("%n"));
		message.append(String.format("%n    Requested: '%s' (from @HttpServiceClient)", cause.getRequestedGroup()));
		message.append(String.format("%n    Actual: '%s'", cause.getActualGroup()));
		message.append(String.format("%n"));
		message.append(String
			.format("%nEnsure that the interface has not be direcly registered by an @ImportHttpServices annotation"));
		message.append(String.format("%nand has not been imported by an AbstractHttpServiceRegistrar."));
		return message.toString();
	}

	private String getAction(HttpServiceClientGroupMismatchException cause) {
		StringBuilder action = new StringBuilder();
		action.append(String.format("Update your code to ensure '%s' is registered to the correct group by",
				cause.getServiceType().getName()));
		action.append(String.format(
				"%neither removing it from any direct HTTP Service registration, or deleting the @HttpServiceClient annotation."));
		return action.toString();
	}

}
