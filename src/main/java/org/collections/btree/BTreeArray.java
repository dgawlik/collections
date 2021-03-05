package org.collections.btree;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;


/**
 * Mutable persistent vector implementation
 *
 * @param <T>
 */
public class BTreeArray<T extends Comparable<? super T>> implements
    List<T>, RandomAccess {

  private static class Iterator<T> implements java.util.Iterator<T> {

    private final T[] arr;
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

  public static class Node<T> {

    boolean isLeaf;

    T[] pivots;
    Integer[] counts;
    int top;
    Node<T>[] links;

    public Node(T[] values, int top) {
      this.pivots = values;
      this.isLeaf = true;
      this.top = top;
    }

    public Node(Node<T>[] links, int top) {
      this.links = links;
      this.top = top;
      this.isLeaf = false;
      this.counts = new Integer[links.length];
      for (int i = 0; i < this.top; i++) {
        this.counts[i] = this.links[i].getTotalCount();
      }

    }

    public int getTotalCount() {
      if (this.isLeaf) {
        return this.top;
      } else {
        int sum = 0;
        for (int i = 0; i < this.top; i++) {
          sum += this.counts[i];
        }
        return sum;
      }
    }

    public static <U> Node<U> singleton(U value, int BUCKET_MAX_SIZE) {
      U[] arr = (U[]) Array
          .newInstance(value.getClass(),
              BUCKET_MAX_SIZE + 1);
      arr[0] = value;
      return new Node<>(arr, 1);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < top; i++) {
        if (this.isLeaf) {
          sb.append(this.pivots[i]).append(",");
        } else {
          sb.append(this.links[i]).append(",");
        }
      }
      sb.append("]");
      return sb.toString();
    }
  }

  private Node<T> root;
  private int size;
  private int BUCKET_MAX_SIZE;

  public BTreeArray(int BUCKET_MAX_SIZE) {
    this.size = 0;
    this.BUCKET_MAX_SIZE = BUCKET_MAX_SIZE;
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
    int offset = this.findIndex((T) value, this.root, 0);
    return offset != -1;
  }


  @Override
  public Iterator<T> iterator() {
    T[] arr = (T[]) createArray(this.size,
        this.root.pivots.getClass().getComponentType());
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
      this.root = Node.singleton(value, this.BUCKET_MAX_SIZE);
    } else {
      this.insertAt(value, this.size);
    }
    this.size++;
    return true;
  }

  @Override
  public boolean remove(Object value) {
    if (this.size == 0 || !this.contains(value)) {
      return false;
    }
    int offset = this.findIndex((T) value, this.root, 0);
    this.removeAt(offset);
    this.size--;
    if (this.size == 0) {
      this.root = null;
    }
    return true;
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
  public boolean addAll(int index, Collection<? extends T> c) {
    return false;
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
  public T get(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }

    Node<T> it = this.root;
    while (!it.isLeaf) {
      int offSum = 0;
      int i = 0;
      while (i < it.top
          && offSum <= index) {
        offSum += it.counts[i];
        i++;
      }
      i--;
      offSum -= it.counts[i];
      it = it.links[i];
      index -= offSum;
    }
    return it.pivots[index];
  }

  @Override
  public T set(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }
    Node<T> it = this.root;
    while (!it.isLeaf) {
      int offSum = 0;
      int i = 0;
      while (i < it.top
          && offSum <= index) {
        offSum += it.counts[i];
        i++;
      }
      i--;
      offSum -= it.counts[i];
      it = it.links[i];
      index -= offSum;
    }
    return it.pivots[index] = element;
  }

  @Override
  public void add(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }
    if (element == null) {
      throw new IllegalArgumentException("element is null");
    }

    if (this.root == null) {
      this.root = Node.singleton(element, this.BUCKET_MAX_SIZE);
    } else {
      this.insertAt(element, index);
    }

    this.size++;
  }

  @Override
  public T remove(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }
    T val = this.get(index);
    this.removeAt(index);
    this.size--;
    return val;
  }

  @Override
  public int indexOf(Object o) {
    if (this.size == 0) {
      return -1;
    }
    return this.findIndex((T) o, this.root, 0);
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public ListIterator<T> listIterator() {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    throw new IllegalStateException("Not implemented!");
  }

  @Override
  public String toString() {
    return this.root.toString();
  }

  private void insertAt(T value, int offset) {
    LinkedList<Node<T>> stack = new LinkedList<>();
    LinkedList<Integer> stackInd = new LinkedList<>();
    Node<T> it = this.root;

    while (!it.isLeaf) {
      int offSum = 0;
      int i = 0;
      while (i < it.top
          && offSum <= offset) {
        offSum += it.counts[i];
        i++;
      }
      i--;
      offSum -= it.counts[i];
      stack.offerLast(it);
      stackInd.offerLast(i);
      it = it.links[i];
      offset -= offSum;
    }

    this.insertToArray(it.pivots, value, offset, it.top);
    it.top++;

    Node<T> carry = null;
    if (it.top > this.BUCKET_MAX_SIZE) {
      carry = this.grow(it);
    }

    while (!stack.isEmpty()) {
      it = stack.pollLast();
      int ind2 = stackInd.pollLast();

      it.counts[ind2] = it.links[ind2].getTotalCount();

      if(carry != null) {
        this.insertToArray(it.links, carry, ind2 + 1, it.top);
        this.insertToArray(it.counts, carry.getTotalCount(), ind2 + 1, it.top);
        it.top++;

        if (it.top > this.BUCKET_MAX_SIZE) {
          carry = growLinks(it);
        } else {
          carry = null;
        }
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


  private void removeAt(int offset) {
    Node<T> it = this.root;
    LinkedList<Node<T>> stack = new LinkedList<>();
    LinkedList<Integer> stackInd = new LinkedList<>();

    while (!it.isLeaf) {
      int i = 0;
      int offSum = 0;
      while (i < it.top
          && offSum <= offset) {
        offSum += it.counts[i];
        i++;
      }
      i--;
      offSum -= it.counts[i];
      stack.offerLast(it);
      stackInd.offerLast(i);
      it = it.links[i];
      offset -= offSum;
    }

    System
        .arraycopy(it.pivots, offset + 1, it.pivots, offset,
            it.top - 1 - offset);
    it.top--;

    while (!stack.isEmpty()) {
      it = stack.pollLast();
      int ind = stackInd.pollLast();

      it.counts[ind] = it.links[ind].getTotalCount();

      if (ind >= 1
          && it.links[ind - 1].top + it.links[ind].top
          <= this.BUCKET_MAX_SIZE / 2) {
        if (it.links[ind].isLeaf) {
          T[] mergedArr = (T[]) createArray(this.BUCKET_MAX_SIZE + 1,
              it.links[0].pivots.getClass().getComponentType());

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

  private int shrinkLinks(Node<T> it, int ind,
      Node<T>[] mergedArr) {
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
        .arraycopy(it.counts, ind + 1, it.counts, ind, it.top - 1 - ind);
    it.links[ind - 1] = mergedN;
    it.counts[ind - 1] = mergedN.getTotalCount();

    it.top--;
  }

  private Node<T> grow(Node<T> it) {
    T[] rightArr = (T[]) createArray(this.BUCKET_MAX_SIZE + 1,
        it.pivots[0].getClass());

    System.arraycopy(it.pivots, it.top / 2, rightArr, 0,
        it.top - it.top / 2);

    int prevCount = it.top;
    it.top /= 2;

    return new Node<>(rightArr, prevCount - prevCount / 2);
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

  private <U> void insertToArray(U[] arr, U value, int ind, int top) {
    if (top - ind > 0) {
      System.arraycopy(arr, ind, arr, ind + 1, top - ind);
    }
    arr[ind] = value;
  }


  private int findIndex(T value, Node<T> node, int offset) {
    var sum = 0;
    for (int j = 0; j < node.top; j++) {
      if (node.isLeaf && node.pivots[j].equals(value)) {
        return offset + j;
      } else if(!node.isLeaf){
        int sub = findIndex(value, node.links[j], offset + sum);
        if (sub != -1) {
          return sub;
        }
        sum += node.counts[j];
      }
    }
    return -1;
  }

}
