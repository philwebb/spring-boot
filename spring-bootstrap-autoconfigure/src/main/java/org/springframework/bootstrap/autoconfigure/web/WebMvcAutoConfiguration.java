
package org.springframework.bootstrap.autoconfigure.web;

import javax.servlet.Servlet;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnClass;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@AutoConfiguration
@ConditionalOnClass(Servlet.class)
@EnableWebMvc
@ConditionalOnMissingBean({ HandlerAdapter.class, HandlerMapping.class })
public class WebMvcAutoConfiguration {

}
