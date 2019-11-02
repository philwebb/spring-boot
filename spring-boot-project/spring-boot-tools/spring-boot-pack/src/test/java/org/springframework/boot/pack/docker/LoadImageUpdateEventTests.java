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

package org.springframework.boot.pack.docker;

import org.junit.jupiter.api.Test;

import org.springframework.boot.pack.docker.ProgressUpdateEvent.ProgressDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadImageUpdateEvent}.
 *
 * @author Phillip Webb
 */
class LoadImageUpdateEventTests extends ProgressUpdateEventTests {

	@Test
	void getStreamReturnsStream() {
		LoadImageUpdateEvent event = (LoadImageUpdateEvent) createEvent();
		assertThat(event.getStream()).isEqualTo("stream");
	}

	@Override
	protected ProgressUpdateEvent createEvent(String status, ProgressDetail progressDetail, String progress) {
		return new LoadImageUpdateEvent("stream", status, progressDetail, progress);
	}

}
