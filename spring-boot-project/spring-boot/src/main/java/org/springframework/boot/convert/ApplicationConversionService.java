/*
 * Copyright 2012-2024 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * A specialization of {@link FormattingConversionService} configured by default with
 * converters and formatters appropriate for most Spring Boot applications.
 * <p>
 * Designed for direct instantiation but also exposes the static
 * {@link #addApplicationConverters} and
 * {@link #addApplicationFormatters(FormatterRegistry)} utility methods for ad-hoc use
 * against registry instance.
 *
 * @author Phillip Webb
 * @author Shixiong Guo
 * @since 2.0.0
 */
public class ApplicationConversionService extends FormattingConversionService {

	private static volatile ApplicationConversionService sharedInstance;

	private final boolean unmodifiable;

	public ApplicationConversionService() {
		this(null);
	}

	public ApplicationConversionService(StringValueResolver embeddedValueResolver) {
		this(embeddedValueResolver, false);
	}

	private ApplicationConversionService(StringValueResolver embeddedValueResolver, boolean unmodifiable) {
		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		configure(this);
		this.unmodifiable = unmodifiable;
	}

	/**
	 * Add {@link GenericConverter}, {@link Converter}, {@link Printer}, {@link Parser}
	 * and {@link Formatter} beans from the specified context.
	 * @param registry the service to register beans with
	 * @param beanFactory the bean factory to get the beans from
	 * @since 2.2.0
	 */
	public static void addBeans(FormatterRegistry registry, ListableBeanFactory beanFactory) {
		ConfigurableListableBeanFactory configurableBeanFactory = getConfigurableListableBeanFactory(beanFactory);
		Map<String, Object> beans = new LinkedHashMap<>();
		beans.putAll(beanFactory.getBeansOfType(GenericConverter.class));
		beans.putAll(beanFactory.getBeansOfType(Converter.class));
		beans.putAll(beanFactory.getBeansOfType(Printer.class));
		beans.putAll(beanFactory.getBeansOfType(Parser.class));
		beans.forEach((beanName, bean) -> {
			try {
				addToRegistry(registry, bean);
			}
			catch (IllegalArgumentException ex) {
				if (!tryAddFactoryMethodBean(registry, beanFactory, beanName, bean)) {
					throw ex;
				}
			}
		});
	}

	@Override
	public void addPrinter(Printer<?> printer) {
		addPrinter(printer, null);
	}

	public void addPrinter(Printer<?> printer, ResolvableType type) {
		assertModifiable();
		if (type != null) {
			super.addConverter(new PrinterAdapter(printer, type));
		}
		else {
			super.addPrinter(printer);
		}
	}

	@Override
	public void addParser(Parser<?> parser) {
		addParser(parser, null);
	}

	public void addParser(Parser<?> parser, ResolvableType parserType) {
		assertModifiable();
		if (parserType != null) {
			super.addConverter(new TypedParserAdapter(parser, parserType));
		}
		else {
			super.addParser(parser);
		}
	}

	@Override
	public void addFormatter(Formatter<?> formatter) {
		addFormatter(formatter, null);
	}

	public void addFormatter(Formatter<?> formatter, ResolvableType type) {
		assertModifiable();
		if (type != null) {
			addConverter(new PrinterAdapter(formatter, type));
			addConverter(new TypedParserAdapter(formatter, type));
		}
		else {
			super.addFormatter(formatter);
		}
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		assertModifiable();
		super.addFormatterForFieldType(fieldType, formatter);
	}

	@Override
	public void addConverter(Converter<?, ?> converter) {
		addConverter(converter, null);
	}

	public void addConverter(Converter<?, ?> converter, ResolvableType type) {
		assertModifiable();
		if (type != null) {
			super.addConverter(new TypedConverterAdapter(converter, type));
		}
		else {
			super.addConverter(converter);
		}
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
		assertModifiable();
		super.addFormatterForFieldType(fieldType, printer, parser);
	}

