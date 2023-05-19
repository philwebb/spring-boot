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

package org.springframework.boot.context.properties;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.ConfigurationPropertiesBeanFactoryInitializationAotProcessor.ConfigurationPropertiesReflectionHintsContribution;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessorTests {

	private final ConfigurationPropertiesBeanFactoryInitializationAotProcessor processor = new ConfigurationPropertiesBeanFactoryInitializationAotProcessor();

	@Test
	void configurationPropertiesBeanFactoryInitializationAotProcessorIsRegistered() {
		assertThat(AotServices.factories().load(BeanFactoryInitializationAotProcessor.class))
			.anyMatch(ConfigurationPropertiesBeanFactoryInitializationAotProcessor.class::isInstance);
	}

	@Test
	void processNoMatchesReturnsNullContribution() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("test", new RootBeanDefinition(String.class));
		assertThat(this.processor.processAheadOfTime(beanFactory)).isNull();
	}

	@Test
	void manuallyRegisteredSingletonBindsAsJavaBean() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		beanFactory.registerSingleton("test", new SampleProperties());
		ConfigurationPropertiesReflectionHintsContribution contribution = process(beanFactory);
		assertThat(contribution.getBindables()).singleElement().satisfies((bindable) -> {
			assertThat(bindable.getType().getRawClass()).isEqualTo(SampleProperties.class);
			assertThat(bindable.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
		assertThat(typeHints(contribution)).singleElement()
			.extracting(TypeHint::getType)
			.isEqualTo(TypeReference.of(SampleProperties.class));
	}

	@Test
	void javaBeanConfigurationPropertiesBindAsJavaBean() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				JavaBeanPropertiesConfiguration.class);
		assertThat(contribution.getBindables()).singleElement().satisfies((bindable) -> {
			assertThat(bindable.getType().getRawClass()).isEqualTo(JavaBeanProperties.class);
			assertThat(bindable.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
		assertThat(typeHints(contribution)).singleElement()
			.extracting(TypeHint::getType)
			.isEqualTo(TypeReference.of(JavaBeanProperties.class));
	}

	@Test
	void constructorBindingConfigurationPropertiesBindAsValueObject() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				ConstructorBindingPropertiesConfiguration.class);
		assertThat(contribution.getBindables()).singleElement().satisfies((bindable) -> {
			assertThat(bindable.getType().getRawClass()).isEqualTo(ConstructorBindingProperties.class);
			assertThat(bindable.getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
		});
		assertThat(typeHints(contribution)).singleElement()
			.extracting(TypeHint::getType)
			.isEqualTo(TypeReference.of(ConstructorBindingProperties.class));
	}

	@Test
	void possibleConstructorBindingPropertiesDefinedThroughBeanMethodBindAsJavaBean() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				PossibleConstructorBindingPropertiesDefinedThroughBeanMethodConfiguration.class);
		assertThat(contribution.getBindables()).singleElement().satisfies((bindable) -> {
			assertThat(bindable.getType().getRawClass()).isEqualTo(PossibleConstructorBindingProperties.class);
			assertThat(bindable.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
		});
		assertThat(typeHints(contribution)).singleElement()
			.extracting(TypeHint::getType)
			.isEqualTo(TypeReference.of(PossibleConstructorBindingProperties.class));
	}

	@Test
	void possibleConstructorBindingPropertiesDefinedThroughEnabledAnnotationBindAsValueObject() {
		ConfigurationPropertiesReflectionHintsContribution contribution = process(
				PossibleConstructorBindingPropertiesDefinedThroughEnableAnnotationConfiguration.class);
		assertThat(contribution.getBindables()).singleElement().satisfies((bindable) -> {
			assertThat(bindable.getType().getRawClass()).isEqualTo(PossibleConstructorBindingProperties.class);
			assertThat(bindable.getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
		});
		assertThat(typeHints(contribution)).singleElement()
			.extracting(TypeHint::getType)
			.isEqualTo(TypeReference.of(PossibleConstructorBindingProperties.class));
	}

	private Stream<TypeHint> typeHints(ConfigurationPropertiesReflectionHintsContribution contribution) {
		TestGenerationContext generationContext = new TestGenerationContext();
		contribution.applyTo(generationContext, null);
		return generationContext.getRuntimeHints().reflection().typeHints();
	}

	private ConfigurationPropertiesReflectionHintsContribution process(Class<?> config) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(config)) {
			return process(context.getBeanFactory());
		}
	}

	private ConfigurationPropertiesReflectionHintsContribution process(ConfigurableListableBeanFactory beanFactory) {
		return (ConfigurationPropertiesReflectionHintsContribution) this.processor.processAheadOfTime(beanFactory);
	}

	@ConfigurationProperties("test")
	static class SampleProperties {

	}

	@EnableConfigurationProperties(JavaBeanProperties.class)
	static class JavaBeanPropertiesConfiguration {

	}

	@ConfigurationProperties("java-bean")
	static class JavaBeanProperties {

		private String value;

		String getValue() {
			return this.value;
		}

		void setValue(String value) {
			this.value = value;
		}

	}

	@EnableConfigurationProperties(ConstructorBindingProperties.class)
	static class ConstructorBindingPropertiesConfiguration {

	}

	@ConfigurationProperties("constructor-binding")
	static class ConstructorBindingProperties {

		private final String value;

		ConstructorBindingProperties(String value) {
			this.value = value;
		}

		String getValue() {
			return this.value;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PossibleConstructorBindingPropertiesDefinedThroughBeanMethodConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "bean-method")
		PossibleConstructorBindingProperties possibleConstructorBindingProperties() {
			return new PossibleConstructorBindingProperties("alpha");
		}

	}

	@EnableConfigurationProperties(PossibleConstructorBindingProperties.class)
	static class PossibleConstructorBindingPropertiesDefinedThroughEnableAnnotationConfiguration {

	}

	@ConfigurationProperties("possible-constructor-binding")
	static class PossibleConstructorBindingProperties {

		private String value;

		PossibleConstructorBindingProperties(String arg) {

		}

		String getValue() {
			return this.value;
		}

		void setValue(String value) {
			this.value = value;
		}

	}

}
