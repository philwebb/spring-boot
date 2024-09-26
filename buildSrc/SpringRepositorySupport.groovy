/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// This script can be used in the `pluginManagement` block of a `settings.gradle` file to provide
// support for spring maven repositories.
//
// To use the script add the following as the first line in the `pluginManagement` block:
//
//     evaluate(new File("${rootDir}/buildSrc/SpringRepositorySupport.groovy")).apply(this)
//
// You can then use `spring.mavenRepositories()` to add the Spring repositories required for the
// version being built.
//


def apply(settings) {
	version = settings.ext['version']
	buildType = settings.ext['spring.build-type']
	SpringRepositoriesExtension.addTo(settings.pluginManagement.repositories, version, buildType)
	settings.gradle.allprojects {
		SpringRepositoriesExtension.addTo(repositories, version, buildType)
	}
}

return this

class SpringRepositoriesExtension {

	@javax.inject.Inject
	SpringRepositoriesExtension(repositories, version, buildType) {
		this(repositories, version, buildType, System::getenv)
	}

	SpringRepositoriesExtension(repositories, version, buildType, environment) {
	}

	def mavenRepositories() {
		println "mavenRepositories"
	}

	def mavenRepositories(condition) {
		println "mavenRepositories " + condition
	}

	def mavenRepositoriesExcludingBootGroup() {
		println "mavenRepositoriesExcludingBootGroup"
	}

	static def addTo(repositories, version, buildType) {
		repositories.extensions.create("spring", SpringRepositoriesExtension.class, repositories, version, buildType)
	}

}