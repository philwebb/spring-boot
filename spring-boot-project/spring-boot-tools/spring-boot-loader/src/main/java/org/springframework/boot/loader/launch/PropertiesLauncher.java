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

package org.springframework.boot.loader.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.loader.launch.Archive.Entry;
import org.springframework.boot.loader.log.DebugLogger;

/**
 * {@link Launcher} for archives with user-configured classpath and main class through a
 * properties file. This model is often more flexible and more amenable to creating
 * well-behaved OS-level services than a model based on executable jars.
 * <p>
 * Looks in various places for a properties file to extract loader settings, defaulting to
 * {@code loader.properties} either on the current classpath or in the current working
 * directory. The name of the properties file can be changed by setting a System property
 * {@code loader.config.name} (e.g. {@code -Dloader.config.name=my} will look for
 * {@code my.properties}. If that file doesn't exist then tries
 * {@code loader.config.location} (with allowed prefixes {@code classpath:} and
 * {@code file:} or any valid URL). Once that file is located turns it into Properties and
 * extracts optional values (which can also be provided overridden as System properties in
 * case the file doesn't exist):
 * <ul>
 * <li>{@code loader.path}: a comma-separated list of directories (containing file
 * resources and/or nested archives in *.jar or *.zip or archives) or archives to append
 * to the classpath. {@code BOOT-INF/classes,BOOT-INF/lib} in the application archive are
 * always used</li>
 * <li>{@code loader.main}: the main method to delegate execution to once the class loader
 * is set up. No default, but will fall back to looking for a {@code Start-Class} in a
 * {@code MANIFEST.MF}, if there is one in <code>${loader.home}/META-INF</code>.</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Janne Valkealahti
 * @author Andy Wilkinson
 * @since 3.2.0
 */
public class PropertiesLauncher extends Launcher {

	/**
	 * Properties key for main class. As a manifest entry can also be specified as
	 * {@code Start-Class}.
	 */
	public static final String MAIN = "loader.main";

	/**
	 * Properties key for classpath entries (directories possibly containing jars or
	 * jars). Multiple entries can be specified using a comma-separated list. {@code
	 * BOOT-INF/classes,BOOT-INF/lib} in the application archive are always used.
	 */
	public static final String PATH = "loader.path";

	/**
	 * Properties key for home directory. This is the location of external configuration
	 * if not on classpath, and also the base path for any relative paths in the
	 * {@link #PATH loader path}. Defaults to current working directory (
	 * <code>${user.dir}</code>).
	 */
	public static final String HOME = "loader.home";

	/**
	 * Properties key for default command line arguments. These arguments (if present) are
	 * prepended to the main method arguments before launching.
	 */
	public static final String ARGS = "loader.args";

	/**
	 * Properties key for name of external configuration file (excluding suffix). Defaults
	 * to "application". Ignored if {@link #CONFIG_LOCATION loader config location} is
	 * provided instead.
	 */
	public static final String CONFIG_NAME = "loader.config.name";

	/**
	 * Properties key for config file location (including optional classpath:, file: or
	 * URL prefix).
	 */
	public static final String CONFIG_LOCATION = "loader.config.location";

	/**
	 * Properties key for boolean flag (default false) which, if set, will cause the
	 * external configuration properties to be copied to System properties (assuming that
	 * is allowed by Java security).
	 */
	public static final String SET_SYSTEM_PROPERTIES = "loader.system";

	private static final URL[] NO_URLS = new URL[0];

	private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");

	private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;

	private static final DebugLogger debug = DebugLogger.get(PropertiesLauncher.class);

	private final Archive rootArchive;

	private final File home;

	private final List<String> paths;

	private final Properties properties = new Properties();

	private volatile ClassPathArchives classPathArchives;

