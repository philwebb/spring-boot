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

package org.springframework.boot.context.properties.bind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.boot.context.properties.bind.convert.BinderConversionService;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A container object which Binds objects from one or more
 * {@link ConfigurationPropertySource ConfigurationPropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class Binder {

	private static final Set<Class<?>> NON_BEAN_CLASSES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList(Object.class, Class.class)));

	private static final List<BeanBinder> BEAN_BINDERS;

	static {
		List<BeanBinder> beanBinders = new ArrayList<>();
		beanBinders.add(new JavaBeanBinder());
		BEAN_BINDERS = Collections.unmodifiableList(beanBinders);
	}

	private final Iterable<ConfigurationPropertySource> sources;

	private final PlaceholdersResolver placeholdersResolver;

	private final BinderConversionService conversionService;

	/**
	 * Create a new {@link Binder} instance for the specified sources. A
	 * {@link DefaultFormattingConversionService} will be used for all conversion.
	 * @param sources the sources used for binding
	 */
	public Binder(ConfigurationPropertySource... sources) {
		this(Arrays.asList(sources), null, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources. A
	 * {@link DefaultFormattingConversionService} will be used for all conversion.
	 * @param sources the sources used for binding
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources) {
		this(sources, null, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property place-holders
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources,
			PlaceholdersResolver placeholdersResolver) {
		this(sources, placeholdersResolver, null);
	}

	/**
	 * Create a new {@link Binder} instance for the specified sources.
	 * @param sources the sources used for binding
	 * @param placeholdersResolver strategy to resolve any property place-holders
	 * @param conversionService the conversion service to convert values
	 */
	public Binder(Iterable<ConfigurationPropertySource> sources,
			PlaceholdersResolver placeholdersResolver,
			ConversionService conversionService) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources = sources;
		this.placeholdersResolver = (placeholdersResolver != null ? placeholdersResolver
				: PlaceholdersResolver.NONE);
		this.conversionService = (conversionService instanceof BinderConversionService
				? (BinderConversionService) conversionService
				: new BinderConversionService(conversionService));
	}

	/**
	 * Bind the specified target {@link Class} using this binders
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target class
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(String name, Class<T> target) {
		return bind(name, Bindable.of(target));
	}

	/**
	 * Bind the specified target {@link Bindable} using this binders
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(String name, Bindable<T> target) {
		return bind(ConfigurationPropertyName.of(name), target, null);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binders
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 * @see #bind(ConfigurationPropertyName, Bindable, BindHandler)
	 */
	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target) {
		return bind(name, target, null);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binders
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler (may be {@code null})
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 */
	public <T> BindResult<T> bind(String name, Bindable<T> target, BindHandler handler) {
		return bind(ConfigurationPropertyName.of(name), target, handler);
	}

	/**
	 * Bind the specified target {@link Bindable} using this binders
	 * {@link ConfigurationPropertySource property sources}.
	 * @param name the configuration property name to bind
	 * @param target the target bindable
	 * @param handler the bind handler (may be {@code null})
	 * @param <T> the bound type
	 * @return the binding result (never {@code null})
	 */
	public <T> BindResult<T> bind(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(target, "Target must not be null");
		handler = (handler != null ? handler : BindHandler.DEFAULT);
		Context context = new Context();
		T bound = bind(name, target, handler, context);
		return BindResult.of(bound);
	}

	protected final <T> T bind(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context) {
		try {
			if (!handler.onStart(name, target, context)) {
				return null;
			}
			Object bound = bindObject(name, target, handler, context);
			return handleBindResult(name, target, handler, context, bound);
		}
		catch (Exception ex) {
			return handleBindError(name, target, handler, context, ex);
		}
	}

	private <T> T handleBindResult(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context, Object result) throws Exception {
		result = convert(result, target);
		if (result != null) {
			result = handler.onSuccess(name, target, context, result);
			result = convert(result, target);
		}
		handler.onFinish(name, target, context, result);
		return convert(result, target);
	}

	private <T> T handleBindError(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context, Exception error) {
		try {
			Object result = handler.onFailure(name, target, context, error);
			return convert(result, target);
		}
		catch (Exception ex) {
			if (ex instanceof BindException) {
				throw (BindException) ex;
			}
			throw new BindException(name, target, context.getConfigurationProperty(), ex);
		}
	}

	private <T> T convert(Object value, Bindable<T> target) {
		if (value == null) {
			return null;
		}
		return this.conversionService.convert(value, target);
	}

	private <T> Object bindObject(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context) throws Exception {
		AggregateBinder<?> aggregateBinder = getAggregateBinder(target, context);
		if (aggregateBinder != null) {
			return bindAggregate(name, target, handler, context, aggregateBinder);
		}
		ConfigurationProperty property = findProperty(name);
		if (property != null) {
			return bindProperty(name, target, handler, context, property);
		}
		return bindBean(name, target, handler, context);
	}

	private AggregateBinder<?> getAggregateBinder(Bindable<?> target, Context context) {
		if (Map.class.isAssignableFrom(target.getType().resolve())) {
			return new MapBinder(context);
		}
		if (Collection.class.isAssignableFrom(target.getType().resolve())) {
			return new CollectionBinder(context);
		}
		if (target.getType().isArray()) {
			return new ArrayBinder(context);
		}
		return null;
	}

	private <T> Object bindAggregate(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context, AggregateBinder<?> aggregateBinder) {
		AggregateElementBinder elementBinder = (itemName, itemTarget, source) -> {
			Binder binder = (source == null ? Binder.this : new Binder(source));
			return binder.bind(itemName, itemTarget, handler, context.increaseDepth());
		};
		return aggregateBinder.bind(name, target, elementBinder);
	}

	private ConfigurationProperty findProperty(ConfigurationPropertyName name) {
		return streamSources().map((source) -> source.getConfigurationProperty(name))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	private <T> Object bindProperty(ConfigurationPropertyName name, Bindable<T> target,
			BindHandler handler, Context context, ConfigurationProperty property) {
		context.setConfigurationProperty(property);
		Object result = property.getValue();
		result = this.placeholdersResolver.resolvePlaceholders(result);
		result = this.conversionService.convert(result, target);
		return result;
	}

	private Object bindBean(ConfigurationPropertyName name, Bindable<?> target,
			BindHandler handler, Context context) throws Exception {
		BeanPropertyBinder propertyBinder = getPropertyBinder(context.increaseDepth(),
				name, handler);
		boolean noKnownBindableProperties = !propertyBinder.hasKnownBindableProperties();
		if (noKnownBindableProperties && isUnbindableBean(target)) {
			return null;
		}
		Class<?> type = target.getType().resolve();
		if (context.hasBoundBean(type)) {
			return null;
		}
		context.setBean(type);
		return BEAN_BINDERS.stream().map((b) -> b.bind(target, propertyBinder))
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	private BeanPropertyBinder getPropertyBinder(Context context,
			ConfigurationPropertyName name, BindHandler handler) {
		return new BeanPropertyBinder() {

			@Override
			public boolean hasKnownBindableProperties() {
				return streamSources()
						.flatMap((s) -> s.filter(name::isAncestorOf).stream()).findAny()
						.isPresent();
			}

			@Override
			public Object bindProperty(String propertyName, Bindable<?> target) {
				return Binder.this.bind(name.append(propertyName), target, handler,
						context);
			}

		};
	}

	private boolean isUnbindableBean(Bindable<?> target) {
		Class<?> resolved = target.getType().resolve();
		if (NON_BEAN_CLASSES.contains(resolved)) {
			return true;
		}
		String packageName = ClassUtils.getPackageName(resolved);
		return packageName.startsWith("java.");
	}

	private Stream<ConfigurationPropertySource> streamSources() {
		return StreamSupport.stream(this.sources.spliterator(), false);
	}

	/**
	 * Create a new {@link Binder} instance from the specified environment.
	 * @param environment the environment (must be a {@link ConfigurableEnvironment})
	 * @return a {@link Binder} instance
	 */
	public static Binder get(Environment environment) {
		Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
		return new Binder(
				ConfigurationPropertySources.get((ConfigurableEnvironment) environment),
				new PropertySourcesPlaceholdersResolver(environment));
	}

	/**
	 * {@link BindContext} implementation.
	 */
	final class Context implements BindContext {

		private final Context parent;

		private ConfigurationProperty configurationProperty;

		private Class<?> bean;

		Context() {
			this(null, null);
		}

		Context(Context parent, Class<?> bean) {
			this.parent = parent;
			this.bean = bean;
		}

		public boolean hasBoundBean(Class<?> bean) {
			if (this.bean != null && this.bean.equals(bean)) {
				return true;
			}
			return (this.parent != null ? this.parent.hasBoundBean(bean) : false);
		}

		public void setBean(Class<?> bean) {
			this.bean = bean;
		}

		public Context increaseDepth() {
			return new Context(this, null);
		}

		@Override
		public int getDepth() {
			return (this.parent == null ? 0 : this.parent.getDepth() + 1);
		}

		@Override
		public Iterable<ConfigurationPropertySource> getSources() {
			return Binder.this.sources;
		}

		@Override
		public ConfigurationProperty getConfigurationProperty() {
			return this.configurationProperty;
		}

		public void setConfigurationProperty(
				ConfigurationProperty configurationProperty) {
			this.configurationProperty = configurationProperty;
		}

		@Override
		public PlaceholdersResolver getPlaceholdersResolver() {
			return Binder.this.placeholdersResolver;
		}

		@Override
		public BinderConversionService getConversionService() {
			return Binder.this.conversionService;
		}

	}

}
