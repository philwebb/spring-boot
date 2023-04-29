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

package smoketest.session.redis;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;

public class TestSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main)
			.with(MyBeanPostProcessor.class, TestcontainersConfiguration.class)
			.run(args);
	}

	// @ImportTestcontainers
	// static class Testcontainers {
	//
	// @ServiceConnection
	// private static RedisContainer redis = new RedisContainer();
	//
	// }

	@TestConfiguration(proxyBeanMethods = false)
	static class TestcontainersConfiguration {

		@Bean
		Dunno dynamicPropertyRegistry() {
			return new Dunno();
		}

		@Bean
		// @ServiceConnection("redis")
		RedisContainer redisContainer(DynamicPropertyRegistry propertyRegistry) {
			RedisContainer redisContainer = new RedisContainer();
			propertyRegistry.add("spring.data.redis.host", redisContainer::getHost);
			propertyRegistry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
			return redisContainer;
		}

	}

	static class Dunno implements DynamicPropertyRegistry {

		Map<String, Supplier<Object>> map = new LinkedHashMap<>();

		@Override
		public void add(String name, Supplier<Object> valueSupplier) {
			this.map.put(name, valueSupplier);
		}

	}

	private static class MyBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, EnvironmentAware {

		private volatile boolean started;

		private ConfigurableListableBeanFactory beanFactory;

		private Environment environment;

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (this.started) {
				return bean;
			}
			this.started = true;
			if (bean instanceof Startable s) {
				System.err.println(">> skipping startable " + beanName);
				return bean;
			}
			if (this.beanFactory.isConfigurationFrozen()) {

			}
			System.err.println(beanName);
			String[] namesForType = this.beanFactory.getBeanNamesForType(Startable.class);
			for (String name : namesForType) {
				System.err.println(">> getting startable " + name);
				this.beanFactory.getBean(name);
			}
			Dunno dunno = this.beanFactory.getBean(Dunno.class);
			TestPropertyValues propertyValues = TestPropertyValues.empty();
			for (Map.Entry<String, Supplier<Object>> e : dunno.map.entrySet()) {
				propertyValues = propertyValues.and(e.getKey(), e.getValue()::get);
			}
			propertyValues.applyTo((ConfigurableEnvironment) this.environment);
			return bean;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

}
