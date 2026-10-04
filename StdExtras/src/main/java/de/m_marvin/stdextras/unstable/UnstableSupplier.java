package de.m_marvin.stdextras.unstable;

@FunctionalInterface
public interface UnstableSupplier<R, E extends Throwable> {
	
	public R get() throws E;
	
}
