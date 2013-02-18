
package org.springframework.bootstrap.autoconfigure.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.bootstrap.autoconfigure.AutoConfiguration;
import org.springframework.bootstrap.autoconfigure.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.ClassUtils;

@AutoConfiguration
@Conditional(EmbeddedDatabaseAutoConfiguration.EmbeddedDatabaseCondition.class)
@ConditionalOnMissingBean(DataSource.class)
public class EmbeddedDatabaseAutoConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_TYPE_CLASSES;
	static {
		EMBEDDED_DATABASE_TYPE_CLASSES = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_TYPE_CLASSES.put(EmbeddedDatabaseType.HSQL, "org.hsqldb.Database");
	}

	@Bean
	//@Attribute(name="embedded", value=Boolean.TRUE)
	public DataSource dataSource() {
		return getEmbeddedDatabaseBuilder().build();
	}

	static EmbeddedDatabaseBuilder getEmbeddedDatabaseBuilder() {
		for (Map.Entry<EmbeddedDatabaseType, String> entry : EMBEDDED_DATABASE_TYPE_CLASSES.entrySet()) {
			if(ClassUtils.isPresent(entry.getValue(), EmbeddedDatabaseAutoConfiguration.class.getClassLoader())) {
				return new EmbeddedDatabaseBuilder().setType(entry.getKey());
			}
		}
		return null;
	}

	static class EmbeddedDatabaseCondition implements Condition {

		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			return getEmbeddedDatabaseBuilder() != null;
		}
	}

}
