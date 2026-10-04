package de.m_marvin.stdextras.unstable;

@FunctionalInterface
public interface UnstableBiFunction<T1, T2, R, E extends Throwable> {
	
	public R apply(T1 value1, T2 value2) throws E;
	
}
