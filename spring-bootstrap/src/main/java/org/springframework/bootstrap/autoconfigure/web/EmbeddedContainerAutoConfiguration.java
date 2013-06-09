/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for an embedded servlet container.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
public class EmbeddedContainerAutoConfiguration implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		// Don't import the classes directly because that might trigger loading them - use
		// an import selector and the class name instead
		return new String[] { ServerPropertiesConfiguration.class.getName(),
				EmbeddedJettyAutoConfiguration.class.getName(),
				EmbeddedTomcatAutoConfiguration.class.getName() };
		// FIXME can be imports
		// FIXME loose ServerPropertiesConfiguration from this
	}

}
