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

package org.springframework.boot.pack.build;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.boot.pack.docker.LogUpdateEvent;
import org.springframework.boot.pack.docker.TotalProgressBar;
import org.springframework.boot.pack.docker.TotalProgressEvent;
import org.springframework.boot.pack.docker.type.Image;
import org.springframework.boot.pack.docker.type.ImageReference;
import org.springframework.boot.pack.docker.type.VolumeName;

/**
 * {@link BuildLog} implementation that prints output to a {@link PrintStream}.
 *
 * @author Phillip Webb
 * @see BuildLog#to(PrintStream)
 */
class PrintStreamBuildLog implements BuildLog {

	private final PrintStream out;

	PrintStreamBuildLog(PrintStream out) {
		this.out = out;
	}

	@Override
	public void start(BuildRequest request) {
		this.out.println("Building image '" + request.getName() + "'");
		this.out.println();
	}

	@Override
	public Consumer<TotalProgressEvent> pullingBuilder(BuildRequest request, ImageReference imageReference) {
		return new TotalProgressBar(" > Pulling builder image '" + imageReference + "'", '.', false, this.out);
	}

	@Override
	public void pulledBulder(BuildRequest request, Image image) {
		this.out.println(" > Pulled builder image '" + getDigest(image) + "'");
	}

	@Override
	public Consumer<TotalProgressEvent> pullingRunImage(BuildRequest request, ImageReference imageReference) {
		return new TotalProgressBar(" > Pulling run image '" + imageReference + "'", '.', false, this.out);
	}

	@Override
	public void pulledRunImage(BuildRequest request, Image image) {
		this.out.println(" > Pulled run image '" + getDigest(image) + "'");
	}

	@Override
	public void executingLifecycle(BuildRequest request, LifecycleVersion version, VolumeName buildCacheVolume) {
		this.out.println(" > Executing lifecycle version " + version);
		this.out.println(" > Using build cache volume '" + buildCacheVolume + "'");
	}

	@Override
	public Consumer<LogUpdateEvent> runningPhase(BuildRequest request, String name) {
		this.out.println();
		this.out.println(" > Running " + name);
		String prefix = String.format("    %-14s", "[" + name + "] ");
		return (event) -> this.out.println(prefix + event);
	}

	@Override
	public void executedLifecycle(BuildRequest request) {
		this.out.println();
		this.out.println("Successfully built image '" + request.getName() + "'");
		this.out.println();
	}

	private String getDigest(Image image) {
		List<String> digests = image.getDigests();
		return (digests.isEmpty() ? "" : digests.get(0));
	}

}
