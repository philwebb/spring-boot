/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.context;

import java.util.UUID;

import com.google.gson.Gson;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Abstract tests for {@link AbstractApplicationContextTester} implementations.
 *
 * @param <T> The tester type
 * @param <C> the context type
 * @param <A> the assertable context type
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public abstract class AbstractApplicationContextTesterTests<T extends AbstractApplicationContextTester<T, C, A>, C extends ConfigurableApplicationContext, A extends AssertProviderApplicationContext<C>> {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ApplicationContextTester contextLoader = new ApplicationContextTester(
			AnnotationConfigApplicationContext::new);

	@Test
	public void runWithSystemPropertiesShouldSetAndRemoveProperties() {
		String key = "test." + UUID.randomUUID().toString();
		assertThat(System.getProperties().containsKey(key)).isFalse();
		this.contextLoader.withSystemProperties(key + "=value").run(context -> {
			assertThat(System.getProperties()).containsEntry(key, "value");
		});
		assertThat(System.getProperties().containsKey(key)).isFalse();
	}

	@Test
	public void runWithSystemPropertiesWhenContextFailsShouldRemoveProperties()
			throws Exception {

	}

	// @Test
	// public void systemPropertyIsRemovedIfContextFailed() {
	// String key = "test." + UUID.randomUUID().toString();
	// assertThat(System.getProperties().containsKey(key)).isFalse();
	// this.contextLoader.withSystemProperty(key, "value")
	// .withConfiguration(ConfigC.class).loadAndFail(e -> {
	// });
	// assertThat(System.getProperties().containsKey(key)).isFalse();
	// }
	//

	@Test
	public void runWithSystemPropertiesShouldRestoreOriginalProperties()
			throws Exception {

	}

	// @Test
	// public void systemPropertyIsRestoredToItsOriginalValue() {
	// String key = "test." + UUID.randomUUID().toString();
	// System.setProperty(key, "value");
	// try {
	// assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
	// this.contextLoader.withSystemProperty(key, "newValue").run(context -> {
	// assertThat(System.getProperties().getProperty(key)).isEqualTo("newValue");
	// });
	// assertThat(System.getProperties().getProperty(key)).isEqualTo("value");
	// }
	// finally {
	// System.clearProperty(key);
	// }
	// }

	@Test
	public void runWithSystemPropertiesWhenValueIsNullShouldRemoveProperty()
			throws Exception {

	}

	@Test
	public void runWithMultiplePropertyValuesShouldAllAllValues() throws Exception {

	}

	// @Test
	// public void envIsAdditive() {
	// this.contextLoader.withPropertyValues("test.foo=1")
	// .withPropertyValues("test.bar=2").run(context -> {
	// ConfigurableEnvironment environment = context
	// .getBean(ConfigurableEnvironment.class);
	// assertThat(environment.getProperty("test.foo", Integer.class))
	// .isEqualTo(1);
	// assertThat(environment.getProperty("test.bar", Integer.class))
	// .isEqualTo(2);
	// });
	// }
	//

	@Test
	public void runWithPropertyValuesWhenHasExistingShouldReplaceValue()
			throws Exception {

	}

	// @Test
	// public void envOverridesExistingKey() {
	// this.contextLoader.withPropertyValues("test.foo=1")
	// .withPropertyValues("test.foo=2")
	// .run(context -> assertThat(context.getBean(ConfigurableEnvironment.class)
	// .getProperty("test.foo", Integer.class)).isEqualTo(2));
	// }
	//

	@Test
	public void runWithConfigurationsShouldRegisterConfigurations() throws Exception {

	}

	// @Test
	// public void configurationIsProcessedInOrder() {
	// this.contextLoader.withUserConfiguration(ConfigA.class, AutoConfigA.class).run(
	// context -> assertThat(context.getBean("a")).isEqualTo("autoconfig-a"));
	// }

	// @Test
	// public void configurationIsProcessedBeforeAutoConfiguration() {
	// this.contextLoader.autoConfig(AutoConfigA.class).register(ConfigA.class).load(
	// context -> assertThat(context.getBean("a")).isEqualTo("autoconfig-a"));
	// }
	//

	@Test
	public void runWithMultipleConfigurationsShouldRegisterAllConfigurations()
			throws Exception {

	}

	// @Test
	// public void configurationIsAdditive() {
	// this.contextLoader.withUserConfiguration(AutoConfigA.class)
	// .withUserConfiguration(AutoConfigB.class).run(context -> {
	// assertThat(context.containsBean("a")).isTrue();
	// assertThat(context.containsBean("b")).isTrue();
	// });
	// }

	// @Test
	// public void autoConfigureFirstIsAppliedProperly() {
	// this.contextLoader.autoConfig(ConfigA.class).autoConfigFirst(AutoConfigA.class)
	// .load(context -> assertThat(context.getBean("a")).isEqualTo("a"));
	// }
	//
	// @Test
	// public void autoConfigureFirstWithSeveralConfigsIsAppliedProperly() {
	// this.contextLoader.autoConfig(ConfigA.class, ConfigB.class)
	// .autoConfigFirst(AutoConfigA.class, AutoConfigB.class).load(context -> {
	// assertThat(context.getBean("a")).isEqualTo("a");
	// assertThat(context.getBean("b")).isEqualTo(1);
	// });
	// }
	//
	// @Test
	// public void autoConfigurationIsAdditive() {
	// this.contextLoader.autoConfig(AutoConfigA.class).autoConfig(AutoConfigB.class)
	// .load(context -> {
	// assertThat(context.containsBean("a")).isTrue();
	// assertThat(context.containsBean("b")).isTrue();
	// });
	// }
	//
	// @Test
	// public void loadAndFailWithExpectedException() {
	// this.contextLoader.withUserConfiguration(ConfigC.class).loadAndFail(
	// BeanCreationException.class, ex -> assertThat(ex.getMessage())
	// .contains("Error creating bean with name 'c'"));
	// }
	//
	// @Test
	// public void loadAndFailWithWrongException() {
	// this.thrown.expect(AssertionError.class);
	// this.thrown.expectMessage("Wrong application context failure exception");
	// this.contextLoader.withUserConfiguration(ConfigC.class)
	// .loadAndFail(IllegalArgumentException.class, ex -> {
	// });
	// }

	@Test
	public void runWithClassLoaderShouldSetClassLoader() throws Exception {
		get().withClassLoader(
				new HidePackagesClassLoader(Gson.class.getPackage().getName()))
				.run((loaded) -> {
					try {
						ClassUtils.forName(Gson.class.getName(), loaded.getClassLoader());
						fail("Should have thrown a ClassNotFoundException");
					}
					catch (ClassNotFoundException e) {
						// expected
					}
				});
	}

	protected abstract AbstractApplicationContextTester<T, C, A> get();

}
