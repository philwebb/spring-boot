package org.springframework.boot

import org.springframework.boot.context.properties.PropertyMapper
import org.springframework.boot.context.properties.PropertyMapper2

fun main() {
		val nullableString: String? = null
		PropertyMapper2.get().from { nullableString }.`as` { it.substring(0) }.withNulls().to {
			if (it == null) {
				throw RuntimeException("Boom")
			}
			println(it)
		}
	}
