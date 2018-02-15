/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ConfigurationProperties} annotated beans.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class ConfigurationPropertiesTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void cleanup() {
		this.context.close();
		System.clearProperty("name");
		System.clearProperty("nested.name");
		System.clearProperty("nested_name");
	}

	@Test
	public void loadShouldBind() {
		load(BasicConfiguration.class, "name=foo");
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.containsBean(BasicProperties.class.getName())).isTrue();
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBind() {
		System.setProperty("name", "foo");
		load(BasicConfiguration.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void loadShouldBindNested() {
		System.setProperty("name", "foo");
		System.setProperty("nested.name", "bar");
		load(NestedConfiguration.class);
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void loadWhenHasIgnoreUnknownFieldsFalseAndNoUnknownFieldsShouldBind() {
		removeSystemProperties();
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo");
		IgnoreUnknownFieldsFalseProperties bean = this.context
				.getBean(IgnoreUnknownFieldsFalseProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenHasIgnoreUnknownFieldsFalseAndUnknownFieldsShouldFail() {
		fail();
	}

	@Test
	public void loadWhenHasPrefixShouldBind() {
		load(PrefixConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchShouldThrow() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedJsr303Configuration.class, "name:foo");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchAndNotValidatedAnnotationShouldBind() {
		load(NonValidatedJsr303Configuration.class, "name:foo");
		NonValidatedJsr303Properties bean = this.context
				.getBean(NonValidatedJsr303Properties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	private void load(Class<?> configuration, String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				inlinedProperties);
		this.context.refresh();
	}

	/**
	 * Strict tests need a known set of properties so we remove system items which may be
	 * environment specific.
	 */
	private void removeSystemProperties() {
		MutablePropertySources sources = this.context.getEnvironment()
				.getPropertySources();
		sources.remove("systemProperties");
		sources.remove("systemEnvironment");
	}

	@Configuration
	@EnableConfigurationProperties(BasicProperties.class)
	protected static class BasicConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(NestedProperties.class)
	protected static class NestedConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(IgnoreUnknownFieldsFalseProperties.class)
	protected static class IgnoreUnknownFieldsFalseConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(PrefixProperties.class)
	protected static class PrefixConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(ValidatedJsr303Properties.class)
	protected static class ValidatedJsr303Configuration {

	}

	@Configuration
	@EnableConfigurationProperties(NonValidatedJsr303Properties.class)
	protected static class NonValidatedJsr303Configuration {

	}

	@ConfigurationProperties
	protected static class BasicProperties {

		private String name;

		private int[] array;

		private List<Integer> list = new ArrayList<>();

		// No getter - you should be able to bind to a write-only bean

		public void setName(String name) {
			this.name = name;
		}

		public void setArray(int... values) {
			this.array = values;
		}

		public int[] getArray() {
			return this.array;
		}

		public List<Integer> getList() {
			return this.list;
		}

		public void setList(List<Integer> list) {
			this.list = list;
		}

	}

	@ConfigurationProperties
	protected static class NestedProperties {

		private String name;

		private final Nested nested = new Nested();

		public void setName(String name) {
			this.name = name;
		}

		public Nested getNested() {
			return this.nested;
		}

		protected static class Nested {

			private String name;

			public void setName(String name) {
				this.name = name;
			}

		}

	}

	@ConfigurationProperties(ignoreUnknownFields = false)
	protected static class IgnoreUnknownFieldsFalseProperties extends BasicProperties {

	}

	@ConfigurationProperties(prefix = "spring.foo")
	protected static class PrefixProperties extends BasicProperties {

	}

	protected static class Jsr303Properties extends BasicProperties {

		@NotNull
		private String description;

		public String getDescription() {
			return this.description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	@ConfigurationProperties
	@Validated
	protected static class ValidatedJsr303Properties extends Jsr303Properties {

	}

	@ConfigurationProperties
	protected static class NonValidatedJsr303Properties extends Jsr303Properties {

	}

}
