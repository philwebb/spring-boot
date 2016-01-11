/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

/**
 * Decorator to apply an {@link Archive.EntryFilter} to an existing {@link Archive}.
 *
 * @author Dave Syer
 */
public class FilteredArchive implements Archive {

	private final Archive parent;

	private final EntryFilter filter;

	public FilteredArchive(Archive parent, EntryFilter filter) {
		this.parent = parent;
		this.filter = filter;
	}

	@Override
	public URL getUrl() throws MalformedURLException {
		return this.parent.getUrl();
	}

	@Override
	public Manifest getManifest() throws IOException {
		return this.parent.getManifest();
	}

	@Override
	public Iterator<Entry> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Archive> getNestedArchives(final EntryFilter filter) throws IOException {
		return this.parent.getNestedArchives(new EntryFilter() {
			@Override
			public boolean matches(Entry entry) {
				return FilteredArchive.this.filter.matches(entry)
						&& filter.matches(entry);
			}
		});
	}

	@Override
	public String toString() {
		return super.toString() + " (filtered)";
	}

}
