/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.webservices.client;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.webservices.client.HttpWebServiceMessageSenderBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.boot.webservices.client.WebServiceTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
@AutoConfiguration(after = HttpClientAutoConfiguration.class)
@ConditionalOnClass({ WebServiceTemplate.class, Unmarshaller.class, Marshaller.class })
public class WebServiceTemplateAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HttpWebServiceMessageSenderBuilder httpWebServiceMessageSenderBuilder(
			ObjectProvider<ClientHttpRequestFactoryBuilder<?>> clientHttpRequestFactoryBuilder,
			ObjectProvider<ClientHttpRequestFactorySettings> clientHttpRequestFactorySettings) {
		HttpWebServiceMessageSenderBuilder builder = new HttpWebServiceMessageSenderBuilder();
		clientHttpRequestFactoryBuilder.ifAvailable(builder::requestFactoryBuilder);
		clientHttpRequestFactorySettings.ifAvailable(builder::requestFactorySettings);
		return builder;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebServiceTemplateBuilder webServiceTemplateBuilder(
			ObjectProvider<HttpWebServiceMessageSenderBuilder> httpWebServiceMessageSenderBuilder,
			ObjectProvider<WebServiceTemplateCustomizer> webServiceTemplateCustomizers) {
		WebServiceTemplateBuilder templateBuilder = new WebServiceTemplateBuilder();
		HttpWebServiceMessageSenderBuilder messageSenderBuilder = httpWebServiceMessageSenderBuilder.getIfAvailable();
		if (messageSenderBuilder != null) {
			templateBuilder = templateBuilder.messageSenderBuilder(messageSenderBuilder);
		}
		List<WebServiceTemplateCustomizer> customizers = webServiceTemplateCustomizers.orderedStream().toList();
		if (!customizers.isEmpty()) {
			templateBuilder = templateBuilder.customizers(customizers);
		}
		return templateBuilder;
	}

}
