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

package org.springframework.boot.convert;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService.ConverterBeanAdapter;
import org.springframework.boot.convert.ApplicationConversionService.ParserBeanAdapter;
import org.springframework.boot.convert.ApplicationConversionService.PrinterBeanAdapter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ApplicationConversionService}.
 *
 * @author Phillip Webb
 * @author Shixiong Guo
 */
class ApplicationConversionServiceTests {

	private final FormatterRegistry registry = mock(FormatterRegistry.class,
			withSettings().extraInterfaces(ConversionService.class));

	@Test
	void addBeansWhenHasGenericConverterBeanAddConverter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				ExampleGenericConverter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			then(this.registry).should().addConverter(context.getBean(ExampleGenericConverter.class));
			then(this.registry).shouldHaveNoMoreInteractions();
		}
	}

	@Test
	void addBeansWhenHasConverterBeanAddConverter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleConverter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			then(this.registry).should().addConverter(context.getBean(ExampleConverter.class));
			then(this.registry).shouldHaveNoMoreInteractions();
		}
	}

	@Test
	void addBeansWhenHasFormatterBeanAddsOnlyFormatter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleFormatter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			then(this.registry).should().addFormatter(context.getBean(ExampleFormatter.class));
			then(this.registry).shouldHaveNoMoreInteractions();
		}
	}

	@Test
	void addBeansWhenHasPrinterBeanAddPrinter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExamplePrinter.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			then(this.registry).should().addPrinter(context.getBean(ExamplePrinter.class));
			then(this.registry).shouldHaveNoMoreInteractions();
		}
	}

	@Test
	void addBeansWhenHasParserBeanAddParser() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExampleParser.class)) {
			ApplicationConversionService.addBeans(this.registry, context);
			then(this.registry).should().addParser(context.getBean(ExampleParser.class));
			then(this.registry).shouldHaveNoMoreInteractions();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void addBeansWhenHasConverterBeanMethodAddConverter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				ConverterBeanMethodConfiguration.class)) {
			Converter<String, Integer> converter = (Converter<String, Integer>) context.getBean("converter");
			willThrow(IllegalArgumentException.class).given(this.registry).addConverter(converter);
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry).addConverter(any(ConverterBeanAdapter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void addBeansWhenHasPrinterBeanMethodAddPrinter() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				PrinterBeanMethodConfiguration.class)) {
			Printer<Integer> printer = (Printer<Integer>) context.getBean("printer");
			willThrow(IllegalArgumentException.class).given(this.registry).addPrinter(printer);
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry, never()).addPrinter(printer);
			verify(this.registry).addConverter(any(PrinterBeanAdapter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void addBeansWhenHasParserBeanMethodAddParser() {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				ParserBeanMethodConfiguration.class)) {
			Parser<Integer> parser = (Parser<Integer>) context.getBean("parser");
			willThrow(IllegalArgumentException.class).given(this.registry).addParser(parser);
			ApplicationConversionService.addBeans(this.registry, context);
			verify(this.registry, never()).addParser(parser);
			verify(this.registry).addConverter(any(ParserBeanAdapter.class));
			verifyNoMoreInteractions(this.registry);
		}
	}

	@Test
	void isConvertViaObjectSourceTypeWhenObjectSourceReturnsTrue() {
		// Uses ObjectToCollectionConverter
		ApplicationConversionService conversionService = new ApplicationConversionService();
		TypeDescriptor sourceType = TypeDescriptor.valueOf(Long.class);
		TypeDescriptor targetType = TypeDescriptor.valueOf(List.class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.isConvertViaObjectSourceType(sourceType, targetType)).isTrue();
	}

	@Test
	void isConvertViaObjectSourceTypeWhenNotObjectSourceReturnsFalse() {
		// Uses StringToCollectionConverter
		ApplicationConversionService conversionService = new ApplicationConversionService();
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.valueOf(List.class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.isConvertViaObjectSourceType(sourceType, targetType)).isFalse();
	}

	@Test
	void sharedInstanceCannotBeModified() {
		ApplicationConversionService instance = (ApplicationConversionService) ApplicationConversionService
			.getSharedInstance();
		assertUnmodifiableExceptionThrown(() -> instance.addPrinter(null));
		assertUnmodifiableExceptionThrown(() -> instance.addParser(null));
		assertUnmodifiableExceptionThrown(() -> instance.addFormatter(null));
		assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldType(null, null));
		assertUnmodifiableExceptionThrown(() -> instance.addConverter((Converter<?, ?>) null));
		assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldType(null, null, null));
		assertUnmodifiableExceptionThrown(() -> instance.addFormatterForFieldAnnotation(null));
		assertUnmodifiableExceptionThrown(() -> instance.addConverter(null, null, null));
		assertUnmodifiableExceptionThrown(() -> instance.addConverter((GenericConverter) null));
		assertUnmodifiableExceptionThrown(() -> instance.addConverterFactory(null));
		assertUnmodifiableExceptionThrown(() -> instance.removeConvertible(null, null));
	}
	//
	// @Test
	// void addPrinterWithTypeConvertsUsingTypeInformation() {
	// ApplicationConversionService service = new ApplicationConversionService();
	// Printer<?> printer = (object, locale) -> object.toString().toUpperCase(locale);
	// service.addPrinter(printer, ResolvableType.forClassWithGenerics(Printer.class,
	// ExampleRecord.class));
	// assertThat(service.convert(new ExampleRecord("test"),
	// String.class)).isEqualTo("TEST");
	// assertThatExceptionOfType(ConverterNotFoundException.class)
	// .isThrownBy(() -> service.convert(new UnknownRecord("test"), String.class));
	// assertThatIllegalArgumentException().isThrownBy(() -> service.addPrinter(printer))
	// .withMessageContaining("Unable to extract");
	// }
	//
	// @Test
	// void addParserWithTypeConvertsUsingTypeInformation() {
	// ApplicationConversionService service = new ApplicationConversionService();
	// Parser<?> parser = (text, locale) -> new ExampleRecord(text.toString());
	// service.addParser(parser, ResolvableType.forClassWithGenerics(Parser.class,
	// ExampleRecord.class));
	// assertThat(service.convert("test", ExampleRecord.class)).isEqualTo(new
	// ExampleRecord("test"));
	// assertThatExceptionOfType(ConverterNotFoundException.class)
	// .isThrownBy(() -> service.convert("test", UnknownRecord.class));
	// assertThatIllegalArgumentException().isThrownBy(() -> service.addParser(parser))
	// .withMessageContaining("Unable to extract");
	// }
	//
	// @Test
	// @SuppressWarnings("rawtypes")
	// void addFormatterWithType() {
	// ApplicationConversionService service = new ApplicationConversionService();
	// Formatter<?> formatter = new Formatter() {
	//
	// @Override
	// public String print(Object object, Locale locale) {
	// return object.toString().toUpperCase(locale);
	// }
	//
	// @Override
	// public Object parse(String text, Locale locale) throws ParseException {
	// return new ExampleRecord(text.toString());
	// }
	//
	// };
	// service.addFormatter(formatter,
	// ResolvableType.forClassWithGenerics(Formatter.class, ExampleRecord.class));
	// assertThat(service.convert(new ExampleRecord("test"),
	// String.class)).isEqualTo("TEST");
	// assertThat(service.convert("test", ExampleRecord.class)).isEqualTo(new
	// ExampleRecord("test"));
	// assertThatExceptionOfType(ConverterNotFoundException.class)
	// .isThrownBy(() -> service.convert(new UnknownRecord("test"), String.class));
	// assertThatExceptionOfType(ConverterNotFoundException.class)
	// .isThrownBy(() -> service.convert("test", UnknownRecord.class));
	// assertThatIllegalArgumentException().isThrownBy(() ->
	// service.addFormatter(formatter))
	// .withMessageContaining("Unable to extract");
	// }

	@Test
	@Disabled
	void addConverterWithType() {

	}

	@Test
	@Disabled
	void addConverterFactoryWithType() {

	}

	private void assertUnmodifiableExceptionThrown(ThrowingCallable throwingCallable) {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(throwingCallable)
			.withMessage("This ApplicationConversionService cannot be modified");
	}

	static class ExampleGenericConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}

	}

	static class ExampleConverter implements Converter<String, Integer> {

		@Override
		public Integer convert(String source) {
			return null;
		}

	}

	static class ExampleFormatter implements Formatter<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

		@Override
		public Integer parse(String text, Locale locale) throws ParseException {
			return null;
		}

	}

	static class ExampleParser implements Parser<Integer> {

		@Override
		public Integer parse(String text, Locale locale) throws ParseException {
			return null;
		}

	}

	static class ExamplePrinter implements Printer<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

	}

	@Configuration
	static class ConverterBeanMethodConfiguration {

		@Bean
		Converter<String, Integer> converter() {
			return Integer::valueOf;
		}

	}

	@Configuration
	static class PrinterBeanMethodConfiguration {

		@Bean
		Printer<Integer> printer() {
			return (object, locale) -> object.toString();
		}

	}

	@Configuration
	static class ParserBeanMethodConfiguration {

		@Bean
		Parser<Integer> parser() {
			return (text, locale) -> Integer.valueOf(text);
		}

	}

	record ExampleRecord(String value) {

		@Override
		public final String toString() {
			return value();
		}

	}

	record UnknownRecord(String value) {

	}

}
