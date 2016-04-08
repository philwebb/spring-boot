/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiOutput.Enabled;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageBanner}.
 *
 * @author Craig Burke
 * @author Phillip Webb
 */
public class ImageBannerTests {

	private static final String BACKGROUND_DEFAULT_ANSI = getAnsiOutput(
			AnsiBackground.DEFAULT);

	private static final String BACKGROUND_DARK_ANSI = getAnsiOutput(
			AnsiBackground.BLACK);

	private static final char HIGH_LUMINANCE_CHARACTER = ' ';

	private static final char LOW_LUMINANCE_CHARACTER = '@';

	@Before
	public void setup() {
		AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
	}

	@After
	public void cleanup() {
		AnsiOutput.setEnabled(Enabled.DETECT);
	}

	@Test
	public void printBannerShouldResetForegroundAndBackground() {
		String banner = printBanner("black-and-white.gif");
		String expected = AnsiOutput.encode(AnsiColor.DEFAULT)
				+ AnsiOutput.encode(AnsiBackground.DEFAULT);
		assertThat(banner).startsWith(expected);
	}
	//
	// @Test
	// public void renderDarkBackground() {
	// this.properties.put("banner.image.invert", true);
	// String banner = printBanner("black-and-white.gif");
	//
	// assertThat(banner).startsWith(BACKGROUND_DARK_ANSI);
	// }
	//
	// @Test
	// public void renderWhiteCharactersWithColors() {
	// String banner = printBanner("black-and-white.gif");
	// String expectedFirstLine = getAnsiOutput(AnsiColor.BRIGHT_WHITE)
	// + HIGH_LUMINANCE_CHARACTER;
	//
	// assertThat(banner).contains(expectedFirstLine);
	// }
	//
	// @Test
	// public void renderWhiteCharactersOnDarkBackground() {
	// this.properties.put("banner.image.invert", true);
	// String banner = printBanner("black-and-white.gif");
	// String expectedFirstLine = getAnsiOutput(AnsiColor.BRIGHT_WHITE)
	// + LOW_LUMINANCE_CHARACTER;
	//
	// assertThat(banner).contains(expectedFirstLine);
	// }
	//
	// @Test
	// public void renderBlackCharactersOnDefaultBackground() {
	// String banner = printBanner("black-and-white.gif");
	// String blackCharacter = getAnsiOutput(AnsiColor.BLACK) + LOW_LUMINANCE_CHARACTER;
	//
	// assertThat(banner).contains(blackCharacter);
	// }
	//
	// @Test
	// public void renderBlackCharactersOnDarkBackground() {
	// this.properties.put("banner.image.invert", true);
	// String banner = printBanner("black-and-white.gif");
	// String blackCharacter = getAnsiOutput(AnsiColor.BLACK) + HIGH_LUMINANCE_CHARACTER;
	//
	// assertThat(banner).contains(blackCharacter);
	// }
	//
	// @Test
	// public void renderBannerWithAllColors() {
	// String banner = printBanner("colors.gif");
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BLACK));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.RED));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.GREEN));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.YELLOW));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BLUE));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.MAGENTA));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.CYAN));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.WHITE));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_BLACK));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_RED));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_GREEN));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_YELLOW));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_BLUE));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_MAGENTA));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_CYAN));
	// assertThat(banner).contains(getAnsiOutput(AnsiColor.BRIGHT_WHITE));
	// }
	//
	// @Test
	// public void renderSimpleGradient() {
	// AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
	// String banner = printBanner("gradient.gif");
	// String expectedResult = "@#8&o:*. ";
	// assertThat(banner).startsWith(expectedResult);
	// }
	//
	// @Test
	// public void renderBannerWithDefaultAspectRatio() {
	// String banner = printBanner("black-and-white.gif");
	// int bannerHeight = getBannerHeight(banner);
	// assertThat(bannerHeight).isEqualTo(2);
	// }
	//
	// @Test
	// public void renderBannerWithCustomAspectRatio() {
	// this.properties.put("banner.image.aspect-ratio", 1.0d);
	// String banner = printBanner("black-and-white.gif");
	// int bannerHeight = getBannerHeight(banner);
	//
	// assertThat(bannerHeight).isEqualTo(4);
	// }
	//
	// @Test
	// public void renderLargeBanner() {
	// String banner = printBanner("large.gif");
	// int bannerWidth = getBannerWidth(banner);
	// assertThat(bannerWidth).isEqualTo(72);
	// }
	//
	// @Test
	// public void renderLargeBannerWithACustomWidth() {
	// this.properties.put("banner.image.width", 60);
	// String banner = printBanner("large.gif");
	// int bannerWidth = getBannerWidth(banner);
	//
	// assertThat(bannerWidth).isEqualTo(60);
	// }

	private int getBannerHeight(String banner) {
		return banner.split("\n").length;
	}

	private int getBannerWidth(String banner) {
		String strippedBanner = banner.replaceAll("\u001B\\[.*?m", "");
		String firstLine = strippedBanner.split("\n")[0];
		return firstLine.length();
	}

	private static String getAnsiOutput(AnsiElement ansi) {
		return "\u001B[" + ansi.toString() + "m";
	}

	private String printBanner(String path, String... properties) {
		ImageBanner banner = new ImageBanner(new ClassPathResource(path, getClass()));
		ConfigurableEnvironment environment = new MockEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				properties);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		banner.printBanner(environment, getClass(), new PrintStream(out));
		return out.toString();
	}

}
