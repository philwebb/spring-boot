/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface used for {@link InetAddress}-based matching.
 * <p>
 * Allow HTTP clients to offer Server-Side Request Forgery (SSRF) mitigation features, for
 * example by only allowing local addresses to be called.
 * <p>
 * Matchers are typically built using the static factory methods on this interface,
 * optionally combined with one or more of the logic methods. For example:
 * <pre class="code">
 * InetAddressMatcher.of("192.168.0.0/24")
 * 	.andNot("192.168.0.0");
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @author Phillip Webb
 * @since 4.1.0
 * @see UnmatchedHostException
 */
@FunctionalInterface
public interface InetAddressMatcher {

	/**
	 * Test if the given IP address string matches.
	 * @param address the IP address string to check (may be {@code null})
	 * @return {@code true} if the address matches, {@code false} otherwise
	 */
	default boolean matches(@Nullable String address) {
		return matches((address != null) ? IpInetAddress.parseIpAddress(address) : null);
	}

	/**
	 * Test if the given socket address matches.
	 * @param address the socket address string to check (may be {@code null})
	 * @return {@code true} if the address matches, {@code false} otherwise
	 */
	default boolean matches(@Nullable InetSocketAddress address) {
		return matches((address != null) ? address.getAddress() : null);
	}

	/**
	 * Whether the given address matches.
	 * @param address the address to check
	 * @return if the address matches
	 */
	boolean matches(@Nullable InetAddress address);

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other IP addresses.
	 * @param addresses the addresses that will be logically-ANDed with this matcher in
	 * any form supported by {@link #of(String...)}
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher and(String... addresses) {
		return and(Arrays.stream(addresses).map(IpInetAddress::of).map(IpInetAddress::matcher).toList());
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other matchers.
	 * @param matchers the matchers that will be logically-ANDed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher and(InetAddressMatcher... matchers) {
		return and(Arrays.asList(matchers));
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other matchers.
	 * @param matchers the matchers that will be logically-ANDed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher and(Collection<? extends InetAddressMatcher> matchers) {
		InetAddressMatcher result = this;
		for (InetAddressMatcher matcher : matchers) {
			InetAddressMatcher ours = result;
			result = (address) -> ours.matches(address) && matcher.matches(address);
		}
		return result;
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other {@link #negate() negated} IP addresses.
	 * @param addresses the addresses that will be {@link #negate() negated} and
	 * logically-ANDed with this matcher in any form supported by {@link #of(String...)}
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher andNot(String... addresses) {
		return andNot(Arrays.stream(addresses).map(IpInetAddress::of).map(IpInetAddress::matcher).toList());
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other {@link #negate() negated} IP addresses.
	 * @param matchers the matchers that will be {@link #negate() negated} and
	 * logically-ANDed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher andNot(InetAddressMatcher... matchers) {
		return andNot(Arrays.asList(matchers));
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical AND of this
	 * matcher and other {@link #negate() negated} IP addresses.
	 * @param matchers the matchers that will be {@link #negate() negated} and
	 * logically-ANDed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher andNot(Collection<? extends InetAddressMatcher> matchers) {
		InetAddressMatcher result = this;
		for (InetAddressMatcher matcher : matchers) {
			InetAddressMatcher ours = result;
			result = (address) -> ours.matches(address) && !matcher.matches(address);
		}
		return result;
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical OR of this
	 * matcher and other IP addresses.
	 * @param addresses the addresses that will be logically-ORed with this matcher in any
	 * form supported by {@link #of(String...)}
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher or(String... addresses) {
		return or(Arrays.stream(addresses).map(IpInetAddress::of).map(IpInetAddress::matcher).toList());
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical OR of this
	 * matcher and other matchers.
	 * @param matchers the matchers that will be logically-ORed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher or(InetAddressMatcher... matchers) {
		return or(Arrays.asList(matchers));
	}

	/**
	 * Return a composed matcher that represents a short-circuiting logical OR of this
	 * matcher and other matchers.
	 * @param matchers the matchers that will be logically-ORed with this matcher
	 * @return a new composed matcher instance
	 */
	default InetAddressMatcher or(Collection<? extends InetAddressMatcher> matchers) {
		InetAddressMatcher result = this;
		for (InetAddressMatcher matcher : matchers) {
			InetAddressMatcher ours = result;
			result = (address) -> ours.matches(address) || matcher.matches(address);
		}
		return result;
	}

	/**
	 * Return a new matcher that represents the logical negation of this matcher.
	 * @return the negated matcher
	 */
	default InetAddressMatcher negate() {
		return (address) -> !matches(address);
	}

	/**
	 * Return a matcher that will match external (non-private) IP addresses. External
	 * addresses are all non-{@link #internalAddresses() internal addresses}
	 * @return a matcher for external IP addresses
	 * @see #internalAddresses()
	 */
	static InetAddressMatcher externalAddresses() {
		return of((address) -> address != null).andNot(internalAddresses());
	}

	/**
	 * Return a matcher that will match internal (private) IP addresses.
	 * <p>
	 * Internal addresses include loopback addresses ({@code 127.0.0.0/8} for IPv4,
	 * {@code ::1} for IPv6), private IPv4 address ranges ({@code 10.0.0.0/8},
	 * {@code 172.16.0.0/12}, {@code 192.168.0.0/16}), and IPv6 Unique Local Addresses
	 * ({@code fc00::/7}).
	 * @return a matcher for external IP addresses
	 * @see #externalAddresses()
	 */
	static InetAddressMatcher internalAddresses() {
		return InternalInetAddressMatcher.instance;
	}

	/**
	 * Return a matcher that is the negation of all the given addresses.
	 * @param addresses the addresses to negate in any form supported by
	 * {@link #of(String...)}
	 * @return a negated matcher
	 * @see #negate()
	 */
	static InetAddressMatcher not(String... addresses) {
		return all().andNot(addresses);
	}

	/**
	 * Return a matcher that is the negation of all the given matchers.
	 * @param matchers the matchers to negate
	 * @return a negated matcher
	 * @see #negate()
	 */
	static InetAddressMatcher not(InetAddressMatcher... matchers) {
		return all().andNot(matchers);
	}

	/**
	 * Return a matcher that is the negation of all the given matchers.
	 * @param matchers the matchers to negate
	 * @return a negated matcher
	 * @see #negate()
	 */
	static InetAddressMatcher not(Collection<? extends InetAddressMatcher> matchers) {
		return all().andNot(matchers);
	}

	/**
	 * Return a matcher that matches any of the given IP addresses. Address may be either
	 * a full IP address (e.g. {@code 192.168.1.1}) or an IP address block spcified using
	 * CIDR notations (for example {@code 192.168.1.0/24}). Both IPv4 and IPv6 addresses
	 * are supported.
	 * @param addresses the IP addresses to match
	 * @return a matcher that matches any of the given addresses
	 */
	static InetAddressMatcher of(String... addresses) {
		return none().or(addresses);
	}

	/**
	 * Return a matcher that matches any of the given matchers.
	 * @param matchers the matchers to include
	 * @return a matcher that matches any of the matchers
	 */
	static InetAddressMatcher of(InetAddressMatcher... matchers) {
		return none().or(matchers);
	}

	/**
	 * Return a matcher that matches any of the given matchers.
	 * @param matchers the matchers to include
	 * @return a matcher that matches any of the matchers
	 */
	static InetAddressMatcher of(Collection<? extends InetAddressMatcher> matchers) {
		return none().or(matchers);
	}

	/**
	 * Return a matcher that matches all addresses.
	 * @return a matcher that matches all
	 */
	static InetAddressMatcher all() {
		return (address) -> true;
	}

	/**
	 * Return a matcher that matches no addresses.
	 * @return a matcher that matches none
	 */
	static InetAddressMatcher none() {
		return (address) -> false;
	}

}
