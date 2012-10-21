
package org.springframework.go.context.auto;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tests for {@link AutoAnnotationConfigApplicationContext}.
 *
 * @author Phillip Webb
 */
public class AutoAnnotationConfigApplicationContextTest {

	private MockAutoAnnotationConfigApplicationContext context = new MockAutoAnnotationConfigApplicationContext();

	@Mock
	private AutoConfigurationProvider provider;

	private List<AutoConfigurationProvider> providers;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		providers = new ArrayList<AutoConfigurationProvider>();
		providers.add(provider);
	}

	@Test
	public void shouldCallAutoConfigure() throws Exception {
		context.refresh();
		verify(provider).apply(any(AutoConfigurationApplicationContext.class),
				any(ConfigurableListableBeanFactory.class));
	}

	@Test
	public void shouldCallAutoConfigureScanningBasePackagesOnConstruction()
			throws Exception {
		// FIXME
	}

	@Test
	public void shouldCallAutoConfigureRegisteringClassesOnConstruction()
			throws Exception {
		// FIXME
	}

	@Test
	public void shouldHaveAccessToConfiguredBeansFromAutoConfigure() throws Exception {
		providers.add(new AutoConfigurationProvider() {

			@Override
			public void apply(AutoConfigurationApplicationContext context,
					ConfigurableListableBeanFactory beanFactory) {
				System.out.println("Auto Configure");
				BeanDefinition definition = beanFactory.getBeanDefinition("example");
				assertThat(definition, is(not(nullValue())));
			}
		});
		context.register(ExampleConfiguration.class);
		context.refresh();
		assertThat(context.getBean("example"), is(ExampleBean.class));
		verify(provider).apply(any(AutoConfigurationApplicationContext.class),
				any(ConfigurableListableBeanFactory.class));
	}

	@Test
	public void shouldRegisterConfigurationFromAutoConfigure() throws Exception {
		providers.add(new AutoConfigurationProvider() {

			@Override
			public void apply(AutoConfigurationApplicationContext context,
					ConfigurableListableBeanFactory beanFactory) {
				System.out.println("Auto Configure");
				context.register(ExampleAutoConfiguration.class);
			}
		});
		context.register(ExampleConfiguration.class);
		context.refresh();
		assertThat(context.getBean("example"), is(ExampleBean.class));
		assertThat(context.getBean("autoConfiguredExample"), is(ExampleBean.class));
	}

	@Test
	public void shouldConditionallyRegisterWithAutoConfigure() throws Exception {
		// FIXME
	}

	private class MockAutoAnnotationConfigApplicationContext extends
			AutoAnnotationConfigApplicationContext {

		@Override
		public Iterator<AutoConfigurationProvider> getAutoConfigurationProviders() {
			return providers.iterator();
		}
	}

	@Configuration
	static class ExampleConfiguration {

		@Bean
		public ExampleBean example() {
			return new ExampleBean();
		}

	}

	static class ExampleBean {

		public ExampleBean() {
			System.out.println("Created Bean");
		}
	}

	@Configuration
	static class ExampleAutoConfiguration {

		@Bean
		public ExampleBean autoConfiguredExample() {
			return new ExampleBean();
		}
	}
}
