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

package org.springframework.boot.reload;

import org.springframework.boot.Reloader;
import org.springframework.boot.reload.log.Log;

/**
 * Default {@link Reloader} for use with Spring Boot applications.
 *
 * @author Phillip Webb
 */
public class SpringBootReloader extends Reloader {

	@Override
	protected void start(String[] args) {
		ReloadProperties properties = new ReloadProperties();
		Log.setEnabled(properties.isDebug());
		Log.debug("Starting SpringBootReloader");
		if (properties.isShowBanner()) {
			ReloadBanner.print();
		}
	}

}
