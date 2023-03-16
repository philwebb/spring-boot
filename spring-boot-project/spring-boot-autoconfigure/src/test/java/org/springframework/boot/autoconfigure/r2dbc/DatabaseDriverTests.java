/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.r2dbc;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatabaseDriver}.
 *
 * @author Moritz Halbritter
 */
class DatabaseDriverTests {

	@ParameterizedTest
	@MethodSource("argumentProvider")
	void test(String productName, DatabaseDriver expectedDriver, Collection<String> expectedUrlPrefixes) {
		DatabaseDriver databaseDriver = DatabaseDriver.fromProductName(productName);
		assertThat(databaseDriver).isEqualTo(expectedDriver);
		assertThat(databaseDriver.getUrlPrefixes()).containsExactlyInAnyOrderElementsOf(expectedUrlPrefixes);
	}

	static Stream<Arguments> argumentProvider() {
		return Stream.of(Arguments.arguments("H2", DatabaseDriver.H2, List.of("h2")),
				Arguments.arguments("MySQL", DatabaseDriver.MYSQL, List.of("mysql")),
				Arguments.arguments("MariaDB", DatabaseDriver.MARIADB, List.of("mariadb")),
				Arguments.arguments("Oracle", DatabaseDriver.ORACLE, List.of("oracle")),
				Arguments.arguments("PostgreSQL", DatabaseDriver.POSTGRESQL, List.of("postgresql")),
				Arguments.arguments("HDB", DatabaseDriver.HANA, List.of("sap")),
				Arguments.arguments("Microsoft SQL Server", DatabaseDriver.SQLSERVER, List.of("sqlserver")),
				Arguments.arguments("SQL Server", DatabaseDriver.SQLSERVER, List.of("sqlserver")),
				Arguments.arguments("Something unknown", DatabaseDriver.UNKNOWN, Collections.emptyList()),
				Arguments.arguments(null, DatabaseDriver.UNKNOWN, Collections.emptyList()));
	}

}
