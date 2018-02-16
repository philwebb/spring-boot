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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ConfigurationProperties} annotated beans. Covers
 * {@link EnableConfigurationProperties},
 * {@link ConfigurationPropertiesBindingPostProcessorRegistrar} and
 * {@link ConfigurationPropertiesBindingPostProcessor}.
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
	public void loadShouldBindNested() {
		load(NestedConfiguration.class, "name=foo", "nested.name=bar");
		assertThat(this.context.getBeanNamesForType(NestedProperties.class)).hasSize(1);
		assertThat(this.context.getBean(NestedProperties.class).name).isEqualTo("foo");
		assertThat(this.context.getBean(NestedProperties.class).nested.name)
				.isEqualTo("bar");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBind() {
		System.setProperty("name", "foo");
		load(BasicConfiguration.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicProperties.class).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenUsingSystemPropertiesShouldBindNested() {
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
		removeSystemProperties();
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(IgnoreUnknownFieldsFalseConfiguration.class, "name=foo", "bar=baz");
	}

	@Test
	public void loadWhenHasPrefixShouldBind() {
		load(PrefixConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenPropertiesHaveAnnotationOnBaseClassShouldBind() {
		load(AnnotationOnBaseClassConfiguration.class, "name=foo");
		AnnotationOnBaseClassProperties bean = this.context
				.getBean(AnnotationOnBaseClassProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenBindingArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "array=1,2,3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.array).containsExactly(1, 2, 3);
	}

	@Test
	public void loadWhenBindingArrayFromYamlArrayShouldBind() {
		load(BasicConfiguration.class, "name=foo", "list[0]=1", "list[1]=2", "list[2]=3");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.list).containsExactly(1, 2, 3);
	}

	@Test
	public void loadWhenBindingOver256ElementsShouldBind() {
		List<String> pairs = new ArrayList<>();
		pairs.add("name:foo");
		for (int i = 0; i < 1000; i++) {
			pairs.add("list[" + i + "]:" + i);
		}
		load(BasicConfiguration.class, pairs.toArray(new String[] {}));
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.list).hasSize(1000);
	}

	@Test
	public void loadWhenBindingWithoutAndAnnotationShouldFail() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("No ConfigurationProperties annotation found");
		load(WithoutAndAnnotationConfiguration.class, "name:foo");
	}

	@Test
	public void loadWhenBindingWithoutAnnotationValueShouldBind() {
		load(WithoutAnnotationValueConfiguration.class, "name=foo");
		WithoutAnnotationValueProperties bean = this.context
				.getBean(WithoutAnnotationValueProperties.class);
		assertThat(bean.name).isEqualTo("foo");
	}

	@Test
	public void loadWhenBindingWithDefaultsInXmlShouldBind() {
		load(new Class<?>[] { BasicConfiguration.class,
				DefaultsInXmlConfiguration.class });
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingWithDefaultsInJavaConfigurationShouldBind() {
		load(DefaultsInJavaConfiguration.class);
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingTwoBeansShouldBind() {
		load(new Class<?>[] { WithoutAnnotationValueConfiguration.class,
				BasicConfiguration.class });
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(WithoutAnnotationValueProperties.class))
				.isNotNull();
	}

	@Test
	public void loadWhenBindingWithParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class,
				"name=parent");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(new Class[] { BasicConfiguration.class, BasicPropertiesConsumer.class },
				"name=child");
		assertThat(this.context.getBean(BasicProperties.class)).isNotNull();
		assertThat(parent.getBean(BasicProperties.class)).isNotNull();
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName())
				.isEqualTo("parent");
		parent.close();
	}

	@Test
	public void loadWhenBindingOnlyParentContextShouldBind() {
		AnnotationConfigApplicationContext parent = load(BasicConfiguration.class,
				"name=foo");
		this.context = new AnnotationConfigApplicationContext();
		this.context.setParent(parent);
		load(BasicPropertiesConsumer.class);
		assertThat(this.context.getBeanNamesForType(BasicProperties.class)).isEmpty();
		assertThat(parent.getBeanNamesForType(BasicProperties.class)).hasSize(1);
		assertThat(this.context.getBean(BasicPropertiesConsumer.class).getName())
				.isEqualTo("foo");
	}

	@Test
	public void loadWhenPrefixedPropertiesDecalredAsBeanShouldBind() {
		load(PrefixPropertiesDecalredAsBeanConfiguration.class, "spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenPrefixedPropertiesDecalredAsAnnotationValueShouldBind() {
		load(PrefixPropertiesDecalredAsAnnotationValueConfiguration.class,
				"spring.foo.name=foo");
		PrefixProperties bean = this.context.getBean(
				"spring.foo-" + PrefixProperties.class.getName(), PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadWhenMultiplePrefixedPropertiesDecalredAsAnnotationValueShouldBind() {
		load(MultiplePrefixPropertiesDecalredAsAnnotationValueConfiguration.class,
				"spring.foo.name=foo", "spring.bar.name=bar");
		PrefixProperties bean1 = this.context.getBean(PrefixProperties.class);
		AnotherPrefixProperties bean2 = this.context
				.getBean(AnotherPrefixProperties.class);
		assertThat(((BasicProperties) bean1).name).isEqualTo("foo");
		assertThat(((BasicProperties) bean2).name).isEqualTo("bar");
	}

	@Test
	public void loadWhenBindingToMapKeyWithPeriodShouldBind() {
		load(MapProperties.class, "mymap.key1.key2:value12", "mymap.key3:value3");
		MapProperties bean = this.context.getBean(MapProperties.class);
		assertThat(bean.mymap).containsOnly(entry("key3", "value3"),
				entry("key1.key2", "value12"));
	}

	@Test
	public void loadWhenPrefixedPropertiesAreReplacedOnBeanMethodShouldBind() {
		load(PrefixedPropertiesReplacedOnBeanMethodConfiguration.class,
				"external.name=bar", "spam.name=foo");
		PrefixProperties bean = this.context.getBean(PrefixProperties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	public void loadShouldBindToJavaTimeDuration() {
		load(BasicConfiguration.class, "duration=PT1M");
		BasicProperties bean = this.context.getBean(BasicProperties.class);
		assertThat(bean.getDuration().getSeconds()).isEqualTo(60);
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchShouldFail() {
		this.thrown.expectCause(Matchers.instanceOf(BindException.class));
		load(ValidatedJsr303Configuration.class, "name=foo");
	}

	@Test
	public void loadWhenJsr303ConstraintMatchesShouldBind() {
		load(ValidatedJsr303Configuration.class, "description=foo");
		ValidatedJsr303Properties bean = this.context
				.getBean(ValidatedJsr303Properties.class);
		assertThat(bean.getDescription()).isEqualTo("foo");
	}

	@Test
	public void loadWhenJsr303ConstraintDoesNotMatchAndNotValidatedAnnotationShouldBind() {
		load(NonValidatedJsr303Configuration.class, "name=foo");
		NonValidatedJsr303Properties bean = this.context
				.getBean(NonValidatedJsr303Properties.class);
		assertThat(((BasicProperties) bean).name).isEqualTo("foo");
	}

	@Test
	@Ignore
	public void loadWhenHasConfigurationPropertiesValidatorShouldApplyValidator() {
		fail(); // FIXME
	}

	@Ignore
	@Test
	public void loadWhenConfigurationPropertiesIsAlsoValidatorShouldApplyValidator() {
		fail(); // FIXME
	}

	@Ignore
	@Test
	public void loadWhenOutterClassIsValidatedAndNestedClassIsNotShouldValidateNested() {
		fail(); // FIXME
	}

	@Test
	public void loadWhenBindingToValidatedImplementationOfInterfaceShouldBind() {
		load(ValidatedImplementationConfiguration.class, "test.foo=bar");
		ValidatedImplementationProperties bean = this.context
				.getBean(ValidatedImplementationProperties.class);
		assertThat(bean.getFoo()).isEqualTo("bar");
	}

	@Test
	public void bindWithValueDefault() {
		load(WithPropertyPlaceholderValueConfiguration.class, "default.value=foo");
		WithPropertyPlaceholderValueProperties bean = this.context
				.getBean(WithPropertyPlaceholderValueProperties.class);
		assertThat(bean.getValue()).isEqualTo("foo");
	}

	@Test
	public void loadWhenHasPostConstructShouldTriggerPostConstructWithBoundBean() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("bar", "foo");
		this.context.setEnvironment(environment);
		this.context.register(WithPostConstructConfiguration.class);
		this.context.refresh();
		WithPostConstructConfiguration bean = this.context
				.getBean(WithPostConstructConfiguration.class);
		assertThat(bean.initialized).isTrue();
	}

	@Test
	public void loadShouldNotInitializeFactoryBeans() {
		WithFactoryBeanConfiguration.factoryBeanInitialized = false;
		this.context = new AnnotationConfigApplicationContext() {

			@Override
			protected void onRefresh() throws BeansException {
				assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized)
						.as("Initialized too early").isFalse();
				super.onRefresh();
			}

		};
		this.context.register(WithFactoryBeanConfiguration.class);
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(FactoryBeanTester.class);
		beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		this.context.registerBeanDefinition("test", beanDefinition);
		this.context.refresh();
		assertThat(WithFactoryBeanConfiguration.factoryBeanInitialized)
				.as("Not Initialized").isTrue();
	}

	private AnnotationConfigApplicationContext load(Class<?> configuration,
			String... inlinedProperties) {
		return load(new Class<?>[] { configuration }, inlinedProperties);
	}

	private AnnotationConfigApplicationContext load(Class<?>[] configuration,
			String... inlinedProperties) {
		this.context.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				inlinedProperties);
		this.context.refresh();
		return this.context;
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

	@Configuration
	@EnableConfigurationProperties(AnnotationOnBaseClassProperties.class)
	protected static class AnnotationOnBaseClassConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(WithoutAndAnnotationConfiguration.class)
	protected static class WithoutAndAnnotationConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties(WithoutAnnotationValueProperties.class)
	protected static class WithoutAnnotationValueConfiguration {

	}

	@Configuration
	@ImportResource("org/springframework/boot/context/properties/testProperties.xml")
	protected static class DefaultsInXmlConfiguration {

	}

	@Configuration
	protected static class DefaultsInJavaConfiguration {

		@Bean
		public BasicProperties basicProperties() {
			BasicProperties test = new BasicProperties();
			test.setName("bar");
			return test;
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class PrefixPropertiesDecalredAsBeanConfiguration {

		@Bean
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties(PrefixProperties.class)
	public static class PrefixPropertiesDecalredAsAnnotationValueConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties({ PrefixProperties.class,
			AnotherPrefixProperties.class })
	public static class MultiplePrefixPropertiesDecalredAsAnnotationValueConfiguration {

	}

	@Configuration
	@EnableConfigurationProperties
	public static class PrefixedPropertiesReplacedOnBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spam")
		public PrefixProperties prefixProperties() {
			return new PrefixProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class ValidatedImplementationConfiguration {

		@Bean
		public ValidatedImplementationProperties testProperties() {
			return new ValidatedImplementationProperties();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	@ConfigurationProperties
	public static class WithPostConstructConfiguration {

		private String bar;

		private boolean initialized;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		@PostConstruct
		public void init() {
			assertThat(this.bar).isNotNull();
			this.initialized = true;
		}

	}

	@Configuration
	@EnableConfigurationProperties(WithPropertyPlaceholderValueProperties.class)
	public static class WithPropertyPlaceholderValueConfiguration {

		@Bean
		public static PropertySourcesPlaceholderConfigurer configurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	@EnableConfigurationProperties
	public static class WithFactoryBeanConfiguration {

		public static boolean factoryBeanInitialized;

	}

	// Must be a raw type
	@SuppressWarnings("rawtypes")
	static class FactoryBeanTester implements FactoryBean, InitializingBean {

		@Override
		public Object getObject() {
			return Object.class;
		}

		@Override
		public Class<?> getObjectType() {
			return null;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}

		@Override
		public void afterPropertiesSet() {
			WithFactoryBeanConfiguration.factoryBeanInitialized = true;
		}

	}

	@ConfigurationProperties
	protected static class BasicProperties {

		private String name;

		private int[] array;

		private List<Integer> list = new ArrayList<>();

		private Duration duration;

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

		public Duration getDuration() {
			return this.duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
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

	@ConfigurationProperties(prefix = "spring.bar")
	protected static class AnotherPrefixProperties extends BasicProperties {

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

	protected static class AnnotationOnBaseClassProperties extends BasicProperties {

	}

	@ConfigurationProperties
	protected static class WithoutAnnotationValueProperties {

		private String name;

		public void setName(String name) {
			this.name = name;
		}

		// No getter - you should be able to bind to a write-only bean

	}

	@EnableConfigurationProperties
	@ConfigurationProperties
	protected static class MapProperties {

		private Map<String, String> mymap;

		public void setMymap(Map<String, String> mymap) {
			this.mymap = mymap;
		}

		public Map<String, String> getMymap() {
			return this.mymap;
		}

	}

	@Component
	protected static class BasicPropertiesConsumer {

		@Autowired
		private BasicProperties properties;

		@PostConstruct
		public void init() {
			assertThat(this.properties).isNotNull();
		}

		public String getName() {
			return this.properties.name;
		}

	}

	public interface InterfaceForValidatedImplementation {

		String getFoo();
	}

	@ConfigurationProperties("test")
	@Validated
	public static class ValidatedImplementationProperties
			implements InterfaceForValidatedImplementation {

		@NotNull
		private String foo;

		@Override
		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

	}

	@ConfigurationProperties(prefix = "test")
	@Validated
	public static class WithPropertyPlaceholderValueProperties {

		@Value("${default.value}")
		private String value;

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

}
