/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;

/**
 * Configures the {@link SslOptions}, {@link SslBundleKey @SslBundleKey} and
 * {@link SslBundle#getProtocol() protocol} to use with an {@link SslBundle SSL} supported
 * {@link ServiceConnection @ServiceConnection}.
 *
 * @author Phillip Webb
 * @since 3.4.0
 */
public @interface Ssl {

	/**
	 * The protocol to use for the SSL connection.
	 * @return the SSL protocol
	 * @see SslBundle#getProtocol()
	 */
	String protocol() default SslBundle.DEFAULT_PROTOCOL;

	/**
	 * The ciphers that can be used for the SSL connection.
	 * @return the SSL ciphers
	 * @see SslOptions#getCiphers()
	 */
	String[] ciphers() default {};

	/**
	 * The protocols that are enabled for the SSL connection.
	 * @return the enabled SSL protocols
	 * @see SslOptions#getEnabledProtocols()
	 */
	String[] enabledProtocols() default {};

	/**
	 * The password that should be used to access the key.
	 * @return the key password
	 * @see SslBundleKey#getPassword()
	 */
	String keyPassword() default "";

	/**
	 * The alias that should be used to access the key.
	 * @return the key alias
	 * @see SslBundleKey#getAlias()
	 */
	String keyAlias() default "";

}
