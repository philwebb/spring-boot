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

package org.springframework.boot.autoconfigure.validation;

import javax.validation.Validator;

import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * Adapter that takes a JSR-303 {@code javax.validator.Validator} and exposes it as a
 * Spring {@link org.springframework.validation.Validator} without implementing the
 * JSR-303 Validator interface itself.
 *
 * @author Phillip Webb
 */
class SpringOnlyValidatorAdapter implements SmartValidator {

	private final SpringValidatorAdapter delegate;

	SpringOnlyValidatorAdapter(Validator targetValidator) {
		this.delegate = new SpringValidatorAdapter(targetValidator);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return this.delegate.supports(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		this.delegate.validate(target, errors);
	}

	@Override
	public void validate(Object target, Errors errors, Object... validationHints) {
		this.delegate.validate(target, errors, validationHints);
	}

}
