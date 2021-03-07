package org.collections.btree;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;


/**
 * Sorted multiset implemented as tree with high branching factor.
 * Number of branches in nodes is configurable.
 *
 * D - depth, B - branching factor
 * Insertion: O(D*B) ~ O(1)
 * Removal: O(D*B) ~ O(1)
 * Contains: O(D*logB) ~ O(1)
 *
 * @param <T> type that this collection holds
 */
public class BTree<T extends Comparable<? super T>> implements
    org.collections.Multiset<T> {

  private static class Iterator<T> implements java.util.Iterator<T> {

    private T[] arr;
    private int top;

    public Iterator(T[] arr) {
      this.arr = arr;
      this.top = 0;
    }

    @Override
    public boolean hasNext() {
      return this.top != arr.length;
    }

    @Override
    public T next() {
      return this.arr[this.top++];
    }
  }

  /** BTree single node can be leaf or internal.
   *
   * @param <T> held value type
   */
  private static class Node<T> {

    //if leaf holds sorted values
    //and if node pivots[i] holds minimum value
    //held by subtree at links[i]
    T[] pivots;

    //links to subtrees if leaf this field is null
    Node<T>[] links;

    //is leaf or node?
    boolean isLeaf;

    //upper exclusive bound of elements contained
    //in pivots and links
    int top;

    public Node(T[] values, int top) {
      this.pivots = values;
      this.isLeaf = true;
      this.top = top;
    }

    public Node(Node<T>[] links, int top) {
      this.links = links;
      this.top = top;
      this.isLeaf = false;
      this.pivots = (T[]) Array
          .newInstance(links[0].pivots[0].getClass(),
              links.length);
      for (int i = 0; i < top; i++) {
        this.pivots[i] = this.links[i].pivots[0];
      }
    }

    public static <U> Node<U> createLeaf(U[] values, int top) {
      return new Node<>(values, top);
    }

    public static <U> Node<U> createNode(Node<U>[] links, int top) {
      return new Node<>(links, top);
    }

    public static <U> Node<U> createSingleton(U value, int BUCKET_MAX_SIZE) {
      U[] arr = (U[]) Array
          .newInstance(value.getClass(),
              BUCKET_MAX_SIZE + 1);
      arr[0] = value;
      return new Node<>(arr, 1);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("[");
      for (int i = 0; i < top; i++) {
        if (this.isLeaf) {
          builder.append(this.pivots[i]).append(",");
        } else {
          builder.append(this.links[i]).append(",");
        }
      }
      builder.append("]");
      return builder.toString();
    }
  }

  private final Comparator<T> comparator;

  private final int BUCKET_MAX_SIZE;

  private Node<T> root;

  private int size;


  public BTree(Comparator<T> comparator, int BUCKET_MAX_SIZE) {
    this.comparator = comparator;
    this.size = 0;
    this.BUCKET_MAX_SIZE = BUCKET_MAX_SIZE;
  }

  public BTree(int BUCKET_MAX_SIZE) {
    this(Comparator.naturalOrder(), BUCKET_MAX_SIZE);
  }

  @Override
  public Comparator<? super T> comparator() {
    return this.comparator;
  }


  @Override
  public T first() {
    var node = this.root;
    while (!node.isLeaf) {
      node = node.links[0];
    }
    return node.pivots[0];
  }

  @Override
  public T last() {
    var node = this.root;
    while (!node.isLeaf) {
      node = node.links[node.top - 1];
    }
    return node.pivots[node.top - 1];
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
  public boolean contains(Object value) {
    if (this.size == 0) {
      return false;
    }
    Node<T> it = root;
    Node<T> it1 = it;
    while (!it1.isLeaf) {
      int ind1 = this.searchNode(it1.pivots, (T) value, it1.top - 1);
      it1 = it1.links[ind1];
    }
    it = it1;
    int ind = this.searchLeaf(it.pivots, (T) value, it.top - 1);
    return this.comparator.compare((T) value, it.pivots[ind]) == 0;
  }


  @Override
  public Iterator<T> iterator() {
    T[] arr = (T[]) createArray(this.size, this.root.pivots[0].getClass());
    fillArray(arr, 0, this.root);
    return new Iterator<>(arr);
  }

  @Override
  public Object[] toArray() {
    Object[] arr = new Object[this.size];
    fillArray(arr, 0, this.root);
    return arr;
  }

  @Override
  public <U> U[] toArray(U[] a) {
    U[] arr = (U[]) Array
        .newInstance(a.getClass().getComponentType(), this.size);
    fillArray(arr, 0, this.root);
    return arr;
  }

  @Override
  public boolean add(T value) {
    if (this.root == null) {
      this.root = Node.createSingleton(value, this.BUCKET_MAX_SIZE);
    } else {
      this.insert(value);
    }
    this.size++;
    return true;
  }

  @Override
  public boolean remove(Object value) {
    if (this.size == 0) {
      return false;
    }
    var removed = this.delete((T) value);
    if (removed) {
      this.size--;
      if (this.size == 0) {
        this.root = null;
      }
    }
    return removed;
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
    for (T val : c) {
      this.add(val);
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    var changed = false;
    for (T val : this) {
      if (!c.contains(val)) {
        this.remove(val);
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    var changed = false;
    for (Object o : c) {
      var ch = this.remove(o);
      if (!changed) {
        changed = ch;
      }
    }
    return changed;
  }

  @Override
  public void clear() {
    this.size = 0;
    this.root = null;
  }

  @Override
  public String toString() {
    return this.root.toString();
  }

  private void insert(T value) {
    LinkedList<Node<T>> stack = new LinkedList<>();
    LinkedList<Integer> stackInd = new LinkedList<>();
    Node<T> it = this.root;

    Node<T> carry = insertOptionallyGrowLeaf(value, stack, stackInd, it);

    bubbleUpGrowLinks(stack, stackInd, carry);
  }

  private Node<T> insertOptionallyGrowLeaf(T value, LinkedList<Node<T>> stack,
      LinkedList<Integer> stackInd, Node<T> it) {
    while (!it.isLeaf) {
      int ind = this.searchNode(it.pivots, value, it.top - 1);
      stack.offerLast(it);
      stackInd.offerLast(ind);
      it = it.links[ind];
    }

    int ind = this.searchLeaf(it.pivots, value, it.top - 1);
    if (this.comparator.compare(value, it.pivots[ind]) > 0) {
      ind++;
    }

    this.fillIn(it.pivots, value, ind, it.top);
    it.top++;

    if (it.top > this.BUCKET_MAX_SIZE) {
      return grow(it);
    }
    return null;
  }

  private void bubbleUpGrowLinks(LinkedList<Node<T>> stack,
      LinkedList<Integer> stackInd, Node<T> carry) {
    Node<T> it;
    while (!stack.isEmpty() && carry != null) {
      it = stack.pollLast();
      int ind2 = stackInd.pollLast();
      this.fillIn(it.links, carry, ind2 + 1, it.top);
      this.fillIn(it.pivots, carry.pivots[0], ind2 + 1, it.top);
      it.top++;
      if (it.top > this.BUCKET_MAX_SIZE) {
        carry = growLinks(it);
      } else {
        carry = null;
      }
    }

    if (carry != null) {
      Node<T>[] newLinks = createArray(this.BUCKET_MAX_SIZE + 1,
          carry.getClass());
      newLinks[0] = this.root;
      newLinks[1] = carry;
      this.root = new Node<>(newLinks, 2);
    }
  }

  private boolean delete(T value) {
    Node<T> it = this.root;
    boolean found = false;
    LinkedList<Node<T>> stack = new LinkedList<>();
    LinkedList<Integer> stackInd = new LinkedList<>();

    found = deleteFromLeaf(value, it, stack, stackInd);

    bubbleUpRebalance(stack, stackInd);

    return found;
  }

  private boolean deleteFromLeaf(T value, Node<T> it, LinkedList<Node<T>> stack,
      LinkedList<Integer> stackInd) {
    boolean found;
    while (!it.isLeaf) {
      int ind = this.searchNode(it.pivots, value, it.top - 1);
      stack.offerLast(it);
      stackInd.offerLast(ind);
      it = it.links[ind];
    }

    int ind = this.searchLeaf(it.pivots, value, it.top - 1);
    if (ind > 0 && this.comparator.compare(value, it.pivots[ind]) < 0) {
      ind--;
    }
    if (this.comparator.compare(value, it.pivots[ind]) == 0) {
      System
          .arraycopy(it.pivots, ind + 1, it.pivots, ind, it.top - 1 - ind);

      it.top--;
      found = true;
    } else {
      found = false;
    }
    return found;
  }

  private void bubbleUpRebalance(LinkedList<Node<T>> stack,
      LinkedList<Integer> stackInd) {
    Node<T> it;
    while (!stack.isEmpty()) {
      it = stack.pollLast();
      int ind = stackInd.pollLast();

      it.pivots[ind] = it.links[ind].pivots[0];

      if (ind >= 1
          && it.links[ind - 1].top + it.links[ind].top
          < this.BUCKET_MAX_SIZE / 2) {
        if (it.links[ind].isLeaf) {
          T[] mergedArr = (T[]) Array
              .newInstance(it.pivots[0].getClass(),
                  this.BUCKET_MAX_SIZE + 1);

          int ind2 = shrinkPivots(it, ind, mergedArr);
          insertMergeNode(it, ind, mergedArr, ind2);
        } else {
          Node<T>[] mergedArr = (Node<T>[]) Array
              .newInstance(it.links[0].getClass(),
                  this.BUCKET_MAX_SIZE + 1);
          int ind2 = shrinkLinks(it, ind, mergedArr);
          insertMergeNode(it, ind, mergedArr, ind2);
        }
      }
    }
  }

  private int shrinkPivots(Node<T> it, int ind, T[] mergedArr) {
    int ind2 = 0;
    System.arraycopy(it.links[ind - 1].pivots, 0, mergedArr, 0,
        it.links[ind - 1].top);
    ind2 += it.links[ind - 1].top;
    System.arraycopy(it.links[ind].pivots, 0, mergedArr, ind2,
        it.links[ind].top);
    ind2 += it.links[ind].top;
    return ind2;
  }

  private int shrinkLinks(Node<T> it, int ind, Node<T>[] mergedArr) {
    int ind2 = 0;
    System.arraycopy(it.links[ind - 1].links, 0, mergedArr, 0,
        it.links[ind - 1].top);
    ind2 += it.links[ind - 1].top;
    System.arraycopy(it.links[ind].links, 0, mergedArr, ind2,
        it.links[ind].top);
    ind2 += it.links[ind].top;
    return ind2;
  }

  private void insertMergeNode(Node<T> it, int ind, Object mergedArr,
      int ind2) {
    Node<T> mergedN;
    if (it.links[0].isLeaf) {
      mergedN = new Node<>((T[]) mergedArr, ind2);
    } else {
      mergedN = new Node<>((Node[]) mergedArr, ind2);
    }

    System
        .arraycopy(it.links, ind + 1, it.links, ind, it.top - 1 - ind);
    System
        .arraycopy(it.pivots, ind + 1, it.pivots, ind, it.top - 1 - ind);
    it.top--;
    it.links[ind - 1] = mergedN;
    it.pivots[ind - 1] = mergedN.pivots[0];
  }

  private Node<T> grow(Node<T> it) {
    Node<T> carry;
    T[] rightArr = (T[]) createArray(this.BUCKET_MAX_SIZE + 1,
        it.pivots[0].getClass());

    System.arraycopy(it.pivots, it.top / 2, rightArr, 0,
        it.top - it.top / 2);

    int prevTop = it.top;
    it.top /= 2;

    carry = new Node<>(rightArr, prevTop - prevTop / 2);
    return carry;
  }

  private Node<T> growLinks(Node<T> it) {
    Node<T>[] rightArr = createArray(this.BUCKET_MAX_SIZE + 1,
        it.links[0].getClass());

    System.arraycopy(it.links, it.top / 2, rightArr, 0,
        it.top - it.top / 2);

    int prevTop = it.top;
    it.top /= 2;

    return new Node<>(rightArr, prevTop - prevTop / 2);
  }

  private <U> U[] createArray(int size, Class<U> cls) {
    return (U[]) Array.newInstance(cls, size);
  }

  private <U> int fillArray(U[] arr, int ind, Node node) {
    for (int i = 0; i < node.top; i++) {
      if (node.isLeaf) {
        arr[ind++] = (U) node.pivots[i];
      } else {
        ind = fillArray(arr, ind, node.links[i]);
      }
    }
    return ind;
  }

  private <U> void fillIn(U[] arr, U value, int ind, int top) {
    if (top - ind > 0) {
      System.arraycopy(arr, ind, arr, ind + 1, top - ind);
    }
    arr[ind] = value;
  }


  /** Exponential search in array.
   * Returns index of value or index of adjacent value.
   * If value is lower than middle value, search
   * is from left, otherwise from right.
   *
   * @param arr array to search
   * @param value searched value
   * @param end limit of contents of array inclusive
   * @return
   */
  private int searchLeaf(T[] arr, T value, int end) {
    int middle = end / 2;
    int checkpoint, nextCheckpoint, offset, sign;

    if (this.comparator.compare(value, arr[middle]) < 0) {
      checkpoint = 0;
      nextCheckpoint = 0;
      offset = 1;
      sign = 1;
    } else {
      checkpoint = end;
      nextCheckpoint = end;
      offset = 1;
      sign = -1;
    }

    while (true) {
      if (
          (sign == 1
              ? (checkpoint + offset <= end)
              : (checkpoint - offset >= 0)
          ) && comparator.compare(
              value,
              arr[checkpoint + offset * sign]
          ) * sign >= 0
      ) {
        nextCheckpoint = checkpoint + offset * sign;
        offset <<= 1;
      } else {
        checkpoint = nextCheckpoint;
        offset = 1;
      }
      if (checkpoint == nextCheckpoint) {
        return checkpoint;
      }
    }
  }

  /** Exponential search in array.
   * If value lower than middle point search is from left
   * otherwise from right.
   * Always returns index on the left of first greater
   * point than value.
   *
   * @param arr searched links array
   * @param value value in search
   * @param end limit of the elements inclusive
   * @return
   */
  private int searchNode(T[] arr, T value, int end) {
    int middle = end / 2;
    int checkpoint, nextCheckpoint, offset, sign;
    if (this.comparator.compare(value, arr[middle]) < 0) {
      checkpoint = 0;
      nextCheckpoint = 0;
      offset = 1;
      sign = 1;
    } else {
      checkpoint = end;
      nextCheckpoint = end;
      offset = 1;
      sign = -1;
    }
    while (true) {
      if (
          (sign == 1
              ? (checkpoint + offset + 1 <= end) :
              (checkpoint - offset >= 0)
          ) && (sign == 1
              ? comparator.compare(value, arr[checkpoint + offset + 1]) >= 0
              : comparator.compare(value, arr[checkpoint - offset + 1]) < 0)
      ){
        nextCheckpoint = checkpoint + offset*sign;
        offset <<= 1;
      } else {
        checkpoint = nextCheckpoint;
        offset = 1;
      }
      if (checkpoint == nextCheckpoint) {
        return checkpoint;
      }
    }
  }
}
