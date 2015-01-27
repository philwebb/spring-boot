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

package org.springframework.boot.xreload.watchx;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * @author pwebb
 */
public class FileWatcher {

	/*
	 * file:/Users/pwebb/projects/spring-boot/samples/spring-petclinic/target/classes/
	 */

	public static void main(String[] args) throws IOException, InterruptedException {
		FileSystem fs = FileSystems.getDefault();
		Path path = fs.getPath("/Users/pwebb/tmp/watch");
		System.out.println(path);
		WatchService watcher = fs.newWatchService();
		path.register(watcher, new WatchEvent.Kind[] {
				StandardWatchEventKinds.ENTRY_CREATE,
				StandardWatchEventKinds.ENTRY_DELETE,
				StandardWatchEventKinds.ENTRY_MODIFY },
				com.sun.nio.file.SensitivityWatchEventModifier.HIGH);
		while (true) {
			WatchKey key = watcher.take();
			for (WatchEvent<?> event : key.pollEvents()) {
				if (event.kind() != StandardWatchEventKinds.OVERFLOW) {
					Path context = ((WatchEvent<Path>) event).context();
					System.out.println(context);
				}
			}
			key.reset();
		}
	}

}
