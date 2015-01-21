/*
 * Copyright 2012-2013 the original author or authors.
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
package sample.tomcat.web;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

//import sample.tomcat.ReloadingSampleTomcatApplication;

@Controller
public class ReceiverController {

	private static final Logger logger = LoggerFactory
			.getLogger(ReceiverController.class);

	// curl
	// -Ffilename=/Users/aclement/forks/spring-boot/spring-boot-samples/spring-boot-sample-tomcat/src/main/java/sample/tomcat/web/xx
	// -Ffile=@/Users/aclement/a http://localhost:8080/updateResource

	@RequestMapping(value = "/updateResource", method = RequestMethod.POST)
	@ResponseBody
	public String updateResource(@RequestParam("filename") String filename,
			@RequestParam("file") MultipartFile file) throws IOException {
		if (!file.isEmpty()) {
			byte[] data = file.getBytes();
			logger.info("updating file " + filename + " new data length = " + data.length);
			// The supplied filename is a partial (e.g. sample/tomcat/web/Foo.class,
			// relative to /home/vcap/app/)
			File f = new File("/home/vcap/app", filename);
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
			dos.write(data);
			dos.close();
		}
		// TODO need to batch up the changes, not just restart after each...
		// or maybe this receives a batch
		// ReloadingSampleTomcatApplication.restart();
		return "Updated " + filename;
	}

}
