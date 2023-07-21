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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Inflater;

import org.springframework.boot.loader.zip.ZipContent;

/**
 * Resources created managed and cleaned by a {@link NestedJarFile} instance.
 */
class NestedJarFileResources implements Runnable {

	private static final int INFLATER_CACHE_LIMIT = 20;

	ZipContent zipContent;

	private final Set<InputStream> inputStreams = Collections.newSetFromMap(new WeakHashMap<>());

	private Deque<Inflater> inflaterCache = new ArrayDeque<>();

	NestedJarFileResources(File file, String nestedEntryName) throws IOException {
		this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
	}

	ZipContent zipContent() {
		return this.zipContent;
	}

	Set<InputStream> inputStreams() {
		return this.inputStreams;
	}

	@Override
	public void run() {
		IOException exceptionChain = null;
		exceptionChain = releaseInflators(exceptionChain);
		exceptionChain = releaseInputStreams(exceptionChain);
		exceptionChain = releaseZipContent(exceptionChain);
		if (exceptionChain != null) {
			throw new UncheckedIOException(exceptionChain);
		}
	}

	private IOException releaseInflators(IOException exceptionChain) {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			synchronized (inflaterCache) {
				inflaterCache.stream().forEach(Inflater::end);
			}
			this.inflaterCache = null;
		}
		return exceptionChain;
	}

	private IOException releaseInputStreams(IOException exceptionChain) {
		synchronized (this.inputStreams) {
			for (InputStream inputStream : this.inputStreams) {
				try {
					inputStream.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
				this.inputStreams.clear();
			}
		}
		return exceptionChain;
	}

	private IOException releaseZipContent(IOException exceptionChain) {
		if (this.zipContent != null) {
			try {
				this.zipContent.close();
			}
			catch (IOException ex) {
				exceptionChain = addToExceptionChain(exceptionChain, ex);
			}
		}
		return exceptionChain;
	}

	private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
		if (exceptionChain != null) {
			exceptionChain.addSuppressed(ex);
			return exceptionChain;
		}
		return ex;
	}

	Runnable createInflatorCleanupAction(Inflater inflater) {
		return () -> endOrCacheInflater(inflater);
	}

	Inflater getOrCreateInflater() {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			synchronized (inflaterCache) {
				Inflater inflater = this.inflaterCache.poll();
				if (inflater != null) {
					return inflater;
				}
			}
		}
		return new Inflater(true);
	}

	private void endOrCacheInflater(Inflater inflater) {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			synchronized (inflaterCache) {
				if (this.inflaterCache == inflaterCache && inflaterCache.size() < INFLATER_CACHE_LIMIT) {
					inflater.reset();
					this.inflaterCache.add(inflater);
					return;
				}
			}
		}
		inflater.end();
	}

}
