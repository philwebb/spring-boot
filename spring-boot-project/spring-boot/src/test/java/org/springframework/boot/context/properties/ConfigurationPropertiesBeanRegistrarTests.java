/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link EnableConfigurationPropertiesRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesBeanRegistrarTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(
			this.beanFactory);

	@Test
	void typeWithDefaultConstructorShouldRegisterGenericBeanDefinition() throws Exception {
		this.registrar.register(TestConfiguration.class);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"foo-org.springframework.boot.context.properties.ConfigurationPropertiesBeanRegistrarTests$FooProperties");
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	@Test
	void typeWithOneConstructorWithParametersShouldRegisterConfigurationPropertiesBeanDefinition() throws Exception {
		this.registrar.register(TestConfiguration.class);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"bar-org.springframework.boot.context.properties.ConfigurationPropertiesBeanRegistrarTests$BarProperties");
		assertThat(beanDefinition).isExactlyInstanceOf(ConfigurationPropertiesValueObjectBeanDefinition.class);
	}

	@Test
	void typeWithMultipleConstructorsShouldRegisterGenericBeanDefinition() throws Exception {
		this.registrar.register(TestConfiguration.class);
		BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(
				"bing-org.springframework.boot.context.properties.ConfigurationPropertiesBeanRegistrarTests$BingProperties");
		assertThat(beanDefinition).isExactlyInstanceOf(GenericBeanDefinition.class);
	}

	@Test
	void typeWithNoAnnotationShouldFail() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.registrar.register(InvalidConfiguration.class))
				.withMessageContaining("No ConfigurationProperties annotation found")
				.withMessageContaining(ConfigurationPropertiesBeanRegistrar.class.getName());
	}

	@Test
	void registrationWithDuplicatedTypeShouldRegisterSingleBeanDefinition() throws IOException {
		DefaultListableBeanFactory factory = spy(this.beanFactory);
		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(this.beanFactory);
		registrar.register(DuplicateConfiguration.class);
		verify(factory, times(1)).registerBeanDefinition(anyString(), any());
	}

	@Test
	void registrationWithNoTypeShouldNotRegisterAnything() throws IOException {
		DefaultListableBeanFactory factory = spy(this.beanFactory);
		ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(this.beanFactory);
		registrar.register(EmptyConfiguration.class);
		verifyZeroInteractions(factory);
	}

	@EnableConfigurationProperties({ FooProperties.class, BarProperties.class, BingProperties.class })
	static class TestConfiguration {

	}

	@EnableConfigurationProperties(ConfigurationPropertiesBeanRegistrarTests.class)
	static class InvalidConfiguration {

	}

	@EnableConfigurationProperties({ FooProperties.class, FooProperties.class })
	static class DuplicateConfiguration {

	}

	@EnableConfigurationProperties
	static class EmptyConfiguration {

	}

	@ConfigurationProperties(prefix = "foo")
	static class FooProperties {

	}

	@ConfigurationProperties(prefix = "bar")
	static class BarProperties {

		BarProperties(String foo) {
		}

	}

	@ConfigurationProperties(prefix = "bing")
	static class BingProperties {

		BingProperties() {
		}

		BingProperties(String foo) {
		}

	}

}
