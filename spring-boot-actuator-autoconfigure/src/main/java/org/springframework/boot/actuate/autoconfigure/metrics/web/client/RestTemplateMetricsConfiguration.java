/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.client;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.metrics.web.client.DefaultRestTemplateExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for {@link RestTemplate}-related metrics.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
public class RestTemplateMetricsConfiguration {

	@Bean
	@ConditionalOnMissingBean(DefaultRestTemplateExchangeTagsProvider.class)
	public DefaultRestTemplateExchangeTagsProvider restTemplateTagConfigurer() {
		return new DefaultRestTemplateExchangeTagsProvider();
	}

	@Bean
	public MetricsRestTemplateInterceptor metricsRestTemplateIntercaptor(
			MeterRegistry meterRegistry,
			DefaultRestTemplateExchangeTagsProvider restTemplateTagConfigurer,
			MetricsProperties properties) {
		return new MetricsRestTemplateInterceptor(meterRegistry,
				restTemplateTagConfigurer, properties.getWeb().getClientRequestsName(),
				properties.getWeb().getClientRequestPercentiles());
	}

	@Bean
	public static BeanPostProcessor restTemplateInterceptorPostProcessor(
			ApplicationContext context) {
		return new MetricsInterceptorPostProcessor(context);
	}

	@Bean
	public MetricsRestTemplateCustomizer metricsRestTemplateCustomizer(
			MetricsRestTemplateInterceptor metricsRestTemplateInterceptor) {
		return new MetricsRestTemplateCustomizer(metricsRestTemplateInterceptor);
	}

	private static class MetricsInterceptorPostProcessor implements BeanPostProcessor {

		private final ApplicationContext context;

		private MetricsRestTemplateInterceptor interceptor;

		MetricsInterceptorPostProcessor(ApplicationContext context) {
			this.context = context;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean instanceof RestTemplate) {
				postProcessAfterInitialization((RestTemplate) bean);
			}
			return bean;
		}

		private void postProcessAfterInitialization(RestTemplate restTemplate) {
			// Create a new list as the old one may be unmodifiable (ie Arrays.asList())
			List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
			interceptors.add(getInterceptor());
			interceptors.addAll(restTemplate.getInterceptors());
			restTemplate.setInterceptors(interceptors);
		}

		private MetricsRestTemplateInterceptor getInterceptor() {
			if (this.interceptor == null) {
				this.interceptor = this.context
						.getBean(MetricsRestTemplateInterceptor.class);
			}
			return this.interceptor;
		}

	}

}
