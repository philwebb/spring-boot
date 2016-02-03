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

package org.springframework.boot.actuate.endpoint;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;



/**
 * Tests for {@link SystemPublicMetrics}
 *
 * @author Stephane Nicoll
 */
public class SystemPublicMetricsTests {

	@Test
	public void testSystemMetrics() throws Exception {
		SystemPublicMetrics publicMetrics = new SystemPublicMetrics();
		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : publicMetrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertThat(results.containsKey("mem")).isTrue();
		assertThat(results.containsKey("mem.free")).isTrue();
		assertThat(results.containsKey("processors")).isTrue();
		assertThat(results.containsKey("uptime")).isTrue();
		assertThat(results.containsKey("systemload.average")).isTrue();

		assertThat(results.containsKey("heap.committed")).isTrue();
		assertThat(results.containsKey("heap.init")).isTrue();
		assertThat(results.containsKey("heap.used")).isTrue();
		assertThat(results.containsKey("heap")).isTrue();

		assertThat(results.containsKey("nonheap.committed")).isTrue();
		assertThat(results.containsKey("nonheap.init")).isTrue();
		assertThat(results.containsKey("nonheap.used")).isTrue();
		assertThat(results.containsKey("nonheap")).isTrue();

		assertThat(results.containsKey("threads.peak")).isTrue();
		assertThat(results.containsKey("threads.daemon")).isTrue();
		assertThat(results.containsKey("threads.totalStarted")).isTrue();
		assertThat(results.containsKey("threads")).isTrue();

		assertThat(results.containsKey("classes.loaded")).isTrue();
		assertThat(results.containsKey("classes.unloaded")).isTrue();
		assertThat(results.containsKey("classes")).isTrue();
	}

}
