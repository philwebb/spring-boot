/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.loader.time;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import org.springframework.util.StopWatch;

/**
 * @author Phillip Webb
 */
public class TimeTests {

	@Test
	public void file() throws Exception {
		InputStream stream = getClass().getResourceAsStream("/typical.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		Set<String> strings = new HashSet<String>();
		String line = null;
		while ((line = br.readLine()) != null) {
			strings.add(line);
		}
		br.close();

		System.out.println(strings.size());
		// System.out.println(strings);

		Set<Integer> hashes = new HashSet<>();
		for (String string : strings) {
			hashes.add(string.hashCode());
		}
		// System.out.println(hashes);
		System.out.println(hashes.size());

		int[] randoms = new int[100000];
		for (int i = 0; i < randoms.length; i++) {
			Random random = new Random();
			randoms[i] = random.nextInt();
		}

		System.out.println();
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i : randoms) {
			hashes.contains(i);
		}
		stopWatch.stop();
		System.out.println(stopWatch.prettyPrint());

		int[] items = new int[hashes.size()];
		int i = 0;
		for (Integer integer : hashes) {
			items[i++] = integer;
		}
		Arrays.sort(items);

		stopWatch = new StopWatch();
		stopWatch.start();
		for (int j : randoms) {
			Arrays.binarySearch(items, j);
		}
		stopWatch.stop();
		System.out.println(stopWatch.prettyPrint());

	}

}
