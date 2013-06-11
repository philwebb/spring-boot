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

package org.springframework.bootstrap.actuate.endpoint;

import java.util.List;

import org.springframework.bootstrap.actuate.trace.Trace;
import org.springframework.bootstrap.actuate.trace.TraceRepository;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose {@link Trace} information.
 * 
 * @author Dave Syer
 */
public class TraceEndpoint extends AbstractEndpoint<List<Trace>> {

	private TraceRepository repository;

	/**
	 * @param repository
	 */
	public TraceEndpoint(TraceRepository repository) {
		Assert.notNull(repository, "Repository must not be null");
		this.repository = repository;
	}

	@Override
	public String getId() {
		return "trace";
	}

	@Override
	public List<Trace> execute() {
		return this.repository.findAll();
	}
}
