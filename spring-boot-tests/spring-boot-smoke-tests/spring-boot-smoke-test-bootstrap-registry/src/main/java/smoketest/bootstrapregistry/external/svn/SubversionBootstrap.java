/*
 * Copyright 2012-2020 the original author or authors.
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

package smoketest.bootstrapregistry.external.svn;

import java.util.function.Function;

import org.springframework.boot.BootstrapRegistry.Registration;
import org.springframework.boot.Bootstrapper;

/**
 * Allows the user to register a {@link Bootstrapper} with a custom
 * {@link SubversionClient}.
 *
 * @author Phillip Webb
 */
public final class SubversionBootstrap {

	private SubversionBootstrap() {
	}

	/**
	 * Return a {@link Bootstrapper} for the given client factory.
	 * @param clientFactory the client factory
	 * @return a {@link Bootstrapper} instance
	 */
	public static Bootstrapper withCustomClient(Function<SubversionServerCertificate, SubversionClient> clientFactory) {
		return (registry) -> {
			registry.register(SubversionClient.class, Registration.suppliedBy(() -> {
				SubversionServerCertificate certificate = registry.get(SubversionServerCertificate.class);
				return clientFactory.apply(certificate);
			}));
		};
	}

}
