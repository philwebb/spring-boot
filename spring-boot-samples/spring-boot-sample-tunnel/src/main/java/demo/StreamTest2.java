/*
 * Copyright 2012-2015 the original author or authors.
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

package demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.Args;

/**
 * @author pwebb
 */
public class StreamTest2 {

	public static void main(String[] args) throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		// HttpPost post = new HttpPost("http://localhost:8080/streamin");
		HttpPost post = new HttpPost("http://tunnel.cfapps.io/streamin");

		MultipartEntity entity = new MultipartEntity();
		entity.addPart("file", new FileBody2(new File(
				"/Users/pwebb/Downloads/IMG_20150311_093402.jpg")));
		post.setEntity(entity);

		HttpResponse response = client.execute(post);
	}

	private static class FileBody2 extends FileBody {
		private File file;

		public FileBody2(File file) {
			super(file);
			this.file = file;
		}

		@Override
		public void writeTo(final OutputStream out) throws IOException {
			Args.notNull(out, "Output stream");
			final InputStream in = new FileInputStream(this.file);
			try {
				final byte[] tmp = new byte[300];
				int l;
				while ((l = in.read(tmp)) != -1) {
					out.write(tmp, 0, l);
					out.flush();
					try {
						Thread.sleep(400);
					}
					catch (InterruptedException e) {
					}
				}
			}
			finally {
				in.close();
			}
		}

	}

}
