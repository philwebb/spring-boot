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

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.generate.MethodGenerator;
import org.springframework.aot.generate.MethodReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.DynamicClassLoader;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.beans.factory.aot.BeanRegistrationCode;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.SecurityRequestMatchersManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.validation.beanvalidation.MethodValidationExcludeFilter;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

/**
 * AOT tests for {@link ChildManagementContextInitializer}.
 *
 * @author Phillip Webb
 */
class ManagementContextAutoConfigurationAotTests {

	@Test
	@CompileWithTargetClassAccess(classes = { ChildManagementContextInitializer.class,
			ServletWebServerFactoryAutoConfiguration.class, ConfigurationProperties.class,
			WebEndpointAutoConfiguration.class, EventListenerMethodProcessor.class, TomcatServletWebServerFactory.class,
			PropertyPlaceholderAutoConfiguration.class, MethodValidationExcludeFilter.class,
			WebServerFactoryCustomizerBeanPostProcessor.class,
			SecurityRequestMatchersManagementContextConfiguration.class,
			ServletManagementContextAutoConfiguration.class, DelegatingWebMvcConfiguration.class })
	@SuppressWarnings("unchecked")
	void test() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class,
								ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
								EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("server.port=0", "management.server.port=0").prepare((context) -> {
			InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
			DefaultGenerationContext generationContext = new DefaultGenerationContext(generatedFiles);
			ClassName className = ClassName.get("com.example", "TestInitializer");
			new ApplicationContextAotGenerator().generateApplicationContext(
					(GenericApplicationContext) context.getSourceApplicationContext(), generationContext, className);
			generationContext.writeGeneratedContent();
			TestCompiler compiler = TestCompiler.forSystem();
			compiler.withFiles(generatedFiles).printFiles(System.out).compile((compiled) -> {
				try {
					ClassLoader classLoader = compiled.getClassLoader();
					Field field = DynamicClassLoader.class.getDeclaredField("classFiles");
					ReflectionUtils.makeAccessible(field);
					Map<String, ?> map = (Map<String, ?>) ReflectionUtils.getField(field, classLoader);
					for (String name : map.keySet()) {
						System.err.println(name);
						compiled.getInstance(Object.class, name);
					}
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				ServletWebServerApplicationContext freshApplicationContext = new ServletWebServerApplicationContext();
				TestPropertyValues.of("server.port=0", "management.server.port=0").applyTo(freshApplicationContext);
				ApplicationContextInitializer<GenericApplicationContext> initializer = compiled
						.getInstance(ApplicationContextInitializer.class, className.toString());
				initializer.initialize(freshApplicationContext);
				freshApplicationContext.refresh();
			});
		});
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
