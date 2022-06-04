/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.io.InputStreamSource;
import org.springframework.javapoet.ClassName;

/**
 * AOT tests for {@link ChildManagementContextInitializer}.
 *
 * @author Phillip Webb
 */
class ManagementContextAutoConfigurationAotTests {

	@Test
	void test() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class,
								ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
								EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0").prepare((context) -> {
			String beanName = context.getBeanNamesForType(ChildManagementContextInitializer.class)[0];
			ChildManagementContextInitializer initializer = context.getBean(beanName,
					ChildManagementContextInitializer.class);
			RegisteredBean registeredBean = RegisteredBean.of(context.getBeanFactory(), beanName);
			BeanRegistrationAotContribution contribution = initializer.processAheadOfTime(registeredBean);
			InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
			GenerationContext generationContext = new DefaultGenerationContext(generatedFiles);
			BeanRegistrationCode beanRegistrationsCode = new MockBeanRegistrationCode();
			contribution.applyTo(generationContext, beanRegistrationsCode);
			Map<String, InputStreamSource> map = generatedFiles.getGeneratedFiles(Kind.SOURCE);
			map.forEach((name, content) -> {
				System.out.println(name);
				System.out.println(content);
			});
		});
		TestCompiler compiler = TestCompiler.forSystem();
	}

	static class MockBeanRegistrationCode implements BeanRegistrationCode {

		@Override
		public ClassName getClassName() {
			return null;
		}

		@Override
		public MethodGenerator getMethodGenerator() {
			return null;
		}

		@Override
		public void addInstancePostProcessor(MethodReference methodReference) {
		}

	}

}
