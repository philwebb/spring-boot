/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.properties.source;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.context.properties.source.XConfigurationPropertyCache.ThreadLocalCaching;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link XConfigurationPropertyCacheDelegate}.
 *
 * @author Phillip Webb
 */
class XConfigurationPropertyCacheDelegateTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2020-01-02T09:00:00Z"), ZoneOffset.UTC);

	private Map<ConfigurationPropertySource, AtomicInteger> itemCount = new HashMap<>();

	private XConfigurationPropertyCacheDelegate cache = new XConfigurationPropertyCacheDelegate(FIXED_CLOCK);

	@Mock
	private ConfigurationPropertySource source;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void setTimeToLiveWhenNullDisablesGlobalCaching() {
		this.cache.setTimeToLive(null);
		assertThat(getIndexFromCache()).isEqualTo(0);
		assertThat(getIndexFromCache()).isEqualTo(1);
	}

	@Test
	void setTimeToLiveChangesCacheTime() {
		this.cache.setTimeToLive(Duration.ofHours(1));
		assertThat(getIndexFromCache()).isEqualTo(0);
		this.cache.offsetClock(Duration.ofMinutes(10));
		assertThat(getIndexFromCache()).isEqualTo(0);
		this.cache.offsetClock(Duration.ofHours(1));
		assertThat(getIndexFromCache()).isEqualTo(1);
	}

	@Test
	void withThreadLocalCacheRunnableRunsWithThreadLocal() {
		this.cache.setTimeToLive(null);
		assertThat(getIndexFromCache()).isEqualTo(0);
		this.cache.withThreadLocalCache(() -> {
			assertThat(getIndexFromCache()).isEqualTo(1);
			assertThat(getIndexFromCache()).isEqualTo(1);
		});
		assertThat(getIndexFromCache()).isEqualTo(2);
	}

	@Test
	void withThreadLocalCacheStartsThreadLocalCaching() {
		this.cache.setTimeToLive(null);
		assertThat(getIndexFromCache()).isEqualTo(0);
		ThreadLocalCaching threadLocalCache = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(1);
		assertThat(getIndexFromCache()).isEqualTo(1);
		threadLocalCache.close();
		assertThat(getIndexFromCache()).isEqualTo(2);
	}

	@Test
	void withThreadLocalCacheWhenAlreadyUsingThreadLocalCacheNests() {
		this.cache.setTimeToLive(null);
		assertThat(getIndexFromCache()).isEqualTo(0);
		ThreadLocalCaching outer = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(1);
		assertThat(getIndexFromCache()).isEqualTo(1);
		ThreadLocalCaching inner = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(1);
		inner.close();
		assertThat(getIndexFromCache()).isEqualTo(1);
		outer.close();
		assertThat(getIndexFromCache()).isEqualTo(2);
	}

	@Test
	void clearClearsCaches() {
		this.cache.setTimeToLive(Duration.ofHours(1));
		assertThat(getIndexFromCache()).isEqualTo(0);
		assertThat(getIndexFromCache()).isEqualTo(0);
		this.cache.clear();
		assertThat(getIndexFromCache()).isEqualTo(1);
	}

	@Test
	void clearPropertySourceClearsIndividualCache() {
		MapPropertySource m1 = new MapPropertySource("m1", new LinkedHashMap<>());
		MapPropertySource m2 = new MapPropertySource("m2", new LinkedHashMap<>());
		SpringConfigurationPropertySource s1 = SpringConfigurationPropertySource.from(m1);
		SpringConfigurationPropertySource s2 = SpringConfigurationPropertySource.from(m2);
		this.cache.setTimeToLive(Duration.ofHours(1));
		assertThat(getIndexFromCache(s1)).isEqualTo(0);
		assertThat(getIndexFromCache(s1)).isEqualTo(0);
		assertThat(getIndexFromCache(s2)).isEqualTo(0);
		assertThat(getIndexFromCache(s2)).isEqualTo(0);
		this.cache.clear(m2);
		assertThat(getIndexFromCache(s1)).isEqualTo(0);
		assertThat(getIndexFromCache(s2)).isEqualTo(1);
	}

	@Test
	void getWhenHasThreadLocalCacheGetsFromThreadLocal() {
		this.cache.setTimeToLive(null);
		ThreadLocalCaching threadLocalCache = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(0);
		assertThat(getIndexFromCache()).isEqualTo(0);
		threadLocalCache.close();
		assertThat(getIndexFromCache()).isEqualTo(1);
	}

	@Test
	void getWhenHasThreadLocalCacheUpdatesGlobalCache() {
		this.cache.setTimeToLive(Duration.ofHours(1));
		ThreadLocalCaching threadLocalCache = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(0);
		assertThat(getIndexFromCache()).isEqualTo(0);
		threadLocalCache.close();
		assertThat(getIndexFromCache()).isEqualTo(0);
	}

	@Test
	void getWhenWhenHasThreadLocalMissUsesGlobalCache() {
		this.cache.setTimeToLive(Duration.ofHours(1));
		assertThat(getIndexFromCache()).isEqualTo(0);
		ThreadLocalCaching threadLocalCache = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(0);
		threadLocalCache.close();
		assertThat(getIndexFromCache()).isEqualTo(0);
	}

	@Test
	void getWhenMissUpdatesGlobalAndThreadLocalCache() {
		this.cache.setTimeToLive(Duration.ofHours(1));
		ThreadLocalCaching threadLocalCache = this.cache.withThreadLocalCache();
		assertThat(getIndexFromCache()).isEqualTo(0);
		threadLocalCache.close();
		assertThat(getIndexFromCache()).isEqualTo(0);
	}

	@Test
	void getWhenHasNoThreadLocalCacheAndNoTimeToLiveAlwaysReturnsNew() {
		this.cache.setTimeToLive(null);
		assertThat(getIndexFromCache()).isEqualTo(0);
		assertThat(getIndexFromCache()).isEqualTo(1);
		assertThat(getIndexFromCache()).isEqualTo(2);
	}

	private int getIndexFromCache() {
		return getIndexFromCache(this.source);
	}

	private int getIndexFromCache(ConfigurationPropertySource source) {
		return this.cache.get(source, TestItem.class, () -> createTestItem(source)).getIndex();
	}

	private TestItem createTestItem(ConfigurationPropertySource source) {
		AtomicInteger count = this.itemCount.computeIfAbsent(source, (key) -> new AtomicInteger());
		return new TestItem(count.getAndIncrement());
	}

	static class TestItem {

		private final int index;

		TestItem(int index) {
			this.index = index;
		}

		int getIndex() {
			return this.index;
		}

	}

}
