package org.collections;

import java.util.Collection;
import java.util.Comparator;

public interface Multiset<T extends Comparable<? super T>> extends Collection<T>{

  Comparator<? super T> comparator();

  T first();

  T last();
}
