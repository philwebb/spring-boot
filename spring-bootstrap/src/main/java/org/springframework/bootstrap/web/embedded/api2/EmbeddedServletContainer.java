package org.springframework.bootstrap.web.embedded.api2;

import javax.servlet.Servlet;


public interface EmbeddedServletContainer {

	void start();
	
	void stop();
	
	Servlet[] getServlets();
	
}
