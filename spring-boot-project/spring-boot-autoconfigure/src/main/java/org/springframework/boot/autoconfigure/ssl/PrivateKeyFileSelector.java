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

package org.springframework.boot.autoconfigure.ssl;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Strategy interface used to select a single {@link PrivateKeyFile} when multiple
 * candidates exist. Selectors may be registered as beans where they will be checked
 * {@link ObjectProvider#orderedStream() using standard ordering rules}. The first
 * selector to provide a {@code non-null} result will be used. If not suitable selector is
 * found then the selector settings from {@link SslProperties} will be used.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.2.0
 * @see PrivateKeyFile
 * @see CertificateFileSelector
 */
@FunctionalInterface
public interface PrivateKeyFileSelector {

	/**
	 * Selects a {@link PrivateKeyFile} from the given set of candidates taking into
	 * consideration the bundle name.
	 * @param bundleName the bundle name
	 * @param certificateFile the previously selected certificate
	 * @param candidates the candidates to consider
	 * @return a single candidate or {@code null}
	 */
	default PrivateKeyFile selectPrivateKeyFile(String bundleName, CertificateFile certificateFile,
			List<PrivateKeyFile> candidates) {
		return selectPrivateKeyFile(certificateFile, candidates);
	}

	/**
	 * Selects a {@link PrivateKeyFile} from the given set of candidates without
	 * considering the bundle name.
	 * @param certificateFile the previously selected certificate
	 * @param candidates the candidates to consider
	 * @return a single candidate or {@code null}
	 * @see #forBundles(String...)
	 * @see #forBundles(Predicate)
	 */
	PrivateKeyFile selectPrivateKeyFile(CertificateFile certificateFile, List<PrivateKeyFile> candidates);

	/**
	 * Return a new {@link PrivateKeyFileSelector} instance that is limited to the given
	 * SSL bundle names.
	 * @param bundleNames the bundle names to accept
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	default PrivateKeyFileSelector forBundles(String... bundleNames) {
		Assert.notNull(bundleNames, "BundleNames must not be null");
		Set<String> bundleNameSet = Set.of(bundleNames);
		return forBundles(bundleNameSet::contains);
	}

	/**
	 * Return a new {@link PrivateKeyFileSelector} instance that is limited to specific
	 * SSL bundle names.
	 * @param predicate the predicate used to test if an SSL bundle name is accepted
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	default PrivateKeyFileSelector forBundles(Predicate<String> predicate) {
		return new PrivateKeyFileSelector() {

			@Override
			public PrivateKeyFile selectPrivateKeyFile(String bundleName, CertificateFile certificate,
					List<PrivateKeyFile> candidates) {
				return (!predicate.test(bundleName)) ? null : selectPrivateKeyFile(certificate, candidates);
			}

			@Override
			public PrivateKeyFile selectPrivateKeyFile(CertificateFile certificate, List<PrivateKeyFile> candidates) {
				return PrivateKeyFileSelector.this.selectPrivateKeyFile(certificate, candidates);
			}

		};
	}

	/**
	 * Factory method that returns a {@link PrivateKeyFileSelector} that selects the
	 * {@link PrivateKeyFile} using a unique filename match against the certificate file.
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	static PrivateKeyFileSelector usingFileName() {
		return usingUniquePathMatch((certificatePath, candidatePath) -> {
			String certificateFileName = certificatePath.getFileName().toString();
			String candidateFileName = candidatePath.getFileName().toString();
			return Objects.equals(StringUtils.stripFilenameExtension(certificateFileName),
					StringUtils.stripFilenameExtension(candidateFileName));
		});
	}

	/**
	 * Factory method that returns a {@link PrivateKeyFileSelector} that selects the
	 * {@link PrivateKeyFile} using a unique path matching strategy.
	 * @param predicate predicate that is passed the certificate path and candidate path
	 * to determine if they match
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	static PrivateKeyFileSelector usingUniquePathMatch(BiPredicate<Path, Path> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		return usingUniqueMatch(
				(certificateFile, candidate) -> predicate.test(certificateFile.path(), candidate.path()));
	}

	/**
	 * Factory method that returns a {@link PrivateKeyFileSelector} that selects the
	 * {@link PrivateKeyFile} using a unique certificate match.
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	static PrivateKeyFileSelector usingCertificateMatch() {
		return usingUniqueMatch(PrivateKeyFileSelector::isCertificateMatch);
	}

	private static boolean isCertificateMatch(CertificateFile certificateFile, PrivateKeyFile candidate) {
		List<X509Certificate> chain = certificateFile.certificates();
		return !chain.isEmpty() && new CertificateMatcher(candidate.privateKey()).matches(chain.get(0));
	}

	/**
	 * Factory method that returns a {@link PrivateKeyFileSelector} that selects the
	 * {@link PrivateKeyFile} using a unique matching strategy.
	 * @param predicate predicate that is passed the certificate file and candidate to
	 * determine if they match
	 * @return a new {@link PrivateKeyFileSelector} instance
	 */
	static PrivateKeyFileSelector usingUniqueMatch(BiPredicate<CertificateFile, PrivateKeyFile> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		return (certificateFile, candidates) -> {
			List<PrivateKeyFile> matches = candidates.stream()
				.filter((candidate) -> predicate.test(certificateFile, candidate))
				.toList();
			Assert.state(matches.size() <= 1, () -> "Unable to select PrivateKey due to multiple matches %s"
				.formatted(matches.stream().map(PrivateKeyFile::path).toList()));
			return (!matches.isEmpty()) ? matches.get(0) : null;
		};
	}

	/**
	 * Factory method that can be used when constructing a {@link PrivateKeyFileSelector}
	 * from a lambda.
	 * @param selector the certificate selector to use
	 * @return the {@link PrivateKeyFileSelector} instance
	 */
	static PrivateKeyFileSelector of(PrivateKeyFileSelector selector) {
		Assert.notNull(selector, "Selector must not be null");
		return selector;
	}

}
