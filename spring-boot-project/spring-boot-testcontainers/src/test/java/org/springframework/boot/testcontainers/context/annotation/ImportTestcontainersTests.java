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

package org.springframework.boot.testcontainers.context.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.boot.testcontainers.ImportTestcontainers;
import org.springframework.boot.testcontainers.TestcontainerBeanDefinition;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ImportTestcontainers}.
 *
 * @author Phillip Webb
 */
class ImportTestcontainersTests {

	private AnnotationConfigApplicationContext applicationContext;

	@AfterEach
	void teardown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	void importWithoutValueRegistersBeans() {
		this.applicationContext = new AnnotationConfigApplicationContext(ImportWithoutValue.class);
		String[] beanNames = this.applicationContext.getBeanNamesForType(PostgreSQLContainer.class);
		assertThat(beanNames).hasSize(1);
		assertThat(this.applicationContext.getBean(beanNames[0])).isSameAs(ImportWithoutValue.container);
		TestcontainerBeanDefinition beanDefinition = (TestcontainerBeanDefinition) this.applicationContext
			.getBeanDefinition(beanNames[0]);
		assertThat(beanDefinition.getDockerImageName()).isEqualTo(ImportWithoutValue.container.getDockerImageName());
		assertThat(beanDefinition.getAnnotations().isPresent(ContainerAnnotation.class)).isTrue();
	}

	@Test
	void importWithValueRegistersBeans() {
		this.applicationContext = new AnnotationConfigApplicationContext(ImportWithValue.class);
		String[] beanNames = this.applicationContext.getBeanNamesForType(PostgreSQLContainer.class);
		assertThat(beanNames).hasSize(1);
		assertThat(this.applicationContext.getBean(beanNames[0])).isSameAs(ContainerDefinitions.container);
		TestcontainerBeanDefinition beanDefinition = (TestcontainerBeanDefinition) this.applicationContext
			.getBeanDefinition(beanNames[0]);
		assertThat(beanDefinition.getDockerImageName()).isEqualTo(ContainerDefinitions.container.getDockerImageName());
		assertThat(beanDefinition.getAnnotations().isPresent(ContainerAnnotation.class)).isTrue();
	}

	@Test
	void importWhenHasNoContainerFieldsThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.applicationContext = new AnnotationConfigApplicationContext(NoContainers.class))
			.withMessageContaining("does not container any container fields");
	}

	@Test
	void importWhenHasNullContainerFieldThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> this.applicationContext = new AnnotationConfigApplicationContext(NullContainer.class))
			.withMessageContaining("declares the null container field [container]");
	}

	@Test
	void importWhenHasNonStaticContainerFieldThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(
					() -> this.applicationContext = new AnnotationConfigApplicationContext(NonStaticContainer.class))
			.withMessageContaining("declares non-static container fields [container]");
	}

	@ImportTestcontainers
	static class ImportWithoutValue {

		@ContainerAnnotation
		static PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageNames.postgresql());

	}

	@ImportTestcontainers(ContainerDefinitions.class)
	static class ImportWithValue {

	}

	@ImportTestcontainers
	static class NoContainers {

	}

	@ImportTestcontainers
	static class NullContainer {

		static PostgreSQLContainer<?> container = null;

	}

	@ImportTestcontainers
	static class NonStaticContainer {

		PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageNames.postgresql());

	}

	interface ContainerDefinitions {

		@ContainerAnnotation
		PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageNames.postgresql());

	}

	@Retention(RetentionPolicy.RUNTIME)
	static @interface ContainerAnnotation {

	}

}
