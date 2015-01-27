/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.boot.xreload.watch;


/**
 * Sample runner for FileSystemWatcher - just for testing
 *
 * @author Andy Clement
 */
public class Runner {
	//
	// static String prefix;
	//
	// /**
	// * args[0] would be the path to watch.
	// */
	// public static void main(String[] args) throws Exception {
	// System.out.println("Watching everything in this folder:" + args[0]);
	// Listener l = new Listener();
	// FileSystemWatcher fsw = new FileSystemWatcher(l);
	// fsw.register(new File(args[0]));
	// prefix = args[0];
	// Thread.sleep(100000); // sit here for 100seconds
	// fsw.shutdown();
	// }
	//
	// /**
	// * Sample listener that just prints what it heard
	// */
	// static class Listener implements FileChangeListener {
	// @Override
	// public void filesChanged(File... files) {
	// System.out.println("Received batch of changes: #" + files.length);
	// for (int i = 0; i < files.length; i++) {
	// File f = files[i];
	// String uploadPath = null;
	// if (f.toString().startsWith(prefix)) {
	// uploadPath = f.toString().substring(prefix.length() + 1);
	// }
	// System.out.println("Sending update to " + uploadPath);
	// String cmd = "curl -Ffilename=" + uploadPath + " -Ffile=@" + f.toString()
	// + " http://asc.cfapps.io/updateResource";
	// try {
	// Runtime.getRuntime().exec(cmd);
	// }
	// catch (IOException e) {
	// e.printStackTrace();
	// }
	// // System.out.println(files[i] + "  (#" + i + ")");
	// }
	// }
	// }

}
