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

package org.springframework.boot.context.properties.bind;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.BindableTests.JavaBeanOrValueObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BindMethod}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
public class BindMethodTests {

	@Test
	void defaultBindMethodWhenNoConstructorBindingReturnsJavaBean() {
		BindMethod bindType = BindMethod.get(NoConstructorBinding.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void defaultBindMethodWhenConstructorBindingOnConstructorReturnsValueObject() {
		BindMethod bindType = BindMethod.get(ConstructorBindingOnConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	@Test
	void defaultBindMethodWhenNoConstructorBindingAnnotationOnSingleParameterizedConstructorReturnsValueObject() {
		BindMethod bindType = BindMethod.get(ConstructorBindingNoAnnotation.class);
		assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	@Test
	void defaultBindMethodWhenConstructorBindingOnMultipleConstructorsThrowsException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> BindMethod.get(ConstructorBindingOnMultipleConstructors.class))
			.withMessage(ConstructorBindingOnMultipleConstructors.class.getName()
					+ " has more than one @ConstructorBinding constructor");
	}

	@Test
	void defaultBindMethodWhenMultipleConstructorsReturnJavaBean() {
		BindMethod bindType = BindMethod.get(NoConstructorBindingOnMultipleConstructors.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void defaultBindMethodWithNoArgConstructorReturnsJavaBean() {
		BindMethod bindType = BindMethod.get(JavaBeanWithNoArgConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void defaultBindMethodWithSingleArgAutowiredConstructorReturnsJavaBean() {
		BindMethod bindType = BindMethod.get(JavaBeanWithAutowiredConstructor.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void defaultBindMethodWhenConstructorBindingAndAutowiredConstructorsShouldThrowException() {
		assertThatIllegalStateException()
			.isThrownBy(() -> BindMethod.get(ConstructorBindingAndAutowiredConstructors.class));
	}

	@Test
	void defaultBindMethodWithInnerClassWithSyntheticFieldShouldReturnJavaBean() {
		BindMethod bindType = BindMethod.get(Inner.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void innerClassWithParameterizedConstructorShouldReturnJavaBean() {
		BindMethod bindType = BindMethod.get(ParameterizedConstructorInner.class);
		assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
	}

	@Test
	void defaultBindMethodWhenTypeCouldUseJavaBeanOrValueObjectReturnsValueObject() {
		BindMethod bindMethod = BindMethod.get(JavaBeanOrValueObject.class);
		assertThat(bindMethod).isEqualTo(BindMethod.VALUE_OBJECT);
	}

	static class NoConstructorBinding {

	}

	static class ConstructorBindingNoAnnotation {

		ConstructorBindingNoAnnotation(String name) {
		}

	}

	static class ConstructorBindingOnConstructor {

		ConstructorBindingOnConstructor(String name) {
			this(name, -1);
		}

		@ConstructorBinding
		ConstructorBindingOnConstructor(String name, int age) {
		}

	}

	static class ConstructorBindingOnMultipleConstructors {

		@ConstructorBinding
		ConstructorBindingOnMultipleConstructors(String name) {
			this(name, -1);
		}

		@ConstructorBinding
		ConstructorBindingOnMultipleConstructors(String name, int age) {
		}

	}

	static class NoConstructorBindingOnMultipleConstructors {

		NoConstructorBindingOnMultipleConstructors(String name) {
			this(name, -1);
		}

		NoConstructorBindingOnMultipleConstructors(String name, int age) {
		}

	}

	static class JavaBeanWithNoArgConstructor {

		JavaBeanWithNoArgConstructor() {
		}

	}

	static class JavaBeanWithAutowiredConstructor {

		@Autowired
		JavaBeanWithAutowiredConstructor(String name) {
		}

	}

	static class ConstructorBindingAndAutowiredConstructors {

		@Autowired
		ConstructorBindingAndAutowiredConstructors(String name) {
		}

		@ConstructorBinding
		ConstructorBindingAndAutowiredConstructors(Integer age) {
		}

	}

	class Inner {

	}

	class ParameterizedConstructorInner {

		ParameterizedConstructorInner(Integer age) {

		}

	}

}
