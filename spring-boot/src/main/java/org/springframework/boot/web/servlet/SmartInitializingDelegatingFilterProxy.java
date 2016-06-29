/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.web.servlet;

import javax.servlet.ServletException;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * @author Phillip Webb
 */
public class SmartInitializingDelegatingFilterProxy extends DelegatingFilterProxy
		implements SmartInitializingSingleton {

	public SmartInitializingDelegatingFilterProxy(String targetBeanName,
			WebApplicationContext webApplicationContext) {
		super(targetBeanName, webApplicationContext);
	}

	@Override
	public void afterSingletonsInstantiated() {
		try {
			super.initFilterBean();
		}
		catch (ServletException ex) {
			throw new IllegalStateException();
		}
	}

	@Override
	protected void initFilterBean() throws ServletException {
	}

}
