/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.structured;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.boot.logging.StandardStackTracePrinter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StackTrace.Include;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StackTrace.RootOrder;
import org.springframework.boot.util.Instantiator;

/**
 * Factory to create a {@link StackTracePrinter} from
 * {@link StructuredLoggingJsonProperties} or return {@code null} if not properties are
 * set.
 *
 * @author Phillip Webb
 */
class StackTracePrinterFactory {

	StackTracePrinter createStackTracePrinter(StructuredLoggingJsonProperties.StackTrace properties) {
		Class<? extends StackTracePrinter> printerClass = properties.printer();
		if (printerClass != null) {
			return instantiator(properties).instantiateType(printerClass);
		}
		return createStandardPrinter(properties, true);
	}

	private Instantiator<StackTracePrinter> instantiator(StructuredLoggingJsonProperties.StackTrace properties) {
		return new Instantiator<>(StackTracePrinter.class, (parameters) -> parameters
			.add(StandardStackTracePrinter.class, (type) -> createStandardPrinter(properties, false)));
	}

	private StandardStackTracePrinter createStandardPrinter(StructuredLoggingJsonProperties.StackTrace properties,
			boolean allowNull) {
		StandardStackTracePrinter initial = (properties.root() != RootOrder.FIRST)
				? StandardStackTracePrinter.rootFirst() : StandardStackTracePrinter.rootLast();
		StandardStackTracePrinter result = null;
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		result = map.from(properties::maxLength)
			.to(resultOrInitial(result, initial), StandardStackTracePrinter::withMaximumLength);
		result = map.from(properties::maxThrowableDepth)
			.to(resultOrInitial(result, initial), StandardStackTracePrinter::withMaximumThrowableDepth);
		result = map.from(properties::include)
			.when((include) -> include.contains(Include.COMMON_FRAMES))
			.to(resultOrInitial(result, initial), (printer, include) -> printer.withCommonFrames());
		result = map.from(properties::include)
			.when((include) -> !include.contains(Include.SUPRESSED))
			.to(resultOrInitial(result, initial), (printer, include) -> printer.withoutSuppressed());
		result = map.from(properties::singleLine)
			.whenTrue()
			.to(resultOrInitial(result, initial), (printer, singleLine) -> printer.withEscapedLineSeprator());
		result = (result != null || allowNull) ? result : resultOrInitial(result, initial);
		return result;
	}

	private StandardStackTracePrinter resultOrInitial(StandardStackTracePrinter result,
			StandardStackTracePrinter initial) {
		return (result != null) ? result : initial;
	}

}
