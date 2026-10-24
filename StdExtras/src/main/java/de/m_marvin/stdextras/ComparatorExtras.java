package de.m_marvin.stdextras;

import java.util.Comparator;
import java.util.function.ToIntFunction;

public final class ComparatorExtras {
	
	private ComparatorExtras() {}
	
	public static <T> Comparator<T> inverse(Comparator<T> comp) {
		return (a, b) -> comp.compare(b, a);
	}
	
	public static <T> Comparator<T> toInt(ToIntFunction<T> func) {
		return (a, b) -> {
			if (a == b)
				return 0;
			else if (a == null)
				return -1;
			else if (b == null)
				return +1;
			else
				return Integer.compare(func.applyAsInt(a), func.applyAsInt(b));
		};
	}
	
}
