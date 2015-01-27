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

package org.springframework.boot.xreload;

import org.springframework.boot.Reloader;

/**
 * {@link Reloader} implementation that replaces the classloader to offer dynamic
 * reloading.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class DefaultReloader extends Reloader {

	private static final String[] BANNER = new String[] {
			" +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++",
			"                              _                 _                         ",
			"   /\\    |_ _  _ _  _ |_. _  |_) _ | _  _  _|  (_ _  _ |_ | _  _|         ",
			"  /—-\\!_|| (_)| | |(_|| |(_  | \\(/_|(_)(_|(_|  (_| |(_||_)|(/_(_| . . . .",
			"                                                                          ",
			"  Monitoring for local file changes.  LiveReload Server Started           ",
			"                                                                          ",
			" +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" };

	private ReloadLog log = new ReloadLog();

	@Override
	protected void start(String[] args) {

	}

	private void assertIsMainThread() {
	}

}
