/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.developertools.autoconfigure;

import java.net.URL;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.developertools.classpath.ClassPathChangedEvent;
import org.springframework.boot.developertools.classpath.ClassPathFileSystemWatcher;
import org.springframework.boot.developertools.classpath.ClassPathRestartStrategy;
import org.springframework.boot.developertools.filewatch.ChangedFile;
import org.springframework.boot.developertools.livereload.LiveReloadServer;
import org.springframework.boot.developertools.restart.ConditionalOnInitializedRestarter;
import org.springframework.boot.developertools.restart.RestartScope;
import org.springframework.boot.developertools.restart.Restarter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for local development support.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
@Configuration
@ConditionalOnInitializedRestarter
public class LocalDeveloperToolsAutoConfiguration {

	@Autowired(required = false)
	private LiveReloadServer liveReloadServer;

	@Bean
	@ConditionalOnMissingBean
	public ClassPathFileSystemWatcher classPathFileSystemWatcher() {
		URL[] urls = Restarter.getInstance().getInitialUrls();
		System.out.println(Arrays.asList(urls));
		return new ClassPathFileSystemWatcher(classPathRestartStrategy(), urls);
	}

	@Bean
	@ConditionalOnMissingBean
	public ClassPathRestartStrategy classPathRestartStrategy() {
		return new ClassPathRestartStrategy() {

			@Override
			public boolean isRestartRequired(ChangedFile file) {
				System.out.println(file.getFile().getName());
				return (file.getFile().getName().endsWith(".class"));
			}

		};
	}

	@EventListener
	public void onClassPathChanged(ClassPathChangedEvent event) {
		if (event.isRestartRequired()) {
			Restarter.getInstance().restart();
		}
	}

	@Bean
	public static LocalDeveloperPropertyDefaultsPostProcessor localDeveloperPropertyDefaultsPostProcessor() {
		return new LocalDeveloperPropertyDefaultsPostProcessor();
	}

	@Bean
	@RestartScope
	@ConditionalOnMissingBean
	public LiveReloadServer liveReloadServer() {
		// FIXME enable + port?
		return new LiveReloadServer(Restarter.getInstance().getThreadFactory());
	}

	@Bean
	public LiveReloadServerManager liveReloadServerManager() {
		return new LiveReloadServerManager(this.liveReloadServer);
	}

	// FIXME it would be nice if we could capture a real exit to gracefully close the LRS

}
