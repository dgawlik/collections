package org.collections.skip;

class SkipListNode<T> {
  T value;
  LevelNode<T> levels;

  public SkipListNode(T value) {
    this.value = value;
  }
}
