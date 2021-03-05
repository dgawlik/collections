package org.collections.skip;

class LevelNode<T> {

  SkipListNode<T> next;
  LevelNode<T> up;

  public LevelNode<T> moveUp(int steps) {
    LevelNode<T> it = this;
    while (it != null && steps-- > 0) {
      it = it.up;
    }
    return it;
  }

  public int levelCount() {
    int cnt = 0;
    LevelNode<T> it = this;
    while (it.up != null) {
      it = it.up;
      cnt++;
    }
    return cnt;
  }

}
