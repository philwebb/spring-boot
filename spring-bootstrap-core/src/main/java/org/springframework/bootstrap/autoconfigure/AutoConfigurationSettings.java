package org.springframework.bootstrap.autoconfigure;


public interface AutoConfigurationSettings {

	public static final String BEAN_NAME = "AutoConfigurationSettings";

	//FIXME packages should return iterables?

	String getDomainPackage();

	String getRepositoryPackage();

}
