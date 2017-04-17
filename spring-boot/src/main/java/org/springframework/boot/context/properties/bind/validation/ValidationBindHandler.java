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

package org.springframework.boot.context.properties.bind.validation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

/**
 * {@link BindHandler} to apply {@link Validator Validators} to bound results.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class ValidationBindHandler extends AbstractBindHandler {

	private final Validator[] validators;

	private boolean validate;

	private Set<ConfigurationProperty> bound = new LinkedHashSet<>();

	public ValidationBindHandler(Validator... validators) {
		super();
		this.validators = validators;
	}

	public ValidationBindHandler(BindHandler parent, Validator... validators) {
		super(parent);
		this.validators = validators;
	}

	@Override
	public boolean onStart(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context) {
		if (context.getDepth() == 0) {
			this.validate = shouldValidate(target);
		}
		return super.onStart(name, target, context);
	}

	private boolean shouldValidate(Bindable<?> target) {
		Validated annotation = AnnotationUtils.findAnnotation(target.getBoxedType().resolve(),
				Validated.class);
		return (annotation != null);
	}

	@Override
	public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, ConfigurationProperty property, Object result) {
		if (property != null) {
			this.bound.add(property);
		}
		return super.onSuccess(name, target, context, property, result);
	}

	@Override
	public void onFinish(ConfigurationPropertyName name, Bindable<?> target,
			BindContext context, Object result) throws Exception {
		if (this.validate && result != null) {
			validate(name, target, result);
		}
		super.onFinish(name, target, context, result);
	}

	private void validate(ConfigurationPropertyName name,
			Bindable target, Object result) {
		BindingResult errors = new BeanPropertyBindingResult(result, name.toString());
		Class<?> resolve = target.getBoxedType().resolve();
		for (Validator validator : this.validators) {
			if (validator.supports(resolve)) {
				validator.validate(result, errors);
			}
		}
		if (errors.hasErrors()) {
			throwBindValidationException(name, errors);
		}
	}

	private void throwBindValidationException(ConfigurationPropertyName name,
			BindingResult errors) {
		Set<ConfigurationProperty> boundProperties = this.bound.stream()
				.filter((property) -> name.isAncestorOf(property.getName()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		ValidationErrors validationErrors = new ValidationErrors(name, boundProperties,
				errors.getAllErrors());
		throw new BindValidationException(validationErrors);
	}

}
