package de.m_marvin.stdextras.functional;

@FunctionalInterface
public interface UnstableConsumer<T, E extends Throwable> {
	
	public void accept(T value) throws E;
	
}
