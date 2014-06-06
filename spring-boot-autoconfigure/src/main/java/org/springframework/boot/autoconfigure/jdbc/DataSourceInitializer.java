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

package org.springframework.boot.autoconfigure.jdbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

/**
 * Bean to handle {@link DataSource} initialization by running {@literal schema.sql} and
 * {@literal data.sql} SQL scripts (including platform specific variants).
 * 
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.1.0
 * @see DataSourceAutoConfiguration
 */
class DataSourceInitializer implements ApplicationListener<DataSourceInitializedEvent> {

	private static Log logger = LogFactory.getLog(DataSourceInitializer.class);

	private DataSource dataSource;

	private DataSourceProperties properties;

	@Autowired
	private ApplicationContext applicationContext;

	private boolean initialized = false;

	public DataSourceInitializer(DataSourceProperties properties, DataSource dataSource) {
		this.properties = properties;
		this.dataSource = dataSource;
	}

	@PostConstruct
	protected void initialize() {
		if (!this.properties.isInitialize()) {
			logger.debug("Initialization disabled (not running DDL scripts)");
			return;
		}
		if (this.dataSource == null) {
			logger.debug("No DataSource found so not initializing");
			return;
		}
		runSchemaScripts();
		this.applicationContext.publishEvent(new DataSourceInitializedEvent(
				this.dataSource));
	}

	@Override
	public void onApplicationEvent(DataSourceInitializedEvent event) {
		// NOTE the even can happen more than once
		if (!this.initialized) {
			runDataScripts();
			this.initialized = true;
		}
	}

	private void runSchemaScripts() {
		runScriptOrUseFallback(this.properties.getSchema(), "schema");
	}

	private void runDataScripts() {
		runScriptOrUseFallback(this.properties.getData(), "data");
	}

	private void runScriptOrUseFallback(String schema, String fallBackResource) {
		if (schema == null) {
			String platform = this.properties.getPlatform();
			schema = "classpath*:" + fallBackResource + "-" + platform + ".sql,";
			schema += "classpath*:" + fallBackResource + ".sql";
		}
		List<Resource> resources = getSchemaResources(schema);
		runScripts(resources);
	}

	private List<Resource> getSchemaResources(String schema) {
		List<Resource> resources = new ArrayList<Resource>();
		for (String schemaLocation : StringUtils.commaDelimitedListToStringArray(schema)) {
			try {
				resources.addAll(Arrays.asList(this.applicationContext
						.getResources(schemaLocation)));
			}
			catch (IOException ex) {
				throw new IllegalStateException("Unable to load resource from "
						+ schemaLocation, ex);
			}
		}
		return resources;
	}

	private void runScripts(List<Resource> resources) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(this.properties.isContinueOnError());
		populator.setSeparator(this.properties.getSeparator());
		boolean exists = false;
		for (Resource resource : resources) {
			if (resource.exists()) {
				exists = true;
				populator.addScript(resource);
			}
		}
		if (exists) {
			DatabasePopulatorUtils.execute(populator, this.dataSource);
		}
	}

}
