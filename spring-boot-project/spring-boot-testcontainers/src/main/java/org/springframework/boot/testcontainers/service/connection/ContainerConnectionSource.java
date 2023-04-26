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

import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Passed to {@link ContainerConnectionDetailsFactory} to provide details of the
 * {@link ServiceConnection @ServiceConnection} annotated {@link Container} that provides
 * the service.
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

	private final String connectionName;

	private final Set<Class<?>> connectionDetailsTypes;

	private final String beanNameSuffix;

	private final Supplier<C> containerSupplier;

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType,
			Supplier<String> containerName, MergedAnnotation<ServiceConnection> annotation,
			Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.connectionName = getConnectionName(annotation.getString("name"), containerName);
		this.connectionDetailsTypes = Set.of(annotation.getClassArray("type"));
		this.containerSupplier = containerSupplier;
	}

	ContainerConnectionSource(String beanNameSuffix, Origin origin, Class<C> containerType,
			Supplier<String> containerName, ServiceConnection annotation, Supplier<C> containerSupplier) {
		this.beanNameSuffix = beanNameSuffix;
		this.origin = origin;
		this.containerType = containerType;
		this.connectionName = getConnectionName(annotation.name(), containerName);
		this.connectionDetailsTypes = Set.of(annotation.type());
		this.containerSupplier = containerSupplier;
	}

	private static String getConnectionName(String declaredName, Supplier<String> containerName) {
		if (StringUtils.hasLength(declaredName)) {
			return declaredName;
		}
		DockerImageName imageName = DockerImageName.parse(containerName.get());
		imageName.assertValid();
		return imageName.getRepository();
	}

	boolean accepts(String connectionName, Class<?> connectionDetailsType, Class<?> containerType) {
		if (!containerType.isAssignableFrom(this.containerType)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as %s is not an instance of %s".formatted(this,
					this.containerType.getName(), containerType.getName())));
			return false;
		}
		if (StringUtils.hasLength(connectionName) && !connectionName.equalsIgnoreCase(this.connectionName)) {
			logger.trace(LogMessage.of(() -> "%s not accepted as connection names '%s' and '%s' do not match"
				.formatted(this, connectionName, this.connectionName)));
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
	public String toString() {
		return "@ServiceConnection source for %s".formatted(this.origin);
	}

}
