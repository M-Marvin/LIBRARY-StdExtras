package de.m_marvin.stdextras.functional;

@FunctionalInterface
public interface UnstableFunction<T, R, E extends Throwable> {
	
	public R apply(T value) throws E;
	
}
