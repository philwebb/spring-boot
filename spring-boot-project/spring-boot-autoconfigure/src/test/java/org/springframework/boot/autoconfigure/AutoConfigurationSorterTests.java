/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfigurationSorter}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class AutoConfigurationSorterTests {

	private static final String DEFAULT = OrderUnspecified.class.getName();

	private static final String LOWEST = OrderLowest.class.getName();

	private static final String HIGHEST = OrderHighest.class.getName();

	private static final String A = AutoConfigureA.class.getName();

	private static final String DEPA = DepAutoConfigureA.class.getName();

	private static final String B = AutoConfigureB.class.getName();

	private static final String DEPB = DepAutoConfigureB.class.getName();

	private static final String C = AutoConfigureC.class.getName();

	private static final String D = AutoConfigureD.class.getName();

	private static final String E = AutoConfigureE.class.getName();

	private static final String W = AutoConfigureW.class.getName();

	private static final String X = AutoConfigureX.class.getName();

	private static final String Y = AutoConfigureY.class.getName();

	private static final String Z = AutoConfigureZ.class.getName();

	private static final String DEPZ = DepAutoConfigureZ.class.getName();

	private static final String A2 = AutoConfigureA2.class.getName();

	private static final String W2 = AutoConfigureW2.class.getName();

	private AutoConfigurationMetadata autoConfigurationMetadata = mock(
			AutoConfigurationMetadata.class);

	@Test
	public void byOrderAnnotation() {
		List<String> actual = sort(Arrays.asList(LOWEST, HIGHEST, DEFAULT));
		assertThat(actual).containsExactly(HIGHEST, DEFAULT, LOWEST);
	}

	@Test
	public void byAutoConfigureAfter() {
		List<String> actual = sort(Arrays.asList(A, B, C));
		assertThat(actual).containsExactly(C, B, A);
	}

	@Test
	public void byAutoConfigureAfterWithDeprecated() {
		List<String> actual = sort(Arrays.asList(DEPA, DEPB, C));
		assertThat(actual).containsExactly(C, DEPB, DEPA);
	}

	@Test
	public void byAutoConfigureBefore() {
		List<String> actual = sort(Arrays.asList(X, Y, Z));
		assertThat(actual).containsExactly(Z, Y, X);
	}

	@Test
	public void byAutoConfigureBeforeWithDeprecated() {
		List<String> actual = sort(Arrays.asList(X, Y, DEPZ));
		assertThat(actual).containsExactly(DEPZ, Y, X);
	}

	@Test
	public void byAutoConfigureAfterDoubles() {
		List<String> actual = sort(Arrays.asList(A, B, C, E));
		assertThat(actual).containsExactly(C, E, B, A);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfter() {
		List<String> actual = sort(Arrays.asList(A, B, C, W, X));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfterWithClassNames() {
		List<String> actual = sort(Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	public void byAutoConfigureMixedBeforeAndAfterWithDifferentInputOrder() {
		List<String> actual = sort(Arrays.asList(W, X, A, B, C));
		assertThat(actual).containsExactly(C, W, B, A, X);
	}

	@Test
	public void byAutoConfigureAfterWithMissing() {
		List<String> actual = sort(Arrays.asList(A, B));
		assertThat(actual).containsExactly(B, A);
	}

	@Test
	public void byAutoConfigureAfterWithCycle() {

		assertThatIllegalStateException()
				.isThrownBy(() -> sort(new CachingMetadataReaderFactory(),
						this.autoConfigurationMetadata, Arrays.asList(A, B, C, D)))
				.withMessageContaining("AutoConfigure cycle detected");
	}

	@Test
	public void usesAnnotationPropertiesWhenPossible() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A2, B, C, W2, X);
		List<String> actual = sort(readerFactory, this.autoConfigurationMetadata,
				Arrays.asList(A2, B, C, W2, X));
		assertThat(actual).containsExactly(C, W2, B, A2, X);
	}

	@Test
	public void useAnnotationWithNoDirectLink() throws Exception {
		MetadataReaderFactory readerFactory = new SkipCycleMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, E);
		List<String> actual = sort(readerFactory, this.autoConfigurationMetadata,
				Arrays.asList(A, E));
		assertThat(actual).containsExactly(E, A);
	}

	@Test
	public void useAnnotationWithNoDirectLinkAndCycle() throws Exception {
		MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory();
		this.autoConfigurationMetadata = getAutoConfigurationMetadata(A, B, D);
		assertThatIllegalStateException()
				.isThrownBy(() -> sort(readerFactory, this.autoConfigurationMetadata,
						Arrays.asList(D, B)))
				.withMessageContaining("AutoConfigure cycle detected");
	}

	private AutoConfigurationMetadata getAutoConfigurationMetadata(String... classNames)
			throws Exception {
		Properties properties = new Properties();
		for (String className : classNames) {
			Class<?> type = ClassUtils.forName(className, null);
			properties.put(type.getName(), "");
			AutoConfigureOrder order = type
					.getDeclaredAnnotation(AutoConfigureOrder.class);
			if (order != null) {
				properties.put(className + ".AutoConfigureOrder",
						String.valueOf(order.value()));
			}
			AutoConfigureBefore autoConfigureBefore = type
					.getDeclaredAnnotation(AutoConfigureBefore.class);
			if (autoConfigureBefore != null) {
				properties.put(className + ".AutoConfigureBefore",
						merge(autoConfigureBefore.value(), autoConfigureBefore.name()));
			}
			AutoConfigureAfter autoConfigureAfter = type
					.getDeclaredAnnotation(AutoConfigureAfter.class);
			if (autoConfigureAfter != null) {
				properties.put(className + ".AutoConfigureAfter",
						merge(autoConfigureAfter.value(), autoConfigureAfter.name()));
			}
		}
		return AutoConfigurationMetadataLoader.loadMetadata(properties);
	}

	private String merge(Class<?>[] value, String[] name) {
		Set<String> items = new LinkedHashSet<>();
		for (Class<?> type : value) {
			items.add(type.getName());
		}
		for (String type : name) {
			items.add(type);
		}
		return StringUtils.collectionToCommaDelimitedString(items);
	}

	private List<String> sort(List<String> classNames) {
		return sort(new SkipCycleMetadataReaderFactory(), this.autoConfigurationMetadata,
				classNames);
	}

	private List<String> sort(MetadataReaderFactory metadataReaderFactory,
			AutoConfigurationMetadata autoConfigurationMetadata,
			List<String> classNames) {
		AutoConfigurationClasses classes = new AutoConfigurationClasses(
				metadataReaderFactory, autoConfigurationMetadata, classNames);
		return AutoConfigurationSorter.INSTANCE.getInPriorityOrder(classes);
	}

	@AutoConfigureOrder
	public static class OrderUnspecified {

	}

	@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
	public static class OrderLowest {

	}

	@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
	public static class OrderHighest {

	}

	@AutoConfigureAfter(AutoConfigureB.class)
	public static class AutoConfigureA {

	}

	@AutoConfigureAfter(DepAutoConfigureB.class)
	public static class DepAutoConfigureA {

	}

	@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	public static class AutoConfigureA2 {

	}

	@AutoConfigureAfter({ AutoConfigureC.class, AutoConfigureD.class,
			AutoConfigureE.class })
	public static class AutoConfigureB {

	}

	@AutoConfigureAfter({ DeprecatedAutoConfigureC.class, AutoConfigureD.class,
			AutoConfigureE.class })
	public static class DepAutoConfigureB {

	}

	public static class AutoConfigureC {

	}

	@DeprecatedAutoConfiguration(replacement = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureC")
	public static class DeprecatedAutoConfigureC {

	}

	@AutoConfigureAfter(AutoConfigureA.class)
	public static class AutoConfigureD {

	}

	public static class AutoConfigureE {

	}

	@AutoConfigureBefore(AutoConfigureB.class)
	public static class AutoConfigureW {

	}

	@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureB")
	public static class AutoConfigureW2 {

	}

	public static class AutoConfigureX {

	}

	@AutoConfigureBefore(AutoConfigureX.class)
	public static class AutoConfigureY {

	}

	@DeprecatedAutoConfiguration(replacement = "org.springframework.boot.autoconfigure.AutoConfigurationSorterTests$AutoConfigureY")
	public static class DeprecatedAutoConfigureY {

	}

	@AutoConfigureBefore(AutoConfigureY.class)
	public static class AutoConfigureZ {

	}

	@AutoConfigureBefore(DeprecatedAutoConfigureY.class)
	public static class DepAutoConfigureZ {

	}

	private static class SkipCycleMetadataReaderFactory
			extends CachingMetadataReaderFactory {

		@Override
		public MetadataReader getMetadataReader(String className) throws IOException {
			if (className.equals(D)) {
				throw new IOException();
			}
			return super.getMetadataReader(className);
		}

	}

}
