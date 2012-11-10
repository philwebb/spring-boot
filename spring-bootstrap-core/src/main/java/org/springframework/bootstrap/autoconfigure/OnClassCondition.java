
package org.springframework.bootstrap.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Condition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

class OnClassCondition implements Condition {

	public boolean matches(AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(ConditionalOnClass.class.getName(), true);
		if (attributes != null) {
			List<String> classNames = new ArrayList<String>();
			collectClassNames(classNames, attributes.get("value"));
			collectClassNames(classNames, attributes.get("name"));
			Assert.isTrue(classNames.size() > 0,
					"@ConditionalOnClass annotations must specify at least one class value");
			for (String className : classNames) {
				if (!ClassUtils.isPresent(className, getClass().getClassLoader())) {
					return false;
				}
			}
		}
		return true;
	}

	private void collectClassNames(List<String> classNames, List<Object> values) {
		for (Object value : values) {
			for (Object valueItem : (Object[]) value) {
				classNames.add(valueItem instanceof Class<?> ? ((Class<?>)valueItem).getName(): valueItem.toString());
			}
		}
	}

}
