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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.Assert;

/**
 * Strategy interface used to select a single {@link CertificateFile} when multiple
 * candidates exist. Selectors may be registered as beans where they will be checked
 * {@link ObjectProvider#orderedStream() using standard ordering rules}. The first
 * selector to provide a {@code non-null} result will be used. If not suitable selector is
 * found then the selector settings from {@link SslProperties} will be used.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @since 3.2.0
 * @see CertificateFile
 * @see PrivateKeyFileSelector
 */
@FunctionalInterface
public interface CertificateFileSelector {

	/**
	 * Selects a {@link CertificateFile} from the given set of candidates taking into
	 * consideration the bundle name.
	 * @param bundleName the bundle name
	 * @param candidates the candidates to consider
	 * @return a single candidate or {@code null}
	 */
	default CertificateFile selectCertificateFile(String bundleName, List<CertificateFile> candidates) {
		return selectCertificateFile(candidates);
	}

	/**
	 * Selects a {@link CertificateFile} from the given set of candidates without
	 * considering the bundle name.
	 * @param candidates the candidates to consider
	 * @return a single candidate or {@code null}
	 * @see #forBundles(String...)
	 * @see #forBundles(Predicate)
	 */
	CertificateFile selectCertificateFile(List<CertificateFile> candidates);

	/**
	 * Return a new {@link CertificateFileSelector} instance that is limited to the given
	 * SSL bundle names.
	 * @param bundleNames the bundle names to accept
	 * @return a new {@link CertificateFileSelector} instance
	 */
	default CertificateFileSelector forBundles(String... bundleNames) {
		Assert.notNull(bundleNames, "BundleNames must not be null");
		Set<String> bundleNameSet = Set.of(bundleNames);
		return forBundles(bundleNameSet::contains);
	}

	/**
	 * Return a new {@link CertificateFileSelector} instance that is limited to specific
	 * SSL bundle names.
	 * @param predicate the predicate used to test if an SSL bundle name is accepted
	 * @return a new {@link CertificateFileSelector} instance
	 */
	default CertificateFileSelector forBundles(Predicate<String> predicate) {
		Assert.notNull(predicate, "Predicate must not be null");
		return forMatching((bundleName, candidate) -> bundleName == null || predicate.test(bundleName));
	}

	/**
	 * Return a new {@link CertificateFileSelector} instance that is limited to in-date
	 * leaf certificates.
	 * @return a new {@link CertificateFileSelector} instance
	 */
	default CertificateFileSelector forInDateLeafCertificates() {
		return forInDateLeafCertificates(Clock.systemDefaultZone());
	}

	/**
	 * Return a new {@link CertificateFileSelector} instance that is limited to in-date
	 * leaf certificates.
	 * @param clock the clock to use
	 * @return a new {@link CertificateFileSelector} instance
	 */
	default CertificateFileSelector forInDateLeafCertificates(Clock clock) {
		Assert.notNull(clock, "Clock must not be null");
		Instant now = clock.instant();
		return forMatching((bundleName, candidate) -> {
			Instant notBefore = candidate.leafCertificate().getNotBefore().toInstant();
			Instant notAfter = candidate.leafCertificate().getNotAfter().toInstant();
			return now.isAfter(notBefore) && now.isBefore(notAfter);
		});
	}

	private CertificateFileSelector forMatching(BiPredicate<String, CertificateFile> predicate) {
		return new CertificateFileSelector() {

			@Override
			public CertificateFile selectCertificateFile(String bundleName, List<CertificateFile> candidates) {
				List<CertificateFile> filtered = candidates.stream()
					.filter((candidate) -> predicate.test(bundleName, candidate))
					.toList();
				if (filtered.isEmpty()) {
					return null;
				}
				return CertificateFileSelector.this.selectCertificateFile(bundleName, filtered);
			}

			@Override
			public CertificateFile selectCertificateFile(List<CertificateFile> candidates) {
				List<CertificateFile> filtered = candidates.stream()
					.filter((candidate) -> predicate.test(null, candidate))
					.toList();
				if (filtered.isEmpty()) {
					return null;
				}
				return CertificateFileSelector.this.selectCertificateFile(filtered);
			}

		};
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the newest file creation date.
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static CertificateFileSelector usingFileCreationTime() {
		return using((certificateFile) -> {
			Path path = certificateFile.path();
			try {
				return Files.readAttributes(path, BasicFileAttributes.class).creationTime();
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to get creation time of file %s".formatted(path), ex);
			}
		});
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the maximum {@link X509Certificate#getNotBefore() not
	 * before field} of the leaf certificate.
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static CertificateFileSelector usingLeafCertificateValidityPeriodStart() {
		return usingLeafCertificateField(X509Certificate::getNotBefore);
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the maximum {@link X509Certificate#getNotAfter() not
	 * after field} of the leaf certificate.
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static CertificateFileSelector usingLeafCertificateValidityPeriodEnd() {
		return usingLeafCertificateField(X509Certificate::getNotAfter);
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the maximum extracted field of the leaf certificate.
	 * @param <C> the extracted comparable type
	 * @param fieldValueExtractor the field value extractor to use
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static <C extends Comparable<C>> CertificateFileSelector usingLeafCertificateField(
			Function<X509Certificate, C> fieldValueExtractor) {
		Assert.notNull(fieldValueExtractor, "FieldValueExtractor must not be null");
		return using((certificateFile) -> fieldValueExtractor.apply(certificateFile.leafCertificate()));
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the maximum extracted field.
	 * @param <C> the extracted comparable type
	 * @param extractor the extractor to use
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static <C extends Comparable<C>> CertificateFileSelector using(Function<CertificateFile, C> extractor) {
		Assert.notNull(extractor, "Extractor must not be null");
		return using(Comparator.comparing(extractor));
	}

	/**
	 * Factory method that returns a {@link CertificateFileSelector} that selects the
	 * {@link CertificateFile} using the maximum according to the specified comparator.
	 * @param comparator the comparator used to compare candidates
	 * @return a new {@link CertificateFileSelector} instance
	 */
	static CertificateFileSelector using(Comparator<CertificateFile> comparator) {
		Assert.notNull(comparator, "Comparator must not be null");
		return (candidates) -> candidates.stream().max(comparator).orElse(null);
	}

	/**
	 * Factory method that can be used when constructing a {@link CertificateFileSelector}
	 * from a lambda.
	 * @param selector the certificate selector to use
	 * @return the {@link CertificateFileSelector} instance
	 */
	static CertificateFileSelector of(CertificateFileSelector selector) {
		Assert.notNull(selector, "Selector must not be null");
		return selector;
	}

}
