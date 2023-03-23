/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.readiness;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.devservices.xdockercompose.interop.RunningService;
import org.springframework.boot.docker.compose.service.DockerComposeRunningService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.log.LogMessage;

/**
 * A collection of {@link ServiceReadinessCheck} instances that can be used to
 * {@link #wait() wait} for {@link RunningService services} to be ready.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class ServiceReadinessChecks {

	private static final String DISABLE_LABEL = "org.springframework.boot.readiness-check.disable";

	private static final Log logger = LogFactory.getLog(ServiceReadinessChecks.class);

	private static final Duration SLEEP_BETWEEN_READINESS_TRIES = Duration.ofSeconds(1);

	private final Clock clock;

	private final ReadinessProperties properties;

	private final List<ServiceReadinessCheck> checks;

	public ServiceReadinessChecks(ClassLoader classLoader, Environment environment, Binder binder) {
		this(Clock.systemUTC(), SpringFactoriesLoader.forDefaultResourceLocation(classLoader), classLoader, environment,
				binder);
	}

	ServiceReadinessChecks(Clock clock, SpringFactoriesLoader loader, ClassLoader classLoader, Environment environment,
			Binder binder) {
		ArgumentResolver argumentResolver = ArgumentResolver.of(ClassLoader.class, classLoader)
			.and(Environment.class, environment)
			.and(Binder.class, binder);
		this.clock = clock;
		this.properties = binder.bind(ReadinessProperties.NAME, ReadinessProperties.class)
			.orElseGet(ReadinessProperties::new);
		this.checks = new ArrayList<>(loader.load(ServiceReadinessCheck.class, argumentResolver));
		this.checks.add(new TcpConnectServiceReadinessCheck(this.properties.getTcp()));
	}

	/**
	 * Wait for the given services to be ready.
	 * @param runningServices the services to wait for
	 */
	public void wait(List<DockerComposeRunningService> runningServices) {
		Duration timeout = this.properties.getTimeout();
		Instant start = this.clock.instant();
		while (true) {
			List<ServiceNotReadyException> exceptions = check(runningServices);
			if (exceptions.isEmpty()) {
				return;
			}
			Duration elapsed = Duration.between(start, this.clock.instant());
			if (elapsed.compareTo(timeout) > 0) {
				throw new ReadinessTimeoutException(timeout, exceptions);
			}
			sleep(SLEEP_BETWEEN_READINESS_TRIES);
		}
	}

	private List<ServiceNotReadyException> check(List<DockerComposeRunningService> runningServices) {
		List<ServiceNotReadyException> exceptions = null;
		for (DockerComposeRunningService service : runningServices) {
			if (shouldCheckService(service)) {
				logger.trace(LogMessage.format("Checking readiness of service '%s'", service.name()));
				for (ServiceReadinessCheck check : this.checks) {
					try {
						check.check(service);
						logger.trace(LogMessage.format("Service '%s' is ready", service.name()));
					}
					catch (ServiceNotReadyException ex) {
						logger.trace(LogMessage.format("Service '%s' is not ready", service.name()), ex);
						exceptions = (exceptions != null) ? exceptions : new ArrayList<>();
						exceptions.add(ex);
					}
				}
			}
		}
		return (exceptions != null) ? exceptions : Collections.emptyList();
	}

	private boolean shouldCheckService(DockerComposeRunningService service) {
		return !service.ignore() && !service.labels().containsKey(DISABLE_LABEL);
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
