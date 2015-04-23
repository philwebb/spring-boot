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

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.stereotype.Component;

@Component
public class StreamInFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		System.out.println("Filter");
		if (request instanceof HttpServletRequest
				&& response instanceof HttpServletResponse) {
			doFilterHttp((HttpServletRequest) request, (HttpServletResponse) response,
					chain);
		}
		else {
			chain.doFilter(request, response);
		}
	}

	private void doFilterHttp(HttpServletRequest request, HttpServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		System.out.println(request.getRequestURI());
		if (request.getRequestURI().endsWith("/streamin")) {
			try {
				handleIt2(request, response);
			}
			catch (FileUploadException e) {
				e.printStackTrace();
			}
		}
		else {
			chain.doFilter(request, response);
		}
	}

	/**
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws FileUploadException
	 */
	private void handleIt2(HttpServletRequest request, HttpServletResponse response)
			throws FileUploadException, IOException {
		if (ServletFileUpload.isMultipartContent(request)) {
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String name = item.getFieldName();
				InputStream stream = item.openStream();
				if (item.isFormField()) {
					System.out.println("Form field " + name + " with value "
							+ Streams.asString(stream) + " detected.");
				}
				else {
					System.out.println("File field " + name + " with file name "
							+ item.getName() + " detected.");
					readStream(stream);
					// Process the input stream
				}
			}
		}

	}

	private void handleIt(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		ServletInputStream inputStream = request.getInputStream();
		readStream(inputStream);

	}

	/**
	 * @param inputStream
	 * @param buffer
	 * @throws IOException
	 */
	private void readStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int read = inputStream.read(buffer);
			if (read == -1) {
				return;
			}
			if (read > 0) {
				byte[] data = new byte[read];
				System.arraycopy(buffer, 0, data, 0, read);
				System.out.println(HexUtils.toHexString(data));
			}
		}
	}

	@Override
	public void destroy() {
	}

}
