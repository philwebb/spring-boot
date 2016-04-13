/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * {@link MvcEndpoint} to expose heap dumps.
 *
 * @author Lari Hotari
 * @author Phillip Webb
 * @since 1.4.0
 */
@ConfigurationProperties("endpoints.heapdump")
@HypermediaDisabled
public class HeapdumpMvcEndpoint extends AbstractMvcEndpoint implements MvcEndpoint {

	private static final Log logger = LogFactory.getLog(HeapdumpMvcEndpoint.class);

	private final Lock lock = new ReentrantLock();

	private HeapDumper heapDumper;

	public HeapdumpMvcEndpoint() {
		super("/heapdump", true);
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public void invoke(@RequestParam(defaultValue = "true") boolean live,
			HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
		if (!isEnabled()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		if (this.lock.tryLock()) {
			try {
				if (this.heapDumper == null) {
					this.heapDumper = new HeapDumper();
				}
				if (!this.heapDumper.isAvailable()) {
					response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
					return;
				}
				dumpHeap(this.heapDumper, live, request, response);
			}
			finally {
				this.lock.unlock();
			}
		}
		else {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			return;
		}
	}

	private void dumpHeap(HeapDumper heapDumper, boolean live, HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		File file = createTempFile(live);
		file.delete();
		try {
			heapDumper.dumpHeap(file, live);
			new Handler(file).handleRequest(request, response);
		}
		finally {
			file.delete();
		}
	}

	private File createTempFile(boolean live) throws IOException {
		String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
		return File.createTempFile("heapdump" + date + (live ? "-live" : ""), ".hprof");

	}

	/**
	 * Uses com.sun.management.HotSpotDiagnosticMXBean available on Oracle and OpenJDK to
	 * dump the heap to a file.
	 */
	static class HeapDumper {

		private Object diagnosticMXBean;

		private Method dumpHeapMethod;

		@SuppressWarnings("unchecked")
		HeapDumper() {
			try {
				Class<?> diagnosticMXBeanClass = ClassUtils.resolveClassName(
						"com.sun.management.HotSpotDiagnosticMXBean", null);
				this.diagnosticMXBean = ManagementFactory.getPlatformMXBean(
						(Class<PlatformManagedObject>) diagnosticMXBeanClass);
				this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass,
						"dumpHeap", String.class, Boolean.TYPE);
			}
			catch (Throwable ex) {
				logger.warn("Unable to locate HotSpotDiagnosticMXBean.", ex);
			}
		}

		public boolean isAvailable() {
			return this.diagnosticMXBean != null;
		}

		public void dumpHeap(File file, boolean live) {
			ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean,
					file.getAbsolutePath(), live);
		}

	}

	/**
	 * {@link ResourceHttpRequestHandler} to send the dump file.
	 */
	private static class Handler extends ResourceHttpRequestHandler {

		private final Resource resource;

		Handler(File file) {
			this.resource = new FileSystemResource(file);
			try {
				afterPropertiesSet();
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		@Override
		protected Resource getResource(HttpServletRequest request) throws IOException {
			return this.resource;
		}

		@Override
		protected MediaType getMediaType(Resource resource) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}

		@Override
		protected void setHeaders(HttpServletResponse response, Resource resource,
				MediaType mediaType) throws IOException {
			super.setHeaders(response, resource, mediaType);
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ StringUtils.getFilename(resource.getFilename()) + "\"");
		}

	}

}
