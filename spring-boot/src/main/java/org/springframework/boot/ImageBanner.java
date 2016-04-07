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

package org.springframework.boot;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiColors;
import org.springframework.boot.ansi.AnsiPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Banner implementation that prints ASCII art generated from an image resource
 * {@link Resource}.
 *
 * @author Craig Burke
 */
public class ImageBanner implements Banner {

	private static final Log log = LogFactory.getLog(ImageBanner.class);

	private static final double RED_WEIGHT = 0.2126d;

	private static final double GREEN_WEIGHT = 0.7152d;

	private static final double BLUE_WEIGHT = 0.0722d;

	private static final int DEFAULT_MAX_WIDTH = 72;

	private static final double DEFAULT_ASPECT_RATIO = 0.5d;

	private static final boolean DEFAULT_DARK = false;

	private final Resource image;

	public ImageBanner(Resource image) {
		Assert.notNull(image, "Image must not be null");
		Assert.isTrue(image.exists(), "Image must exist");
		this.image = image;
	}

	@Override
	public void printBanner(Environment environment, Class<?> sourceClass,
			PrintStream out) {
		String headlessProperty = System.getProperty("java.awt.headless");
		try {
			System.setProperty("java.awt.headless", "true");
			BufferedImage sourceImage = ImageIO.read(this.image.getInputStream());

			int maxWidth = environment.getProperty("banner.image.max-width",
					Integer.class, DEFAULT_MAX_WIDTH);
			Double aspectRatio = environment.getProperty("banner.image.aspect-ratio",
					Double.class, DEFAULT_ASPECT_RATIO);
			boolean invert = environment.getProperty("banner.image.dark", Boolean.class,
					DEFAULT_DARK);

			BufferedImage resizedImage = resizeImage(sourceImage, maxWidth, aspectRatio);
			String banner = imageToBanner(resizedImage, invert);

			PropertyResolver ansiResolver = getAnsiResolver();
			banner = ansiResolver.resolvePlaceholders(banner);
			out.println(banner);
		}
		catch (Exception ex) {
			log.warn("Image banner not printable: " + this.image + " (" + ex.getClass()
					+ ": '" + ex.getMessage() + "')", ex);
		}
		finally {
			if (headlessProperty == null) {
				System.clearProperty("java.awt.headless");
			}
			else {
				System.setProperty("java.awt.headless", headlessProperty);
			}
		}
	}

	private PropertyResolver getAnsiResolver() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new AnsiPropertySource("ansi", true));
		return new PropertySourcesPropertyResolver(sources);
	}

	private String imageToBanner(BufferedImage image, boolean dark) {
		StringBuilder banner = new StringBuilder();

		for (int y = 0; y < image.getHeight(); y++) {
			if (dark) {
				banner.append("${AnsiBackground.BLACK}");
			}
			else {
				banner.append("${AnsiBackground.DEFAULT}");
			}
			for (int x = 0; x < image.getWidth(); x++) {
				Color color = new Color(image.getRGB(x, y), false);
				banner.append(getFormatString(color, dark));
			}
			if (dark) {
				banner.append("${AnsiBackground.DEFAULT}");
			}
			banner.append("${AnsiColor.DEFAULT}\n");
		}

		return banner.toString();
	}

	protected String getFormatString(Color color, boolean dark) {
		AnsiColor matchedColor = AnsiColors.getAnsiColor(color, dark);
		return "${AnsiColor." + matchedColor.name() + "}"
				+ getAsciiCharacter(color, dark);
	}

	private static int getLuminance(Color color, boolean inverse) {
		double red = color.getRed();
		double green = color.getGreen();
		double blue = color.getBlue();

		double luminance;

		if (inverse) {
			luminance = (RED_WEIGHT * (255.0d - red)) + (GREEN_WEIGHT * (255.0d - green))
					+ (BLUE_WEIGHT * (255.0d - blue));
		}
		else {
			luminance = (RED_WEIGHT * red) + (GREEN_WEIGHT * green)
					+ (BLUE_WEIGHT * blue);
		}

		return (int) Math.ceil((luminance / 255.0d) * 100);
	}

	private static char getAsciiCharacter(Color color, boolean dark) {
		double luminance = getLuminance(color, dark);

		if (luminance >= 90) {
			return ' ';
		}
		else if (luminance >= 80) {
			return '.';
		}
		else if (luminance >= 70) {
			return '*';
		}
		else if (luminance >= 60) {
			return ':';
		}
		else if (luminance >= 50) {
			return 'o';
		}
		else if (luminance >= 40) {
			return '&';
		}
		else if (luminance >= 30) {
			return '8';
		}
		else if (luminance >= 20) {
			return '#';
		}
		else {
			return '@';
		}
	}

	private static BufferedImage resizeImage(BufferedImage sourceImage, int maxWidth,
			double aspectRatio) {
		int width;
		double resizeRatio;
		if (sourceImage.getWidth() > maxWidth) {
			resizeRatio = (double) maxWidth / (double) sourceImage.getWidth();
			width = maxWidth;
		}
		else {
			resizeRatio = 1.0d;
			width = sourceImage.getWidth();
		}

		int height = (int) (Math
				.ceil(resizeRatio * aspectRatio * sourceImage.getHeight()));
		Image image = sourceImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);

		BufferedImage resizedImage = new BufferedImage(image.getWidth(null),
				image.getHeight(null), BufferedImage.TYPE_INT_RGB);

		resizedImage.getGraphics().drawImage(image, 0, 0, null);
		return resizedImage;
	}

}
