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

package org.springframework.boot.testcontainers.service.connection;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Passed to {@link ContainerConnectionDetailsFactory} to provide details of the
 * {@link ServiceConnection @ServiceConnection} annotated {@link Container} that provides
 * the service.
 * <p>
 * The {@link ContainerConnectionDetailsFactory} can accept a source based on the
 * container type, connection name and details specified in the annotation. The actual
 * {@link Container} instance cannot be accessed until the related
 * {@link ConnectionDetails} bean is initialized.
 *
 * @param <C> the generic container type
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 * @see ContainerConnectionDetailsFactory
 */
public final class ContainerConnectionSource<C extends Container<?>> implements OriginProvider {

	private static final Log logger = LogFactory.getLog(ContainerConnectionSource.class);

	private final Origin origin;

	private final Class<C> containerType;

	private final String containerName;

	private final Set<Class<?>> connectionDetailsTypes;

	private final String beanNameSuffix;

	private final Supplier<C> containerSupplier;

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType,
			Supplier<String> dockerImageNameSupplier, MergedAnnotation<ServiceConnection> annotation,
			Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.containerName = getConnectionName(origin, annotation.getString("name"), dockerImageNameSupplier);
		this.connectionDetailsTypes = Set.of(annotation.getClassArray("type"));
		this.containerSupplier = containerSupplier;
	}

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType,
			Supplier<String> dockerImageNameSupplier, ServiceConnection annotation, Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.containerName = getConnectionName(origin, annotation.name(), dockerImageNameSupplier);
		this.connectionDetailsTypes = Set.of(annotation.type());
		this.containerSupplier = containerSupplier;
	}

	private static String getConnectionName(Origin origin, String declaredName,
			Supplier<String> dockerImageNameSupplier) {
		if (StringUtils.hasLength(declaredName)) {
			return declaredName;
		}
		String repository = getRepository(dockerImageNameSupplier.get());
		Assert.state(StringUtils.hasText(repository),
				() -> "Unable to determine connection name for %s. Please add a @ServiceConnection 'name' attribute"
					.formatted(origin));
		return repository;
	}

	private static String getRepository(String dockerImageName) {
		if (!StringUtils.hasText(dockerImageName)) {
			return null;
		}
		DockerImageName parsedName = DockerImageName.parse(dockerImageName);
		parsedName.assertValid();
		return parsedName.getRepository();
	}

	/**
	 * Returns if this source is a match for the given parameters.
	 * @param connectionName the required connection name or {@code null}
	 * @param connectionDetailsType the required connection details type
	 * @param containerType the required container type
	 * @return {@code true} if the source is a match
	 */
	boolean matches(String connectionName, Class<?> connectionDetailsType, Class<?> containerType) {
		if (!containerType.isAssignableFrom(this.containerType)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as %s is not an instance of %s".formatted(this,
					this.containerType.getName(), containerType.getName())));
			return false;
		}
		if (StringUtils.hasLength(connectionName) && !connectionName.equalsIgnoreCase(this.containerName)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as connection names '%s' and '%s' do not match"
				.formatted(this, connectionName, this.containerName)));
			return false;
		}
		if (!this.connectionDetailsTypes.isEmpty() && this.connectionDetailsTypes.stream()
			.noneMatch((candidate) -> candidate.isAssignableFrom(connectionDetailsType))) {
			logger.trace(LogMessage.of(() -> "%s not accepted as connection details type %s not in %s".formatted(this,
					connectionDetailsType, this.connectionDetailsTypes)));
			return false;
		}
		logger.trace(LogMessage
			.of(() -> "%s accepted for connection name '%s', connection details type %s, container type %s"
				.formatted(this, connectionName, connectionDetailsType.getName(), containerType.getName())));
		return true;
	}

	String getBeanNameSuffix() {
		return this.beanNameSuffix;
	}

	Supplier<C> getContainerSupplier() {
		return this.containerSupplier;
	}

	@Override
	public Origin getOrigin() {
		return this.origin;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ContainerConnectionSource<?> other = (ContainerConnectionSource<?>) obj;
		boolean result = true;
		result = result && Objects.equals(this.containerType, other.containerType);
		result = result && Objects.equals(this.containerName, other.containerName);
		result = result && Objects.equals(this.connectionDetailsTypes, other.connectionDetailsTypes);
		result = result && Objects.equals(this.beanNameSuffix, other.beanNameSuffix);
		return result;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.containerType, this.containerName, this.connectionDetailsTypes, this.beanNameSuffix);
	}

	@Override
	public String toString() {
		return "@ServiceConnection source for %s".formatted(this.origin);
	}

}
