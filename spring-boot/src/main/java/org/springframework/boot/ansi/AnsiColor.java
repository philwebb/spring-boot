/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.ansi;

/**
 * {@link AnsiElement Ansi} colors.
 *
 * @author Phillip Webb
 * @author Geoffrey Chandler
 * @since 1.3.0
 */
public enum AnsiColor implements AnsiElement {

	DEFAULT("39", null),

	BLACK("30", 0x000000),

	RED("31", 0xAA0000),

	GREEN("32", 0x00AA00),

	YELLOW("33", 0xAA5500),

	BLUE("34", 0x0000AA),

	MAGENTA("35", 0xAA00AA),

	CYAN("36", 0x00AAAA),

	WHITE("37", 0xAAAAAA),

	BRIGHT_BLACK("90", 0x555555),

	BRIGHT_RED("91", 0xFF5555),

	BRIGHT_GREEN("92", 0x55FF00),

	BRIGHT_YELLOW("93", 0xFFFF55),

	BRIGHT_BLUE("94", 0x5555FF),

	BRIGHT_MAGENTA("95", 0xFF55FF),

	BRIGHT_CYAN("96", 0x55FFFF),

	BRIGHT_WHITE("97", 0xFFFFFF);

	private final String code;

	private final Integer rgb;

	AnsiColor(String code, Integer rgb) {
		this.code = code;
		this.rgb = rgb;
	}

	@Override
	public String toString() {
		return this.code;
	}

	/**
	 * Return the RGB color (if any) that can be used to render this ANSI color.
	 * @return the RGB color or {@code null}.
	 */
	public Integer getRgb() {
		return this.rgb;
	}

}