	public PropertiesLauncher() throws Exception {
		try {
			this.rootArchive = createRootArchive();
			this.home = getHomeDirectory();
			initializeProperties();
			this.paths = createPaths();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected File getHomeDirectory() throws Exception {
		return new File(getPropertyWithDefault(HOME, "${user.dir}"));
	}

	private void initializeProperties() throws Exception {
		List<String> configs = new ArrayList<>();
		if (getProperty(CONFIG_LOCATION) != null) {
			configs.add(getProperty(CONFIG_LOCATION));
		}
		else {
			String[] names = getPropertyWithDefault(CONFIG_NAME, "loader").split(",");
			for (String name : names) {
				String propertiesFile = name + ".properties";
				configs.add("file:" + this.home + "/" + propertiesFile);
				configs.add("classpath:" + propertiesFile);
				configs.add("classpath:BOOT-INF/classes/" + propertiesFile);
			}
		}
		for (String config : configs) {
			try (InputStream resource = getResource(config)) {
				if (resource == null) {
					debug.log("Not found: %s", config);
					continue;
				}
				debug.log("Found: %s", config);
				loadResource(resource);
				return; // Load the first one we find
			}
		}
	}

	private InputStream getResource(String config) throws Exception {
		if (config.startsWith("classpath:")) {
			return getClasspathResource(config.substring("classpath:".length()));
		}
		config = handleUrl(config);
		if (isUrl(config)) {
			return getURLResource(config);
		}
		return getFileResource(config);
	}

	private InputStream getClasspathResource(String config) {
		while (config.startsWith("/")) {
			config = config.substring(1);
		}
		config = "/" + config;
		debug.log("Trying classpath: %s", config);
		return getClass().getResourceAsStream(config);
	}

	private String handleUrl(String path) throws UnsupportedEncodingException {
		if (path.startsWith("jar:file:") || path.startsWith("file:")) {
			path = URLDecoder.decode(path, "UTF-8");
			if (path.startsWith("file:")) {
				path = path.substring("file:".length());
				if (path.startsWith("//")) {
					path = path.substring(2);
				}
			}
		}
		return path;
	}

	private boolean isUrl(String config) {
		return config.contains("://");
	}

	private InputStream getURLResource(String config) throws Exception {
		URL url = new URL(config);
		if (exists(url)) {
			URLConnection con = url.openConnection();
			try {
				return con.getInputStream();
			}
			catch (IOException ex) {
				// Close the HTTP connection (if applicable).
				if (con instanceof HttpURLConnection httpURLConnection) {
					httpURLConnection.disconnect();
				}
				throw ex;
			}
		}
		return null;
	}

	private boolean exists(URL url) throws IOException {
		// Try a URL connection content-length header...
		URLConnection connection = url.openConnection();
		try {
			connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
			if (connection instanceof HttpURLConnection httpConnection) {
				httpConnection.setRequestMethod("HEAD");
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					return true;
				}
				if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
					return false;
				}
			}
			return (connection.getContentLength() >= 0);
		}
		finally {
			if (connection instanceof HttpURLConnection httpURLConnection) {
				httpURLConnection.disconnect();
			}
		}
	}

	private InputStream getFileResource(String config) throws Exception {
		File file = new File(config);
		debug.log("Trying file: %s", config);
		if (file.canRead()) {
			return new FileInputStream(file);
		}
		return null;
	}

	private void loadResource(InputStream resource) throws Exception {
		this.properties.load(resource);
		resolvePropertyPlaceholders();
		if ("true".equalsIgnoreCase(getProperty(SET_SYSTEM_PROPERTIES))) {
			addToSystemProperties();
		}
	}

	private void resolvePropertyPlaceholders() {
		for (String name : this.properties.stringPropertyNames()) {
			String value = this.properties.getProperty(name);
			String resolved = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
			if (resolved != null) {
				this.properties.put(name, resolved);
			}
		}
	}

	private void addToSystemProperties() {
		debug.log("Adding resolved properties to System properties");
		for (String name : this.properties.stringPropertyNames()) {
			String value = this.properties.getProperty(name);
			System.setProperty(name, value);
		}
	}

	private List<String> createPaths() throws Exception {
		String path = getProperty(PATH);
		List<String> paths = (path != null) ? parsePathsProperty(path) : Collections.emptyList();
		debug.log("Nested archive paths: %s", this.paths);
		return paths;
	}

