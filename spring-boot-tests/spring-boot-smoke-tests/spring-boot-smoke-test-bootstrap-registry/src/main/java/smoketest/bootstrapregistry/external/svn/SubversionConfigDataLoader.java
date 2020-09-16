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

import java.io.IOException;
import java.util.Collections;

import org.springframework.boot.BootstrapRegistry;
import org.springframework.boot.BootstrapRegistry.ApplicationContextPreparedListener;
import org.springframework.boot.BootstrapRegistry.Registration;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataLocationNotFoundException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * {@link ConfigDataLoader} for subversion.
 *
 * @author Phillip Webb
 */
class SubversionConfigDataLoader implements ConfigDataLoader<SubversionConfigDataLocation> {

	private static final ApplicationContextPreparedListener applicationContextPreparedListener = SubversionConfigDataLoader::applicationContextPrepared;

	SubversionConfigDataLoader(BootstrapRegistry bootstrapRegistry) {
		bootstrapRegistry.registerIfAbsent(SubversionClient.class, this::createSubversionClient);
		bootstrapRegistry.addApplicationContextPreparedListener(applicationContextPreparedListener);
	}

	private SubversionClient createSubversionClient(BootstrapRegistry registry) {
		return new SubversionClient(registry.get(SubversionServerCertificate.class));
	}

	@Override
	public ConfigData load(ConfigDataLoaderContext context, SubversionConfigDataLocation location)
			throws IOException, ConfigDataLocationNotFoundException {
		context.getBootstrapRegistry().registerIfAbsent(SubversionServerCertificate.class,
				Registration.of(location.getServerCertificate()));
		SubversionClient client = context.getBootstrapRegistry().get(SubversionClient.class);
		String loaded = client.load(location.getLocation());
		PropertySource<?> propertySource = new MapPropertySource("svn", Collections.singletonMap("svn", loaded));
		return new ConfigData(Collections.singleton(propertySource));
	}

	private static void applicationContextPrepared(BootstrapRegistry bootstrapRegistry,
			ConfigurableApplicationContext applicationContext) {
		applicationContext.getBeanFactory().registerSingleton("subversionClient",
				bootstrapRegistry.get(SubversionClient.class));
	}

}
