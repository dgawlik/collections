package org.collections.skip;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;
import java.util.SortedSet;


/**
 * Implementation of sorted set with possible duplicates via skip list.
 *
 * Insertion: O(logN)
 * Deletion: O(logN)
 * Search: O(logN)
 *
 * @param <T>
 */
public class SkipList<T extends Comparable<? super T>> implements SortedSet<T> {

  private static class Iterator<T> implements java.util.Iterator<T> {

    private SkipListNode<T> it;

    public Iterator(SkipListNode<T> it) {
      this.it = it;
    }

    @Override
    public boolean hasNext() {
      return this.it != null;
    }

    @Override
    public T next() {
      T val = it.value;
      it = it.levels.next;
      return val;
    }
  }

  private final Comparator<T> comp;
  private SkipListNode<T> START;
  private SkipListNode<T> END;
  private final Random rnd;
  private int size;
  private int maxLevel;

  public SkipList(Comparator<T> comp) {
    this.comp = comp;
    this.rnd = new Random(1);
    this.size = 0;
    this.maxLevel = 0;
  }

  public SkipList() {
    this(Comparator.naturalOrder());
  }

  @Override
  public int size() {
    return this.size;
  }

  @Override
  public boolean isEmpty() {
    return this.size == 0;
  }

  @Override
  public boolean contains(Object obj) {
    T value = (T) obj;
    if (this.START == null) {
      return false;
    } else {
      LinkedList<SkipListNode<T>> stack = this.search(value, false);
      return this.comp.compare(value, stack.peekLast().value) == 0;
    }
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>(this.START);
  }

  @Override
  public Object[] toArray() {
    Object[] arr = new Object[this.size];
    if (this.size == 0) {
      return arr;
    }
    fillArray(arr);
    return arr;
  }


  @Override
  public <U> U[] toArray(U[] a) {
    U[] arr = (U[]) Array
        .newInstance(a.getClass().getComponentType(), this.size);
    if (this.size == 0) {
      return arr;
    }
    fillArray(arr);
    return arr;
  }

  @Override
  public boolean add(T value) {
    if (this.START == null) {
      this.START = new SkipListNode<>(value);
      this.END = this.START;
      this.START.levels = new LevelNode<>();
    } else {
      LinkedList<SkipListNode<T>> stack = this.search(value, false);
      int levelIt = 0;
      SkipListNode<T> it = stack.pollLast();
      SkipListNode<T> newN = new SkipListNode<>(value);
      newN.levels = new LevelNode<>();

      //append left
      //it only happens if value < this.START
      if (this.comp.compare(value, it.value) < 0) {
        newN.levels.next = this.START;
        while (++levelIt <= this.maxLevel) {
          LevelNode<T> newLevel = this.growLevel(newN, levelIt);
          newLevel.next = this.START;
        }
        this.START = newN;
      }
      //append right
      //all other cases
      else {
        if (it.levels.next == null) {
          this.END = newN;
        }
        newN.levels.next = it.levels.next;
        it.levels.next = newN;

        levelIt = 1;
        double prob = this.rnd.nextDouble();
        while (prob < 0.5) {
          if (stack.isEmpty()) {
            this.growLevel(newN, levelIt);
            LevelNode<T> startLevel = this.growLevel(this.START, levelIt);
            startLevel.next = newN;
            this.maxLevel++;
            break;
          } else {
            it = stack.pollLast();

            LevelNode<T> newLevel = this.growLevel(newN, levelIt);
            LevelNode<T> newTop = it.levels.moveUp(levelIt);
            newLevel.next = newTop.next;
            newTop.next = newN;
            levelIt++;
          }
          prob = this.rnd.nextDouble();
        }
      }
    }
    this.size++;
    return true;
  }

