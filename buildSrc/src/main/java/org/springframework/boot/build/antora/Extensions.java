/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.boot.build.antora;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Antora and Asciidoc extensions used by Spring Boot.
 *
 * @author Phillip Webb
 */
public class Extensions {

	private static final List<Extension> antora;
	static {
		List<Extension> extensions = new ArrayList<>();
		extensions.add(new Extension("@springio/antora-extensions", "1.8.2",
				"@springio/antora-extensions/root-component-extension"));
		extensions.add(new Extension("@springio/antora-xref-extension", "1.0.0-alpha.3"));
		extensions.add(new Extension("@springio/antora-zip-contents-collector-extension", "1.0.0-alpha.1"));
		antora = List.copyOf(extensions);
	}

	private static final List<Extension> asciidoc;
	static {
		List<Extension> extensions = new ArrayList<>();
		extensions.add(new Extension("@asciidoctor/tabs", "1.0.0-beta.6"));
		extensions
			.add(new Extension("@springio/asciidoctor-extensions", "1.0.0-alpha.10", "@springio/asciidoctor-extensions",
					"@springio/asciidoctor-extensions/configuration-properties-extension",
					"@springio/asciidoctor-extensions/section-ids-extension"));
		asciidoc = List.copyOf(extensions);
	}

	public static Map<String, String> packages() {
		Map<String, String> packages = new TreeMap<>();
		antora.stream().forEach((extension) -> packages.put(extension.name(), extension.version()));
		asciidoc.stream().forEach((extension) -> packages.put(extension.name(), extension.version()));
		return Collections.unmodifiableMap(packages);
	}

	static List<Map<String, Object>> antora(Consumer<AntoraExtensionsConfiguration> extensions) {
		AntoraExtensionsConfiguration result = new AntoraExtensionsConfiguration(
				antora.stream().flatMap(Extension::names).sorted().toList());
		extensions.accept(result);
		return result.config();
	}

	static List<String> asciidoc() {
		return asciidoc.stream().flatMap(Extension::names).sorted().toList();
	}

	private record Extension(String name, String version, String... includeNames) {

		Stream<String> names() {
			return (this.includeNames.length != 0) ? Arrays.stream(this.includeNames) : Stream.of(this.name);
		}

	}

	static class AntoraExtensionsConfiguration {

		private Map<String, Map<String, Object>> extensions = new TreeMap<>();

		private AntoraExtensionsConfiguration(List<String> names) {
			names.forEach((name) -> this.extensions.put(name, null));
		}

		void xref(Consumer<Xref> xref) {
			xref.accept(new Xref());
		}

		void zipContentsCollector(Consumer<ZipContentsCollector> zipContentsCollector) {
			zipContentsCollector.accept(new ZipContentsCollector());
		}

		void rootComponent(Consumer<RootComponent> rootComponent) {
			rootComponent.accept(new RootComponent());
		}

		List<Map<String, Object>> config() {
			List<Map<String, Object>> config = new ArrayList<>();
			this.extensions.forEach((name, customizations) -> {
				Map<String, Object> extensionConfig = new LinkedHashMap<>();
				extensionConfig.put("require", name);
				if (customizations != null) {
					extensionConfig.putAll(customizations);
				}
				config.add(extensionConfig);
			});
			return List.copyOf(config);
		}

		abstract class Customizer {

			private final String name;

			Customizer(String name) {
				this.name = name;
			}

			protected void customize(String key, Object value) {
				AntoraExtensionsConfiguration.this.extensions.computeIfAbsent(this.name, (name) -> new TreeMap<>())
					.put(key, value);
			}

		}

		class Xref extends Customizer {

			Xref() {
				super("@springio/antora-xref-extension");
			}

			void stub(List<String> stub) {
				if (stub != null && !stub.isEmpty()) {
					customize("stub", stub);
				}
			}

		}

		class ZipContentsCollector extends Customizer {

			ZipContentsCollector() {
				super("@springio/antora-zip-contents-collector-extension");
			}

			void versionFile(String versionFile) {
				customize("version_file", versionFile);
			}

			void locations(Path... locations) {
				locations(Arrays.stream(locations).map(Path::toString).toList());
			}

			private void locations(List<String> locations) {
				customize("locations", locations);
			}

			void alwaysInclude(Map<String, String> alwaysInclude) {
				if (alwaysInclude != null && !alwaysInclude.isEmpty()) {
					customize("always_include", List.of(new TreeMap<>(alwaysInclude)));
				}
			}

		}

		class RootComponent extends Customizer {

			RootComponent() {
				super("@springio/antora-extensions/root-component-extension");
			}

			public void name(String name) {
				customize("root_component_name", name);
			}

		}

	}

}
