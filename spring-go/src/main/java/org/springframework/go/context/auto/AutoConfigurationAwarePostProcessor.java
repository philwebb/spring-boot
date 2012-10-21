
package org.springframework.go.context.auto;

import java.util.Iterator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.util.Assert;

/**
 * Internal {@link BeanFactoryPostProcessor} use to apply auto configuration. This class
 * should not be used directly, rather {@link AutoAnnotationConfigApplicationContext} or
 * {@link AutoAnnotationConfigWebApplicationContext} should be used.
 *
 * @author Phillip Webb
 */
public class AutoConfigurationAwarePostProcessor implements BeanFactoryPostProcessor,
		ApplicationContextAware {

	/**
	 * The bean name of the internally managed Configuration annotation processor.
	 */
	public static final String BEAN_NAME = AutoConfigurationAwarePostProcessor.class.getName();

	private AutoConfigurationApplicationContext autoConfigurationApplicationContext;

	public AutoConfigurationAwarePostProcessor() {
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		Assert.notNull(this.autoConfigurationApplicationContext);
		Iterator<AutoConfigurationProvider> providers = this.autoConfigurationApplicationContext.getAutoConfigurationProviders();
		while (providers.hasNext()) {
			providers.next().apply(this.autoConfigurationApplicationContext, beanFactory);
		}

		ConfigurationClassPostProcessor configurationClassPostProcessor = new ConfigurationClassPostProcessor();
		configurationClassPostProcessor.postProcessBeanDefinitionRegistry((BeanDefinitionRegistry) beanFactory);
		configurationClassPostProcessor.postProcessBeanFactory(beanFactory);
	}

	/**
	 * Register the post processor with the specified {@link BeanDefinitionRegistry}.
	 *
	 * @param registry the registry that will contain the post processor.
	 */
	public static void register(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(BEAN_NAME)) {
			RootBeanDefinition beanDefinition = new RootBeanDefinition(
					AutoConfigurationAwarePostProcessor.class);
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN_NAME, beanDefinition);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		Assert.isInstanceOf(AutoConfigurationApplicationContext.class, applicationContext);
		this.autoConfigurationApplicationContext = (AutoConfigurationApplicationContext) applicationContext;
	}

}
