
package org.springframework.bootstrap.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that checks for specific beans.
 *
 * @author Phillip Webb
 * @see ConditionalOnMissingBean
 */
class OnMissingBeanCondition implements Condition {

	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
				ConditionalOnMissingBean.class.getName(), true);
		List<String> beanClasses = collect(attributes, "value");
		List<String> beanNames = collect(attributes, "value");
		Assert.isTrue(beanClasses.size() > 0 || beanNames.size() > 0,
				"@ConditionalOnMissingBean annotations must specify at least one bean");

		for (String beanClass : beanClasses) {
			try {
				String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						context.getBeanFactory(),
						ClassUtils.forName(beanClass, context.getClassLoader()));
				if (beans.length != 0) {
					return false;
				}
			} catch (ClassNotFoundException ex) {
			}
		}

		for (String beanName : beanNames) {
			if (context.getBeanFactory().containsBeanDefinition(beanName)) {
				return false;
			}
		}

		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<String> collect(MultiValueMap<String, Object> attributes, String key) {
		List<String> collected = new ArrayList<String>();
		List<String[]> valueList = (List) attributes.get(key);
		for (String[] valueArray : valueList) {
			for (String value : valueArray) {
				collected.add(value);
			}
		}
		return collected;
	}

}
