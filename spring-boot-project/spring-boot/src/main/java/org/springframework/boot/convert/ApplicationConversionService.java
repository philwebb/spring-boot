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
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
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
import org.springframework.util.Assert;
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

	private static ConfigurableListableBeanFactory getConfigurableListableBeanFactory(ListableBeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableApplicationContext applicationContext) {
			return applicationContext.getBeanFactory();
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory) {
			return configurableListableBeanFactory;
		}
		return null;
	}

	@Override
	public void addPrinter(Printer<?> printer) {
		assertModifiable();
		super.addPrinter(printer);
	}

	@Override
	public void addParser(Parser<?> parser) {
		assertModifiable();
		super.addParser(parser);
	}

	@Override
	public void addFormatter(Formatter<?> formatter) {
		assertModifiable();
		super.addFormatter(formatter);
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		assertModifiable();
		super.addFormatterForFieldType(fieldType, formatter);
	}

	@Override
	public void addConverter(Converter<?, ?> converter) {
		assertModifiable();
		super.addConverter(converter);
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
		assertModifiable();
		super.addConverterFactory(factory);
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

	/**
	 * Add {@link Printer}, {@link Parser}, {@link Formatter}, {@link Converter},
	 * {@link ConverterFactory}, {@link GenericConverter}, and beans from the specified
	 * bean factory.
	 * @param registry the service to register beans with
	 * @param beanFactory the bean factory to get the beans from
	 * @since 2.2.0
	 */
	public static void addBeans(FormatterRegistry registry, ListableBeanFactory beanFactory) {
		addBeans(registry, beanFactory, null);
	}

	/**
	 * Add {@link Printer}, {@link Parser}, {@link Formatter}, {@link Converter},
	 * {@link ConverterFactory}, {@link GenericConverter}, and beans from the specified
	 * bean factory.
	 * @param registry the service to register beans with
	 * @param beanFactory the bean factory to get the beans from
	 * @param qualifier the qualifier required on the beans or {@code null}
	 * @since 3.5.0
	 */
	public static void addBeans(FormatterRegistry registry, ListableBeanFactory beanFactory, String qualifier) {
		ConfigurableListableBeanFactory configurableBeanFactory = getConfigurableListableBeanFactory(beanFactory);
		collectBeans(beanFactory, qualifier).forEach((beanName, bean) -> {
			BeanDefinition beanDefinition = (configurableBeanFactory != null)
					? configurableBeanFactory.getMergedBeanDefinition(beanName) : null;
			ResolvableType type = (beanDefinition != null) ? beanDefinition.getResolvableType() : null;
			addBean(registry, bean, type);
		});
	}

	private static Map<String, Object> collectBeans(ListableBeanFactory beanFactory, String qualifier) {
		Map<String, Object> beans = new LinkedHashMap<>();
		beans.putAll(getBeans(beanFactory, Printer.class, qualifier));
		beans.putAll(getBeans(beanFactory, Parser.class, qualifier));
		beans.putAll(getBeans(beanFactory, Formatter.class, qualifier));
		beans.putAll(getBeans(beanFactory, Converter.class, qualifier));
		beans.putAll(getBeans(beanFactory, ConverterFactory.class, qualifier));
		beans.putAll(getBeans(beanFactory, GenericConverter.class, qualifier));
		return beans;
	}

	private static <T> Map<String, T> getBeans(ListableBeanFactory beanFactory, Class<T> type, String qualifier) {
		return (!StringUtils.hasLength(qualifier)) ? beanFactory.getBeansOfType(type)
				: BeanFactoryAnnotationUtils.qualifiedBeansOfType(beanFactory, type, qualifier);
	}

	static void addBean(FormatterRegistry registry, Object bean, ResolvableType beanType) {
		if (bean instanceof GenericConverter genericConverterBean) {
			addBean(registry, genericConverterBean, beanType, GenericConverter.class, registry::addConverter, null);
		}
		else if (bean instanceof Converter<?, ?> converterBean) {
			addBean(registry, converterBean, beanType, Converter.class, registry::addConverter,
					() -> registry.addConverter(new ConverterBeanAdapter(converterBean, beanType)));
		}
		else if (bean instanceof ConverterFactory<?, ?> converterBean) {
			addBean(registry, converterBean, beanType, ConverterFactory.class, registry::addConverterFactory,
					() -> registry.addConverter(new ConverterFactoryBeanAdapter(converterBean, beanType)));
		}
		else if (bean instanceof Formatter<?> formatterBean) {
			addBean(registry, formatterBean, beanType, Formatter.class, registry::addFormatter, () -> {
				registry.addConverter(new PrinterBeanAdapter(formatterBean, beanType, registry));
				registry.addConverter(new ParserBeanAdapter(formatterBean, beanType, registry));
			});
		}
		else if (bean instanceof Printer<?> printerBean) {
			addBean(registry, printerBean, beanType, Printer.class, registry::addPrinter,
					() -> registry.addConverter(new PrinterBeanAdapter(printerBean, beanType, registry)));
		}
		else if (bean instanceof Parser<?> parserBean) {
			addBean(registry, parserBean, beanType, Parser.class, registry::addParser,
					() -> registry.addConverter(new ParserBeanAdapter(parserBean, beanType, registry)));
		}
	}

	private static <B, T> void addBean(FormatterRegistry registry, B bean, ResolvableType beanType, Class<T> type,
			Consumer<B> standardRegistrar, Runnable beanRegistrar) {
		if (beanType != null && beanRegistrar != null
				&& ResolvableType.forInstance(bean).as(type).hasUnresolvableGenerics()) {
			beanRegistrar.run();
		}
		else {
			standardRegistrar.accept(bean);
		}
	}

	/**
	 * Based class for adapters that adapt a bean to a {@link GenericConverter}.
	 */
	static abstract class BeanAdapter implements ConditionalGenericConverter {

		private final Object bean;

		private final ConvertibleTypes convertibleTypes;

		BeanAdapter(Object bean, ResolvableType beanType, ConvertibleTypes convertibleTypes) {
			Assert.isInstanceOf(beanType.toClass(), bean);
			this.bean = bean;
			this.convertibleTypes = convertibleTypes;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.convertibleTypes.asConvertiblePair());
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return (this.convertibleTypes.target().toClass() == targetType.getObjectType()
					&& matchesTargetType(targetType.getResolvableType()));
		}

		private boolean matchesTargetType(ResolvableType targetType) {
			ResolvableType ours = this.convertibleTypes.target();
			return targetType.getType() instanceof Class || targetType.isAssignableFrom(ours)
					|| this.convertibleTypes.target().hasUnresolvableGenerics();
		}

		protected final boolean conditionalConverterCandidateMatches(Object conditionalConverterCandidate,
				TypeDescriptor sourceType, TypeDescriptor targetType) {
			return ((conditionalConverterCandidate instanceof ConditionalConverter conditionalConverter)
					&& conditionalConverter.matches(sourceType, targetType)) || true;
		}

		protected final Object convert(Object source, TypeDescriptor targetType, Converter<Object, ?> converter) {
			return (source != null) ? converter.convert(source) : convertNull(targetType);
		}

		private Object convertNull(TypeDescriptor targetType) {
			return (targetType.getObjectType() != Optional.class) ? null : Optional.empty();
		}

		protected final ConvertibleTypes types() {
			return this.convertibleTypes;
		}

		@Override
		public String toString() {
			return this.convertibleTypes + " : " + this.bean;
		}

	}

	/**
	 * Adapts a {@link Printer} bean to a {@link GenericConverter}.
	 */
	static class PrinterBeanAdapter extends BeanAdapter {

		private final Printer<Object> printer;

		private final ConversionService conversionService;

		@SuppressWarnings("unchecked")
		PrinterBeanAdapter(Printer<?> bean, ResolvableType beanType, FormatterRegistry registry) {
			super(bean, beanType, ConvertibleTypes.fromGenericToString(beanType, Printer.class));
			this.printer = (Printer<Object>) bean;
			Assert.isInstanceOf(ConversionService.class, registry, "'registry' must implement ConversionService");
			this.conversionService = (ConversionService) registry;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (!types().source().isAssignableFrom(sourceType.getResolvableType())) {
				TypeDescriptor printerInputType = new TypeDescriptor(types().source(), null,
						sourceType.getAnnotations());
				source = this.conversionService.convert(source, sourceType, printerInputType);
			}
			return (source != null) ? print(source) : "";
		}

		private String print(Object object) {
			return this.printer.print(object, LocaleContextHolder.getLocale());
		}

	}

	/**
	 * Adapts a {@link Parser} bean to a {@link GenericConverter}.
	 */
	static class ParserBeanAdapter extends BeanAdapter {

		private final Parser<?> parser;

		private final ConversionService conversionService;

		ParserBeanAdapter(Parser<?> bean, ResolvableType beanType, FormatterRegistry registry) {
			super(bean, beanType, ConvertibleTypes.fromStringToGeneric(beanType, Parser.class));
			this.parser = bean;
			Assert.isInstanceOf(ConversionService.class, registry, "'registry' must implement ConversionService");
			this.conversionService = (ConversionService) registry;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			String text = (String) source;
			if (!StringUtils.hasText(text)) {
				return null;
			}
			Object result = parse(text);
			if (!types().target().isAssignableFrom(targetType.getResolvableType())) {
				TypeDescriptor parserOutputType = new TypeDescriptor(types().target(), null,
						targetType.getAnnotations());
				result = this.conversionService.convert(result, parserOutputType, targetType);
			}
			return result;
		}

		private Object parse(String text) {
			try {
				return this.parser.parse(text, LocaleContextHolder.getLocale());
			}
			catch (IllegalArgumentException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
		}

	}

	/**
	 * Adapts a {@link Converter} bean to a {@link GenericConverter}.
	 */
	static final class ConverterBeanAdapter extends BeanAdapter {

		private final Converter<Object, Object> converter;

		@SuppressWarnings("unchecked")
		public ConverterBeanAdapter(Converter<?, ?> bean, ResolvableType beanType) {
			super(bean, beanType, ConvertibleTypes.fromGenerics(beanType, Converter.class));
			this.converter = (Converter<Object, Object>) bean;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return super.matches(sourceType, targetType)
					&& conditionalConverterCandidateMatches(this.converter, sourceType, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return convert(source, targetType, this.converter);
		}

	}

	/**
	 * Adapts a {@link ConverterFactory} bean to a {@link GenericConverter}.
	 */
	private static final class ConverterFactoryBeanAdapter extends BeanAdapter {

		private final ConverterFactory<Object, Object> converterFactory;

		@SuppressWarnings("unchecked")
		ConverterFactoryBeanAdapter(ConverterFactory<?, ?> bean, ResolvableType beanType) {
			super(bean, beanType, ConvertibleTypes.fromGenerics(beanType, ConverterFactory.class));
			this.converterFactory = (ConverterFactory<Object, Object>) bean;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return super.matches(sourceType, targetType)
					&& conditionalConverterCandidateMatches(this.converterFactory, sourceType, targetType)
					&& conditionalConverterCandidateMatches(getConverter(targetType::getType), sourceType, targetType);
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return convert(source, targetType, getConverter(targetType::getObjectType));
		}

		private Converter<Object, ?> getConverter(Supplier<Class<?>> typeSupplier) {
			return this.converterFactory.getConverter(typeSupplier.get());
		}

		@Override
		public String toString() {
			return types() + " : " + this.converterFactory;
		}

	}

	/**
	 * Convertible type information as extracted from bean generics.
	 *
	 * @param source the source type
	 * @param target the target type
	 */
	static record ConvertibleTypes(ResolvableType source, ResolvableType target) {

		private static final ResolvableType STRING = ResolvableType.forClass(String.class);

		ConvertibleTypes {
			Assert.notNull(source.resolve(), "'source' cannot be resolved");
			Assert.notNull(target.resolve(), "'target' cannot be resolved");
		}

		public ConvertiblePair asConvertiblePair() {
			return new ConvertiblePair(source().toClass(), target().toClass());
		}

		@Override
		public final String toString() {
			return source() + " -> " + target();
		}

		public static ConvertibleTypes fromGenerics(ResolvableType instanceType, Class<?> type) {
			ResolvableType[] generics = instanceType.as(type).getGenerics();
			return new ConvertibleTypes(generics[0], generics[1]);
		}

		public static ConvertibleTypes fromStringToGeneric(ResolvableType instanceType, Class<?> type) {
			return new ConvertibleTypes(STRING, instanceType.as(type).getGeneric());
		}

		public static ConvertibleTypes fromGenericToString(ResolvableType instanceType, Class<?> type) {
			return new ConvertibleTypes(instanceType.as(type).getGeneric(), STRING);
		}

	}

}
