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
import java.util.Locale;

import org.springframework.util.StringUtils;

/**
 * Enumeration of common database drivers.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
enum DatabaseDriver {

	/**
	 * Unknown type.
	 */
	UNKNOWN(null) {
		@Override
		Collection<String> getUrlPrefixes() {
			return Collections.emptyList();
		}
	},
	/**
	 * H2.
	 */
	H2("H2"),
	/**
	 * MySQL.
	 */
	MYSQL("MySQL"),
	/**
	 * Maria DB.
	 */
	MARIADB("MariaDB"),
	/**
	 * Oracle.
	 */
	ORACLE("Oracle"),
	/**
	 * Postgres.
	 */
	POSTGRESQL("PostgreSQL"),
	/**
	 * HANA - SAP HANA Database - HDB.
	 * @since 2.1.0
	 */
	HANA("HDB") {
		@Override
		public Collection<String> getUrlPrefixes() {
			return Collections.singleton("sap");
		}
	},
	/**
	 * SQL Server.
	 */
	SQLSERVER("Microsoft SQL Server") {

		@Override
		protected boolean matchProductName(String productName) {
			return super.matchProductName(productName) || "SQL SERVER".equalsIgnoreCase(productName);
		}

	};

	private final String productName;

	DatabaseDriver(String productName) {
		this.productName = productName;
	}

	/**
	 * Return the url prefixes of this driver.
	 * @return the url prefixes
	 */
	Collection<String> getUrlPrefixes() {
		return Collections.singleton(name().toLowerCase(Locale.ENGLISH));
	}

	boolean matchProductName(String productName) {
		return this.productName != null && this.productName.equalsIgnoreCase(productName);
	}

	/**
	 * Find a {@link DatabaseDriver} for the given product name.
	 * @param productName product name
	 * @return the database driver or {@link #UNKNOWN} if not found
	 */
	static DatabaseDriver fromProductName(String productName) {
		if (StringUtils.hasLength(productName)) {
			for (DatabaseDriver candidate : values()) {
				if (candidate.matchProductName(productName)) {
					return candidate;
				}
			}
		}
		return UNKNOWN;
	}

}
