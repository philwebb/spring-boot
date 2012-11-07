package org.springframework.bootstrap.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class NeverCondition implements Condition {

	public boolean matches(AnnotatedTypeMetadata metadata) {
		return false;
	}

}
