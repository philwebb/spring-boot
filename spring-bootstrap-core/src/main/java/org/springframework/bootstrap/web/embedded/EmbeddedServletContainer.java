package org.springframework.bootstrap.web.embedded;

/**
 * Simple wrapper around an embedded servlet container.
 * @author Phillip Webb
 */
public interface EmbeddedServletContainer {

	/**
	 * Start the container.
	 * @throws Exception
	 */
	void start() throws Exception;

	/**
	 * Stop the container.
	 * @throws Exception
	 */
	void stop() throws Exception;

}
