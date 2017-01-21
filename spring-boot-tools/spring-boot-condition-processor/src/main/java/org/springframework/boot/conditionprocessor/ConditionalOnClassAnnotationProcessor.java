/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Annotation processor to store {@code @ConditionalOnClass} values to a property file.
 *
 * @author Madhura Bhave
 */
@SupportedAnnotationTypes("org.springframework.boot.autoconfigure.condition.ConditionalOnClass")
public class ConditionalOnClassAnnotationProcessor extends AbstractProcessor {

	private static final String CONDITIONAL_ON_CLASS_ANNOTATION = "org.springframework.boot.autoconfigure.condition.ConditionalOnClass";

	protected static final String PROPERTIES_PATH = "META-INF/"
			+ CONDITIONAL_ON_CLASS_ANNOTATION + ".properties";

	private final Properties properties = new Properties();

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
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
				writeProperties();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private void writeProperties() throws IOException {
		if (!this.properties.isEmpty()) {
			FileObject file = this.processingEnv.getFiler()
					.createResource(StandardLocation.CLASS_OUTPUT, "", PROPERTIES_PATH);
			OutputStream outputStream = file.openOutputStream();
			try {
				this.properties.store(outputStream, null);
			}
			finally {
				outputStream.close();
			}
		}
	}

	private void processElement(Element element) {
		try {
			String qualifiedName = getQualifiedName(element);
			AnnotationMirror annotation = getAnnotation(element,
					conditionalOnClassAnnotation());
			if (qualifiedName != null && annotation != null) {
				List<AnnotationValue> values = getValues(annotation);
				this.properties.put(qualifiedName, toCommaDelimitedString(values));
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Error processing configuration meta-data on " + element, ex);
		}
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

	private String toCommaDelimitedString(List<AnnotationValue> list) {
		StringBuffer result = new StringBuffer();
		for (AnnotationValue item : list) {
			result.append(result.length() != 0 ? "," : "");
			result.append(item.getValue());
		}
		return result.toString();
	}

	@SuppressWarnings("unchecked")
	private List<AnnotationValue> getValues(AnnotationMirror annotation) {
		List<AnnotationValue> result = new ArrayList<AnnotationValue>();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation
				.getElementValues().entrySet()) {
			String attributeName = entry.getKey().getSimpleName().toString();
			if ("name".equals(attributeName) || "value".equals(attributeName)) {
				result.addAll((List<AnnotationValue>) entry.getValue().getValue());
			}
		}
		return result;
	}

	private String getQualifiedName(Element element) {
		if (element != null) {
			TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
			if (enclosingElement != null) {
				return getQualifiedName(enclosingElement) + "$"
						+ ((DeclaredType) element.asType()).asElement().getSimpleName()
								.toString();
			}
			if (element instanceof TypeElement) {
				return ((TypeElement) element).getQualifiedName().toString();
			}
		}
		return null;
	}

	protected String conditionalOnClassAnnotation() {
		return CONDITIONAL_ON_CLASS_ANNOTATION;
	}

	private TypeElement getEnclosingTypeElement(TypeMirror type) {
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement != null && enclosingElement instanceof TypeElement) {
				return (TypeElement) enclosingElement;
			}
		}
		return null;
	}
}
