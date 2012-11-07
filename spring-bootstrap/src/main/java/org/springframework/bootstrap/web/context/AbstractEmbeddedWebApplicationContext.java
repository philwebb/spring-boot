
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.WebApplicationContextUtils;

public abstract class AbstractEmbeddedWebApplicationContext extends
		AbstractRefreshableConfigApplicationContext implements WebApplicationContext,
		ThemeSource {

	private static boolean usenew = false;

	private ThemeSource themeSource;

	private ServletContext servletContext;

	private ContextLoader contextLoader = new ContextLoader(this);

	public AbstractEmbeddedWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
		if (!usenew) {
			addBeanFactoryPostProcessor(new EmbeddedServletBeanPostProcessor());
		}
	}

	protected void setServletContext(ConfigurableListableBeanFactory beanFactory,
			ServletContext servletContext) {
		this.servletContext = servletContext;
//		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(
//				this.servletContext));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
		WebApplicationContextUtils.registerWebApplicationScopes(beanFactory,
				this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(beanFactory,
				this.servletContext, null);
		this.contextLoader.initWebApplicationContext(servletContext);
		initPropertySources();
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	@Override
	public String getApplicationName() {
		return (this.servletContext == null ? "" : this.servletContext.getContextPath());
	}

	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardServletEnvironment();
	}

	@Override
	public ConfigurableWebEnvironment getEnvironment() {
		ConfigurableEnvironment env = super.getEnvironment();
		Assert.isInstanceOf(ConfigurableWebEnvironment.class, env,
				"ConfigurableWebApplicationContext environment must be of type "
						+ "ConfigurableWebEnvironment");
		return (ConfigurableWebEnvironment) env;
	}

	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
	}

	@Override
	protected Resource getResourceByPath(String path) {
		return super.getResourceByPath(path);
	}

	@Override
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new ServletContextResourcePatternResolver(this);
	}

	@Override
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		super.registerBeanPostProcessors(beanFactory);
		if (usenew) {
//			try {
//				EmbeddedServletProvider bean = beanFactory.getBean(EmbeddedServletProvider.class);
//				ServletContext servletContext = bean.startEmbeddedServlet(this);
//				setServletContext(beanFactory, servletContext);
//			} catch (Exception ex) {
//				throw new IllegalStateException(ex);
//			}
		}
	}

	@Override
	protected void onRefresh() throws BeansException {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	@Override
	protected void initPropertySources() {
		super.initPropertySources();
		this.getEnvironment().initPropertySources(this.servletContext, null);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

	private class EmbeddedServletBeanPostProcessor implements
			BeanDefinitionRegistryPostProcessor, Ordered {

		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			try {
				ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;
				EmbeddedServletProvider bean = beanFactory.getBean(EmbeddedServletProvider.class);
				ServletContext servletContext = bean.startEmbeddedServlet(AbstractEmbeddedWebApplicationContext.this);
				AbstractEmbeddedWebApplicationContext.this.setServletContext(beanFactory,
						servletContext);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
		}

		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE + 10;
		}
	}

	private class EmeddedServletBeanPostProcessor implements BeanPostProcessor {

		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			// TODO Auto-generated method stub
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

	}

}
