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

package org.springframework.boot.reload.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple log used by the reloader.
 *
 * @author Phillip Webb
 */
public class Log {

	private static Logger logger = null;

	public static void setEnabled(boolean enabled) {
		logger = (enabled ? new Logger() : null);
	}

	public static void debug(String message) {
		debug(message, null);
	}

	public static void debug(String message, Exception ex) {
		if (logger != null) {
			logger.debug(message, ex);
		}
	}

	private static class Logger {

		public void debug(String message, Exception ex) {
			String stackTrace = "";
			if (ex != null) {
				StringWriter writer = new StringWriter();
				ex.printStackTrace(new PrintWriter(writer));
				stackTrace = "\n" + writer.toString();
			}
			System.err.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
					.format(new Date()) + " " + message + stackTrace);
		}

	}

}