	@Override
	public void addFormatterForFieldAnnotation(
			AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
		assertModifiable();
		super.addFormatterForFieldAnnotation(annotationFormatterFactory);
	}

	@Override
	public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType,
			Converter<? super S, ? extends T> converter) {
		assertModifiable();
		super.addConverter(sourceType, targetType, converter);
	}

	@Override
	public void addConverter(GenericConverter converter) {
		assertModifiable();
		super.addConverter(converter);
	}

	@Override
	public void addConverterFactory(ConverterFactory<?, ?> factory) {
		addConverterFactory(factory, null);
	}

	public void addConverterFactory(ConverterFactory<?, ?> factory, ResolvableType factoryType) {
		assertModifiable();
		if (factoryType == null) {
			super.addConverterFactory(factory);
		}
		else {
			super.addConverter(null); // FIXME
		}
	}

	@Override
	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		assertModifiable();
		super.removeConvertible(sourceType, targetType);
	}

	private void assertModifiable() {
		if (this.unmodifiable) {
			throw new UnsupportedOperationException("This ApplicationConversionService cannot be modified");
		}
	}

	/**
	 * Return {@code true} if objects of {@code sourceType} can be converted to the
	 * {@code targetType} and the converter has {@code Object.class} as a supported source
	 * type.
	 * @param sourceType the source type to test
	 * @param targetType the target type to test
	 * @return if conversion happens through an {@code ObjectTo...} converter
	 * @since 2.4.3
	 */
	public boolean isConvertViaObjectSourceType(TypeDescriptor sourceType, TypeDescriptor targetType) {
		GenericConverter converter = getConverter(sourceType, targetType);
		Set<ConvertiblePair> pairs = (converter != null) ? converter.getConvertibleTypes() : null;
		if (pairs != null) {
			for (ConvertiblePair pair : pairs) {
				if (Object.class.equals(pair.getSourceType())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return a shared default application {@code ConversionService} instance, lazily
	 * building it once needed.
	 * <p>
	 * Note: This method actually returns an {@link ApplicationConversionService}
	 * instance. However, the {@code ConversionService} signature has been preserved for
	 * binary compatibility.
	 * @return the shared {@code ApplicationConversionService} instance (never
	 * {@code null})
	 */
	public static ConversionService getSharedInstance() {
		ApplicationConversionService sharedInstance = ApplicationConversionService.sharedInstance;
		if (sharedInstance == null) {
			synchronized (ApplicationConversionService.class) {
				sharedInstance = ApplicationConversionService.sharedInstance;
				if (sharedInstance == null) {
					sharedInstance = new ApplicationConversionService(null, true);
					ApplicationConversionService.sharedInstance = sharedInstance;
				}
			}
		}
		return sharedInstance;
	}

	/**
	 * Configure the given {@link FormatterRegistry} with formatters and converters
	 * appropriate for most Spring Boot applications.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given FormatterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void configure(FormatterRegistry registry) {
		DefaultConversionService.addDefaultConverters(registry);
		DefaultFormattingConversionService.addDefaultFormatters(registry);
		addApplicationFormatters(registry);
		addApplicationConverters(registry);
	}

	/**
	 * Add converters useful for most Spring Boot applications.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void addApplicationConverters(ConverterRegistry registry) {
		addDelimitedStringConverters(registry);
		registry.addConverter(new StringToDurationConverter());
		registry.addConverter(new DurationToStringConverter());
		registry.addConverter(new NumberToDurationConverter());
		registry.addConverter(new DurationToNumberConverter());
		registry.addConverter(new StringToPeriodConverter());
		registry.addConverter(new PeriodToStringConverter());
		registry.addConverter(new NumberToPeriodConverter());
		registry.addConverter(new StringToDataSizeConverter());
		registry.addConverter(new NumberToDataSizeConverter());
		registry.addConverter(new StringToFileConverter());
		registry.addConverter(new InputStreamSourceToByteArrayConverter());
		registry.addConverterFactory(new LenientStringToEnumConverterFactory());
		registry.addConverterFactory(new LenientBooleanToEnumConverterFactory());
		if (registry instanceof ConversionService conversionService) {
			addApplicationConverters(registry, conversionService);
		}
	}

	private static void addApplicationConverters(ConverterRegistry registry, ConversionService conversionService) {
		registry.addConverter(new CharSequenceToObjectConverter(conversionService));
	}

	/**
	 * Add converters to support delimited strings.
	 * @param registry the registry of converters to add to (must also be castable to
	 * ConversionService, e.g. being a {@link ConfigurableConversionService})
	 * @throws ClassCastException if the given ConverterRegistry could not be cast to a
	 * ConversionService
	 */
	public static void addDelimitedStringConverters(ConverterRegistry registry) {
		ConversionService service = (ConversionService) registry;
		registry.addConverter(new ArrayToDelimitedStringConverter(service));
		registry.addConverter(new CollectionToDelimitedStringConverter(service));
		registry.addConverter(new DelimitedStringToArrayConverter(service));
		registry.addConverter(new DelimitedStringToCollectionConverter(service));
	}

	/**
	 * Add formatters useful for most Spring Boot applications.
	 * @param registry the service to register default formatters with
	 */
	public static void addApplicationFormatters(FormatterRegistry registry) {
		registry.addFormatter(new CharArrayFormatter());
		registry.addFormatter(new InetAddressFormatter());
		registry.addFormatter(new IsoOffsetFormatter());
	}

	private static ConfigurableListableBeanFactory getConfigurableListableBeanFactory(ListableBeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableApplicationContext applicationContext) {
			return applicationContext.getBeanFactory();
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
			return configurableListableBeanFactory;
		}
		return null;
	}

	private static void addToRegistry(FormatterRegistry registry, Object bean) {
		if (bean instanceof GenericConverter) {
			registry.addConverter((GenericConverter) bean);
		}
		else if (bean instanceof Converter) {
			registry.addConverter((Converter<?, ?>) bean);
		}
		else if (bean instanceof Formatter) {
			registry.addFormatter((Formatter<?>) bean);
		}
		else if (bean instanceof Printer) {
			registry.addPrinter((Printer<?>) bean);
		}
		else if (bean instanceof Parser) {
			registry.addParser((Parser<?>) bean);
		}
	}

	private static boolean tryAddFactoryMethodBean(FormatterRegistry registry, ListableBeanFactory beanFactory,
			String beanName, Object bean) {
		ConfigurableListableBeanFactory clbf = getConfigurableListableBeanFactory(beanFactory);
		if (clbf == null) {
			return false;
		}
		if (!isFactoryMethod(clbf, beanName)) {
			return false;
		}
		if (bean instanceof Converter) {
			return addConverter(registry, clbf, beanName, (Converter<?, ?>) bean);
		}
		else if (bean instanceof Printer) {
			return addPrinter(registry, clbf, beanName, (Printer<?>) bean);
		}
		else if (bean instanceof Parser) {
			return addParser(registry, clbf, beanName, (Parser<?>) bean);
		}
		return false;
	}

	private static boolean isFactoryMethod(ConfigurableListableBeanFactory clbf, String beanName) {
		BeanDefinition bd = clbf.getMergedBeanDefinition(beanName);
		return bd.getFactoryMethodName() != null;
	}

	private static boolean addConverter(FormatterRegistry registry, ConfigurableListableBeanFactory beanFactory,
			String beanName, Converter<?, ?> converter) {
		XTypedConverter adapter = getConverterAdapter(beanFactory, beanName, converter);
		if (adapter == null) {
			return false;
		}
		registry.addConverter(adapter);
		return true;
	}

	private static XTypedConverter getConverterAdapter(ConfigurableListableBeanFactory beanFactory, String beanName,
			Converter<?, ?> converter) {
		ResolvableType[] types = getResolvableType(beanFactory, beanName);
		if (types.length < 2) {
			return null;
		}
		return new XTypedConverter(converter, types[0], types[1]);
	}

	private static ResolvableType[] getResolvableType(ConfigurableListableBeanFactory beanFactory, String beanName) {
		BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
		ResolvableType resolvableType = beanDefinition.getResolvableType();
		return resolvableType.getGenerics();
	}

	private static boolean addPrinter(FormatterRegistry registry, ConfigurableListableBeanFactory beanFactory,
			String beanName, Printer<?> printer) {
		XTypedPrinterConverter adapter = getPrinterAdapter(beanFactory, beanName, printer);
		if (adapter == null) {
			return false;
		}
		registry.addConverter(adapter);
		return true;
	}

	private static XTypedPrinterConverter getPrinterAdapter(ConfigurableListableBeanFactory beanFactory,
			String beanName, Printer<?> printer) {
		ResolvableType[] types = getResolvableType(beanFactory, beanName);
		if (types.length < 1) {
			return null;
		}
		ConversionService conversionService = beanFactory.getBean(ConversionService.class);
		return new XTypedPrinterConverter(types[0].resolve(), printer, conversionService);
	}

	private static boolean addParser(FormatterRegistry registry, ConfigurableListableBeanFactory beanFactory,
			String beanName, Parser<?> parser) {
		XTypedParserConverter adapter = getParserAdapter(beanFactory, beanName, parser);
		if (adapter == null) {
			return false;
		}
		registry.addConverter(adapter);
		return true;
	}

	private static XTypedParserConverter getParserAdapter(ConfigurableListableBeanFactory beanFactory, String beanName,
			Parser<?> parser) {
		ResolvableType[] types = getResolvableType(beanFactory, beanName);
		if (types.length < 1) {
			return null;
		}
		ConversionService conversionService = beanFactory.getBean(ConversionService.class);
		return new XTypedParserConverter(types[0].resolve(), parser, conversionService);
	}

	static abstract class Adapter implements ConditionalGenericConverter {

		private final Types types;

		Adapter(Types types) {
			this.types = types;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.types.asConvertiblePair());
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return (this.types.target().toClass() == targetType.getObjectType()
					&& matchesTargetType(targetType.getResolvableType()));
		}

		private boolean matchesTargetType(ResolvableType targetType) {
			ResolvableType ours = this.types.target();
			return targetType.getType() instanceof Class || targetType.isAssignableFrom(ours)
					|| this.types.target().hasUnresolvableGenerics();
		}

		protected final boolean conditionalConverterCandidateMatches(Object conditionalConverterCandidate,
				TypeDescriptor sourceType, TypeDescriptor targetType) {
			return ((conditionalConverterCandidate instanceof ConditionalConverter conditionalConverter)
					&& conditionalConverter.matches(sourceType, targetType)) || true;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		protected final Types types() {
			return this.types;
		}

	}

	static class PrinterAdapter extends Adapter {

		PrinterAdapter(Printer<?> printer, ResolvableType parserType) {
			super(Types.fromGenericToString(printer, parserType, Parser.class));
		}

	}

	static class ParserAdapter extends Adapter {

		ParserAdapter(Printer<?> printer, ResolvableType printerType) {
			super(Types.fromStringToGeneric(printer, printerType, Printer.class));
		}

	}

	static final class ConverterAdapter extends Adapter {

		public ConverterAdapter(Converter<?, ?> converter, ResolvableType converterType) {
			super(Types.fromGenerics(converter, converterType, Converter.class));
		}

	}

	private static final class ConverterFactoryAdapter extends Adapter {

		private final ConverterFactory<Object, Object> converterFactory;

		@SuppressWarnings("unchecked")
		ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory, ResolvableType converterFactoryType) {
			super(null);
			this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return super.matches(sourceType, targetType)
					&& conditionalConverterCandidateMatches(this.converterFactory, sourceType, targetType)
					&& conditionalConverterCandidateMatches(getConverter(targetType::getType), sourceType, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				// return convertNullSource(sourceType, targetType);
			}
			return getConverter(targetType::getObjectType).convert(source);
		}

		private Converter<Object, ?> getConverter(Supplier<Class<?>> typeSupplier) {
			return this.converterFactory.getConverter(typeSupplier.get());
		}

		@Override
		public String toString() {
			return types() + " : " + this.converterFactory;
		}

	}

	private static class TypedParserAdapter implements GenericConverter {

		TypedParserAdapter(Parser<?> parser, ResolvableType type) {
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

	}

	@SuppressWarnings("unchecked")
	private static final class XTypedConverter implements ConditionalGenericConverter {

		private final Converter<Object, Object> converter;

		private final ConvertiblePair typeInfo;

		private final ResolvableType targetType;

		XTypedConverter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType) {
			this.converter = (Converter<Object, Object>) converter;
			this.typeInfo = new ConvertiblePair(sourceType.toClass(), targetType.toClass());
			this.targetType = targetType;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (this.typeInfo.getTargetType() != targetType.getObjectType()) {
				return false;
			}
			ResolvableType rt = targetType.getResolvableType();
			if (!(rt.getType() instanceof Class) && !rt.isAssignableFrom(this.targetType)
					&& !this.targetType.hasUnresolvableGenerics()) {
				return false;
			}
			return !(this.converter instanceof ConditionalConverter)
					|| ((ConditionalConverter) this.converter).matches(sourceType, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converter.convert(source);
		}

		@Override
		public String toString() {
			return (this.typeInfo + " : " + this.converter);
		}

		private Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (targetType.getObjectType() == Optional.class) {
				return Optional.empty();
			}
			return null;
		}

	}

	private static class XTypedPrinterConverter implements GenericConverter {

		private final Class<?> fieldType;

		private final TypeDescriptor printerObjectType;

		@SuppressWarnings("rawtypes")
		private final Printer printer;

		private final ConversionService conversionService;

		XTypedPrinterConverter(Class<?> fieldType, Printer<?> printer, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.printerObjectType = TypeDescriptor.valueOf(resolvePrinterObjectType(printer));
			this.printer = printer;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (!sourceType.isAssignableTo(this.printerObjectType)) {
				source = this.conversionService.convert(source, sourceType, this.printerObjectType);
			}
			if (source == null) {
				return "";
			}
			return this.printer.print(source, LocaleContextHolder.getLocale());
		}

		private Class<?> resolvePrinterObjectType(Printer<?> printer) {
			return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
		}

		@Override
		public String toString() {
			return (this.fieldType.getName() + " -> " + String.class.getName() + " : " + this.printer);
		}

	}

	private static class XTypedParserConverter implements GenericConverter {

		private final Class<?> fieldType;

		private final Parser<?> parser;

		private final ConversionService conversionService;

		XTypedParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.parser = parser;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String text = (String) source;
			if (!StringUtils.hasText(text)) {
				return null;
			}
			Object result;
			try {
				result = this.parser.parse(text, LocaleContextHolder.getLocale());
			}
			catch (IllegalArgumentException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
			TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
			if (!resultType.isAssignableTo(targetType)) {
				result = this.conversionService.convert(result, resultType, targetType);
			}
			return result;
		}

		@Override
		public String toString() {
			return (String.class.getName() + " -> " + this.fieldType.getName() + ": " + this.parser);
		}

	}

	static record Types(ResolvableType source, ResolvableType target) {

		public ConvertiblePair asConvertiblePair() {
			return new ConvertiblePair(source().toClass(), target().toClass());
		}

		public static Types fromGenerics(Object instance, ResolvableType type, Class<?> as) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		public static Types fromStringToGeneric(Object instance, ResolvableType type, Class<?> as) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		public static Types fromGenericToString(Object instance, ResolvableType type, Class<?> as) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}
	}

}