	private List<String> parsePathsProperty(String commaSeparatedPaths) {
		List<String> paths = new ArrayList<>();
		for (String path : commaSeparatedPaths.split(",")) {
			path = cleanupPath(path);
			// "" means the user wants root of archive but not current directory
			path = (path == null || path.isEmpty()) ? "/" : path;
			paths.add(path);
		}
		if (paths.isEmpty()) {
			paths.add("lib");
		}
		return paths;
	}

	private String cleanupPath(String path) {
		path = path.trim();
		// No need for current dir path
		if (path.startsWith("./")) {
			path = path.substring(2);
		}
		String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
		if (lowerCasePath.endsWith(".jar") || lowerCasePath.endsWith(".zip")) {
			return path;
		}
		if (path.endsWith("/*")) {
			return path.substring(0, path.length() - 1);
		}
		// It's a directory
		return (!path.endsWith("/") && !path.equals(".")) ? path + "/" : path;
	}

	@Override
	protected ClassLoader createClassLoader(Iterator<Archive> archives) throws Exception {
		String loaderClassName = getProperty("loader.classLoader");
		if (loaderClassName == null) {
			return super.createClassLoader(archives);
		}
		Set<URL> urls = new LinkedHashSet<>();
		archives.forEachRemaining((archive) -> urls.add(archive.getUrl()));
		ClassLoader classLoader = new LaunchedURLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
		debug.log("Classpath for custom loader: %s", urls);
		classLoader = wrapWithCustomClassLoader(classLoader, loaderClassName);
		debug.log("Using custom class loader: %s", loaderClassName);
		return classLoader;
	}

