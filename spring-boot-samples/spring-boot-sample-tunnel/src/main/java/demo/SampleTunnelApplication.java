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

import java.util.Collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter;
import org.springframework.boot.developertools.tunnel.server.HttpTunnelServerFilter;
import org.springframework.boot.livereload.tunnel.TunnelServerWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

@SpringBootApplication
public class SampleTunnelApplication extends WebMvcAutoConfigurationAdapter {

	@Bean
	public SimpleUrlHandlerMapping handlerMapping() {
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setOrder(Integer.MIN_VALUE + 1);
		mapping.setUrlMap(Collections.singletonMap("/tunnel",
				websocketTunnelHttpRequestHandler()));
		return mapping;
	}

	@SuppressWarnings("restriction")
	private Integer getDebugPort() {
		if (false) {
			return 5000;
		}
		String property = sun.misc.VMSupport.getAgentProperties().getProperty(
				"sun.jdwp.listenerAddress");
		if (property == null) {
			return -1;
		}
		System.out.println(property);
		return Integer.valueOf(property.split(":")[1]);
	}

	@Bean
	public WebSocketHttpRequestHandler websocketTunnelHttpRequestHandler() {
		return new WebSocketHttpRequestHandler(tunnelWebSocketHandler());
	}

	@Bean
	public TunnelServerWebSocketHandler tunnelWebSocketHandler() {
		int port = getDebugPort();
		return new TunnelServerWebSocketHandler(port);
	}

	@Bean
	public HttpTunnelServerFilter tunnelServerHttpFilter() {
		int port = getDebugPort();
		return new HttpTunnelServerFilter(port);
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleTunnelApplication.class, args);
	}

}
