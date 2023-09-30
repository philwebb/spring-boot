/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.net.protocol.jar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for {@link JarUrlConnection}.
 *
 * @author Phillip Webb
 */
class JarUrlConnectionTests {

	@Test
	void getJarFileReturnsJarFile() {
		fail("todo");
	}

	@Test
	void getJarEntryReturnsJarEntry() {

	}

	@Test
	void getJarEntryWhenHasNoEntryNameReturnsNull() {

	}

	@Test
	void getContentLengthReturnsContentLength() {

	}

	@Test
	void getContentLengthWhenLengthIsLargerThanMaxIntReturnsMinusOne() {

	}

	@Test
	void getContentLengthLongWhenHasNoEntryReturnsSizeOfJar() {

	}

	@Test
	void getContentLengthLongWhenHasEntryReturnsEntrySize() {

	}

	@Test
	void getContentLengthLongWhenCannotReadFileReturnsMinusOne() {

	}

	@Test
	void getContentTypeWhenHasNoEntryReturnsJavaJar() {

	}

	@Test
	void getContentTypeWhenHasKnownStreamReturnsDeducedType() {

	}

	@Test
	void getContentTypeWhenNotKnownInStreamButKnownNameReturnsDeducedType() {

	}

	@Test
	void getContentTypeWhenCannotBeDeducedReturnsContentUnknown() {

	}

	@Test
	void getHeaderFieldDelegatesToJarFileConnection() {

	}

	@Test
	void getContentWhenHasEntryReturnsContentFromEntry() {

	}

	@Test
	void getContentWhenHasNoEntryReturnsJarFile() {

	}

	@Test
	void getPermissionReturnJarConnectionPermission() {

	}

	@Test
	void getInputStreamWhenHasNoEntryThrowsException() {

	}

	@Test
	void getInputStreamWhenOptimizedAndHasCachedJarWithoutEntryReturnsEmptyInputStream() {

	}

	@Test
	void getInputStreamWhenNoEntryAndOptimzedThrowsException() {

	}

	@Test
	void getInputStreamWhenNoEntryAndNotOptimzedThrowsException() {

	}

	@Test
	void getInputStreamReturnsInputStream() {

	}

	@Test
	void getInputStreamWhenNoCachedClosesJarFileOnClose() {

	}

	@Test
	void getAllowUserInteractionDelegatesToJarFileConnection() {

	}

	@Test
	void setAllowUserInteractionDelegatesToJarFileConnection() {

	}

	@Test
	void getUseCachesDelegatesToJarFileConnection() {

	}

	@Test
	void setUseCachesDelegatesToJarFileConnection() {

	}

	@Test
	void getDefaultUseCachesDelegatesToJarFileConnection() {

	}

	@Test
	void setDefaultUseCachesDelegatesToJarFileConnection() {
	}

	@Test
	void setIfModifiedSinceDelegatesToJarFileConnection() {

	}

	@Test
	void getRequestPropertyDelegatesToJarFileConnection() {

	}

	@Test
	void setRequestPropertyDelegatesToJarFileConnection() {

	}

	@Test
	void addRequestPropertyDelegatesToJarFileConnection() {

	}

	@Test
	void getRequestPropertiesDelegatesToJarFileConnection() {

	}

	@Test
	void connectWhenConnectedDoesNotReconnect() {

	}

	@Test
	void connectWhenHasNotFoundSupplierThrowsException() {

	}

	@Test
	void connectWhenOptimizationsEnabledAndHasCachedJarWithoutEntryThrowsException() {

	}

	@Test
	void connectWhenHasNoEntryConnects() {

	}

	@Test
	void connectWhenEntryDoesNotExistAndOptimizationsEnabledThrowsException() {

	}

	@Test
	void connectWhenEntryDoesNotExistAndNoOptimizationsEnabledThrowsException() {

	}

	@Test
	void connectWhenEntryExists() {

	}

	@Test
	void connectWhenAddedToCacheReconnects() {

	}

	@Test
	void openWhenNestedAndInCachedWithoutEntryReturnsNoFoundConnection() {

	}

	@Test
	void openReturnsConnection() {

	}

}
