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

package org.springframework.boot.testcontainers;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * Extended {@link org.springframework.beans.factory.config.BeanDefinition} interface used
 * to register testcontainer beans.
 *
 * @author Phillip Webb
 */
public interface TestcontainerBeanDefinition extends BeanDefinition {

	/**
	 * Return the docker image name or {@code null} if the image name is not yet known.
	 * @return the docker image name
	 */
	String getDockerImageName();

	/**
	 * Return any annotations declared alongside the container.
	 * @return annotations declared with the container
	 */
	MergedAnnotations getAnnotations();

}
