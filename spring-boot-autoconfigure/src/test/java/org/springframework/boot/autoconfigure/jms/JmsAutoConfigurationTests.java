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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.test.context.ApplicationContextTester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsAutoConfiguration}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Aurélien Leboulanger
 */
public class JmsAutoConfigurationTests {

	private static final String ACTIVEMQ_EMBEDDED_URL = "vm://localhost?broker.persistent=false";

	private static final String ACTIVEMQ_NETWORK_URL = "tcp://localhost:61616";

	private final ApplicationContextTester contextLoader = new ApplicationContextTester()
			.register(AutoConfigurations.of(ActiveMQAutoConfiguration.class,
					JmsAutoConfiguration.class));

	@Test
	public void testDefaultJmsConfiguration() {
		this.contextLoader.register(TestConfiguration.class).run(context -> {
			ActiveMQConnectionFactory connectionFactory = context
					.getBean(ActiveMQConnectionFactory.class);
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			JmsMessagingTemplate messagingTemplate = context
					.getBean(JmsMessagingTemplate.class);
			assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
			assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
			assertThat(((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
					.getBrokerURL()).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
			assertThat(context.containsBean("jmsListenerContainerFactory")).isTrue();
		});
	}

	@Test
	public void testConnectionFactoryBackOff() {
		this.contextLoader.register(TestConfiguration2.class)
				.run(context -> assertThat(
						context.getBean(ActiveMQConnectionFactory.class).getBrokerURL())
								.isEqualTo("foobar"));
	}

	@Test
	public void testJmsTemplateBackOff() {
		this.contextLoader.register(TestConfiguration3.class).run(context -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate.getPriority()).isEqualTo(999);
		});
	}

	@Test
	public void testJmsMessagingTemplateBackOff() {
		this.contextLoader.register(TestConfiguration5.class).run(context -> {
			JmsMessagingTemplate messagingTemplate = context
					.getBean(JmsMessagingTemplate.class);
			assertThat(messagingTemplate.getDefaultDestinationName()).isEqualTo("fooBar");
		});
	}

