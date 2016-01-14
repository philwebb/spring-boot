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

package org.springframework.boot.loader.jar;

/**
 * @author pwebb
 */
class SortSketch {

	private final int[] hashCodes;

	private final int[] offsets;

	SortSketch(int size) {
		this.hashCodes = new int[size];
		this.offsets = new int[size];
	}

	public void add(int index, int hashCode, int offset) {

	}

	public void sort() {
		sort(0, this.hashCodes.length - 1);
	}

	private void sort(int left, int right) {
		if (left < right) {
			int middle = left + (right - left) / 2;
			int pivot = this.hashCodes[middle];
			int i = left;
			int j = right;
			while (i <= j) {
				while (this.hashCodes[i] < pivot) {
					i++;
				}
				while (this.hashCodes[j] > pivot) {
					j--;
				}
				if (i <= j) {
					swap(i, j);
					i++;
					j--;
				}
			}
			if (left < j) {
				sort(left, j);
			}
			if (right > i) {
				sort(i, right);
			}
		}
	}

	private void swap(int i, int j) {
		swap(this.hashCodes, i, j);
		swap(this.offsets, i, j);
	}

	private void swap(int[] array, int i, int j) {
		int temp = array[i];
		array[i] = array[j];
		array[j] = temp;
	}
}
