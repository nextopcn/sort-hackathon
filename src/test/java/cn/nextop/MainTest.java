package cn.nextop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

/**
 * @author Baoyi Chen
 */
public class MainTest {
	
	@Test
	public void test() {
		Comparator<Integer> asc = Integer::compareTo;
		for (int i = 0; i < 10000; i++) {
			List<Integer[]> data1 = random();
			List<Integer[]> data2 = copy(data1);
			
			Main.sort(data1, asc);
			MainTest.sort(data2, asc);
			
			equals(data1, data2);
		}
	}
	
	private List<Integer[]> random() {
		Random random = ThreadLocalRandom.current();
		int n1 = random.nextInt(32);
		int n2 = random.nextInt(16);
		
		List<Integer[]> r = new ArrayList<>(n1);
		for (int i = 0; i < n1; i++) {
			Integer[] v = new Integer[n2];
			for (int j = 0; j < n2; j++) {
				v[j] = random.nextInt();
			}
			r.add(v);
		}
		return r;
	}
	
	private void equals(List<Integer[]> v1, List<Integer[]> v2) {
		assertEquals(v1.size(), v2.size());
		int n = v1.size();
		for (int i = 0; i < n; i++) {
			Integer[] arr1 = v1.get(i);
			Integer[] arr2 = v2.get(i);
			assertEquals(arr1.length, arr2.length);
			assertArrayEquals(arr1, arr2);
		}
	}
	
	private List<Integer[]> copy(List<Integer[]> data) {
		List<Integer[]> r = new ArrayList<>(data.size());
		for (Integer[] arr : data) {
			r.add(Arrays.copyOf(arr, arr.length));
		}
		return r;
	}
	
	public static <T> void sort(List<T[]> v, Comparator<? super T> comparator) {
		int n = v.size();
		for (int a = 0; a < n; a++) {
			T[] arr1 = v.get(a);
			for (int b = 0; b < arr1.length; b++) {
				for (int c = a; c < n; c++) {
					T[] arr2 = v.get(c);
					int d;
					if (arr1 != arr2) {
						d = 0;
					} else {
						d = b + 1;
					}
					for (; d < arr2.length; d++) {
						if (comparator.compare(arr1[b], arr2[d]) > 0) {
							T temp = arr2[d];
							arr2[d] = arr1[b];
							arr1[b] = temp;
						}
					}
				}
			}
		}
	}
	
}