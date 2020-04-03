import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "{version}"
}

// tag::layered[]
tasks.getByName<BootJar>("bootJar") {
	layered {
		application {
			intoLayer("spring-boot-loader") {
				include("org/springframework/boot/loader/**")
			}
			intoLayer("application")
		}
		dependencies {
			intoLayer("snapshot-dependencies") {
				include("*:*:*SNAPSHOT")
			}
			intoLayer("dependencies") {
		}
		layersOrder("dependencies", "spring-boot-loader", "snapshot-dependencies", "application")
	}
}
// end::layered[]
