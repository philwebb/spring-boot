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

package org.springframework.boot.actuate.metrics.writer;

import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link InfluxDBMetricWriter}.
 *
 * @author Mateusz Klimaszewski
 */
public class InfluxDBMetricWriterTests {

	private InfluxDB influx = mock(InfluxDB.class);

	@Test
	public void builderNonDefaultOptions() {
		new InfluxDBMetricWriter.Builder(this.influx).databaseName("testDatabaseName")
				.batchActions(2000).flushDuration(10, TimeUnit.MILLISECONDS)
				.logLevel(InfluxDB.LogLevel.FULL).build();
		verify(this.influx).createDatabase("testDatabaseName");
		verify(this.influx).enableBatch(2000, 10, TimeUnit.MILLISECONDS);
		verify(this.influx).setLogLevel(InfluxDB.LogLevel.FULL);
	}

	@Test
	public void setMetric() {
		InfluxDBMetricWriter writer = new InfluxDBMetricWriter.Builder(this.influx)
				.build();
		Metric<Number> metric = new Metric<Number>("testName", 1);
		writer.set(metric);
		verify(this.influx, times(1)).write(anyString(), eq(metric.getName()),
				any(Point.class));
	}

}
