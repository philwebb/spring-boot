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

package org.springframework.boot.autoconfigure.pulsar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.schema.KeyValueEncodingType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.pulsar.core.DefaultSchemaResolver;
import org.springframework.pulsar.core.DefaultTopicResolver;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarClientBuilderCustomizer;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.SchemaResolver.SchemaResolverCustomizer;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.function.PulsarFunctionAdministration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarConfiguration}.
 *
 * @author Chris Bono
 * @author Alexander Preuß
 * @author Soby Chacko
 * @author Phillip Webb
 */
class PulsarConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
		.withUserConfiguration(PulsarClientBuilderCustomizerConfiguration.class);

	@Nested
	class ClientTests {

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			PulsarClient customClient = mock(PulsarClient.class);
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withBean("customPulsarClient", PulsarClient.class, () -> customClient)
				.run((context) -> assertThat(context).getBean(PulsarClient.class).isSameAs(customClient));
		}

		@Test
		void whenHasUseDefinedCustomizersAppliesInCorrectOrder() {
			new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(PulsarConfiguration.class))
				.withUserConfiguration(ClientCustomizersTestConfiguration.class)
				.run((context) -> {
					PulsarClientBuilderCustomizer customizer1 = context.getBean("clientCustomizerBar",
							PulsarClientBuilderCustomizer.class);
					PulsarClientBuilderCustomizer customizer2 = context.getBean("clientCustomizerFoo",
							PulsarClientBuilderCustomizer.class);
					InOrder ordered = inOrder(customizer1, customizer2);
					ordered.verify(customizer1).customize(any());
					ordered.verify(customizer2).customize(any());
				});
		}

		@Configuration(proxyBeanMethods = false)
		static class ClientCustomizersTestConfiguration {

			@Bean
			@Order(200)
			PulsarClientBuilderCustomizer clientCustomizerFoo() {
				return mock(PulsarClientBuilderCustomizer.class);
			}

			@Bean
			@Order(100)
			PulsarClientBuilderCustomizer clientCustomizerBar() {
				return mock(PulsarClientBuilderCustomizer.class);
			}

		}

	}

	@Nested
	class AdministrationTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			PulsarAdministration pulsarAdministration = mock(PulsarAdministration.class);
			this.contextRunner
				.withBean("customPulsarAdministration", PulsarAdministration.class, () -> pulsarAdministration)
				.run((context) -> assertThat(context).getBean(PulsarAdministration.class)
					.isSameAs(pulsarAdministration));
		}

	}

	@Nested
	class SchemaResolverTests {

		@SuppressWarnings("rawtypes")
		private static final InstanceOfAssertFactory<Map, MapAssert<Class, Schema>> CLASS_SCHEMA_MAP = InstanceOfAssertFactories
			.map(Class.class, Schema.class);

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			SchemaResolver schemaResolver = mock(SchemaResolver.class);
			this.contextRunner.withBean("customSchemaResolver", SchemaResolver.class, () -> schemaResolver)
				.run((context) -> assertThat(context).getBean(SchemaResolver.class).isSameAs(schemaResolver));
		}

		@Test
		void whenHasUserDefinedSchemaResolverCustomizer() {
			SchemaResolverCustomizer<DefaultSchemaResolver> customizer = (schemaResolver) -> schemaResolver
				.addCustomSchemaMapping(TestRecord.class, Schema.STRING);
			this.contextRunner.withBean("schemaResolverCustomizer", SchemaResolverCustomizer.class, () -> customizer)
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, InstanceOfAssertFactories.MAP)
					.containsEntry(TestRecord.class, Schema.STRING));
		}

		@Test
		void whenHasDefaultsTypeMappingForPrimitiveAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=STRING");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, InstanceOfAssertFactories.MAP)
					.containsOnly(entry(TestRecord.class, Schema.STRING)));
		}

		@Test
		void whenHasDefaultsTypeMappingForStructAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=JSON");
			Schema<?> expectedSchema = Schema.JSON(TestRecord.class);
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, CLASS_SCHEMA_MAP)
					.hasEntrySatisfying(TestRecord.class, schemaEqualTo(expectedSchema)));
		}

		@Test
		void whenHasDefaultsTypeMappingForKeyValueAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type=key-value");
			properties.add("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type=java.lang.String");
			Schema<?> expectedSchema = Schema.KeyValue(Schema.STRING, Schema.JSON(TestRecord.class),
					KeyValueEncodingType.INLINE);
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(DefaultSchemaResolver.class)
					.extracting(DefaultSchemaResolver::getCustomSchemaMappings, CLASS_SCHEMA_MAP)
					.hasEntrySatisfying(TestRecord.class, schemaEqualTo(expectedSchema)));
		}

		@SuppressWarnings("rawtypes")
		private Consumer<Schema> schemaEqualTo(Schema<?> expected) {
			return (actual) -> assertThat(actual.getSchemaInfo()).isEqualTo(expected.getSchemaInfo());
		}

	}

	@Nested
	class TopicResolverTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenHasUserDefinedBeanDoesNotAutoConfigureBean() {
			TopicResolver topicResolver = mock(TopicResolver.class);
			this.contextRunner.withBean("customTopicResolver", TopicResolver.class, () -> topicResolver)
				.run((context) -> assertThat(context).getBean(TopicResolver.class).isSameAs(topicResolver));
		}

		@Test
		void whenHasDefaultsTypeMappingAddsToSchemaResolver() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.defaults.type-mappings[0].message-type=" + TestRecord.CLASS_NAME);
			properties.add("spring.pulsar.defaults.type-mappings[0].topic-name=foo-topic");
			properties.add("spring.pulsar.defaults.type-mappings[1].message-type=java.lang.String");
			properties.add("spring.pulsar.defaults.type-mappings[1].topic-name=string-topic");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(TopicResolver.class)
					.asInstanceOf(InstanceOfAssertFactories.type(DefaultTopicResolver.class))
					.extracting(DefaultTopicResolver::getCustomTopicMappings, InstanceOfAssertFactories.MAP)
					.containsOnly(entry(TestRecord.class, "foo-topic"), entry(String.class, "string-topic")));
		}

	}

	@Nested
	class FunctionAdministrationTests {

		private final ApplicationContextRunner contextRunner = PulsarConfigurationTests.this.contextRunner;

		@Test
		void whenNoPropertiesAddsFunctionAdministrationBean() {
			this.contextRunner.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
				.hasFieldOrPropertyWithValue("failFast", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateFailures", Boolean.TRUE)
				.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.FALSE)
				.hasNoNullFieldsOrProperties() // ensures object providers set
				.extracting("pulsarAdministration")
				.isSameAs(context.getBean(PulsarAdministration.class)));
		}

		@Test
		void whenHasFunctionPropertiesAppliesPropertiesToBean() {
			List<String> properties = new ArrayList<>();
			properties.add("spring.pulsar.function.fail-fast=false");
			properties.add("spring.pulsar.function.propagate-failures=false");
			properties.add("spring.pulsar.function.propagate-stop-failures=true");
			this.contextRunner.withPropertyValues(properties.toArray(String[]::new))
				.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
					.hasFieldOrPropertyWithValue("failFast", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateFailures", Boolean.FALSE)
					.hasFieldOrPropertyWithValue("propagateStopFailures", Boolean.TRUE));
		}

		@Test
		void whenHasFunctionDisabledPropertyDoesNotCreateBean() {
			this.contextRunner.withPropertyValues("spring.pulsar.function.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(PulsarFunctionAdministration.class));
		}

		@Test
		void whenHasCustomFunctionAdministrationBean() {
			PulsarFunctionAdministration functionAdministration = mock(PulsarFunctionAdministration.class);
			this.contextRunner.withBean(PulsarFunctionAdministration.class, () -> functionAdministration)
				.run((context) -> assertThat(context).getBean(PulsarFunctionAdministration.class)
					.isSameAs(functionAdministration));
		}

	}

	record TestRecord() {

		private static final String CLASS_NAME = TestRecord.class.getName();

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class PulsarClientBuilderCustomizerConfiguration {

		@Bean
		PulsarClient pulsarClient() {
			// Use a mock because the real PulsarClient is very slow to close
			return mock(PulsarClient.class);
		}

	}

}
