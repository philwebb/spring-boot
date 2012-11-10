package org.springframework.bootstrapsample;

import java.net.URL;
import java.net.URLClassLoader;

import org.springframework.bootstrap.SpringApplication;

public class SampleApplication extends SpringApplication{

	public static void main(String[] args) throws Exception {
		ClassLoader cl = ClassLoader.getSystemClassLoader();
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url: urls){
        	System.out.println(url.getFile());
        }
		new SampleApplication().run(args);
	}

}
