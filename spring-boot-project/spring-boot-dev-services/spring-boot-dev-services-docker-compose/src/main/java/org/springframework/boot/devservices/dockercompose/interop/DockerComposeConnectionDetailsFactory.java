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

package org.springframework.boot.devservices.dockercompose.interop;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginProvider;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author pwebb
 */
public abstract class DockerComposeConnectionDetailsFactory<D extends ConnectionDetails>
		implements ConnectionDetailsFactory<RunningService, D> {

	private final String name;

	private final ClassLoader classLoader;

	private final String requiredClassName;

	protected DockerComposeConnectionDetailsFactory(String name) {
		this(name, null, null);
	}

	protected DockerComposeConnectionDetailsFactory(String name, ClassLoader classLoader, String requiredClassName) {
		this.name = name;
		this.classLoader = classLoader;
		this.requiredClassName = requiredClassName;
	}

	@Override
	public final D getConnectionDetails(RunningService source) {
		return (!accept(source)) ? null : getDockerComposeConnectionDetails(source);
	}

	private boolean accept(RunningService source) {
		return hasRequiredClass() && this.name.equals(source.image().image());
	}

	private boolean hasRequiredClass() {
		return this.requiredClassName == null || ClassUtils.isPresent(this.requiredClassName, this.classLoader);
	}

	/**
	 * Get the {@link ConnectionDetails} from the given {@link RunningService}
	 * {@code source}. May return {@code null} if no connection can be created. Result
	 * types should consider extending {@link DockerComposeConnectionDetails}.
	 * @param source the source
	 * @return the service connection or {@code null}.
	 */
	protected abstract D getDockerComposeConnectionDetails(RunningService source);

	/**
	 * Convenient base class for {@link ConnectionDetails} results that are backed by a
	 * {@link RunningService}.
	 */
	protected static class DockerComposeConnectionDetails implements ConnectionDetails, OriginProvider {

		private final Origin origin;

		/**
		 * Create a new {@link DockerComposeConnectionDetails} instance.
		 * @param source the source {@link RunningService}
		 */
		protected DockerComposeConnectionDetails(RunningService source) {
			Assert.notNull(source, "Source must not be null");
			this.origin = source.origin();
		}

		@Override
		public Origin getOrigin() {
			return this.origin;
		}

	}

}
