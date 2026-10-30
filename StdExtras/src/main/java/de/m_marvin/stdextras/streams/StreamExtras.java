package de.m_marvin.stdextras.streams;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import de.m_marvin.stdextras.Pair;

public final class StreamExtras {
	
	private StreamExtras() {}
	
	public static <A,B> Stream<Pair<A,B>> costream(Stream<A> s1, Stream<B> s2) {
		Iterator<B> iter = s2.iterator();
		return StreamSupport.stream(s1.spliterator(), false).map(a -> new Pair<A, B>(a, iter.next()));
	}
	
	public static <E> Stream<E> forFirst(Stream<E> stream, Consumer<E> action) {
		Spliterator<E> iter = stream.spliterator();
		iter.tryAdvance(action);
		return StreamSupport.stream(iter, false);
	}
	
}
