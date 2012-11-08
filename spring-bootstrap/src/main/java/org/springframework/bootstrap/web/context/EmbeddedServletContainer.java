package org.springframework.bootstrap.web.context;


public interface EmbeddedServletContainer {

	void start() throws Exception;

	void stop() throws Exception;

}
