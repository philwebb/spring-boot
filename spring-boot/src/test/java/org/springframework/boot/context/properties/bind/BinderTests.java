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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InOrder;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link Binder}.
 *
 * @author Phillip Webb
 */
public class BinderTests {

	private static final ResolvableType STRING_STRING_MAP = ResolvableType
			.forClassWithGenerics(Map.class, String.class, String.class);

	private static final ResolvableType STRING_INTEGER_MAP = ResolvableType
			.forClassWithGenerics(Map.class, String.class, Integer.class);

	private static final ResolvableType INTEGER_INTEGER_MAP = ResolvableType
			.forClassWithGenerics(Map.class, Integer.class, Integer.class);

	private static final ResolvableType STRING_OBJECT_MAP = ResolvableType
			.forClassWithGenerics(Map.class, String.class, Object.class);

	private static final ResolvableType INTEGER_LIST = ResolvableType
			.forClassWithGenerics(List.class, Integer.class);

	private static final ResolvableType INTEGER_ARRAY = ResolvableType
			.forArrayComponent(ResolvableType.forClass(Integer.class));

	private static final ResolvableType STRING_LIST = ResolvableType
			.forClassWithGenerics(List.class, String.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
	}

	@Test
	public void createWhenSourcesIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Sources must not be null");
		new Binder((Iterable<ConfigurationPropertySource>) null);
	}

	@Test
	public void bindWhenNameIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Name must not be null");
		this.binder.bind((ConfigurationPropertyName) null, Bindable.of(String.class),
				BindHandler.DEFAULT);
	}

	@Test
	public void bindWhenTargetIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Target must not be null");
		this.binder.bind(ConfigurationPropertyName.of("foo"), null, BindHandler.DEFAULT);
	}

	@Test
	public void bindToValueWhenPropertyIsMissingShouldReturnNull() throws Exception {
		this.sources.add(new MockConfigurationPropertySource());
		String result = this.binder.bind("foo", Bindable.of(String.class));
		assertThat(result).isNull();
	}

	@Test
	public void bindToValueWhenPropertyIsMissingShouldReturnDefault() throws Exception {
		this.sources.add(new MockConfigurationPropertySource());
		Integer result = this.binder.bind("foo",
				Bindable.of(Integer.class).withDefaultValue(123));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueWhenPropertyIsMissingShouldReturnConvertedDefault()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource());
		Integer result = this.binder.bind("foo",
				Bindable.of(Integer.class).withDefaultValue("123"));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueShouldReturnPropertyValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		Integer result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueShouldReturnPropertyValueFromSecondSource() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		this.sources.add(new MockConfigurationPropertySource("bar", 234));
		Integer result = this.binder.bind("bar", Bindable.of(Integer.class));
		assertThat(result).isEqualTo(234);
	}

	@Test
	public void bindToValueShouldReturnConvertedPropertyValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "123"));
		Integer result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueWhenMultipleCandidatesShouldReturnFirst() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", 123));
		this.sources.add(new MockConfigurationPropertySource("foo", 234));
		Integer result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueWithPlaceholdersShouldResolve() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "bar=23");
		this.sources.add(new MockConfigurationPropertySource("foo", "1${bar}"));
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		Integer result = this.binder.bind("foo", Bindable.of(Integer.class));
		assertThat(result).isEqualTo(123);
	}

	@Test
	public void bindToValueShouldTriggerOnSuccess() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "1", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<Integer> target = Bindable.of(Integer.class);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eqName("foo"), eq(target), any(), notNull(),
				eq(1));
	}

	@Test
	public void bindToMapShouldReturnPopulatedMap() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		source.put("foo.[baz]", "2");
		source.put("foo[BiNg]", "3");
		this.sources.add(source);
		Map<String, String> result = this.binder.bind("foo",
				Bindable.of(STRING_STRING_MAP));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", "1");
		assertThat(result).containsEntry("baz", "2");
		assertThat(result).containsEntry("BiNg", "3");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void bindToMapWithEmptyPrefix() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("", Bindable.of(STRING_OBJECT_MAP));
		assertThat((Map<String, Object>) result.get("foo")).containsEntry("bar", "1");
	}

	@Test
	public void bindToMapShouldConvertMapValue() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "1");
		source.put("foo.[baz]", "2");
		source.put("foo[BiNg]", "3");
		source.put("faf.bar", "x");
		this.sources.add(source);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.of(STRING_INTEGER_MAP));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 2);
		assertThat(result).containsEntry("BiNg", 3);
	}

	@Test
	public void bindToMapShouldBindToMapValue() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class,
				ResolvableType.forClass(String.class), STRING_INTEGER_MAP);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar.baz", "1");
		source.put("foo.bar.bin", "2");
		source.put("foo.far.baz", "3");
		source.put("foo.far.bin", "4");
		source.put("faf.far.bin", "x");
		this.sources.add(source);
		Map<String, Map<String, Integer>> result = this.binder.bind("foo",
				Bindable.of(type));
		System.out.println(result);
		assertThat(result).hasSize(2);
		assertThat(result.get("bar")).containsEntry("baz", 1).containsEntry("bin", 2);
		assertThat(result.get("far")).containsEntry("baz", 3).containsEntry("bin", 4);
	}

	@Test
	public void bindToMapShouldBindNestedMapValue() throws Exception {
		ResolvableType nestedType = ResolvableType.forClassWithGenerics(Map.class,
				ResolvableType.forClass(String.class), STRING_INTEGER_MAP);
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class,
				ResolvableType.forClass(String.class), nestedType);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.bar.baz", "1");
		source.put("foo.nested.bar.bin", "2");
		source.put("foo.nested.far.baz", "3");
		source.put("foo.nested.far.bin", "4");
		source.put("faf.nested.far.bin", "x");
		this.sources.add(source);
		Map<String, Map<String, Map<String, Integer>>> result = this.binder.bind("foo",
				Bindable.of(type));
		Map<String, Map<String, Integer>> nested = result.get("nested");
		assertThat(nested).hasSize(2);
		assertThat(nested.get("bar")).containsEntry("baz", 1).containsEntry("bin", 2);
		assertThat(nested.get("far")).containsEntry("baz", 3).containsEntry("bin", 4);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void bindToMapWhenMapValueIsObjectShouldBindNestedMapValue() throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, String.class,
				Object.class);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.bar.baz", "1");
		source.put("foo.nested.bar.bin", "2");
		source.put("foo.nested.far.baz", "3");
		source.put("foo.nested.far.bin", "4");
		source.put("faf.nested.far.bin", "x");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("foo", Bindable.of(type));
		Map<String, Object> nested = (Map<String, Object>) result.get("nested");
		assertThat(nested).hasSize(2);
		Map<String, Object> bar = (Map<String, Object>) nested.get("bar");
		assertThat(bar).containsEntry("baz", "1").containsEntry("bin", "2");
		Map<String, Object> far = (Map<String, Object>) nested.get("far");
		assertThat(far).containsEntry("baz", "3").containsEntry("bin", "4");
	}

	@Test
	public void bindToMapWhenMapValueIsObjectAndNoRootShouldBindNestedMapValue()
			throws Exception {
		ResolvableType type = ResolvableType.forClassWithGenerics(Map.class, String.class,
				Object.class);
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("commit.id", "abcdefg");
		source.put("branch", "master");
		source.put("foo", "bar");
		this.sources.add(source);
		Map<String, Object> result = this.binder.bind("", Bindable.of(type));
		assertThat(result.get("commit"))
				.isEqualTo(Collections.singletonMap("id", "abcdefg"));
		assertThat(result.get("branch")).isEqualTo("master");
		assertThat(result.get("foo")).isEqualTo("bar");
	}

	@Test
	public void bindToMapWhenEmptyRootNameShouldBindMap() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("bar.baz", "1");
		source.put("bar.bin", "2");
		this.sources.add(source);
		Map<String, Integer> result = this.binder.bind("",
				Bindable.of(STRING_INTEGER_MAP));
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("bar.baz", 1).containsEntry("bar.bin", 2);
	}

	@Test
	public void bindToMapWhenMultipleCandidateShouldBindFirst() throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo.bar", "1");
		source1.put("foo.baz", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo.baz", "3");
		source2.put("foo.bin", "4");
		this.sources.add(source2);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.of(STRING_INTEGER_MAP));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 2);
		assertThat(result).containsEntry("bin", 4);
	}

	@Test
	public void bindToMapWhenMultipleInSameSourceCandidateShouldBindFirst()
			throws Exception {
		Map<String, Object> map = new HashMap<>();
		map.put("foo.bar", "1");
		map.put("foo.b-az", "2");
		map.put("foo.ba-z", "3");
		map.put("foo.bin", "4");
		MapConfigurationPropertySource propertySource = new MapConfigurationPropertySource(map);
		this.sources.add(propertySource);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.of(STRING_INTEGER_MAP));
		assertThat(result).hasSize(4);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("b-az", 2);
		assertThat(result).containsEntry("ba-z", 3);
		assertThat(result).containsEntry("bin", 4);
	}

	@Test
	public void bindToMapWhenHasExistingMapShouldReplaceOnlyNewContents()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1"));
		Map<String, Integer> existing = new HashMap<>();
		existing.put("bar", 1000);
		existing.put("baz", 1001);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.of(STRING_INTEGER_MAP, existing));
		assertThat(result).isExactlyInstanceOf(HashMap.class);
		assertThat(result).isSameAs(existing);
		assertThat(result).hasSize(2);
		assertThat(result).containsEntry("bar", 1);
		assertThat(result).containsEntry("baz", 1001);
	}

	@Test
	public void bindToMapShouldRespectMapType() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1"));
		ResolvableType type = ResolvableType.forClassWithGenerics(HashMap.class,
				String.class, Integer.class);
		Object defaultMap = this.binder.bind("foo", Bindable.of(STRING_INTEGER_MAP));
		Object customMap = this.binder.bind("foo", Bindable.of(type));
		assertThat(customMap).isExactlyInstanceOf(HashMap.class)
				.isNotInstanceOf(defaultMap.getClass());
	}

	@Test
	public void bindToMapWhenNoValueShouldReturnNull() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.of(STRING_INTEGER_MAP));
		assertThat(result).isNull();
	}

	@Test
	public void bindToMapWhenNoValueShouldReturnDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		Map<String, Integer> defaultValue = Collections.singletonMap("bar", 123);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.<Map<String, Integer>>of(STRING_INTEGER_MAP)
						.withDefaultValue(defaultValue));
		assertThat(result).isEqualTo(defaultValue);
	}

	@Test
	public void bindToMapWhenNoValueShouldConvertDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		Map<String, Integer> defaultValue = Collections.singletonMap("bar", 123);
		Map<String, Integer> result = this.binder.bind("foo",
				Bindable.<Map<String, Integer>>of(ResolvableType
						.forClassWithGenerics(TreeMap.class, String.class, Integer.class))
				.withDefaultValue(defaultValue));
		assertThat(result).isExactlyInstanceOf(TreeMap.class);
		assertThat(result).isEqualTo(defaultValue);
	}

	@Test
	public void bindToMapShouldConvertKey() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[9]", "3");
		this.sources.add(source);
		Map<Integer, Integer> result = this.binder.bind("foo",
				Bindable.of(INTEGER_INTEGER_MAP));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry(0, 1);
		assertThat(result).containsEntry(1, 2);
		assertThat(result).containsEntry(9, 3);
	}

	@Test
	public void bindToMapShouldBeGreedyForStrings() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.aaa.bbb.ccc", "b");
		source.put("foo.bbb.ccc.ddd", "a");
		source.put("foo.ccc.ddd.eee", "r");
		this.sources.add(source);
		Map<String, String> result = this.binder.bind("foo", Bindable.of(ResolvableType
				.forClassWithGenerics(Map.class, String.class, String.class)));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("aaa.bbb.ccc", "b");
		assertThat(result).containsEntry("bbb.ccc.ddd", "a");
		assertThat(result).containsEntry("ccc.ddd.eee", "r");
	}

	@Test
	public void bindToMapShouldBeGreedyForScalars() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.aaa.bbb.ccc", "foo-bar");
		source.put("foo.bbb.ccc.ddd", "BAR_BAZ");
		source.put("foo.ccc.ddd.eee", "bazboo");
		this.sources.add(source);
		Map<String, ExampleEnum> result = this.binder.bind("foo",
				Bindable.of(ResolvableType.forClassWithGenerics(Map.class, String.class,
						ExampleEnum.class)));
		assertThat(result).hasSize(3);
		assertThat(result).containsEntry("aaa.bbb.ccc", ExampleEnum.FOO_BAR);
		assertThat(result).containsEntry("bbb.ccc.ddd", ExampleEnum.BAR_BAZ);
		assertThat(result).containsEntry("ccc.ddd.eee", ExampleEnum.BAZ_BOO);
	}

	@Test
	public void bindToMapWithPlaceholdersShouldBeGreedyForScalars() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, "foo=boo");
		MockConfigurationPropertySource source = new MockConfigurationPropertySource(
				"foo.aaa.bbb.ccc", "baz-${foo}");
		this.sources.add(source);
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		Map<String, ExampleEnum> result = this.binder.bind("foo",
				Bindable.of(ResolvableType.forClassWithGenerics(Map.class, String.class,
						ExampleEnum.class)));
		assertThat(result).containsEntry("aaa.bbb.ccc", ExampleEnum.BAZ_BOO);
	}

	@Test
	public void bindToMapWithNoPropertiesShouldReturnNull() throws Exception {
		this.binder = new Binder(this.sources);
		Map<String, ExampleEnum> result = this.binder.bind("foo",
				Bindable.of(ResolvableType.forClassWithGenerics(Map.class, String.class,
						ExampleEnum.class)));
		assertThat(result).isNull();
	}

	@Test
	public void bindToMapShouldTriggerOnSuccess() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.bar", "1", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<Map<String, Integer>> target = Bindable.of(STRING_INTEGER_MAP);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eqName("foo.bar"),
				eq(Bindable.of(Integer.class)), any(), notNull(), eq(1));
		inOrder.verify(handler).onSuccess(eqName("foo"), eq(target), any(), isNull(),
				isA(Map.class));
	}

	@Test
	public void bindToCollectionShouldReturnPopulatedCollection() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenNestedShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class,
				INTEGER_LIST);
		List<List<Integer>> result = this.binder.bind("foo", Bindable.of(type));
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).containsExactly(1, 2);
		assertThat(result.get(1)).containsExactly(3, 4);
	}

	@Test
	public void bindToCollectionWhenNotInOrderShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source);
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenNonSequentialShouldThrowException() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "2");
		source.put("foo[1]", "1");
		source.put("foo[3]", "3");
		this.sources.add(source);
		try {
			this.binder.bind("foo", Bindable.of(INTEGER_LIST));
			fail("No exception thrown");
		}
		catch (BindException ex) {
			ex.printStackTrace();
			Set<ConfigurationProperty> unbound = ((UnboundConfigurationPropertiesException) ex
					.getCause()).getUnboundConfigurationProperties();
			assertThat(unbound).hasSize(1);
			ConfigurationProperty property = unbound.iterator().next();
			assertThat(property.getName().toString()).isEqualTo("foo[3]");
			assertThat(property.getValue()).isEqualTo("3");
		}
	}

	@Test
	public void bindToCollectionWhenNonIterableShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		source.setNonIterable(true);
		this.sources.add(source);
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenMultipleSourceShouldOnlyUseFirst() throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("bar", "baz");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "1");
		source2.put("foo[1]", "2");
		this.sources.add(source2);
		MockConfigurationPropertySource source3 = new MockConfigurationPropertySource();
		source3.put("foo[0]", "7");
		source3.put("foo[1]", "8");
		source3.put("foo[2]", "9");
		this.sources.add(source3);
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenHasExistingCollectionShouldReplaceAllContents()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		List<Integer> existing = new LinkedList<>();
		existing.add(1000);
		existing.add(1001);
		List<Integer> result = this.binder.bind("foo",
				Bindable.of(INTEGER_LIST, existing));
		assertThat(result).isExactlyInstanceOf(LinkedList.class);
		assertThat(result).isSameAs(existing);
		assertThat(result).containsExactly(1);
	}

	@Test
	public void bindToCollectionWhenHasExistingCollectionButNoValueShouldReturnExisting()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf[0]", "1"));
		List<Integer> existing = new LinkedList<>();
		existing.add(1000);
		List<Integer> result = this.binder.bind("foo",
				Bindable.of(INTEGER_LIST, existing));
		assertThat(result).isEqualTo(existing);
	}

	@Test
	public void bindToCollectionShouldRespectCollectionType() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		ResolvableType type = ResolvableType.forClassWithGenerics(LinkedList.class,
				Integer.class);
		Object defaultList = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		Object customList = this.binder.bind("foo", Bindable.of(type));
		assertThat(customList).isExactlyInstanceOf(LinkedList.class)
				.isNotInstanceOf(defaultList.getClass());
	}

	@Test
	public void bindToCollectionWhenNoValueShouldReturnNull() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).isNull();
	}

	@Test
	public void bindToCollectionWhenNoValueShouldReturnDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		List<Integer> defaultValue = Collections.singletonList(123);
		List<Integer> result = this.binder.bind("foo",
				Bindable.<List<Integer>>of(INTEGER_LIST).withDefaultValue(defaultValue));
		assertThat(result).isEqualTo(defaultValue);
	}

	@Test
	public void bindToCollectionWhenNoValueShouldConvertDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		List<Integer> defaultValue = Collections.singletonList(123);
		List<Integer> result = this.binder.bind("foo",
				Bindable.<List<Integer>>of(ResolvableType
						.forClassWithGenerics(LinkedList.class, Integer.class))
				.withDefaultValue(defaultValue));
		assertThat(result).isExactlyInstanceOf(LinkedList.class);
		assertThat(result).isEqualTo(defaultValue);
	}

	@Test
	public void bindToCollectionWhenCommaListShouldReturnPopulatedCollection()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "1,2,3"));
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionWhenCommaListWithPlaceholdersShouldReturnPopulatedCollection()
			throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment,
				"bar=1,2,3");
		this.binder = new Binder(this.sources,
				new PropertySourcesPlaceholdersResolver(environment));
		this.sources.add(new MockConfigurationPropertySource("foo", "${bar}"));
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2, 3);

	}

	@Test
	public void bindToCollectionWhenCommaListAndIndexedShouldOnlyUseFirst()
			throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo", "1,2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "2");
		source2.put("foo[1]", "3");
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenIndexedAndCommaListShouldOnlyUseFirst()
			throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo[0]", "1");
		source1.put("foo[1]", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo", "2,3");
		List<Integer> result = this.binder.bind("foo", Bindable.of(INTEGER_LIST));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToCollectionWhenItemContainsCommasShouldReturnPopulatedCollection()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1,2");
		source.put("foo[1]", "3");
		this.sources.add(source);
		List<String> result = this.binder.bind("foo", Bindable.of(STRING_LIST));
		assertThat(result).containsExactly("1,2", "3");
	}

	@Test
	public void bindToArrayShouldReturnArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToCollectionShouldTriggerOnSuccess() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<List<Integer>> target = Bindable.of(INTEGER_LIST);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eqName("foo[0]"),
				eq(Bindable.of(Integer.class)), any(), notNull(), eq(1));
		inOrder.verify(handler).onSuccess(eqName("foo"), eq(target), any(), isNull(),
				isA(List.class));
	}

	@Test
	public void bindToArrayShouldReturnPrimativeArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "1");
		source.put("foo[1]", "2");
		source.put("foo[2]", "3");
		this.sources.add(source);
		int[] result = this.binder.bind("foo", Bindable.of(int[].class));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToArrayWhenNestedShouldReturnPopulatedArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		ResolvableType type = ResolvableType.forArrayComponent(INTEGER_ARRAY);
		Integer[][] result = this.binder.bind("foo", Bindable.of(type));
		assertThat(result).hasSize(2);
		assertThat(result[0]).containsExactly(1, 2);
		assertThat(result[1]).containsExactly(3, 4);
	}

	@Test
	public void bindToArrayWhenNestedListShouldReturnPopulatedArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0][0]", "1");
		source.put("foo[0][1]", "2");
		source.put("foo[1][0]", "3");
		source.put("foo[1][1]", "4");
		this.sources.add(source);
		ResolvableType type = ResolvableType.forArrayComponent(INTEGER_LIST);
		List<Integer>[] result = this.binder.bind("foo", Bindable.of(type));
		assertThat(result).hasSize(2);
		assertThat(result[0]).containsExactly(1, 2);
		assertThat(result[1]).containsExactly(3, 4);
	}

	@Test
	public void bindToArrayWhenNotInOrderShouldReturnPopulatedArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		this.sources.add(source);
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToArrayWhenNonSequentialShouldThrowException() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[0]", "2");
		source.put("foo[1]", "1");
		source.put("foo[3]", "3");
		this.sources.add(source);
		try {
			this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
			fail("No exception thrown");
		}
		catch (BindException ex) {
			Set<ConfigurationProperty> unbound =
					((UnboundConfigurationPropertiesException) ex
							.getCause()).getUnboundConfigurationProperties();
			assertThat(unbound.size()).isEqualTo(1);
			ConfigurationProperty property = unbound.iterator().next();
			assertThat(property.getName().toString()).isEqualTo("foo[3]");
			assertThat(property.getValue()).isEqualTo("3");
		}
	}

	@Test
	public void bindToArrayWhenNonIterableShouldReturnPopulatedArray() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo[1]", "2");
		source.put("foo[0]", "1");
		source.put("foo[2]", "3");
		source.setNonIterable(true);
		this.sources.add(source);
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToArrayWhenMultipleSourceShouldOnlyUseFirst() throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("bar", "baz");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "1");
		source2.put("foo[1]", "2");
		this.sources.add(source2);
		MockConfigurationPropertySource source3 = new MockConfigurationPropertySource();
		source3.put("foo[0]", "7");
		source3.put("foo[1]", "8");
		source3.put("foo[2]", "9");
		this.sources.add(source3);
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToArrayWhenHasExistingCollectionShouldReplaceAllContents()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1"));
		Integer[] existing = new Integer[2];
		existing[0] = 1000;
		existing[1] = 1001;
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY, existing));
		assertThat(result).containsExactly(1);
	}

	@Test
	public void bindToArrayWhenNoValueShouldReturnNull() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		Integer[] result = this.binder.bind("foo", Bindable.of(INTEGER_ARRAY));
		assertThat(result).isNull();
	}

	@Test
	public void bindToArrayWhenNoValueShouldReturnDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		Integer[] defaultValue = new Integer[] { 1 };
		Integer[] result = this.binder.bind("foo",
				Bindable.<Integer[]>of(INTEGER_ARRAY).withDefaultValue(defaultValue));
		assertThat(result).isEqualTo(defaultValue);
		assertThat(result).isSameAs(defaultValue);
	}

	@Test
	public void bindToArrayWhenNoValueShouldConvertDefaultValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("faf.bar", "1"));
		int[] defaultValue = new int[] { 123 };
		Integer[] result = this.binder.bind("foo",
				Bindable.<Integer[]>of(INTEGER_ARRAY).withDefaultValue(defaultValue));
		assertThat(result).isExactlyInstanceOf(Integer[].class);
		assertThat(result).containsExactly(123);
	}

	@Test
	public void bindToArrayShouldTriggerOnSuccess() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo[0]", "1", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<Integer[]> target = Bindable.of(INTEGER_ARRAY);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eqName("foo[0]"),
				eq(Bindable.of(Integer.class)), any(), notNull(), eq(1));
		inOrder.verify(handler).onSuccess(eqName("foo"), eq(target), any(), isNull(),
				isA(Integer[].class));
	}

	@Test
	public void bindToArrayWhenCommaListShouldReturnPopulatedCollection()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "1,2,3"));
		int[] result = this.binder.bind("foo", Bindable.of(int[].class));
		assertThat(result).containsExactly(1, 2, 3);
	}

	@Test
	public void bindToArrayWhenCommaListAndIndexedShouldOnlyUseFirst() throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo", "1,2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo[0]", "2");
		source2.put("foo[1]", "3");
		int[] result = this.binder.bind("foo", Bindable.of(int[].class));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToArrayWhenIndexedAndCommaListShouldOnlyUseFirst() throws Exception {
		MockConfigurationPropertySource source1 = new MockConfigurationPropertySource();
		source1.put("foo[0]", "1");
		source1.put("foo[1]", "2");
		this.sources.add(source1);
		MockConfigurationPropertySource source2 = new MockConfigurationPropertySource();
		source2.put("foo", "2,3");
		int[] result = this.binder.bind("foo", Bindable.of(int[].class));
		assertThat(result).containsExactly(1, 2);
	}

	@Test
	public void bindToJavaBeanShouldReturnPopulatedBean() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.value", "bar"));
		JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class));
		assertThat(result.getValue()).isEqualTo("bar");
	}

	@Test
	public void bindToJavaBeanWhenNonIterableShouldReturnPopulatedBean()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource(
				"foo.value", "bar");
		source.setNonIterable(true);
		this.sources.add(source);
		JavaBean result = this.binder.bind("foo", Bindable.of(JavaBean.class));
		assertThat(result.getValue()).isEqualTo("bar");
	}

	@Test
	public void bindToJavaBeanShouldTriggerOnSuccess() throws Exception {
		this.sources
				.add(new MockConfigurationPropertySource("foo.value", "bar", "line1"));
		BindHandler handler = mockBindHandler();
		Bindable<JavaBean> target = Bindable.of(JavaBean.class);
		this.binder.bind("foo", target, handler);
		InOrder inOrder = inOrder(handler);
		inOrder.verify(handler).onSuccess(eqName("foo.value"),
				eq(Bindable.of(String.class)), any(), notNull(), eq("bar"));
		inOrder.verify(handler).onSuccess(eqName("foo"), eq(target), any(), isNull(),
				isA(JavaBean.class));
	}

	@Test
	public void bindWhenHasMalformedDateShouldThrowException() throws Exception {
		this.thrown.expectCause(instanceOf(ConversionFailedException.class));
		this.sources.add(new MockConfigurationPropertySource("foo", "2014-04-01"));
		this.binder.bind("foo", Bindable.of(LocalDate.class));
	}

	@Test
	public void bindWhenHasAnnotationsShouldChangeConvertedValue() throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo", "2014-04-01"));
		DateTimeFormat annotation = AnnotationUtils.synthesizeAnnotation(
				Collections.singletonMap("iso", DateTimeFormat.ISO.DATE),
				DateTimeFormat.class, null);
		LocalDate result = this.binder.bind("foo",
				Bindable.of(LocalDate.class).withAnnotations(annotation));
		assertThat(result.toString()).isEqualTo("2014-04-01");
	}

	private BindHandler mockBindHandler() {
		return mock(BindHandler.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
	}

	private static ConfigurationPropertyName eqName(String name) {
		return eq(ConfigurationPropertyName.of(name));
	}

	public static class JavaBean {

		private String value;

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public enum ExampleEnum {

		FOO_BAR, BAR_BAZ, BAZ_BOO

	}

}
