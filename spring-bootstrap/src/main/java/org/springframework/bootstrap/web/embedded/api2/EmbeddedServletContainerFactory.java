package org.springframework.bootstrap.web.embedded.api2;


public interface EmbeddedServletContainerFactory {

	EmbeddedServletContainer getEmbdeddedServletContainer(
			EmbeddedServletContainerInitializer... initializers);

}
