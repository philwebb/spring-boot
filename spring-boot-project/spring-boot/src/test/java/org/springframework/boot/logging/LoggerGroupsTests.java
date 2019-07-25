/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LoggerGroups}
 *
 * @author HaiTao Zhang
 */
public class LoggerGroupsTests {

	private LoggingSystem loggingSystem = mock(LoggingSystem.class);

	@Test
	void updateLoggerGroupWithTheConfiguredLevelToAllMembers() {
		Map<String, LoggerGroups.LoggerGroup> groups = Collections.singletonMap("test",
				new LoggerGroups.LoggerGroup("test", Arrays.asList("test.member", "test.member2"), LogLevel.DEBUG));
		LoggerGroups loggerGroups = new LoggerGroups(this.loggingSystem, groups);
		loggerGroups.updateGroupLevel("test", LogLevel.WARN);
		verify(this.loggingSystem).setLogLevel("test.member", LogLevel.WARN);
		verify(this.loggingSystem).setLogLevel("test.member2", LogLevel.WARN);
	}

}
