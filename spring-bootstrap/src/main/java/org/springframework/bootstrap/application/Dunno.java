
package org.springframework.bootstrap.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class Dunno implements Runnable, ApplicationContextAware {

	private ApplicationContext applicationContext;

	public void run() {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			String line;
			do {
				line = reader.readLine();
				System.out.println("You typed " + line);
				if ("refresh".equals(line)) {
					((ConfigurableApplicationContext) applicationContext).refresh();
				}
			} while (!"exit".equals(line));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
