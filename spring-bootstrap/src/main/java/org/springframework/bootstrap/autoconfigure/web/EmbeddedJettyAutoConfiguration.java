/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.mortbay.jetty.Server;
import org.mortbay.util.Loader;
import org.springframework.bootstrap.context.annotation.AutoConfiguration;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.web.embedded.EmbeddedJettyFactory;
import org.springframework.bootstrap.web.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration} for {@link EmbeddedJettyFactory}.
 *
 * @author Phillip Webb
 */
@AutoConfiguration
@ConditionalOnClass({Servlet.class, Server.class, Loader.class})
@ConditionalOnMissingBean(EmbeddedServletContainerFactory.class)
public class EmbeddedJettyAutoConfiguration {

	@Bean
	public EmbeddedJettyFactory embeddedJettyFactory() {
		return new EmbeddedJettyFactory();
	}

}
