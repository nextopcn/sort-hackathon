package cn.nextop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Baoyi Chen
 */
public class Main {
	public static <T> void sort(List<T[]> v, Comparator<? super T> c) {
		throw new UnsupportedOperationException("implement this");
	}
	
	public static <T> void baseline(List<T[]> v, Comparator<? super T> c) {
		List<T> r = new ArrayList<>(512);
		for(final T[] w : v) for(final T t : w) { r.add(t); } r.sort(c);
	}
}