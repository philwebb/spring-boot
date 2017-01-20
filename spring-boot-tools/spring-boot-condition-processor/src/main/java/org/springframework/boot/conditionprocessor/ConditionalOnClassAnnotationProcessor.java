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

package org.springframework.boot.conditionprocessor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.util.StringUtils;

@SupportedAnnotationTypes({"*"})
public class ConditionalOnClassAnnotationProcessor extends AbstractProcessor {

	static final String METADATA_PATH = "META-INF/ConditionalOnClass.properties";

	static final String CONDITIONAL_ON_CLASS_ANNOTATION = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass";

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private TypeUtils typeUtils;

	Map<String, String> metadataCollector = new HashMap<String, String>();

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
		this.typeUtils = new TypeUtils();
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Elements elementUtils = this.processingEnv.getElementUtils();
		TypeElement annotationType = elementUtils
				.getTypeElement(conditionalOnClassAnnotation());
		if (annotationType != null) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
				processElement(element);
			}
		}
		if (roundEnv.processingOver()) {
			try {
				writeMetaData();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private void writeMetaData() throws IOException {
		System.out.println("hello");
		if (!metadataCollector.isEmpty()) {
			OutputStream outputStream = createMetadataResource().openOutputStream();
			try {
				outputStream.write(metadataCollector.toString().getBytes());
			}
			finally {
				outputStream.close();
			}
		}
	}

	private FileObject createMetadataResource() throws IOException {
		FileObject resource = this.processingEnv.getFiler()
				.createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
		return resource;
	}

	protected String conditionalOnClassAnnotation() {
		return CONDITIONAL_ON_CLASS_ANNOTATION;
	}

	private void processElement(Element element) {
		try {
			AnnotationMirror annotation = getAnnotation(element,
					conditionalOnClassAnnotation());
			if (annotation != null) {
				List<String> values = getValues(annotation);
				processAnnotatedTypeElement(values, (TypeElement) element);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processAnnotatedTypeElement(List<String> values, TypeElement element) {
		String type = this.typeUtils.getQualifiedName(element);
		this.metadataCollector.put(type, StringUtils.collectionToCommaDelimitedString(values));
	}

	private List<String> getValues(AnnotationMirror annotation) {
		Map<String, Object> elementValues = getAnnotationElementValues(annotation);
		List<String> values = new ArrayList<String>();
		List<String> name = (List<String>) elementValues.get("name");
		if (name != null && !name.isEmpty()) {
			values.addAll(name);
		}
		List<String> value = (List<String>) elementValues.get("value");
		if (value != null && !value.isEmpty()) {
			values.addAll(value);
		}
		return values;
	}

	private Map<String,Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String,Object> values = new LinkedHashMap<String,Object>();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation
				.getElementValues().entrySet()) {
				values.put(entry.getKey().getSimpleName().toString(), entry.getValue().getValue());
		}
		return values;
	}

	private AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

}
