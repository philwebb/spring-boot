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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.format.annotation.DateTimeFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JavaBeanBinder}.
 *
 * @author Phillip Webb
 */
public class JavaBeanBinderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	private ConfigurationPropertyName name;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
		this.name = ConfigurationPropertyName.of("foo");
	}

	@Test
	public void bindToClassShouldCreateBoundBean() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = bind(Bindable.of(ExampleValueBean.class), this.binder);
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	public void bindToClassWhenHasNoPrefixShouldCreateBoundBean() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("int-value", "12");
		source.put("long-value", "34");
		source.put("string-value", "foo");
		source.put("enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind(ConfigurationPropertyName.of(""),
				Bindable.of(ExampleValueBean.class));
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	public void bindToInstanceShouldBindToInstance() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		ExampleValueBean bean = new ExampleValueBean();
		ExampleValueBean boundBean = bind(Bindable.of(ExampleValueBean.class, bean),
				this.binder);
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	public void bindToInstanceWithNoPropertiesShouldReturnExistingInstance()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source);
		ExampleDefaultsBean bean = new ExampleDefaultsBean();
		ExampleDefaultsBean boundBean = bind(Bindable.of(ExampleDefaultsBean.class, bean),
				this.binder);
		assertThat(boundBean).isSameAs(bean);
		assertThat(boundBean.getFoo()).isEqualTo(123);
		assertThat(boundBean.getBar()).isEqualTo(456);
	}

	@Test
	public void bindToClassShouldLeaveDefaults() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "999");
		this.sources.add(source);
		ExampleDefaultsBean bean = bind(Bindable.of(ExampleDefaultsBean.class),
				this.binder);
		assertThat(bean.getFoo()).isEqualTo(123);
		assertThat(bean.getBar()).isEqualTo(999);
	}

	@Test
	public void bindToExistingInstanceShouldLeaveDefaults() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.bar", "999");
		this.sources.add(source);
		ExampleDefaultsBean bean = new ExampleDefaultsBean();
		bean.setFoo(888);
		ExampleDefaultsBean boundBean = bind(Bindable.of(ExampleDefaultsBean.class, bean),
				this.binder);
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getFoo()).isEqualTo(888);
		assertThat(bean.getBar()).isEqualTo(999);
	}

	@Test
	public void bindToClassShouldBindToMap() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.map.foo-bar", "1");
		source.put("foo.map.bar-baz", "2");
		this.sources.add(source);
		ExampleMapBean bean = bind(Bindable.of(ExampleMapBean.class), this.binder);
		assertThat(bean.getMap()).containsExactly(entry(ExampleEnum.FOO_BAR, 1),
				entry(ExampleEnum.BAR_BAZ, 2));
	}

	@Test
	public void bindToClassShouldBindToList() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[1]", "bar-baz");
		this.sources.add(source);
		ExampleListBean bean = bind(Bindable.of(ExampleListBean.class), this.binder);
		assertThat(bean.getList()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToListIfUnboundElementsPresentShouldThrowException()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[2]", "bar-baz");
		this.sources.add(source);
		this.thrown.expect(BindException.class);
		this.thrown.expectCause(
				Matchers.instanceOf(UnboundConfigurationPropertiesException.class));
		bind(Bindable.of(ExampleListBean.class), this.binder);
	}

	@Test
	public void bindToClassShouldBindToSet() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.set[0]", "foo-bar");
		source.put("foo.set[1]", "bar-baz");
		this.sources.add(source);
		ExampleSetBean bean = bind(Bindable.of(ExampleSetBean.class), this.binder);
		assertThat(bean.getSet()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToClassShouldBindToCollection() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.collection[0]", "foo-bar");
		source.put("foo.collection[1]", "bar-baz");
		this.sources.add(source);
		ExampleCollectionBean bean = bind(Bindable.of(ExampleCollectionBean.class),
				this.binder);
		assertThat(bean.getCollection()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToClassWhenHasNoSetterShouldBindToMap() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.map.foo-bar", "1");
		source.put("foo.map.bar-baz", "2");
		this.sources.add(source);
		ExampleMapBeanWithoutSetter bean = bind(
				Bindable.of(ExampleMapBeanWithoutSetter.class), this.binder);
		assertThat(bean.getMap()).containsExactly(entry(ExampleEnum.FOO_BAR, 1),
				entry(ExampleEnum.BAR_BAZ, 2));
	}

	@Test
	public void bindToClassWhenHasNoSetterShouldBindToList() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.list[0]", "foo-bar");
		source.put("foo.list[1]", "bar-baz");
		this.sources.add(source);
		ExampleListBeanWithoutSetter bean = bind(
				Bindable.of(ExampleListBeanWithoutSetter.class), this.binder);
		assertThat(bean.getList()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToClassWhenHasNoSetterShouldBindToSet() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.set[0]", "foo-bar");
		source.put("foo.set[1]", "bar-baz");
		this.sources.add(source);
		ExampleSetBeanWithoutSetter bean = bind(
				Bindable.of(ExampleSetBeanWithoutSetter.class), this.binder);
		assertThat(bean.getSet()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToClassWhenHasNoSetterShouldBindToCollection() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.collection[0]", "foo-bar");
		source.put("foo.collection[1]", "bar-baz");
		this.sources.add(source);
		ExampleCollectionBeanWithoutSetter bean = bind(
				Bindable.of(ExampleCollectionBeanWithoutSetter.class), this.binder);
		assertThat(bean.getCollection()).containsExactly(ExampleEnum.FOO_BAR,
				ExampleEnum.BAR_BAZ);
	}

	@Test
	public void bindToClassShouldBindNested() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBean bean = bind(Bindable.of(ExampleNestedBean.class), this.binder);
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWhenIterableShouldBindNestedBasedOnInstance()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBeanWithoutSetterOrType bean = bind(
				Bindable.of(ExampleNestedBeanWithoutSetterOrType.class), this.binder);
		ExampleValueBean valueBean = (ExampleValueBean) bean.getValueBean();
		assertThat(valueBean.getIntValue()).isEqualTo(123);
		assertThat(valueBean.getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWhenNotIterableShouldNotBindNestedBasedOnInstance()
			throws Exception {
		// If we can't tell that binding will happen, we don't want to randomly invoke
		// getters on the class and cause side effects
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		source.setNonIterable(true);
		this.sources.add(source);
		ExampleNestedBeanWithoutSetterOrType bean = bind(
				Bindable.of(ExampleNestedBeanWithoutSetterOrType.class), this.binder);
		assertThat(bean).isNull();
	}

	@Test
	public void bindToClassWhenHasNoSetterShouldBindNested() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBeanWithoutSetter bean = bind(
				Bindable.of(ExampleNestedBeanWithoutSetter.class), this.binder);
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWhenHasNoSetterAndImmutableShouldThrowException()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.nested.foo", "bar");
		this.sources.add(source);
		this.thrown.expect(BindException.class);
		bind(Bindable.of(ExampleImmutableNestedBeanWithoutSetter.class), this.binder);
	}

	@Test
	public void bindToInstanceWhenNoNestedShouldLeaveNestedAsNull() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("faf.value-bean.int-value", "123");
		this.sources.add(source);
		ExampleNestedBean bean = new ExampleNestedBean();
		ExampleNestedBean boundBean = bind(Bindable.of(ExampleNestedBean.class, bean),
				this.binder);
		assertThat(boundBean).isEqualTo(bean);
		assertThat(bean.getValueBean()).isNull();
	}

	@Test
	public void bindToClassWhenPropertiesMissingShouldReturnNull() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("faf.int-value", "12");
		this.sources.add(source);
		ExampleValueBean bean = bind(Bindable.of(ExampleValueBean.class), this.binder);
		assertThat(bean).isNull();
	}

	@Test
	public void bindToClassWhenNoDefaultConstructorShouldReturnNull() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "bar");
		this.sources.add(source);
		ExampleWithNonDefaultConstructor bean = bind(
				Bindable.of(ExampleWithNonDefaultConstructor.class), this.binder);
		assertThat(bean).isNull();
	}

	@Test
	public void bindToInstanceWhenNoDefaultConstructorShouldBind() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "bar");
		this.sources.add(source);
		ExampleWithNonDefaultConstructor bean = new ExampleWithNonDefaultConstructor(
				"faf");
		ExampleWithNonDefaultConstructor boundBean = bind(
				Bindable.of(ExampleWithNonDefaultConstructor.class, bean), this.binder);
		assertThat(boundBean).isSameAs(bean);
		assertThat(bean.getValue()).isEqualTo("bar");
	}

	@Test
	public void bindToClassShouldBindHierarchy() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "123");
		source.put("foo.long-value", "456");
		this.sources.add(source);
		ExampleSubclassBean bean = bind(Bindable.of(ExampleSubclassBean.class),
				this.binder);
		assertThat(bean.getIntValue()).isEqualTo(123);
		assertThat(bean.getLongValue()).isEqualTo(456);
	}

	@Test
	public void bindToClassWhenPropertyCannotBeConvertedShouldThrowException()
			throws Exception {
		this.sources.add(new MockConfigurationPropertySource("foo.int-value", "foo"));
		this.thrown.expect(BindException.class);
		bind(Bindable.of(ExampleValueBean.class), this.binder);
	}

	@Test
	public void bindToClassWhenPropertyCannotBeConvertedAndIgnoreErrorsShouldNotSetValue()
			throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "bang");
		source.put("foo.string-value", "foo");
		source.put("foo.enum-value", "foo-bar");
		this.sources.add(source);
		IgnoreErrorsBindHandler handler = new IgnoreErrorsBindHandler();
		ExampleValueBean bean = this.binder.bind(this.name,
				Bindable.of(ExampleValueBean.class), handler);
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(0);
		assertThat(bean.getStringValue()).isEqualTo("foo");
		assertThat(bean.getEnumValue()).isEqualTo(ExampleEnum.FOO_BAR);
	}

	@Test
	public void bindToClassWhenMismatchedGetSetShouldBind() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "123");
		this.sources.add(source);
		ExampleMismatchBean bean = bind(Bindable.of(ExampleMismatchBean.class),
				this.binder);
		assertThat(bean.getValue()).isEqualTo("123");
	}

	@Test
	public void bindToClassShouldNotInvokeExtraMethods() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource(
				"foo.value", "123");
		source.setNonIterable(true);
		this.sources.add(source);
		ExampleWithThrowingGetters bean = bind(
				Bindable.of(ExampleWithThrowingGetters.class), this.binder);
		assertThat(bean.getValue()).isEqualTo(123);
	}

	@Test
	public void bindToClassWithSelfReferenceShouldBind() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value", "123");
		this.sources.add(source);
		ExampleWithSelfReference bean = bind(Bindable.of(ExampleWithSelfReference.class),
				this.binder);
		assertThat(bean.getValue()).isEqualTo(123);
	}

	@Test
	public void bindtoInstanceWithExistingValueShouldReturnExisting() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		this.sources.add(source);
		ExampleNestedBean existingValue = new ExampleNestedBean();
		ExampleValueBean valueBean = new ExampleValueBean();
		existingValue.setValueBean(valueBean);
		ExampleNestedBean bean = bind(Bindable.of(ExampleNestedBean.class, existingValue),
				this.binder);
		assertThat(bean.getValueBean()).isEqualTo(valueBean);
	}

	@Test
	public void bindWithAnnotations() throws Exception {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.date", "2014-04-01");
		this.sources.add(source);
		ConverterAnnotatedExampleBean bean = bind(
				Bindable.of(ConverterAnnotatedExampleBean.class), this.binder);
		assertThat(bean.getDate().toString()).isEqualTo("2014-04-01");
	}

	private <T> T bind(Bindable<T> bindable, Binder binder) {
		return binder.bind(this.name, bindable);
	}

	public static class ExampleValueBean {

		private int intValue;

		private long longValue;

		private String stringValue;

		private ExampleEnum enumValue;

		public int getIntValue() {
			return this.intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		public long getLongValue() {
			return this.longValue;
		}

		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}

		public String getStringValue() {
			return this.stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public ExampleEnum getEnumValue() {
			return this.enumValue;
		}

		public void setEnumValue(ExampleEnum enumValue) {
			this.enumValue = enumValue;
		}

	}

	public static class ExampleDefaultsBean {

		private int foo = 123;

		private int bar = 456;

		public int getFoo() {
			return this.foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}

		public int getBar() {
			return this.bar;
		}

		public void setBar(int bar) {
			this.bar = bar;
		}

	}

	public static class ExampleMapBean {

		private Map<ExampleEnum, Integer> map;

		public Map<ExampleEnum, Integer> getMap() {
			return this.map;
		}

		public void setMap(Map<ExampleEnum, Integer> map) {
			this.map = map;
		}

	}

	public static class ExampleListBean {

		private List<ExampleEnum> list;

		public List<ExampleEnum> getList() {
			return this.list;
		}

		public void setList(List<ExampleEnum> list) {
			this.list = list;
		}

	}

	public static class ExampleSetBean {

		private Set<ExampleEnum> set;

		public Set<ExampleEnum> getSet() {
			return this.set;
		}

		public void setSet(Set<ExampleEnum> set) {
			this.set = set;
		}

	}

	public static class ExampleCollectionBean {

		private Collection<ExampleEnum> collection;

		public Collection<ExampleEnum> getCollection() {
			return this.collection;
		}

		public void setCollection(Collection<ExampleEnum> collection) {
			this.collection = collection;
		}

	}

	public static class ExampleMapBeanWithoutSetter {

		private Map<ExampleEnum, Integer> map = new LinkedHashMap<>();

		public Map<ExampleEnum, Integer> getMap() {
			return this.map;
		}

	}

	public static class ExampleListBeanWithoutSetter {

		private List<ExampleEnum> list = new ArrayList<>();

		public List<ExampleEnum> getList() {
			return this.list;
		}

	}

	public static class ExampleSetBeanWithoutSetter {

		private Set<ExampleEnum> set = new LinkedHashSet<>();

		public Set<ExampleEnum> getSet() {
			return this.set;
		}

	}

	public static class ExampleCollectionBeanWithoutSetter {

		private Collection<ExampleEnum> collection = new ArrayList<>();

		public Collection<ExampleEnum> getCollection() {
			return this.collection;
		}

	}

	public static class ExampleNestedBean {

		private ExampleValueBean valueBean;

		public ExampleValueBean getValueBean() {
			return this.valueBean;
		}

		public void setValueBean(ExampleValueBean valueBean) {
			this.valueBean = valueBean;
		}

	}

	public static class ExampleNestedBeanWithoutSetter {

		private ExampleValueBean valueBean = new ExampleValueBean();

		public ExampleValueBean getValueBean() {
			return this.valueBean;
		}

	}

	public static class ExampleNestedBeanWithoutSetterOrType {

		private ExampleValueBean valueBean = new ExampleValueBean();

		public Object getValueBean() {
			return this.valueBean;
		}

	}

	public static class ExampleImmutableNestedBeanWithoutSetter {

		private NestedImmutable nested = new NestedImmutable();

		public NestedImmutable getNested() {
			return this.nested;
		}

		public static class NestedImmutable {

			public String getFoo() {
				return "foo";
			}

		}

	}

	public static class ExampleWithNonDefaultConstructor {

		private String value;

		public ExampleWithNonDefaultConstructor(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	public abstract static class ExampleSuperClassBean {

		private int intValue;

		public int getIntValue() {
			return this.intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

	}

	public static class ExampleSubclassBean extends ExampleSuperClassBean {

		private long longValue;

		public long getLongValue() {
			return this.longValue;
		}

		public void setLongValue(long longValue) {
			this.longValue = longValue;
		}

	}

	public static class ExampleMismatchBean {

		private int value;

		public String getValue() {
			return String.valueOf(this.value);
		}

		public void setValue(int value) {
			this.value = value;
		}

	}

	public static class ExampleWithThrowingGetters {

		private int value;

		public int getValue() {
			return this.value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public List<String> getNames() {
			throw new RuntimeException();
		}

		public ExampleValueBean getNested() {
			throw new RuntimeException();
		}

	}

	public static class ExampleWithSelfReference {

		private int value;

		private ExampleWithSelfReference self;

		public int getValue() {
			return this.value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public ExampleWithSelfReference getSelf() {
			return this.self;
		}

		public void setSelf(ExampleWithSelfReference self) {
			this.self = self;
		}

	}

	public enum ExampleEnum {

		FOO_BAR,

		BAR_BAZ

	}

	public static class ConverterAnnotatedExampleBean {

		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		private LocalDate date;

		public LocalDate getDate() {
			return this.date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

	}

}