  @Override
  public boolean remove(Object obj) {
    T value = (T) obj;
    if (this.START == null || !this.contains(value)) {
      return false;
    } else if (this.START.levels.next == null) {
      this.START = null;
      this.END = null;
      this.size--;
      return true;
    } else {
      LinkedList<SkipListNode<T>> stack = this.search(value, true);

      //remove this.START
      if (stack.peekLast() == null) {
        SkipListNode<T> nextToStart = this.START.levels.next;
        int lvl = 0;
        while (lvl <= this.maxLevel) {
          if (nextToStart.levels.moveUp(lvl) == null) {
            LevelNode<T> newLevel = this.growLevel(nextToStart, lvl);
            newLevel.next = this.START.levels.moveUp(lvl).next;
          }
          lvl++;
        }
        this.START = nextToStart;
      } else {
        int levelIt = 0;
        SkipListNode<T> toBeRemoved = stack.peekLast().levels.next;
        do {
          SkipListNode<T> top = stack.pollLast();
          LevelNode<T> removedLevel = toBeRemoved.levels.moveUp(levelIt);
          if (top == null || removedLevel == null) {
            break;
          }
          top.levels.moveUp(levelIt).next = removedLevel.next;
          levelIt++;
        } while (!stack.isEmpty() || levelIt <= this.maxLevel);
      }
      this.size--;
      return true;
    }
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!this.contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    if (c.size() == 0) {
      return false;
    }
    for (T o : c) {
      this.add(o);
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    SkipList<T> newSkip = new SkipList<>(this.comp);
    for (Object o : c) {
      if (this.contains(o)) {
        newSkip.add((T) o);
      }
    }
    boolean changed = this.size == newSkip.size;
    this.START = newSkip.START;
    this.END = newSkip.END;
    this.maxLevel = newSkip.maxLevel;
    this.size = newSkip.size;
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    boolean changed = false;
    for (Object o : c) {
      var result = this.remove(o);
      if (!changed) {
        changed = result;
      }
    }
    return changed;
  }

  @Override
  public void clear() {
    this.START = this.END = null;
    this.size = 0;
  }

  @Override
  public Comparator<? super T> comparator() {
    return this.comp;
  }

  @Override
  public SortedSet<T> subSet(T fromElement, T toElement) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public SortedSet<T> headSet(T toElement) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public SortedSet<T> tailSet(T fromElement) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public T first() {
    return this.START.value;
  }

  @Override
  public T last() {
    return this.END.value;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SkipList = ");
    SkipListNode<T> it = this.START;
    while (it != null) {
      sb.append("[").append(it.value).append(",").append(it.levels.levelCount())
          .append("],");
      it = it.levels.next;
    }
    return sb.toString();
  }

  private LinkedList<SkipListNode<T>> search(T value, boolean forRemove) {
    if (this.START == null) {
      return new LinkedList<>();
    }

    int levelIt = this.maxLevel;
    SkipListNode<T> nodeIt = this.START;
    SkipListNode<T> nodePrev = null;
    LinkedList<SkipListNode<T>> stack = new LinkedList<>();

    while (levelIt >= 0) {
      LevelNode<T> currLevel = nodeIt.levels.moveUp(levelIt);
      while (currLevel.next != null
          && this.comp.compare(value, currLevel.next.value) >= 0) {
        nodePrev = nodeIt;
        nodeIt = currLevel.next;
        currLevel = nodeIt.levels.moveUp(levelIt);
      }
      if (nodePrev != null) {
        while (nodePrev.levels.moveUp(levelIt).next != nodeIt) {
          nodePrev = nodePrev.levels.moveUp(levelIt).next;
        }
      }
      levelIt--;
      if (forRemove && this.comp.compare(value, nodeIt.value) == 0) {
        stack.offerLast(nodePrev);
      } else {
        stack.offerLast(nodeIt);
      }
    }
    return stack;
  }

  private <U> void fillArray(U[] arr) {
    SkipListNode<T> it = this.START;
    int ind = 0;
    while (it != null) {
      arr[ind++] = (U) it.value;
      it = it.levels.next;
    }
  }

  private LevelNode<T> growLevel(SkipListNode<T> node, int level) {
    LevelNode<T> newLevel = new LevelNode<>();
    LevelNode<T> oldTop = node.levels.moveUp(level - 1);
    oldTop.up = newLevel;
    return newLevel;
  }

}
