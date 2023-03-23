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

package org.springframework.boot.devservices.xdockercompose;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.devservices.dockercompose.configuration.DockerComposeDevServiceConfigurationProperties;
import org.springframework.boot.devservices.dockercompose.configuration.StopMode;
import org.springframework.boot.devservices.dockercompose.interop.DefinedService;
import org.springframework.boot.devservices.dockercompose.interop.DockerCompose;
import org.springframework.boot.devservices.dockercompose.interop.DockerComposeFactory;
import org.springframework.boot.devservices.dockercompose.interop.RunningService;
import org.springframework.boot.devservices.dockercompose.readiness.ReadinessTimeoutException;
import org.springframework.boot.devservices.dockercompose.readiness.ServiceNotReadyException;
import org.springframework.boot.devservices.dockercompose.readiness.ServiceReadinessCheck;
import org.springframework.boot.devservices.dockercompose.readiness.ServiceReadinessCheckFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.log.LogMessage;

/**
 * Uses docker compose to provide dev services. The docker compose file (usually named
 * {@code compose.yaml}) can be configured using the
 * {@code spring.dev-services.docker-compose.config-file} property. If this property isn't
 * set, it uses the following files:
 * <ul>
 * <li>{@code compose.yaml}</li>
 * <li>{@code compose.yml}</li>
 * <li>{@code docker-compose.yaml}</li>
 * <li>{@code docker-compose.yml}</li>
 * </ul>
 * If no such file is found, it backs off. If docker compose is not already running, it
 * will be started. This can be disabled by setting
 * {@code spring.dev-services.docker-compose.auto-start} to {@code false}. If docker
 * compose has been started by this provider, docker compose will be stopped afterwards.
 * <p>
 * It uses {@link RunningServiceServiceConnectionProvider extractors} to delegate the work
 * of translating running docker compose services to {@link ConnectionDetails service
 * connections}. Those providers can be registered in {@code spring.factories} under the
 * {@link RunningServiceServiceConnectionProvider} key.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class DockerComposeListener implements ApplicationListener<ApplicationPreparedEvent> {

	private static final List<String> COMPOSE_CONFIG_FILES_SEARCH_ORDER = List.of("compose.yaml", "compose.yml",
			"docker-compose.yaml", "docker-compose.yml");

	private static final Duration SLEEP_BETWEEN_READINESS_TRIES = Duration.ofSeconds(1);

	private static final Log logger = LogFactory.getLog(DockerComposeListener.class);

	private final DockerComposeFactory dockerComposeFactory;

	private final ServiceReadinessCheckFactory serviceReadinessCheckFactory;

	private final Path baseDirectory;

	private final SpringApplicationShutdownHandlers shutdownHandlers;

	private Environment environment;

	private Binder binder;

	private ClassLoader classLoader;

	private List<RunningServiceServiceConnectionProvider> serviceConnectionProviders;

	private boolean dockerComposeNeedsShutdown;

	private DockerComposeDevServiceConfigurationProperties configuration;

	DockerComposeListener(ClassLoader classLoader, Environment environment, DockerComposeFactory dockerComposeFactory,
			ServiceReadinessCheckFactory serviceReadinessCheckFactory,
			List<RunningServiceServiceConnectionProvider> serviceConnectionProviders, Path baseDirectory,
			SpringApplicationShutdownHandlers shutdownHandlers) {
		this.environment = environment;
		this.binder = (environment != null) ? Binder.get(environment) : null;
		this.classLoader = classLoader;
		this.dockerComposeFactory = (dockerComposeFactory != null) ? dockerComposeFactory
				: DockerComposeFactory.createDefault();
		this.serviceReadinessCheckFactory = (serviceReadinessCheckFactory != null) ? serviceReadinessCheckFactory
				: ServiceReadinessCheckFactory.createDefault();
		this.baseDirectory = (baseDirectory != null) ? baseDirectory : Path.of(".");
		this.shutdownHandlers = (shutdownHandlers != null) ? shutdownHandlers : SpringApplication.getShutdownHandlers();
		this.serviceConnectionProviders = serviceConnectionProviders;
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		if (!(event.getApplicationContext() instanceof BeanDefinitionRegistry beanDefinitionRegistry)) {
			return;
		}
		if (this.classLoader == null) {
			this.classLoader = event.getSpringApplication().getClassLoader();
		}
		if (this.environment == null) {
			this.environment = event.getApplicationContext().getEnvironment();
		}
		if (this.binder == null) {
			this.binder = Binder.get(this.environment);
		}
		this.configuration = this.binder.bindOrCreate(DockerComposeDevServiceConfigurationProperties.PREFIX,
				DockerComposeDevServiceConfigurationProperties.class);
		// Test detection and `run-in-tests` check
		if (!shouldRun()) {
			return;
		}
		// Use or find the config yaml
		Path composeYaml = findComposeYaml();
		if (composeYaml == null) {
			// If we don't have one don't start
			return;
		}
		composeYaml = composeYaml.toAbsolutePath();
		logger.info(LogMessage.format("Found docker compose config file %s", composeYaml));

		// Create the docker compose with the yaml and active profiles and host name (if
		// any)
		DockerCompose dockerCompose = this.dockerComposeFactory.create(this.configuration, composeYaml);
		try {

			List<RunningService> runningServices = startComposeIfNeeded(dockerCompose);
			if (runningServices.hasDefinedServices()) {
				// We have nothing
				return;
			}
			// This is a reaction to the services being started
			List<RunningService> nonIgnoredServices = runningServices.stream().filter((s) -> !s.ignore()).toList();
			if (nonIgnoredServices.hasDefinedServices()) {
				return;
			}
			if (this.serviceConnectionProviders == null) {
				this.serviceConnectionProviders = loadServiceProviders(this.classLoader, this.environment, this.binder);
			}
			for (RunningServiceServiceConnectionProvider serviceConnectionProvider : this.serviceConnectionProviders) {
				List<? extends ConnectionDetails> serviceConnections = serviceConnectionProvider
					.provideServiceConnection(runningServices);
				registerServiceConnections(serviceConnections, beanDefinitionRegistry);
			}
		}
		catch (RuntimeException ex) {
			// Shouldn't need if we fix the handler bug
			if (this.dockerComposeNeedsShutdown) {
				shutdownCompose(dockerCompose, this.configuration.getStopMode());
			}
			throw ex;
		}
	}

	private boolean shouldRun() {
		boolean inTest = this.environment
			.containsProperty("org.springframework.boot.test.context.SpringBootTestContextBootstrapper");
		if (!inTest) {
			return true;
		}
		if (!this.configuration.isRunInTests()) {
			logger.debug("Detected a running test suite, disabling...");
			return false;
		}
		return true;
	}

	private void registerServiceConnections(List<? extends ConnectionDetails> serviceConnections,
			BeanDefinitionRegistry beanDefinitionRegistry) {
		for (ConnectionDetails serviceConnection : serviceConnections) {
			RootBeanDefinition definition = new RootBeanDefinition(serviceConnection.getClass());
			definition.setInstanceSupplier((() -> serviceConnection));
			String beanName = ""; // FIXME
			logger.debug(LogMessage.format("Registering bean '%s' of type %s for %s", beanName,
					serviceConnection.getClass().getName(), serviceConnection));
			beanDefinitionRegistry.registerBeanDefinition(beanName, definition);
		}
	}

	private List<RunningService> startComposeIfNeeded(DockerCompose dockerCompose) {
		List<RunningService> runningServices = dockerCompose.listRunningServices();
		List<DefinedService> definedServices = dockerCompose.listDefinedServices();
		// ???
		if (!dockerCompose.isRunning(definedServices, runningServices)) {
			logger.debug("docker compose is not running");
			if (!this.configuration.getLifecycleManagement().isStart()) {
				// We don't want to start, so return an empty list I think
				logger.debug("Not starting docker compose");
				return runningServices;
			}
			// We do want to start
			logger.info("Starting services with docker compose");
			dockerCompose.startServices();
			// Not sure about difference between start and stop and stop mode
			if (this.configuration.getLifecycleManagement().isStop()) {
				logger.debug("Registering shutdown handler to stop docker compose");
				this.dockerComposeNeedsShutdown = true;
				StopMode stopMode = this.configuration.getStopMode();
				this.shutdownHandlers.add(() -> shutdownCompose(dockerCompose, stopMode));
			}
			// We now have more running services
			runningServices = dockerCompose.listRunningServices();
		}
		logger.debug("Checking readiness of services");
		// Wait for them to be ready
		waitForReadiness(runningServices, this.configuration.getReadiness().getTimeout());
		return runningServices;
	}

	// This should be extracted
	private void waitForReadiness(List<RunningService> runningServices, Duration timeout) {
		if (runningServices.hasDefinedServices()) {
			return;
		}
		ServiceReadinessCheck readyCheck = this.serviceReadinessCheckFactory.create(this.configuration);
		long start = System.nanoTime();
		for (RunningService service : runningServices) {
			if (!service.readinessCheck()) {
				continue;
			}
			logger.debug(LogMessage.format("Checking readiness of service '%s'", service.logicalTypeName()));
			ServiceNotReadyException lastException = null;
			while (true) {
				Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
				if (elapsed.compareTo(timeout) > 0) {
					throw new ReadinessTimeoutException(service, timeout, lastException);
				}
				try {
					readyCheck.check(service);
					logger.debug(LogMessage.format("Service '%s' is ready", service.logicalTypeName()));
					break;
				}
				catch (ServiceNotReadyException ex) {
					lastException = ex;
					logger.debug(LogMessage.format("Service '%s' is not ready", service.logicalTypeName()));
					logger.trace("Exception details", ex);
					sleep(SLEEP_BETWEEN_READINESS_TRIES);
				}
			}
		}
	}

	// Stop mode ??
	private void shutdownCompose(DockerCompose dockerCompose, StopMode stopMode) {
		logger.info("Stopping services via docker compose");
		dockerCompose.stopServices(stopMode);
	}

	// This should be extracted
	private Path findComposeYaml() {
		if (this.configuration.getConfigFile() != null) {
			Path file = Path.of(this.configuration.getConfigFile()).toAbsolutePath();
			if (!Files.exists(file)) {
				throw new IllegalStateException("docker compose config file '%s' doesn't exist".formatted(file));
			}
			return file;
		}
		for (String file : COMPOSE_CONFIG_FILES_SEARCH_ORDER) {
			Path path = this.baseDirectory.resolve(file);
			if (Files.exists(path)) {
				return path;
			}
		}
		return null;
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			// Ignore
		}
	}

	private static List<RunningServiceServiceConnectionProvider> loadServiceProviders(ClassLoader classLoader,
			Environment environment, Binder binder) {
		return SpringFactoriesLoader.forDefaultResourceLocation(classLoader)
			.load(RunningServiceServiceConnectionProvider.class,
					ArgumentResolver.of(Environment.class, environment)
						.and(ClassLoader.class, classLoader)
						.and(Binder.class, binder));
	}

}
