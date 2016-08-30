/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Phillip Webb
 */
public class ConditionMessage {

	private String message;

	private ConditionMessage() {
		this(null);
	}

	private ConditionMessage(String message) {
		this.message = message;
	}

	private ConditionMessage(ConditionMessage prior, String message) {
		this.message = (prior.isEmpty() ? message : prior + "; " + message);
	}

	public boolean isEmpty() {
		return !StringUtils.hasLength(this.message);
	}

	@Override
	public String toString() {
		return (this.message == null ? "" : this.message);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.message);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !ConditionMessage.class.isInstance(obj)) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return ObjectUtils.nullSafeEquals(((ConditionMessage) obj).message, this.message);
	}

	public ConditionMessage append(String message) {
		if (!StringUtils.hasLength(message)) {
			return this;
		}
		if (!StringUtils.hasLength(this.message)) {
			return new ConditionMessage(message);
		}

		return new ConditionMessage(this.message + " " + message);
	}

	public WithCondition andCondition(Class<? extends Annotation> condition,
			Object... details) {
		Assert.notNull(condition, "Condition must not be null");
		return andCondition("@" + ClassUtils.getShortName(condition), details);
	}

	public WithCondition andCondition(String condition, Object... details) {
		Assert.notNull(condition, "Condition must not be null");
		String detail = StringUtils.arrayToDelimitedString(details, " ");
		if (StringUtils.hasLength(detail)) {
			return new WithCondition(condition + " " + detail);
		}
		return new WithCondition(condition);
	}

	public static ConditionMessage empty() {
		return new ConditionMessage();
	}

	public static ConditionMessage of(String message) {
		return new ConditionMessage(message);
	}

	public static ConditionMessage of(Collection<? extends ConditionMessage> messages) {
		ConditionMessage result = new ConditionMessage();
		if (messages != null) {
			for (ConditionMessage message : messages) {
				result = new ConditionMessage(result, message.toString());
			}
		}
		return result;
	}

	public static WithCondition forCondition(Class<? extends Annotation> condition,
			Object... details) {
		return new ConditionMessage().andCondition(condition, details);
	}

	public static WithCondition forCondition(String condition, Object... details) {
		return new ConditionMessage().andCondition(condition, details);
	}

	public class WithCondition {

		private final String condition;

		private WithCondition(String condition) {
			this.condition = condition;
		}

		public ConditionMessage foundExactly(Object result) {
			return found("").in(result);
		}

		public ForConditionElements found(String message) {
			return found(message, message);
		}

		public ForConditionElements found(String singular, String plural) {
			return new ForConditionElements(this, "found", singular, plural);
		}

		public ForConditionElements didNotFind(String message) {
			return didNotFind(message, message);
		}

		public ForConditionElements didNotFind(String singular, String plural) {
			return new ForConditionElements(this, "did not find", singular, plural);
		}

		public ConditionMessage resultedIn(Object result) {
			return because("resulted in " + result);
		}

		public ConditionMessage notAvailable(String item) {
			return because(item + " is not available");
		}

		public ConditionMessage available(String item) {
			return because(item + " is available");
		}

		public ConditionMessage because(String reason) {
			if (StringUtils.isEmpty(reason)) {
				return new ConditionMessage(ConditionMessage.this, this.condition);
			}
			return new ConditionMessage(ConditionMessage.this,
					this.condition + " " + reason);
		}

	}

	public class ForConditionElements {

		private final WithCondition condition;

		private final String reson;

		private final String singular;

		private final String plural;

		private ForConditionElements(WithCondition condition, String reason,
				String singular, String plural) {
			this.condition = condition;
			this.reson = reason;
			this.singular = singular;
			this.plural = plural;
		}

		public ConditionMessage in(Object... elements) {
			return in(Arrays.asList(elements));
		}

		public ConditionMessage atAll() {
			return in(Collections.emptyList());
		}

		public ConditionMessage in(Collection<?> collection) {
			StringBuilder message = new StringBuilder(this.reson);
			if ((this.condition == null || collection.size() <= 1)
					&& StringUtils.hasLength(this.singular)) {
				message.append(" " + this.singular);
			}
			else if (StringUtils.hasLength(this.plural)) {
				message.append(" " + this.plural);
			}
			if (collection != null && !collection.isEmpty()) {
				message.append(
						" " + StringUtils.collectionToDelimitedString(collection, ", "));
			}
			return this.condition.because(message.toString());
		}

	}

}
