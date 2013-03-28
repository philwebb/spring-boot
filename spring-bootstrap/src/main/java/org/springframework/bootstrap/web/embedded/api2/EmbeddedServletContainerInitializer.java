package org.springframework.bootstrap.web.embedded.api2;

import javax.servlet.ServletContext;


public interface EmbeddedServletContainerInitializer {

	void onStartup(ServletContext context);

}
