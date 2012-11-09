package org.springframework.bootstrap.web.embedded;


public interface EmbeddedServletContainer {

	void start() throws Exception;

	void stop() throws Exception;

}
