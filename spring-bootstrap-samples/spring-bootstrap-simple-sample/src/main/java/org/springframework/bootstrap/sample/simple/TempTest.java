
package org.springframework.bootstrap.sample.simple;

import org.springframework.bootstrap.autoconfigure.data.JpaRepositoriesAutoConfiguration;

public class TempTest {

	public static void main(String[] args) {
		System.out.println(JpaRepositoriesAutoConfiguration.class.getAnnotations());
	}

}
