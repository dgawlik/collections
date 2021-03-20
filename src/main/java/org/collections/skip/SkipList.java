package org.collections.skip;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Random;
import org.collections.Multiset;


/**
 * Skip list as sorted multiset.
 * Each node has randomized number of levels indexed from 0.
 * Each level-node points to first plain-node with same level height.
 * <p>
 * Insertion: O(logN)
 * Deletion: O(logN)
 * Search: O(logN)
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public class SkipList<T extends Comparable<T>> implements Multiset<T> {

  private static class Iterator<T> implements java.util.Iterator<T> {

    private SkipListNode<T> current;

    public Iterator(SkipListNode<T> current) {
      this.current = current;
    }

    @Override
    public boolean hasNext() {
      return this.current != null;
    }

    @Override
    public T next() {
      if (this.current == null) {
        throw new NoSuchElementException("Iterator reached the end.");
      }

      T val = current.value;
      current = current.levels.next;
      return val;
    }
  }

  private final Comparator<T> comparator;

  //leftmost node in skip list
  //holds maximum number of levels
  private SkipListNode<T> start;

  //rightmost node in skip list
  private SkipListNode<T> end;

  private final Random rng;

  //skip list node count
  private int size;

  //maximum number of levels
  private int maxLevel;

  public SkipList(Comparator<T> comparator) {
    this.comparator = comparator;
    this.rng = new Random(1);
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
    LinkedList<SkipListNode<T>> stack = this.search(value, false);
    return !stack.isEmpty()
        && this.comparator.compare(value, stack.peekLast().value) == 0;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>(this.start);
  }

  @Override
  public Object[] toArray() {
    Object[] arr = new Object[this.size];
    if (!this.isEmpty()) {
      fillArray(arr);
    }
    return arr;
  }


  @Override
  public <U> U[] toArray(U[] a) {
    U[] arr = (U[]) Array
        .newInstance(a.getClass().getComponentType(), this.size);
    if (!this.isEmpty()) {
      fillArray(arr);
    }
    return arr;
  }

  @Override
  public boolean add(T value) {
    LinkedList<SkipListNode<T>> visitedStack = this.search(value, false);
    SkipListNode<T> insertedNode = new SkipListNode<>(value);

    if (visitedStack.isEmpty()) {
      //add only node with 0-level
      this.start = this.end = insertedNode;
    } else {

      //stack of visited level-nodes
      //shallowest level on the top
      SkipListNode<T> top = visitedStack.pollLast();

      //it only happens if value < this.START
      //switch START.value with insertedNode.value
      //and add insertedNode as usual
      if (this.comparator.compare(value, top.value) < 0) {
        T temp = this.start.value;
        this.start.value = insertedNode.value;
        insertedNode.value = temp;
      }

      //if shallowest level next points
      //to null we are at the end of list
      if (top.levels.next == null) {
        this.end = insertedNode;
      }
      //insert node and update shallowest level reference
      insertedNode.levels.next = top.levels.next;
      top.levels.next = insertedNode;

      int levelCounter = 1;
      double prob = this.rng.nextDouble();

      //update levels references until maxLevel
      //is reached or randomized variable cuts-off
      //the loop
      while (!visitedStack.isEmpty() && prob < 0.5) {
        top = visitedStack.pollLast();

        LevelNode<T> insertedLevel = this.newLevel(insertedNode, levelCounter);
        LevelNode<T> predecessorLevel = top.levels.moveUpBy(levelCounter);
        insertedLevel.next = predecessorLevel.next;
        predecessorLevel.next = insertedNode;
        levelCounter++;

        prob = this.rng.nextDouble();
      }

      //we reached current maximum number of
      //levels and need to extend this.START
      //and insertedNode by one level
      if (visitedStack.isEmpty() && prob < 0.5) {
        this.newLevel(insertedNode, levelCounter);
        LevelNode<T> startLevel = this.newLevel(this.start, levelCounter);
        startLevel.next = insertedNode;
        this.maxLevel++;
      }
    }

    this.size++;
    return true;
  }

  @Override
  public boolean remove(Object obj) {
    T value = (T) obj;

    if (!this.contains(value)) {
      return false;
    }
    //from this point on presence of value is assumed

    //the only node matches query
    if (this.size == 1) {
      this.start = this.end = null;
      this.size--;
      return true;
    }

    LinkedList<SkipListNode<T>> visitedStack = this.search(value, true);
    return removeDo(visitedStack);
  }

  private boolean removeDo(LinkedList<SkipListNode<T>> visitedStack) {
    if (visitedStack.peekLast() == null) {
      //the value is first in skip list
      SkipListNode<T> nextToStart = this.start.levels.next;
      int currentLevel = 0;

      //pick node next to start if number of levels is
      //smaller than maximum rebuild them moving bottom up
      while (currentLevel <= this.maxLevel) {
        if (nextToStart.levels.moveUpBy(currentLevel) == null) {
          LevelNode<T> insertedLevel = this.newLevel(nextToStart, currentLevel);
          insertedLevel.next = this.start.levels.moveUpBy(currentLevel).next;
        }
        currentLevel++;
      }
      this.start = nextToStart;
    } else {
      int currentLevel = 0;
      SkipListNode<T> toBeRemoved = visitedStack.peekLast().levels.next;

      //moving bottom up rebuild level links of node on left
      //to the removed node to point them to links on the
      //right of removed node
      do {
        SkipListNode<T> top = visitedStack.pollLast();
        LevelNode<T> removedLevel = toBeRemoved.levels.moveUpBy(currentLevel);
        if (top == null || removedLevel == null) {
          break;
        }
        top.levels.moveUpBy(currentLevel).next = removedLevel.next;
        currentLevel++;
      } while (!visitedStack.isEmpty() || currentLevel <= this.maxLevel);
    }
    this.size--;
    return true;
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    for (Object element : collection) {
      if (!this.contains(element)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    if (collection.size() == 0) {
      return false;
    }
    for (T element : collection) {
      this.add(element);
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    SkipList<T> newSkip = new SkipList<>(this.comparator);
    for (Object element : collection) {
      if (this.contains(element)) {
        newSkip.add((T) element);
      }
    }
    boolean changed = this.size == newSkip.size;
    this.start = newSkip.start;
    this.end = newSkip.end;
    this.maxLevel = newSkip.maxLevel;
    this.size = newSkip.size;
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    boolean isChange = false;
    for (Object element : collection) {
      var isElemRemoved = this.remove(element);
      if (!isChange) {
        isChange = isElemRemoved;
      }
    }
    return isChange;
  }

  @Override
  public void clear() {
    this.start = this.end = null;
    this.size = 0;
  }

  @Override
  public Comparator<T> comparator() {
    return this.comparator;
  }

  @Override
  public T first() {
    return this.start.value;
  }

  @Override
  public T last() {
    return this.end.value;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SkipList = ");
    SkipListNode<T> current = this.start;
    while (current != null) {
      builder.append("[")
          .append(current.value)
          .append(",")
          .append(current.levels.levelsToTop())
          .append("],");
      current = current.levels.next;
    }
    return builder.toString();
  }

  /** Traverses skip list top-down pushing nodes
   * of corresponding levels on the stack.
   * If forRemove is true nodes predecessors are
   * pushed instead.
   *
   * @param value searched value
   * @param forRemove if stack is used for remove procedure
   * @return
   */
  private LinkedList<SkipListNode<T>> search(T value, boolean forRemove) {
    if (this.isEmpty()) {
      return new LinkedList<>();
    }

    int currentLevel = this.maxLevel;
    SkipListNode<T> currentNode = this.start;
    SkipListNode<T> predecessorNode = null;
    LinkedList<SkipListNode<T>> visitedStack = new LinkedList<>();

    while (currentLevel >= 0) {
      //get level on given height
      LevelNode<T> currLevel = currentNode.levels.moveUpBy(currentLevel);

      //iterate until end or next node on the right is greater than
      //searched value
      while (currLevel.next != null
          && this.comparator.compare(value, currLevel.next.value) >= 0) {
        //hop on to next link and upgrade level to defined height
        predecessorNode = currentNode;
        currentNode = currLevel.next;
        currLevel = currentNode.levels.moveUpBy(currentLevel);
      }
      //on lower levels predecessor node can be lagging couple
      //of hops behind current node, let's rewind it
      if (predecessorNode != null) {
        while (predecessorNode.levels.moveUpBy(currentLevel).next
            != currentNode) {
          predecessorNode = predecessorNode.levels.moveUpBy(currentLevel).next;
        }
      }
      currentLevel--;
      if (forRemove && this.comparator.compare(value, currentNode.value) == 0) {
        visitedStack.offerLast(predecessorNode);
      } else {
        visitedStack.offerLast(currentNode);
      }
    }
    return visitedStack;
  }

  private <U> void fillArray(U[] arr) {
    SkipListNode<T> it = this.start;
    int ind = 0;
    while (it != null) {
      arr[ind++] = (U) it.value;
      it = it.levels.next;
    }
  }

  private LevelNode<T> newLevel(SkipListNode<T> node, int level) {
    LevelNode<T> newLevel = new LevelNode<>();
    LevelNode<T> oldTop = node.levels.moveUpBy(level - 1);
    oldTop.up = newLevel;
    return newLevel;
  }

}
