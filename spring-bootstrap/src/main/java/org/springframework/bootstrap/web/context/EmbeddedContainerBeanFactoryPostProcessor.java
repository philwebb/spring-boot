
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;

public class EmbeddedContainerBeanFactoryPostProcessor implements
		BeanFactoryPostProcessor, Ordered {

	private static final String POST_PROCESSOR_NAME = "internalThingy";

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		RootBeanDefinition definition = new RootBeanDefinition(PostProcessor.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(
				POST_PROCESSOR_NAME, definition);
		//FIXME we need to remove the default ServletContextAware injector to stop double
	}

	public int getOrder() {
		return HIGHEST_PRECEDENCE + 1;
	}

	public static class PostProcessor implements BeanPostProcessor,
			ApplicationContextAware, Ordered {

		private ServletContext servletContext;

		public void setApplicationContext(ApplicationContext applicationContext)
				throws BeansException {
			ConfigurableWebApplicationContext webApplicationContext = (ConfigurableWebApplicationContext) applicationContext;
			if (webApplicationContext.getServletContext() == null) {
				try {
					EmbeddedServletProvider provider = webApplicationContext.getBeanFactory().getBean(
							EmbeddedServletProvider.class);
					provider.startEmbeddedServlet(webApplicationContext,
							new EmbeddedContextLoaderListener(webApplicationContext));
				} catch (Exception ex) {
					throw new IllegalStateException(ex);
				}
			}

		}

		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (servletContext != null && bean instanceof ServletContextAware) {
				((ServletContextAware) bean).setServletContext(servletContext);
			}
			return bean;
		}


		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		public int getOrder() {
			return LOWEST_PRECEDENCE;
		}

		private class EmbeddedContextLoaderListener extends ContextLoaderListener {

			public EmbeddedContextLoaderListener(WebApplicationContext context) {
				super(context);
			}

			@Override
			public void contextInitialized(ServletContextEvent event) {
				servletContext = event.getServletContext();
				super.contextInitialized(event);
			}

			@Override
			protected void configureAndRefreshWebApplicationContext(
					ConfigurableWebApplicationContext wac, ServletContext sc) {
				wac.setServletContext(sc);
			}
		}

	}


}
