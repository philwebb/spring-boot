
package org.springframework.bootstrap.web.context;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;
import org.springframework.web.context.support.StandardServletEnvironment;
import org.springframework.web.context.support.WebApplicationContextUtils;

public abstract class AbstractEmbeddedWebApplicationContext extends
		AbstractRefreshableConfigApplicationContext implements WebApplicationContext,
		ThemeSource {

	private ThemeSource themeSource;

	private ServletContext servletContext;

	public AbstractEmbeddedWebApplicationContext() {
		setDisplayName("Root WebApplicationContext");
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
		beanFactory.addBeanPostProcessor(new ServletContextAwareBeanPostProcessor());
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
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
	protected void onRefresh() throws BeansException {
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
		dunno();
	}

	private void dunno() {
		try {
			EmbeddedServletProvider provider = getBeanFactory().getBean(
					EmbeddedServletProvider.class);
			provider.startEmbeddedServlet(this, new EmbeddedContextLoaderListener(this));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected void contextInitialized(ServletContext servletContext) {
		this.servletContext = servletContext;
		WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory(),
				this.servletContext);
		WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(),
				this.servletContext, null);
		initPropertySources();
	}

	@Override
	protected void initPropertySources() {
		super.initPropertySources();
		this.getEnvironment().initPropertySources(this.servletContext, null);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

	private class EmbeddedContextLoaderListener extends ContextLoaderListener {

		public EmbeddedContextLoaderListener(WebApplicationContext context) {
			super(context);
		}

		@Override
		public void contextInitialized(ServletContextEvent event) {
			AbstractEmbeddedWebApplicationContext.this.contextInitialized(event.getServletContext());
			super.contextInitialized(event);
		}

	}

	private class ServletContextAwareBeanPostProcessor implements BeanPostProcessor {

		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ServletContextAware) {
				((ServletContextAware) bean).setServletContext(servletContext);
			}
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}
	}
}
