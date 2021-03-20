package org.collections;

import java.util.Collection;
import java.util.Comparator;

public interface Multiset<T extends Comparable<T>> extends Collection<T>{

  Comparator<T> comparator();

  T first();

  T last();
}
