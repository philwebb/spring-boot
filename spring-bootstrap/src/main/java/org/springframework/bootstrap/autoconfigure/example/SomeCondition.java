package org.springframework.bootstrap.autoconfigure.example;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.Condition;
import org.springframework.core.type.AnnotatedTypeMetadata;


public class SomeCondition implements Condition, BeanFactoryAware {

	private BeanFactory beanFactory;

	public boolean matches(AnnotatedTypeMetadata metadata) {
		System.out.println("Considering from "+beanFactory);
		return true;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
