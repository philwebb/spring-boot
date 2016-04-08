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

package org.springframework.boot.ansi;

import java.awt.Color;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiColors}.
 *
 * @author Phillip Webb
 */
public class AnsiColorsTests {

	@Test
	public void getClosestWhenExactMatchShouldReturnAnsiColor() throws Exception {
		for (AnsiColor ansiColor : AnsiColor.values()) {
			if (ansiColor.getRgb() != null) {
				Color color = new Color(ansiColor.getRgb());
				assertThat(AnsiColors.getClosest(color)).isEqualTo(ansiColor);
			}
		}
	}

	@Test
	public void getClosestWhenCloseShouldReturnAnsiColor() throws Exception {
		assertThat(getClosest(0x292424)).isEqualTo(AnsiColor.BLACK);
		assertThat(getClosest(0x8C1919)).isEqualTo(AnsiColor.RED);
		assertThat(getClosest(0x0BA10B)).isEqualTo(AnsiColor.GREEN);
		assertThat(getClosest(0xB55F09)).isEqualTo(AnsiColor.YELLOW);
		assertThat(getClosest(0x0B0BA1)).isEqualTo(AnsiColor.BLUE);
		assertThat(getClosest(0xA312A3)).isEqualTo(AnsiColor.MAGENTA);
		assertThat(getClosest(0x0BB5B5)).isEqualTo(AnsiColor.CYAN);
		assertThat(getClosest(0xBAB6B6)).isEqualTo(AnsiColor.WHITE);
		assertThat(getClosest(0x615A5A)).isEqualTo(AnsiColor.BRIGHT_BLACK);
		assertThat(getClosest(0xF23333)).isEqualTo(AnsiColor.BRIGHT_RED);
		assertThat(getClosest(0x55E80C)).isEqualTo(AnsiColor.BRIGHT_GREEN);
		assertThat(getClosest(0xF5F54C)).isEqualTo(AnsiColor.BRIGHT_YELLOW);
		assertThat(getClosest(0x5656F0)).isEqualTo(AnsiColor.BRIGHT_BLUE);
		assertThat(getClosest(0xFA50FA)).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
		assertThat(getClosest(0x56F5F5)).isEqualTo(AnsiColor.BRIGHT_CYAN);
		assertThat(getClosest(0xEDF5F5)).isEqualTo(AnsiColor.BRIGHT_WHITE);
	}

	private AnsiColor getClosest(int rgb) {
		return AnsiColors.getClosest(new Color(rgb));
	}

}
