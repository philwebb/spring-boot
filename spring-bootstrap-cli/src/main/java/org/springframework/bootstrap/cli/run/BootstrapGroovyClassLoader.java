package org.springframework.bootstrap.cli.run;
import groovy.lang.GroovyClassLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.SourceUnit;

public class BootstrapGroovyClassLoader extends GroovyClassLoader {

	private Map<String, byte[]> classResources = new HashMap<String, byte[]>();

	public BootstrapGroovyClassLoader(ClassLoader loader, CompilerConfiguration config) {
		super(loader, config);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream resourceStream = super.getResourceAsStream(name);
		if(resourceStream == null) {
			byte[] bytes = this.classResources.get(name);
			resourceStream = bytes == null ? null : new ByteArrayInputStream(bytes);
		}
		return resourceStream;
	}

	@Override
	protected ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
		InnerLoader loader = AccessController.doPrivileged(new PrivilegedAction<InnerLoader>() {
			public InnerLoader run() {
				return new InnerLoader(BootstrapGroovyClassLoader.this);
			}
		});
		return new ExtendedClassCollector(loader, unit, su);
	}

	protected class ExtendedClassCollector extends ClassCollector {

		protected ExtendedClassCollector(InnerLoader loader, CompilationUnit unit,
				SourceUnit su) {
			super(loader, unit, su);
		}

		protected Class createClass(byte[] code, ClassNode classNode) {
			Class createdClass = super.createClass(code, classNode);
			BootstrapGroovyClassLoader.this.classResources.put(
					classNode.getName().replace(".", "/") + ".class", code);
			return createdClass;
		}
	}

}
