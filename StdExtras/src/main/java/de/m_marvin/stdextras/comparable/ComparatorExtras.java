package de.m_marvin.stdextras.comparable;

import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public final class ComparatorExtras {
	
	private ComparatorExtras() {}
	
	public static <T> Comparator<T> inverse(Comparator<T> comp) {
		return (a, b) -> comp.compare(b, a);
	}

	public static <T> Comparator<T> toDouble(ToDoubleFunction<T> func) {
		return (a, b) -> {
			if (a == b)
				return 0;
			else if (a == null)
				return -1;
			else if (b == null)
				return +1;
			else
				return Double.compare(func.applyAsDouble(a), func.applyAsDouble(b));
		};
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

	public static <T> Comparator<T> toLong(ToLongFunction<T> func) {
		return (a, b) -> {
			if (a == b)
				return 0;
			else if (a == null)
				return -1;
			else if (b == null)
				return +1;
			else
				return Long.compare(func.applyAsLong(a), func.applyAsLong(b));
		};
	}
	
}
