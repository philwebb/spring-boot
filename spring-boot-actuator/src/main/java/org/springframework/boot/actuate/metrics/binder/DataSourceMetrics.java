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

package org.springframework.boot.actuate.metrics.binder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProviders;

/**
 * A {@link MeterBinder} for a {@link DataSource}.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
public class DataSourceMetrics implements MeterBinder {

	/**
	 * Instrumented pools kept to prevents the poolMetadata that we base the gauges on
	 * from being garbage collected.
	 */
	private static Collection<DataSourcePoolMetadata> instrumentedPools = new ArrayList<>();

	private final String name;

	private final Iterable<Tag> tags;

	private final DataSourcePoolMetadata poolMetadata;

	public DataSourceMetrics(DataSource dataSource,
			Collection<DataSourcePoolMetadataProvider> metadataProviders, String name,
			Iterable<Tag> tags) {
		this.name = name;
		this.tags = tags;
		this.poolMetadata = new DataSourcePoolMetadataProviders(metadataProviders)
				.getDataSourcePoolMetadata(dataSource);
		instrumentedPools.add(this.poolMetadata);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		if (this.poolMetadata != null) {
			bindPoolMetadata(registry, "active", DataSourcePoolMetadata::getActive);
			bindPoolMetadata(registry, "max", DataSourcePoolMetadata::getMax);
			bindPoolMetadata(registry, "min", DataSourcePoolMetadata::getMin);
		}
	}

	private <N extends Number> void bindPoolMetadata(MeterRegistry registry, String name,
			Function<DataSourcePoolMetadata, N> function) {
		if (function.apply(this.poolMetadata) != null) {
			registry.gauge(this.name + "." + name + ".connections", this.tags,
					this.poolMetadata, (m) -> function.apply(m).doubleValue());
		}
	}

}
