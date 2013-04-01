
package org.springframework.bootstrap.shell;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

public class WatchExample {

	public static void main(String[] args) {
		try {
			FileSystem fs = FileSystems.getDefault();
			WatchService watchService = fs.newWatchService();
			Path path = fs.getPath("/Users/pwebb/projects/spring/spring-bootstrap/code/spring-bootstrap-shell/src/main/resources");
			WatchKey watchKey = path.register(watchService,
					StandardWatchEventKinds.ENTRY_MODIFY);
			WatchKey take = watchService.take();
			for (WatchEvent<?> event : take.pollEvents()) {
				System.out.println(event.kind());
				System.out.println(event.context());
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
