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

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanBinder} for mutable Java Beans.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class JavaBeanBinder implements BeanBinder {

	@Override
	public <T> T bind(Bindable<T> target, boolean hasKnownBindableProperties,
			BeanPropertyBinder propertyBinder) {
		Bean<T> bean = Bean.get(target, hasKnownBindableProperties);
		if (bean == null) {
			return null;
		}
		boolean bound = bind(target, propertyBinder, bean);
		return (bound ? bean.getInstance(target) : null);
	}

	private <T> boolean bind(Bindable<T> target, BeanPropertyBinder propertyBinder,
			Bean<T> bean) {
		boolean bound = false;
		for (Map.Entry<String, BeanProperty<T>> entry : bean.getProperties().entrySet()) {
			bound |= bind(target, propertyBinder, entry.getValue());
		}
		return bound;
	}

	private <T> boolean bind(Bindable<T> target, BeanPropertyBinder propertyBinder,
			BeanProperty<T> property) {
		String propertyName = property.getName();
		ResolvableType type = property.getType();
		Supplier<Object> value = property.getValue(target);
		Annotation[] annotations = property.getAnnotations();
		Object bound = propertyBinder.bindProperty(propertyName,
				Bindable.of(type).withSuppliedValue(value).withAnnotations(annotations));
		if (bound == null) {
			return false;
		}
		if (property.isSettable()) {
			property.setValue(target, bound);
		}
		else if (value == null || !bound.equals(value.get())) {
			throw new IllegalStateException(
					"No setter found for property: " + property.getName());
		}
		return true;
	}

	/**
	 * The bean being bound.
	 */
	private static final class Bean<T> {

		private static Bean<?> cached;

		private final Class<?> type;

		private final Map<String, BeanProperty<T>> properties = new LinkedHashMap<>();

		private T instance;

		Bean(Class<?> type) {
			this.type = type;
			putProperties(type);
		}

		private void putProperties(Class<?> type) {
			ReflectionUtils.doWithMethods(type, this::addGetter, this::isGetter);
			ReflectionUtils.doWithMethods(type, this::addSetter, this::isSetter);
			ReflectionUtils.doWithFields(type, this::addField);
		}

		private boolean isGetter(Method method) {
			return (isPropertyCandidate(method) && (method.getParameterCount() == 0)
					&& (method.getName().startsWith("get")
							|| (method.getName().startsWith("is")
									&& Boolean.TYPE.equals(method.getReturnType()))));
		}

		private boolean isSetter(Method method) {
			return (isPropertyCandidate(method) && (method.getParameterCount() == 1)
					&& method.getName().startsWith("set"));
		}

		private boolean isPropertyCandidate(Method method) {
			return Modifier.isPublic(method.getModifiers())
					&& !Object.class.equals(method.getDeclaringClass())
					&& !Class.class.equals(method.getDeclaringClass());
		}

		private void addGetter(Method method) {
			String name = method.getName();
			name = (name.startsWith("get") ? name.substring(3) : name);
			name = (name.startsWith("is") ? name.substring(2) : name);
			name = Introspector.decapitalize(name);
			this.properties.computeIfAbsent(name,
					(key) -> new BeanProperty<T>(this, key, method));
		}

		private void addSetter(Method method) {
			String name = method.getName();
			name = (name.startsWith("set") ? name.substring(3) : name);
			name = Introspector.decapitalize(name);
			BeanProperty<T> property = this.properties.computeIfAbsent(name,
					(key) -> new BeanProperty<T>(this, key, null));
			property.addSetter(method);
		}

		private void addField(Field field) {
			BeanProperty<T> property = this.properties.get(field.getName());
			if (property != null) {
				property.addField(field);
			}
		}

		public Class<?> getType() {
			return this.type;
		}

		public Map<String, BeanProperty<T>> getProperties() {
			return this.properties;
		}

		@SuppressWarnings("unchecked")
		public T getInstance(Bindable<T> bindable) {
			if (this.instance == null) {
				if (bindable.getValue() != null) {
					this.instance = bindable.getValue().get();
				}
				if (this.instance == null) {
					this.instance = (T) BeanUtils.instantiateClass(this.type);
				}
			}
			return this.instance;
		}

		@SuppressWarnings("unchecked")
		public static <T> Bean<T> get(Bindable<T> bindable,
				boolean useExistingValueForType) {
			Class<?> type = bindable.getType().resolve();
			Supplier<T> value = bindable.getValue();
			if (value == null && !isInstantiatable(type)) {
				return null;
			}
			if (useExistingValueForType && value != null) {
				T instance = value.get();
				type = (instance != null ? instance.getClass() : type);
			}
			Bean<?> bean = Bean.cached;
			if (bean == null || !type.equals(bean.getType())) {
				bean = new Bean<>(type);
				cached = bean;
			}
			return (Bean<T>) bean;
		}

		private static boolean isInstantiatable(Class<?> type) {
			if (type.isInterface()) {
				return false;
			}
			try {
				type.getDeclaredConstructor();
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}

	}

	/**
	 * A bean property being bound.
	 */
	private static final class BeanProperty<T> {

		private final Bean<T> bean;

		private final String name;

		private final Method getter;

		private Method setter;

		private Field field;

		public BeanProperty(Bean<T> bean, String name, Method getter) {
			this.bean = bean;
			this.name = BeanPropertyName.toDashedForm(name);
			this.getter = getter;
		}

		public void addSetter(Method setter) {
			if (this.setter == null) {
				this.setter = setter;
			}
		}

		public void addField(Field field) {
			if (this.field == null) {
				this.field = field;
			}
		}

		public String getName() {
			return this.name;
		}

		public ResolvableType getType() {
			if (this.setter != null) {
				return ResolvableType.forMethodParameter(this.setter, 0);
			}
			return ResolvableType.forMethodReturnType(this.getter);
		}

		public Annotation[] getAnnotations() {
			try {
				return (this.field == null ? null : this.field.getDeclaredAnnotations());
			}
			catch (Exception ex) {
				return null;
			}
		}

		public Supplier<Object> getValue(Bindable<T> bindable) {
			if (this.getter == null) {
				return null;
			}
			return () -> {
				try {
					this.getter.setAccessible(true);
					return this.getter.invoke(this.bean.getInstance(bindable));
				}
				catch (Exception ex) {
					throw new IllegalStateException(
							"Unable to get value for property " + this.name, ex);
				}
			};
		}

		public boolean isSettable() {
			return this.setter != null;
		}

		public void setValue(Bindable<T> bindable, Object value) {
			try {
				this.setter.setAccessible(true);
				this.setter.invoke(this.bean.getInstance(bindable), value);
			}
			catch (Exception ex) {
				throw new IllegalStateException(
						"Unable to set value for property " + this.name, ex);
			}
		}

	}

}
