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

package org.springframework.boot.reload.reloader;

/**
 * Banner printed when reloading is enabled.
 *
 * @author Phillip Webb
 */
public class ReloadBanner {

	private static final String LINE = " +++++++++++++++++++++++++++++++++++++++"
			+ "++++++++++++++++++++++++++++++++++";

	private static final String[] BANNER = new String[] {
			"                              _                 _                         ",
			"   /\\    |_ _  _ _  _ |_. _  |_) _ | _  _  _|  (_ _  _ |_ | _  _|         ",
			"  /—-\\!_|| (_)| | |(_|| |(_  | \\(/_|(_)(_|(_|  (_| |(_||_)|(/_(_| . . . .",
			"                                                                          " };

	public static void print() {
		System.out.println(LINE);
		for (String line : BANNER) {
			System.out.println(line);
		}
		System.out.println(LINE);
	}

}
