package org.collections.skip;

class LevelNode<T> {

  SkipListNode<T> next;
  LevelNode<T> up;

  public LevelNode<T> moveUpBy(int steps) {
    LevelNode<T> current = this;
    while (current != null && steps-- > 0) {
      current = current.up;
    }
    return current;
  }

  public int levelsToTop() {
    int count = 0;
    LevelNode<T> current = this;
    while (current.up != null) {
      current = current.up;
      count++;
    }
    return count;
  }

}