	@Test
	public void testJmsTemplateBackOffEverything() {
		this.contextLoader.register(TestConfiguration2.class, TestConfiguration3.class,
				TestConfiguration5.class).run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					assertThat(jmsTemplate.getPriority()).isEqualTo(999);
					assertThat(context.getBean(ActiveMQConnectionFactory.class)
							.getBrokerURL()).isEqualTo("foobar");
					JmsMessagingTemplate messagingTemplate = context
							.getBean(JmsMessagingTemplate.class);
					assertThat(messagingTemplate.getDefaultDestinationName())
							.isEqualTo("fooBar");
					assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
				});
	}

	@Test
	public void testEnableJmsCreateDefaultContainerFactory() {
		this.contextLoader.register(EnableJmsConfiguration.class).run(context -> {
			JmsListenerContainerFactory<?> jmsListenerContainerFactory = context.getBean(
					"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
			assertThat(jmsListenerContainerFactory.getClass())
					.isEqualTo(DefaultJmsListenerContainerFactory.class);
		});
	}

	@Test
	public void testJmsListenerContainerFactoryBackOff() {
		this.contextLoader
				.register(TestConfiguration6.class, EnableJmsConfiguration.class)
				.run(context -> {
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("jmsListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory.getClass())
							.isEqualTo(SimpleJmsListenerContainerFactory.class);
				});
	}

	@Test
	public void testJmsListenerContainerFactoryWithCustomSettings() {
		this.contextLoader.register(EnableJmsConfiguration.class)
				.env("spring.jms.listener.autoStartup=false",
						"spring.jms.listener.acknowledgeMode=client",
						"spring.jms.listener.concurrency=2",
						"spring.jms.listener.maxConcurrency=10")
				.run(context -> {
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("jmsListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory.getClass())
							.isEqualTo(DefaultJmsListenerContainerFactory.class);
					DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(listenerContainer.isAutoStartup()).isFalse();
					assertThat(listenerContainer.getSessionAcknowledgeMode())
							.isEqualTo(Session.CLIENT_ACKNOWLEDGE);
					assertThat(listenerContainer.getConcurrentConsumers()).isEqualTo(2);
					assertThat(listenerContainer.getMaxConcurrentConsumers())
							.isEqualTo(10);
				});
	}

	@Test
	public void testDefaultContainerFactoryWithJtaTransactionManager() {
		this.contextLoader
				.register(TestConfiguration7.class, EnableJmsConfiguration.class)
				.run(context -> {
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("jmsListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory.getClass())
							.isEqualTo(DefaultJmsListenerContainerFactory.class);
					DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(listenerContainer.isSessionTransacted()).isFalse();
					assertThat(new DirectFieldAccessor(listenerContainer)
							.getPropertyValue("transactionManager")).isSameAs(
									context.getBean(JtaTransactionManager.class));
				});
	}

	@Test
	public void testDefaultContainerFactoryNonJtaTransactionManager() {
		this.contextLoader
				.register(TestConfiguration8.class, EnableJmsConfiguration.class)
				.run(context -> {
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("jmsListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory.getClass())
							.isEqualTo(DefaultJmsListenerContainerFactory.class);
					DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(listenerContainer.isSessionTransacted()).isTrue();
					assertThat(new DirectFieldAccessor(listenerContainer)
							.getPropertyValue("transactionManager")).isNull();
				});
	}

	@Test
	public void testDefaultContainerFactoryNoTransactionManager() {
		this.contextLoader.register(EnableJmsConfiguration.class).run(context -> {
			JmsListenerContainerFactory<?> jmsListenerContainerFactory = context.getBean(
					"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
			assertThat(jmsListenerContainerFactory.getClass())
					.isEqualTo(DefaultJmsListenerContainerFactory.class);
			DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
					.createListenerContainer(mock(JmsListenerEndpoint.class));
			assertThat(listenerContainer.isSessionTransacted()).isTrue();
			assertThat(new DirectFieldAccessor(listenerContainer)
					.getPropertyValue("transactionManager")).isNull();
		});
	}

	@Test
	public void testDefaultContainerFactoryWithMessageConverters() {
		this.contextLoader.register(MessageConvertersConfiguration.class,
				EnableJmsConfiguration.class).run(context -> {
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("jmsListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory.getClass())
							.isEqualTo(DefaultJmsListenerContainerFactory.class);
					DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(listenerContainer.getMessageConverter())
							.isSameAs(context.getBean("myMessageConverter"));
				});
	}

	@Test
	public void testCustomContainerFactoryWithConfigurer() {
		this.contextLoader
				.register(TestConfiguration9.class, EnableJmsConfiguration.class)
				.env("spring.jms.listener.autoStartup=false").run(context -> {
					assertThat(context.containsBean("jmsListenerContainerFactory"))
							.isTrue();
					JmsListenerContainerFactory<?> jmsListenerContainerFactory = context
							.getBean("customListenerContainerFactory",
									JmsListenerContainerFactory.class);
					assertThat(jmsListenerContainerFactory)
							.isInstanceOf(DefaultJmsListenerContainerFactory.class);
					DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(listenerContainer.getCacheLevel())
							.isEqualTo(DefaultMessageListenerContainer.CACHE_CONSUMER);
					assertThat(listenerContainer.isAutoStartup()).isFalse();
				});
	}

	@Test
	public void testJmsTemplateWithMessageConverter() {
		this.contextLoader.register(MessageConvertersConfiguration.class)
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					assertThat(jmsTemplate.getMessageConverter())
							.isSameAs(context.getBean("myMessageConverter"));
				});
	}

	@Test
	public void testJmsTemplateWithDestinationResolver() {
		this.contextLoader.register(DestinationResolversConfiguration.class)
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					assertThat(jmsTemplate.getDestinationResolver())
							.isSameAs(context.getBean("myDestinationResolver"));
				});
	}

	@Test
	public void testJmsTemplateFullCustomization() {
		this.contextLoader.register(MessageConvertersConfiguration.class)
				.env("spring.jms.template.default-destination=testQueue",
						"spring.jms.template.delivery-delay=500",
						"spring.jms.template.delivery-mode=non-persistent",
						"spring.jms.template.priority=6",
						"spring.jms.template.time-to-live=6000",
						"spring.jms.template.receive-timeout=2000")
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					assertThat(jmsTemplate.getMessageConverter())
							.isSameAs(context.getBean("myMessageConverter"));
					assertThat(jmsTemplate.isPubSubDomain()).isFalse();
					assertThat(jmsTemplate.getDefaultDestinationName())
							.isEqualTo("testQueue");
					assertThat(jmsTemplate.getDeliveryDelay()).isEqualTo(500);
					assertThat(jmsTemplate.getDeliveryMode()).isEqualTo(1);
					assertThat(jmsTemplate.getPriority()).isEqualTo(6);
					assertThat(jmsTemplate.getTimeToLive()).isEqualTo(6000);
					assertThat(jmsTemplate.isExplicitQosEnabled()).isTrue();
					assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(2000);
				});
	}

	@Test
	public void testPubSubDisabledByDefault() {
		this.contextLoader.register(TestConfiguration.class).run(context -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate.isPubSubDomain()).isFalse();
		});
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsTrue() {
		this.contextLoader.register(TestConfiguration4.class).run(context -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate.isPubSubDomain()).isTrue();
		});
	}

	@Test
	public void testPubSubDomainActive() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.jms.pubSubDomain:true").run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					DefaultMessageListenerContainer defaultMessageListenerContainer = context
							.getBean(DefaultJmsListenerContainerFactory.class)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(jmsTemplate.isPubSubDomain()).isTrue();
					assertThat(defaultMessageListenerContainer.isPubSubDomain()).isTrue();
				});
	}

	@Test
	public void testPubSubDomainOverride() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.jms.pubSubDomain:false").run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ActiveMQConnectionFactory connectionFactory = context
							.getBean(ActiveMQConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(jmsTemplate.isPubSubDomain()).isFalse();
					assertThat(connectionFactory).isNotNull();
					assertThat(connectionFactory)
							.isEqualTo(jmsTemplate.getConnectionFactory());
				});
	}

	@Test
	public void testActiveMQOverriddenStandalone() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.activemq.inMemory:false").run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ActiveMQConnectionFactory connectionFactory = context
							.getBean(ActiveMQConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(connectionFactory).isNotNull();
					assertThat(connectionFactory)
							.isEqualTo(jmsTemplate.getConnectionFactory());
					assertThat(((ActiveMQConnectionFactory) jmsTemplate
							.getConnectionFactory()).getBrokerURL())
									.isEqualTo(ACTIVEMQ_NETWORK_URL);
				});
	}

	@Test
	public void testActiveMQOverriddenRemoteHost() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.activemq.brokerUrl:tcp://remote-host:10000")
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ActiveMQConnectionFactory connectionFactory = context
							.getBean(ActiveMQConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(connectionFactory).isNotNull();
					assertThat(connectionFactory)
							.isEqualTo(jmsTemplate.getConnectionFactory());
					assertThat(((ActiveMQConnectionFactory) jmsTemplate
							.getConnectionFactory()).getBrokerURL())
									.isEqualTo("tcp://remote-host:10000");
				});
	}

	@Test
	public void testActiveMQOverriddenPool() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.activemq.pool.enabled:true").run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					PooledConnectionFactory pool = context
							.getBean(PooledConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
							.getConnectionFactory();
					assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
				});
	}

	@Test
	public void testActiveMQOverriddenPoolAndStandalone() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.activemq.pool.enabled:true",
						"spring.activemq.inMemory:false")
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					PooledConnectionFactory pool = context
							.getBean(PooledConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
							.getConnectionFactory();
					assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_NETWORK_URL);
				});
	}

	@Test
	public void testActiveMQOverriddenPoolAndRemoteServer() {
		this.contextLoader.register(TestConfiguration.class)
				.env("spring.activemq.pool.enabled:true",
						"spring.activemq.brokerUrl:tcp://remote-host:10000")
				.run(context -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					PooledConnectionFactory pool = context
							.getBean(PooledConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
							.getConnectionFactory();
					assertThat(factory.getBrokerURL())
							.isEqualTo("tcp://remote-host:10000");
				});
	}

	@Test
	public void enableJmsAutomatically() throws Exception {
		this.contextLoader.register(NoEnableJmsConfiguration.class).run(context -> {
			context.getBean(
					JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
			context.getBean(
					JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
		});
	}

	@Configuration
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class TestConfiguration2 {

		@Bean
		ConnectionFactory connectionFactory() {
			return new ActiveMQConnectionFactory() {
				{
					setBrokerURL("foobar");
				}
			};
		}

	}

	@Configuration
	protected static class TestConfiguration3 {

		@Bean
		JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setPriority(999);
			return jmsTemplate;
		}

	}

	@Configuration
	protected static class TestConfiguration4 implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean.getClass().isAssignableFrom(JmsTemplate.class)) {
				JmsTemplate jmsTemplate = (JmsTemplate) bean;
				jmsTemplate.setPubSubDomain(true);
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

	}

	@Configuration
	protected static class TestConfiguration5 {

		@Bean
		JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(
					jmsTemplate);
			messagingTemplate.setDefaultDestinationName("fooBar");
			return messagingTemplate;
		}

	}

	@Configuration
	protected static class TestConfiguration6 {

		@Bean
		JmsListenerContainerFactory<?> jmsListenerContainerFactory(
				ConnectionFactory connectionFactory) {
			SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			return factory;
		}

	}

	@Configuration
	protected static class TestConfiguration7 {

		@Bean
		JtaTransactionManager transactionManager() {
			return mock(JtaTransactionManager.class);
		}

	}

	@Configuration
	protected static class TestConfiguration8 {

		@Bean
		DataSourceTransactionManager transactionManager() {
			return mock(DataSourceTransactionManager.class);
		}

	}

	@Configuration
	protected static class MessageConvertersConfiguration {

		@Bean
		@Primary
		public MessageConverter myMessageConverter() {
			return mock(MessageConverter.class);
		}

		@Bean
		public MessageConverter anotherMessageConverter() {
			return mock(MessageConverter.class);
		}

	}

	@Configuration
	protected static class DestinationResolversConfiguration {

		@Bean
		@Primary
		public DestinationResolver myDestinationResolver() {
			return mock(DestinationResolver.class);
		}

		@Bean
		public DestinationResolver anotherDestinationResolver() {
			return mock(DestinationResolver.class);
		}

	}

	@Configuration
	protected static class TestConfiguration9 {

		@Bean
		JmsListenerContainerFactory<?> customListenerContainerFactory(
				DefaultJmsListenerContainerFactoryConfigurer configurer,
				ConnectionFactory connectionFactory) {
			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			configurer.configure(factory, connectionFactory);
			factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
			return factory;

		}

	}

	@Configuration
	@EnableJms
	protected static class EnableJmsConfiguration {

	}

	@Configuration
	protected static class NoEnableJmsConfiguration {

	}

}
