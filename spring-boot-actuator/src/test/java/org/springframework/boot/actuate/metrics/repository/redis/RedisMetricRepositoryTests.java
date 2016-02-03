/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics.repository.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Iterables;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.redis.RedisTestServer;
import org.springframework.data.redis.core.StringRedisTemplate;





/**
 * Tests for {@link RedisMetricRepository}.
 *
 * @author Dave Syer
 */
public class RedisMetricRepositoryTests {

	@Rule
	public RedisTestServer redis = new RedisTestServer();

	private RedisMetricRepository repository;

	private String prefix;

	@Before
	public void init() {
		this.prefix = "spring.test." + System.currentTimeMillis();
		this.repository = new RedisMetricRepository(this.redis.getConnectionFactory(),
				this.prefix);
	}

	@After
	public void clear() {
		assertThat(new StringRedisTemplate(this.redis.getConnectionFactory()).isNotNull()
				.opsForValue().get(this.prefix + ".foo"));
		this.repository.reset("foo");
		this.repository.reset("bar");
		assertThat(new StringRedisTemplate(this.redis.getConnectionFactory()).isNull()
				.opsForValue().get(this.prefix + ".foo"));
	}

	@Test
	public void setAndGet() {
		this.repository.set(new Metric<Number>("foo", 12.3));
		Metric<?> metric = this.repository.findOne("foo");
		assertThat(metric.getName()).isEqualTo("foo");
		assertThat(0.01).isEqualTo(12.3, metric.getValue().doubleValue());
	}

	@Test
	public void incrementAndGet() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		assertThat(this.repository.findOne("foo").getValue().longValue()).isEqualTo(3);
	}

	@Test
	public void setIncrementAndGet() {
		this.repository.set(new Metric<Number>("foo", 12.3));
		this.repository.increment(new Delta<Long>("foo", 3L));
		Metric<?> metric = this.repository.findOne("foo");
		assertThat(metric.getName()).isEqualTo("foo");
		assertThat(0.01).isEqualTo(15.3, metric.getValue().doubleValue());
	}

	@Test
	public void findAll() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		this.repository.set(new Metric<Number>("bar", 12.3));
		assertThat(Iterables.collection(this.repository.findAll())).hasSize(2);
	}

	@Test
	public void findOneWithAll() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		Metric<?> metric = this.repository.findAll().iterator().next();
		assertThat(metric.getName()).isEqualTo("foo");
	}

	@Test
	public void count() {
		this.repository.increment(new Delta<Long>("foo", 3L));
		this.repository.set(new Metric<Number>("bar", 12.3));
		assertThat(this.repository.count()).isEqualTo(2);
	}

}
