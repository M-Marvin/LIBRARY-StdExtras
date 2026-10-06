package de.m_marvin.stdextras;

import java.util.Map;

public record Pair<A, B>(A first, B second) implements Map.Entry<A, B> {

	@Override
	public A getKey() {
		return first;
	}

	@Override
	public B getValue() {
		return second;
	}

	@Override
	public B setValue(B value) {
		throw new UnsupportedOperationException("setValue");
	}
	
	public static <K, V> Pair<K, V> forEntry(Map.Entry<K, V> entry) {
		return new Pair<>(entry.getKey(), entry.getValue());
	}
	
}
