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

package org.springframework.boot.autoconfigure.condition;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link Condition} that checks for the presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OnClassCondition extends SpringBootAutoConfigurationCondition {

	private Map<String, Set<String>> onClasses;

	// public static MultiValueMap<String, String> onClass = new
	// LinkedMultiValueMap<String, String>();
	// public static MultiValueMap<String, String> onMissingClass = new
	// LinkedMultiValueMap<String, String>();

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			String configurationClass) {
		Map<String, Set<String>> onClasses = getOnClasses(context.getClassLoader());
		Set<String> candidates = onClasses.get(configurationClass);
		if (candidates != null) {
			List<String> missing = getMatchingClasses(candidates, MatchType.MISSING,
					context);
			if (!missing.isEmpty()) {
				return ConditionOutcome
						.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
								.didNotFind("required class", "required classes")
								.items(Style.QUOTE, missing));
			}
		}
		return null;
	}

	private Map<String, Set<String>> getOnClasses(ClassLoader classLoader) {
		if (this.onClasses == null) {
			this.onClasses = loadOnClasses(classLoader);
		}
		return this.onClasses;
	}

	private Map<String, Set<String>> loadOnClasses(ClassLoader classLoader) {
		String name = "META-INF/" + ConditionalOnClass.class.getName() + ".properties";
		try {
			Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(name)
					: ClassLoader.getSystemResources(name));
			Map<String, Set<String>> result = new HashMap<String, Set<String>>();
			while (urls.hasMoreElements()) {
				addProperties(result, urls.nextElement());
			}
			for (String key : result.keySet()) {
				result.put(key, Collections.unmodifiableSet(result.get(key)));
			}
			return Collections.unmodifiableMap(result);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Unable to load @ConditionalOnClass location [" + name + "]", ex);
		}
	}

	private void addProperties(Map<String, Set<String>> result, URL url)
			throws IOException {
		Properties properties = PropertiesLoaderUtils
				.loadProperties(new UrlResource(url));
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String[] conditionalClasses = StringUtils
					.commaDelimitedListToStringArray((String) entry.getValue());
			Set<String> resultValues = result.get(entry.getKey());
			if (resultValues == null) {
				resultValues = new HashSet<String>();
				result.put((String) entry.getKey(), resultValues);
			}
			resultValues.addAll(Arrays.asList(conditionalClasses));
		}
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		MultiValueMap<String, Object> onClasses = getAttributes(metadata,
				ConditionalOnClass.class);
		if (onClasses != null) {
			// dunno(onClass, metadata, getClasses(onClasses));
			List<String> missing = getMatchingClasses(onClasses, MatchType.MISSING,
					context);
			if (!missing.isEmpty()) {
				return ConditionOutcome
						.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
								.didNotFind("required class", "required classes")
								.items(Style.QUOTE, missing));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
					.found("required class", "required classes").items(Style.QUOTE,
							getMatchingClasses(onClasses, MatchType.PRESENT, context));
		}
		MultiValueMap<String, Object> onMissingClasses = getAttributes(metadata,
				ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			// dunno(onMissingClass, metadata, getClasses(onMissingClasses));
			List<String> present = getMatchingClasses(onMissingClasses, MatchType.PRESENT,
					context);
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(
						ConditionMessage.forCondition(ConditionalOnMissingClass.class)
								.found("unwanted class", "unwanted classes")
								.items(Style.QUOTE, present));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
					.didNotFind("unwanted class", "unwanted classes")
					.items(Style.QUOTE, getMatchingClasses(onMissingClasses,
							MatchType.MISSING, context));
		}
		return ConditionOutcome.match(matchMessage);
	}

	private MultiValueMap<String, Object> getAttributes(AnnotatedTypeMetadata metadata,
			Class<?> annotationType) {
		return metadata.getAllAnnotationAttributes(annotationType.getName(), true);
	}

	private List<String> getMatchingClasses(MultiValueMap<String, Object> attributes,
			MatchType matchType, ConditionContext context) {
		List<String> candidates = getClasses(attributes);
		return getMatchingClasses(candidates, matchType, context);
	}

	private List<String> getClasses(MultiValueMap<String, Object> attributes) {
		List<String> candidates = new ArrayList<String>();
		addAll(candidates, attributes.get("value"));
		addAll(candidates, attributes.get("name"));
		return candidates;
	}

	private List<String> getMatchingClasses(Collection<String> candidates,
			MatchType matchType, ConditionContext context) {
		List<String> matches = new ArrayList<String>(candidates);
		Iterator<String> iterator = matches.iterator();
		while (iterator.hasNext()) {
			if (!matchType.matches(iterator.next(), context)) {
				iterator.remove();
			}
		}
		return matches;
	}

	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private enum MatchType {

		PRESENT {

			@Override
			public boolean matches(String className, ConditionContext context) {
				return isPresent(className, context);
			}

		},

		MISSING {

			@Override
			public boolean matches(String className, ConditionContext context) {
				return !isPresent(className, context);
			}

		};

		public abstract boolean matches(String className, ConditionContext context);

		private static boolean isPresent(String className, ConditionContext context) {
			ClassLoader classLoader = context.getClassLoader();
			// boolean theirs = ClassUtils.isPresent(className, classLoader);
			boolean mine = dunno(className, classLoader);
			// Assert.state(theirs == mine, "Fail " + className + " theirs " + theirs);
			if (!mine) {
				// System.err.println(className);
			}
			return mine;

		}

		private static boolean dunno(String className, ClassLoader classLoader) {
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			try {
				if (classLoader != null) {
					// if (classLoader instanceof URLClassLoader) {
					// new Dunno((URLClassLoader) classLoader);
					// }
					// if (classLoader.getResource(
					// className.replace(".", "/") + ".class") == null) {
					// // System.err.println(className);
					// return false;
					// }
					return classLoader.loadClass(className) != null;
				}
				return Class.forName(className) != null;
			}
			catch (Throwable ex) {
				return false;
			}
		}

	}

	private static class Dunno {

		public Dunno(URLClassLoader cl) throws IOException {
			URL[] urLs = cl.getURLs();
			for (URL url : urLs) {
				URLConnection connection = url.openConnection();
				if (connection instanceof JarURLConnection) {
					JarFile jarFile = ((JarURLConnection) connection).getJarFile();
					System.out.println(jarFile);
				}
			}
		}

	}

}
