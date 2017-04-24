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

package org.springframework.boot.context.properties.bind;

import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

/**
 * A container object to return result of a {@link Binder} bind operation. May contain
 * either a successfully bound object or an empty result.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class BindResult<T> {

	public void ifBound(Consumer<? super T> consumer) {

	}

	public boolean isBound() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	public <U> BindResult<U> map(Function<? super T, ? extends U> mapper) {
		return null;
	}

	/**
	 *
	 */
	public T get() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param emptySet
	 * @return
	 */
	public T orElse(T result) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @param result
	 * @return
	 */
	public T orElseGet(Supplier<? extends T> result) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * @return
	 */

	public T orElseCreate(Class<? extends T> type) {
		return null;
	}

	/**
	 * @param bound
	 * @return
	 */
	public static <T> BindResult<T> of(T bound) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