	private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String loaderClassName) throws Exception {
		Instantiator<ClassLoader> instantiator = new Instantiator<>(parent, loaderClassName);
		ClassLoader loader = instantiator.declaredConstructor(ClassLoader.class).newInstance(parent);
		loader = (loader != null) ? loader
				: instantiator.declaredConstructor(URL[].class, ClassLoader.class).newInstance(NO_URLS, parent);
		loader = (loader != null) ? loader : instantiator.constructWithoutParameters();
		if (loader != null) {
			return loader;
		}
		throw new IllegalStateException("Unable to create class loader for " + loaderClassName);
	}

	@Override
	protected String getMainClass() throws Exception {
		String mainClass = getProperty(MAIN, "Start-Class");
		if (mainClass == null) {
			throw new IllegalStateException("No '" + MAIN + "' or 'Start-Class' specified");
		}
		return mainClass;
	}

	@Override
	protected Iterator<Archive> getClassPathArchives() throws Exception {
		ClassPathArchives classPathArchives = this.classPathArchives;
		if (classPathArchives == null) {
			classPathArchives = new ClassPathArchives();
			this.classPathArchives = classPathArchives;
		}
		return classPathArchives.iterator();
	}

	protected String[] getArgs(String... args) throws Exception {
		String loaderArgs = getProperty(ARGS);
		return (loaderArgs != null) ? merge(loaderArgs.split("\\s+"), args) : args;
	}

	private String[] merge(String[] a1, String[] a2) {
		String[] result = new String[a1.length + a2.length];
		System.arraycopy(a1, 0, result, 0, a1.length);
		System.arraycopy(a2, 0, result, a1.length, a2.length);
		return result;
	}

	private String getProperty(String name) throws Exception {
		return getProperty(name, null, null);
	}

	private String getProperty(String name, String manifestKey) throws Exception {
		return getProperty(name, manifestKey, null);
	}

	private String getPropertyWithDefault(String name, String defaultValue) throws Exception {
		return getProperty(name, null, defaultValue);
	}

	private String getProperty(String name, String manifestKey, String defaultValue) throws Exception {
		manifestKey = (manifestKey != null) ? manifestKey : toCamelCase(name.replace('.', '-'));
		String value = SystemPropertyUtils.getProperty(name);
		if (value != null) {
			return getResolvedProperty(name, manifestKey, value, "environment");
		}
		if (this.properties.containsKey(name)) {
			value = this.properties.getProperty(name);
			return getResolvedProperty(name, manifestKey, value, "properties");
		}
		// Prefer home dir for MANIFEST if there is one
		if (this.home != null) {
			try {
				try (ExplodedArchive archive = new ExplodedArchive(this.home, false)) {
					value = getManifestValue(archive, manifestKey);
					if (value != null) {
						return getResolvedProperty(name, manifestKey, value, "home directory manifest");
					}
				}
			}
			catch (IllegalStateException ex) {
			}
		}
		// Otherwise try the root archive
		value = getManifestValue(this.rootArchive, manifestKey);
		if (value != null) {
			return getResolvedProperty(name, manifestKey, value, "manifest");
		}
		return (defaultValue != null) ? SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue)
				: defaultValue;
	}

	String getManifestValue(Archive archive, String manifestKey) throws Exception {
		Manifest manifest = archive.getManifest();
		return (manifest != null) ? manifest.getMainAttributes().getValue(manifestKey) : null;
	}

	private String getResolvedProperty(String name, String manifestKey, String value, String from) {
		value = SystemPropertyUtils.resolvePlaceholders(this.properties, value);
		String altName = (manifestKey != null && !manifestKey.equals(name)) ? "[%s] ".formatted(manifestKey) : "";
		debug.log("Property '%s'%s from %s: %s", name, altName, from, value);
		return value;

	}

	void close() throws Exception {
		if (this.classPathArchives != null) {
			this.classPathArchives.close();
		}
		if (this.rootArchive != null) {
			this.rootArchive.close();
		}
	}

	public static String toCamelCase(CharSequence string) {
		if (string == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		Matcher matcher = WORD_SEPARATOR.matcher(string);
		int pos = 0;
		while (matcher.find()) {
			builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
			pos = matcher.end();
		}
		builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
		return builder.toString();
	}

	private static String capitalize(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	public static void main(String[] args) throws Exception {
		PropertiesLauncher launcher = new PropertiesLauncher();
		args = launcher.getArgs(args);
		launcher.launch(args);
	}

	/**
	 * An iterable collection of the classpath archives.
	 */
	private class ClassPathArchives implements Iterable<Archive> {

		private final List<Archive> classPathArchives;

		private final List<AutoCloseable> closeables = new ArrayList<>();

		ClassPathArchives() throws Exception {
			this.classPathArchives = new ArrayList<>();
			for (String path : PropertiesLauncher.this.paths) {
				for (Archive archive : getClassPathArchives(path)) {
					addClassPathArchive(archive);
				}
			}
			addNestedEntries();
		}

		private List<Archive> getClassPathArchives(String path) throws Exception {
			path = cleanupPath(handleUrl(path));
			File file = new File(path);
			List<Archive> classPathArchives = new ArrayList<>();
			if (!"/".equals(path)) {
				file = (!isAbsolutePath(path)) ? new File(PropertiesLauncher.this.home, path) : file;
				if (file.isDirectory()) {
					debug.log("Adding classpath entries from directory %s", file);
					classPathArchives.add(new ExplodedArchive(file, false));
				}
			}
			Archive archive = getArchive(file);
			if (archive != null) {
				debug.log("Adding classpath entries from archive %s", archive.getUrl() + path);
				classPathArchives.add(archive);
			}
			List<Archive> nestedArchives = getNestedArchives(path);
			if (nestedArchives != null) {
				debug.log("Adding classpath entries from nested %s", path);
				classPathArchives.addAll(nestedArchives);
			}
			return classPathArchives;
		}

		private boolean isAbsolutePath(String root) {
			// Windows contains ":" others start with "/"
			return root.contains(":") || root.startsWith("/");
		}

		private Archive getArchive(File file) throws IOException {
			String name = file.getName().toLowerCase(Locale.ENGLISH);
			if (!isNestedArchivePath(file) && (name.endsWith(".jar") || name.endsWith(".zip"))) {
				return getJarFileArchive(file);
			}
			return null;
		}

		private boolean isNestedArchivePath(File file) {
			return file.getPath().contains(NESTED_ARCHIVE_SEPARATOR);
		}

		private List<Archive> getNestedArchives(String path) throws Exception {
			Archive parent = PropertiesLauncher.this.rootArchive;
			String rootPath = path;
			if (!rootPath.equals("/") && rootPath.startsWith("/")
					|| parent.getUrl().toURI().equals(PropertiesLauncher.this.home.toURI())) {
				// If home dir is same as parent archive, no need to add it twice.
				return null;
			}
			int index = rootPath.indexOf('!');
			if (index != -1) {
				File file = new File(PropertiesLauncher.this.home, rootPath.substring(0, index));
				if (rootPath.startsWith("jar:file:")) {
					file = new File(rootPath.substring("jar:file:".length(), index));
				}
				parent = getJarFileArchive(file);
				rootPath = rootPath.substring(index + 1);
				while (rootPath.startsWith("/")) {
					rootPath = rootPath.substring(1);
				}
			}
			if (rootPath.endsWith(".jar")) {
				File file = new File(PropertiesLauncher.this.home, rootPath);
				if (file.exists()) {
					parent = getJarFileArchive(file);
					rootPath = "";
				}
			}
			if (rootPath.equals("/") || rootPath.equals("./") || rootPath.equals(".")) {
				// The prefix for nested jars is actually empty if it's at the root
				rootPath = "";
			}
			List<Archive> archives = new ArrayList<>();
			parent.getNestedArchives(null, filterByPrefix(rootPath)).forEachRemaining(archives::add);
			if ((rootPath == null || rootPath.isEmpty() || ".".equals(rootPath)) && !path.endsWith(".jar")
					&& parent != PropertiesLauncher.this.rootArchive) {
				// You can't find the root with an entry filter so it has to be added
				// explicitly. But don't add the root of the parent archive.
				archives.add(parent);
			}
			return archives;
		}

		private void addClassPathArchive(Archive archive) throws IOException {
			this.classPathArchives.add(archive);
			if (archive.isExploded()) {
				archive.getNestedArchives(null, this::isArchive).forEachRemaining(this.classPathArchives::add);
			}
		}

		private Predicate<Entry> filterByPrefix(String prefix) {
			return (entry) -> (entry.isDirectory() && entry.getName().equals(prefix))
					|| (isArchive(entry) && entry.getName().startsWith(prefix));
		}

		private boolean isArchive(Entry entry) {
			String name = entry.getName();
			return name.endsWith(".jar") || name.endsWith(".zip");
		}

		private void addNestedEntries() {
			// The parent archive might have "BOOT-INF/lib/" and "BOOT-INF/classes/"
			// directories, meaning we are running from an executable JAR. We add nested
			// entries from there with low priority (i.e. at end).
			try {
				PropertiesLauncher.this.rootArchive.getNestedArchives(null, this::isNestedArchive)
					.forEachRemaining(this.classPathArchives::add);
			}
			catch (IOException ex) {
				// Ignore
			}
		}

		private boolean isNestedArchive(Archive.Entry entry) {
			return (entry.isDirectory() && entry.getName().equals("BOOT-INF/classes/"))
					|| entry.getName().startsWith("BOOT-INF/lib/");
		}

		private JarFileArchive getJarFileArchive(File file) throws IOException {
			JarFileArchive archive = new JarFileArchive(file);
			this.closeables.add(archive);
			return archive;
		}

		@Override
		public Iterator<Archive> iterator() {
			return this.classPathArchives.iterator();
		}

		void close() throws Exception {
			for (AutoCloseable closeable : this.closeables) {
				closeable.close();
			}
		}

	}

	/**
	 * Simple utility to help instantiate objects.
	 */
	private record Instantiator<T>(ClassLoader parent, Class<?> type) {

		Instantiator(ClassLoader parent, String className) throws ClassNotFoundException {
			this(parent, Class.forName(className, true, parent));
		}

		T constructWithoutParameters() throws Exception {
			return declaredConstructor().newInstance();
		}

		Using<T> declaredConstructor(Class<?>... parameterTypes) {
			return new Using<>(this, parameterTypes);
		}

		private record Using<T>(Instantiator<T> instantiator, Class<?>... parameterTypes) {

			@SuppressWarnings("unchecked")
			T newInstance(Object... initargs) throws Exception {
				try {
					Constructor<?> constructor = this.instantiator.type().getDeclaredConstructor(this.parameterTypes);
					constructor.setAccessible(true);
					return (T) constructor.newInstance(initargs);
				}
				catch (NoSuchMethodException ex) {
					return null;
				}
			}

		}
	}

}
