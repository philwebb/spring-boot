/*
 * Copyright 2012-2025 the original author or authors.
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

package smoktest.interfaceclient.scenario9.springcloud;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class SpringCloudAutoConfiguration {

	// We might need to provide access to the class metadata somehow in case custom
	// annotations are to be supported

	@Bean
	RestClientCustomizer springCloudRestClientCustomizer() {
		return RestClientCustomizer.of(SpringCloudSupport::apply);
	}

}
