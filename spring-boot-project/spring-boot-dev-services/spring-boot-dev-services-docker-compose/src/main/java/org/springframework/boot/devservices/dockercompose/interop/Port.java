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

package org.springframework.boot.devservices.dockercompose.interop;

/**
 * Port with protocol.
 *
 * @param number port number
 * @param protocol port protocol
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @since 3.1.0
 */
public record Port(int number, Protocol protocol) {

	@Override
	public String toString() {
		return "%d/%s".formatted(this.number, this.protocol.name().toLowerCase());
	}

	/**
	 * Parses the given port with procotol (e.g. '8080/tcp') into a {@link Port}.
	 * @param portWithProtocol the port with protocol
	 * @return the parsed port
	 */
	public static Port parsePortWithProtocol(String portWithProtocol) {
		int slash = portWithProtocol.indexOf('/');
		if (slash == -1) {
			throw new IllegalArgumentException("No protocol found in '%s'".formatted(portWithProtocol));
		}
		String protocol = portWithProtocol.substring(slash + 1);
		int number = Integer.parseInt(portWithProtocol.substring(0, slash));
		return new Port(number, Port.Protocol.parse(protocol));
	}

	/**
	 * Protocol.
	 */
	public enum Protocol {

		/**
		 * TCP.
		 */
		TCP,
		/**
		 * UDP.
		 */
		UDP,
		/**
		 * Other.
		 */
		OTHER;

		/**
		 * Parses the given string into a {@link Protocol}.
		 * @param input input or {@code null}
		 * @return parsed {@link Protocol}
		 */
		public static Protocol parse(String input) {
			if (input == null) {
				return OTHER;
			}
			return switch (input.toLowerCase()) {
				case "tcp" -> TCP;
				case "udp" -> UDP;
				default -> OTHER;
			};
		}

	}
}
