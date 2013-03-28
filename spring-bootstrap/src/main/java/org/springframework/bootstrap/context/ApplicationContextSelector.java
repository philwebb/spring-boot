/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.bootstrap.context;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;

/**
 * @author Dave Syer
 * 664a1a7b2fca0a292685b288f9514eda2537a214
 */
public class ApplicationContextSelector {

	public static ConfigurableApplicationContext select(Class<?>... configs) {
		ConfigurableApplicationContext result;

		// Register the config classes, can be @Configuration or @Component etc.
		if (ClassUtils.isPresent("javax.servlet.ServletContext", ApplicationContextSelector.class.getClassLoader())
				&& ClassUtils.isPresent(
						"org.springframework.web.context.support.AbstractRefreshableWebApplicationContext",
						ApplicationContextSelector.class.getClassLoader())) {
			@SuppressWarnings("resource")
			AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
			context.register(configs);
			result = context;
		}
		else {
			@SuppressWarnings("resource")
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(configs);
			result = context;
		}

		return result;

	}

}
