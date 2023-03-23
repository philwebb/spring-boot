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

package org.springframework.boot.docker.compose.service;

/**
 * @author pwebb
 */
class ContainerPort {

	// FIXME move

	/**
	 * @param hostPortAsInt
	 * @param protocol
	 */
	public ContainerPort(int hostPortAsInt, Protocol protocol) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return
	 */
	public int number() {
		return 0;
	}

	/**
	 * @return
	 */
	public Protocol protocol() {
		return null;
	}

	// FIXME we don't need
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

	/**
	 * @param entry
	 * @return
	 */
	static ContainerPort parsePortWithProtocol(String entry) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
