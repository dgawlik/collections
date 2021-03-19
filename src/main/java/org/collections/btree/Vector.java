package org.collections.btree;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.RandomAccess;


/**
 * Mutable persistent vector implementation
 *
 * @param <T>
 */
public class Vector<T> implements RandomAccess,
    Iterable<T> {

  private static class Iterator<T> implements java.util.Iterator<T> {

    private final Object[] arr;
    private int top;

    public Iterator(Object[] arr) {
      this.arr = arr;
      this.top = 0;
    }

    @Override
    public boolean hasNext() {
      return this.top != arr.length;
    }

    @Override
    public T next() {
      return (T) this.arr[this.top++];
    }
  }

  public static class Node implements Cloneable {

    boolean isLeaf;

    Object[] values;
    Object[] links;
    Integer[] counts;

    int top;

    private Node() {
    }

    private Node(Object[] values, int top, boolean isLeaf) {
      this.isLeaf = isLeaf;
      this.top = top;

      if (!isLeaf) {
        this.links = values;
        this.counts = new Integer[links.length];
        for (int i = 0; i < this.top; i++) {
          this.counts[i] = ((Node) this.links[i]).getTotalCount();
        }
      } else {
        this.values = values;
      }
    }


    public int getTotalCount() {
      if (this.isLeaf) {
        return this.top;
      } else {
        int subtreeCount = 0;
        for (int i = 0; i < this.top; i++) {
          subtreeCount += this.counts[i];
        }
        return subtreeCount;
      }
    }

    public static <U> Node singleton(U value, int BUCKET_MAX_SIZE) {
      Object[] arr = new Object[BUCKET_MAX_SIZE + 1];
      arr[0] = value;
      return new Node(arr, 1, true);
    }

    public static Node createNode(Object[] links, int top) {
      return new Node(links, top, false);
    }

    public static Node createLeaf(Object[] values, int top) {
      return new Node(values, top, true);
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      for (int i = 0; i < top; i++) {
        if (this.isLeaf) {
          sb.append(this.values[i]).append(",");
        } else {
          sb.append(this.links[i]).append(",");
        }
      }
      sb.append("]");
      return sb.toString();
    }

    @Override
    protected Object clone() {
      Node cloned = new Node();
      cloned.isLeaf = this.isLeaf;
      cloned.top = this.top;
      if (this.counts != null) {
        cloned.counts = Arrays.copyOf(this.counts, this.counts.length);
      }
      if (this.links != null) {
        cloned.links = Arrays.copyOf(this.links, this.links.length);
      }
      if (this.values != null) {
        cloned.values = Arrays.copyOf(this.values, this.values.length);
      }
      return cloned;
    }
  }

  private Node root;
  private int size;
  private int BUCKET_MAX_SIZE;

  public Vector(int BUCKET_MAX_SIZE) {
    this.size = 0;
    this.BUCKET_MAX_SIZE = BUCKET_MAX_SIZE;
  }


  public int size() {
    return this.size;
  }

  public boolean isEmpty() {
    return this.size == 0;
  }

  public boolean contains(Object value) {
    if (this.size == 0) {
      return false;
    }
    int offset = this.findIndex(value, this.root, 0);
    return offset != -1;
  }


  public Iterator<T> iterator() {
    Object[] arr = new Object[this.size];

    if (this.size > 0) {
      fillArray(arr, 0, this.root);
    }
    return new Iterator<>(arr);
  }

  public Object[] toArray() {
    Object[] arr = new Object[this.size];
    if (this.size > 0) {
      fillArray(arr, 0, this.root);
    }
    return arr;
  }

  public <U> U[] toArray(U[] a) {
    U[] arr = (U[]) Array
        .newInstance(a.getClass().getComponentType(), this.size);
    fillArray(arr, 0, this.root);
    return arr;
  }

  public Vector<T> add(T value) {
    Vector<T> newVect = new Vector<>(this.BUCKET_MAX_SIZE);

    if (this.root == null) {
      newVect.root = Node.singleton(value, this.BUCKET_MAX_SIZE);
      newVect.size = 1;
    } else {
      newVect.root = this.insertAt(value, this.size);
      newVect.size = this.size + 1;
    }
    return newVect;
  }

  public Vector<T> remove(Object value) {
    Vector<T> newVect = new Vector<>(this.BUCKET_MAX_SIZE);
    if (this.size == 0 || !this.contains(value)) {
      for (T elem : this) {
        newVect = newVect.add(elem);
      }
      return newVect;
    }
    int offset = this.findIndex((T) value, this.root, 0);
    newVect.root = this.removeAt(offset);
    newVect.size = this.size - 1;
    if (newVect.size == 0) {
      newVect.root = null;
    }
    return newVect;
  }

  public boolean containsAll(Collection<?> c) {
    for (Object o : c) {
      if (!this.contains(o)) {
        return false;
      }
    }
    return true;
  }

  public Vector<T> addAll(Collection<? extends T> c) {
    Vector<T> cursor = this;
    for (T val : c) {
      cursor = cursor.add(val);
    }
    return cursor;
  }

  public Vector<T> removeAll(Collection<?> c) {
    Vector<T> cursor = this;
    for (Object o : c) {
      cursor = this.remove(o);
    }
    return cursor;
  }

  public T get(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }

    Node drillDownNode = this.root;
    while (!drillDownNode.isLeaf) {
      int offSum = 0;
      int i = 0;
      while (i < drillDownNode.top
          && offSum <= index) {
        offSum += drillDownNode.counts[i];
        i++;
      }
      i--;
      offSum -= drillDownNode.counts[i];
      drillDownNode = (Node) drillDownNode.links[i];
      index -= offSum;
    }
    return (T) drillDownNode.values[index];
  }

  public Vector<T> set(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }
    Node drillDownNode = this.root;
    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();
    Node copiedLast;
    while (!drillDownNode.isLeaf) {
      int offSum = 0;
      int i = 0;
      while (i < drillDownNode.top
          && offSum <= index) {
        offSum += drillDownNode.counts[i];
        i++;
      }
      i--;
      offSum -= drillDownNode.counts[i];
      visitedStack.offerLast(drillDownNode);
      visitedIndicesStack.offerLast(i);
      drillDownNode = (Node) drillDownNode.links[i];
      index -= offSum;
    }
    copiedLast = (Node) drillDownNode.clone();
    copiedLast.values[index] = element;

    while (!visitedStack.isEmpty()) {
      Node cloned = (Node) visitedStack.pollLast().clone();
      Integer upIndex = visitedIndicesStack.pollLast();
      cloned.links[upIndex] = copiedLast;
      copiedLast = cloned;
    }

    Vector<T> newVect = new Vector<>(BUCKET_MAX_SIZE);
    newVect.root = copiedLast;
    newVect.size = this.size;
    return newVect;
  }

  public Vector<T> add(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }
    if (element == null) {
      throw new IllegalArgumentException("element is null");
    }

    Vector<T> newVect = new Vector<>(this.BUCKET_MAX_SIZE);
    if (this.root == null) {
      newVect.root = Node.singleton(element, this.BUCKET_MAX_SIZE);
      newVect.size = 1;
    } else {
      newVect.root = this.insertAt(element, index);
      newVect.size = this.size + 1;
    }
    return newVect;
  }

  public Vector<T> removeAtIndex(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException("index out of bounds");
    }

    Vector<T> newVect = new Vector<>(this.BUCKET_MAX_SIZE);

    newVect.root = this.removeAt(index);
    newVect.size = this.size - 1;
    return newVect;
  }

  public int indexOf(Object o) {
    if (this.size == 0) {
      return -1;
    }
    return this.findIndex((T) o, this.root, 0);
  }


  public String toString() {
    return this.root.toString();
  }


  private Node insertAt(T value, int offset) {
    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();
    Node cursor = this.root;
    Node lastCopied = null;

    while (!cursor.isLeaf) {
      int cumulativeOffsets = 0;
      int i = 0;
      while (i < cursor.top
          && cumulativeOffsets <= offset) {
        cumulativeOffsets += cursor.counts[i];
        i++;
      }
      i--;
      cumulativeOffsets -= cursor.counts[i];
      visitedStack.offerLast(cursor);
      visitedIndicesStack.offerLast(i);
      cursor = (Node) cursor.links[i];

      //reduce offset to leaf-offset
      offset -= cumulativeOffsets;
    }

    lastCopied = (Node) cursor.clone();
    this.insertToArray(lastCopied.values, value, offset, lastCopied.top);
    lastCopied.top++;

    Node carry = null;
    if (lastCopied.top > this.BUCKET_MAX_SIZE) {
      carry = this.expandLeafs(lastCopied);
    }

    while (!visitedStack.isEmpty()) {
      cursor = visitedStack.pollLast();
      int index = visitedIndicesStack.pollLast();

      Node cloned = (Node) cursor.clone();

      cloned.counts[index] = lastCopied.getTotalCount();
      cloned.links[index] = lastCopied;

      if (carry != null) {
        this.insertToArray(cloned.links, carry, index + 1, cloned.top);
        this.insertToArray(cloned.counts, carry.getTotalCount(), index + 1,
            cloned.top);
        cloned.top++;

        if (cloned.top > this.BUCKET_MAX_SIZE) {
          carry = expandLinks(cloned);
        } else {
          carry = null;
        }
      }
      lastCopied = cloned;
    }

    if (carry != null) {
      Object[] newLinks = new Object[BUCKET_MAX_SIZE + 1];
      newLinks[0] = lastCopied;
      newLinks[1] = carry;
      return Node.createNode(newLinks, 2);
    } else {
      return lastCopied;
    }
  }


  private Node removeAt(int offset) {
    Node cursor = this.root;
    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();

    Node lastCopied;

    while (!cursor.isLeaf) {
      int i = 0;
      int cumulativeOffset = 0;
      while (i < cursor.top
          && cumulativeOffset <= offset) {
        cumulativeOffset += cursor.counts[i];
        i++;
      }
      i--;
      cumulativeOffset -= cursor.counts[i];
      visitedStack.offerLast(cursor);
      visitedIndicesStack.offerLast(i);
      cursor = (Node) cursor.links[i];

      //reduce offset to leaf-internal offset
      offset -= cumulativeOffset;
    }

    lastCopied = (Node) cursor.clone();

    System
        .arraycopy(lastCopied.values, offset + 1, lastCopied.values, offset,
            lastCopied.top - 1 - offset);
    lastCopied.top--;

    while (!visitedStack.isEmpty()) {
      cursor = visitedStack.pollLast();
      int index = visitedIndicesStack.pollLast();

      Node cloned = (Node) cursor.clone();

      cloned.counts[index] = lastCopied.getTotalCount();
      cloned.links[index] = lastCopied;

      if (index >= 1
          && ((Node) cloned.links[index - 1]).top
          + ((Node) cloned.links[index]).top
          <= this.BUCKET_MAX_SIZE / 2
      ) {
        Object[] mergedArr = new Object[BUCKET_MAX_SIZE + 1];
        int newTop;
        if (((Node) cloned.links[index]).isLeaf) {
          newTop = shrinkPivots(cloned, index, mergedArr);
        } else {
          newTop = shrinkLinks(cloned, index, mergedArr);
        }
        insertMergeNode(cloned, index, mergedArr, newTop);
      }

      lastCopied = cloned;
    }

    return lastCopied;
  }


  private int shrinkPivots(Node nodeUnderOp, int indexUnderOp,
      Object[] mergedArr) {
    int index = 0;

    System.arraycopy(
        ((Node) nodeUnderOp.links[indexUnderOp - 1]).values,
        0,
        mergedArr,
        0,
        ((Node) nodeUnderOp.links[indexUnderOp - 1]).top);

    index += ((Node) nodeUnderOp.links[indexUnderOp - 1]).top;

    System.arraycopy(
        ((Node) nodeUnderOp.links[indexUnderOp]).values,
        0,
        mergedArr,
        index,
        ((Node) nodeUnderOp.links[indexUnderOp]).top);

    index += ((Node) nodeUnderOp.links[indexUnderOp]).top;
    return index;
  }

  private int shrinkLinks(Node nodeUnderOp, int indexUnderOp,
      Object mergedArr) {
    int index = 0;

    System.arraycopy(
        ((Node) nodeUnderOp.links[indexUnderOp - 1]).links,
        0,
        mergedArr,
        0,
        ((Node) nodeUnderOp.links[indexUnderOp - 1]).top);

    index += ((Node) nodeUnderOp.links[indexUnderOp - 1]).top;

    System.arraycopy(
        ((Node) nodeUnderOp.links[indexUnderOp]).links,
        0,
        mergedArr,
        index,
        ((Node) nodeUnderOp.links[indexUnderOp]).top);

    index += ((Node) nodeUnderOp.links[indexUnderOp]).top;
    return index;
  }

  private void insertMergeNode(Node onNode, int index, Object[] mergedArr,
      int top) {
    Node mergeNode;
    if (((Node) onNode.links[0]).isLeaf) {
      mergeNode = Node.createLeaf(mergedArr, top);
    } else {
      mergeNode = Node.createNode(mergedArr, top);
    }

    System
        .arraycopy(onNode.links, index + 1, onNode.links, index,
            onNode.top - 1 - index);
    System
        .arraycopy(onNode.counts, index + 1, onNode.counts, index,
            onNode.top - 1 - index);
    onNode.links[index - 1] = mergeNode;
    onNode.counts[index - 1] = mergeNode.getTotalCount();

    onNode.top--;
  }

  private Node expandLeafs(Node toBeExpanded) {
    Object[] newArray = new Object[BUCKET_MAX_SIZE + 1];

    System.arraycopy(toBeExpanded.values, toBeExpanded.top / 2, newArray, 0,
        toBeExpanded.top - toBeExpanded.top / 2);

    int prevCount = toBeExpanded.top;
    toBeExpanded.top /= 2;

    return Node.createLeaf(newArray, prevCount - prevCount / 2);
  }

  private Node expandLinks(Node toBeExpanded) {
    Object[] newArray = new Object[BUCKET_MAX_SIZE + 1];

    System.arraycopy(toBeExpanded.links, toBeExpanded.top / 2, newArray, 0,
        toBeExpanded.top - toBeExpanded.top / 2);

    int prevTop = toBeExpanded.top;
    toBeExpanded.top /= 2;

    return Node.createNode(newArray, prevTop - prevTop / 2);
  }


  private <U> int fillArray(U[] arr, int arrIndex, Node node) {
    for (int i = 0; i < node.top; i++) {
      if (node.isLeaf) {
        arr[arrIndex++] = (U) node.values[i];
      } else {
        arrIndex = fillArray(arr, arrIndex, (Node) node.links[i]);
      }
    }
    return arrIndex;
  }

  private void insertToArray(Object[] arr, Object value, int atIndex, int top) {
    if (top - atIndex > 0) {
      System.arraycopy(arr, atIndex, arr, atIndex + 1, top - atIndex);
    }
    arr[atIndex] = value;
  }


  private int findIndex(Object value, Node node, int offset) {
    var sum = 0;
    for (int j = 0; j < node.top; j++) {
      if (node.isLeaf && node.values[j].equals(value)) {
        return offset + j;
      } else if (!node.isLeaf) {
        int sub = findIndex(value, (Node) node.links[j], offset + sum);
        if (sub != -1) {
          return sub;
        }
        sum += node.counts[j];
      }
    }
    return -1;
  }

}
