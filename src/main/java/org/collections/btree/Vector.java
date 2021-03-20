package org.collections.btree;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.RandomAccess;


/**
 * Immutable persistent vector implementation as BTree with
 * configurable branching factor.
 *
 * Each add/remove/set operation creates new instance with
 * only affected nodes cloned recursively.
 *
 * Amount of copying of elements is bounded by O(levels*bucket size).
 *
 * @param <T>
 */
@SuppressWarnings({"unchecked", "ConstantConditions"})
public class Vector<T> implements RandomAccess,
    Iterable<T> {

  public static final String INDEX_OUT_OF_BOUNDS_MESSAGE = "Index out of bounds: ";

  @SuppressWarnings("unchecked")
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
      if (this.top >= this.arr.length) {
        throw new NoSuchElementException("Iterator reached the end.");
      }

      return (T) this.arr[this.top++];
    }
  }

  /**
   * Node of the BTree, can be either leaf with values or
   * node with links to subtrees. The index is calculated
   * by drilling down to leaf and subtracting cumulative
   * subtrees counts.
   */
  public static class Node {

    boolean isLeaf;

    //if leaf holds objects, else null
    Object[] values;

    //if node holds references to subtrees
    Object[] links;

    //if node holds counts of elements in respective subtrees
    Integer[] counts;

    //upper limit of occupied space in the arrays
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


    /** Answers how what is aggregate number of elements
     * in this node.
     *
     * @return total count
     */
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

    public static <U> Node singleton(U value, int bucketMaxSize) {
      Object[] arr = new Object[bucketMaxSize + 1];
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

    public static Node copy(Node node) {
      Node cloned = new Node();
      cloned.isLeaf = node.isLeaf;
      cloned.top = node.top;
      if (node.counts != null) {
        cloned.counts = Arrays.copyOf(node.counts, node.counts.length);
      }
      if (node.links != null) {
        cloned.links = Arrays.copyOf(node.links, node.links.length);
      }
      if (node.values != null) {
        cloned.values = Arrays.copyOf(node.values, node.values.length);
      }
      return cloned;
    }
  }

  private Node root;
  private int size;
  private final int bucketMaxSize;

  public Vector(int bucketMaxSize) {
    this.size = 0;
    this.bucketMaxSize = bucketMaxSize;
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

  /** Drill down to corresponding leaf, copy it insert to copy, and
   * roll up copying affected nodes up to the root.
   *
   * @param value element to be added
   * @return new vector
   */
  public Vector<T> add(T value) {
    Vector<T> newVect = new Vector<>(this.bucketMaxSize);

    if (this.root == null) {
      newVect.root = Node.singleton(value, this.bucketMaxSize);
      newVect.size = 1;
    } else {
      newVect.root = this.insertAt(value, this.size);
      newVect.size = this.size + 1;
    }
    return newVect;
  }

  /** Drill down to corresponding leaf, copy it and delete from copy,
   * and roll up to root copying affected nodes. Return new vector
   * with new copy of root.
   *
   * @param value value to be removed
   * @return new vector
   */
  public Vector<T> remove(Object value) {
    if (this.size == 0 || !this.contains(value)) {
      return this;
    }

    Vector<T> newVect = new Vector<>(this.bucketMaxSize);
    int offset = this.findIndex(value, this.root, 0);
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
      cursor = cursor.remove(o);
    }
    return cursor;
  }

  /** Find corresponding leaf and get element at reduced index.
   *
   * @param index position of retrieved element
   * @return new vector
   */
  public T get(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUNDS_MESSAGE + index);
    }

    Node drillDownNode = this.root;

    //until leaf is found find index at current level
    //with index corresponding to given subtree range
    //and subtract from offset previous subtree aggregate counts
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

  /** Drill down to corresponding leaf noting visited nodes
   * and their indices. Change element at leaf and copy
   * affected nodes up to the root.
   *
   * @param index position of element
   * @param element new value of element
   * @return new vector
   */
  public Vector<T> set(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUNDS_MESSAGE + index);
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
    copiedLast = Node.copy(drillDownNode);
    copiedLast.values[index] = element;

    while (!visitedStack.isEmpty()) {
      Node cloned = Node.copy(visitedStack.pollLast());
      Integer upIndex = visitedIndicesStack.pollLast();
      cloned.links[upIndex] = copiedLast;
      copiedLast = cloned;
    }

    Vector<T> newVect = new Vector<>(this.bucketMaxSize);
    newVect.root = copiedLast;
    newVect.size = this.size;
    return newVect;
  }

  public Vector<T> add(int index, T element) {
    if (index < 0 || index > this.size) {
      throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUNDS_MESSAGE + index);
    }
    if (element == null) {
      throw new IllegalArgumentException("Element is null");
    }

    Vector<T> newVect = new Vector<>(this.bucketMaxSize);
    if (this.root == null) {
      newVect.root = Node.singleton(element, this.bucketMaxSize);
      newVect.size = 1;
    } else {
      newVect.root = this.insertAt(element, index);
      newVect.size = this.size + 1;
    }
    return newVect;
  }

  public Vector<T> removeAtIndex(int index) {
    if (index < 0 || index >= this.size) {
      throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUNDS_MESSAGE + index);
    }

    Vector<T> newVect = new Vector<>(this.bucketMaxSize);

    newVect.root = this.removeAt(index);
    newVect.size = this.size - 1;
    return newVect;
  }

  public int indexOf(Object o) {
    if (this.size == 0) {
      return -1;
    }
    return this.findIndex(o, this.root, 0);
  }


  public String toString() {
    return this.root.toString();
  }


  /** Starting at root we cumulate counts at successive subtrees
   * until we breach value of offset. Then we back up by one
   * and subtract found sum from offset.
   *
   * Then at found index we drill down to given subtree and
   * repeat same procedure until leaf is reached.
   *
   * When in leaf value is inserted at reduced offset, it is
   * inserted to copy. If maximum capacity has been reached
   * the leaf is split in two and right half carried to upper level.
   *
   * At upper node right half is inserted at corresponding position
   * and node (as it is affected copied). Again if overflow splitting
   * repeat until root is reached.
   *
   * @param value element to be inserted
   * @param offset position to insert at
   * @return new root
   */
  private Node insertAt(T value, int offset) {
    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();
    Node cursor = this.root;
    Node lastCopied;

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

    lastCopied = Node.copy(cursor);
    this.insertToArray(lastCopied.values, value, offset, lastCopied.top);
    lastCopied.top++;

    Node carry = null;
    if (lastCopied.top > this.bucketMaxSize) {
      carry = this.expandLeafs(lastCopied);
    }

    while (!visitedStack.isEmpty()) {
      cursor = visitedStack.pollLast();
      int index = visitedIndicesStack.pollLast();

      Node copied = Node.copy(cursor);

      copied.counts[index] = lastCopied.getTotalCount();
      copied.links[index] = lastCopied;

      if (carry != null) {
        this.insertToArray(copied.links, carry, index + 1, copied.top);
        this.insertToArray(copied.counts, carry.getTotalCount(), index + 1,
            copied.top);
        copied.top++;

        if (copied.top > this.bucketMaxSize) {
          carry = expandLinks(copied);
        } else {
          carry = null;
        }
      }
      lastCopied = copied;
    }

    if (carry != null) {
      Object[] newLinks = new Object[bucketMaxSize + 1];
      newLinks[0] = lastCopied;
      newLinks[1] = carry;
      return Node.createNode(newLinks, 2);
    } else {
      return lastCopied;
    }
  }


  /** First drill down to leaf noting visited nodes and their indices.
   * Then delete from copy and roll up popping values from stack. At
   * each level if affected node and its neighbour are less than half
   * of maximum capacity merge them in one node. Repeat procedure while
   * copying affected nodes up to the root.
   *
   * @param offset position to remove
   * @return new root
   */
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

    lastCopied = Node.copy(cursor);

    System
        .arraycopy(lastCopied.values, offset + 1, lastCopied.values, offset,
            lastCopied.top - 1 - offset);
    lastCopied.top--;

    while (!visitedStack.isEmpty()) {
      cursor = visitedStack.pollLast();
      int index = visitedIndicesStack.pollLast();

      Node cloned = Node.copy(cursor);

      cloned.counts[index] = lastCopied.getTotalCount();
      cloned.links[index] = lastCopied;

      if (index >= 1
          && ((Node) cloned.links[index - 1]).top
          + ((Node) cloned.links[index]).top
          <= this.bucketMaxSize / 2
      ) {
        Object[] mergedArr = new Object[bucketMaxSize + 1];
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
      Object[] mergedArr) {
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
    Object[] newArray = new Object[bucketMaxSize + 1];

    System.arraycopy(toBeExpanded.values, toBeExpanded.top / 2, newArray, 0,
        toBeExpanded.top - toBeExpanded.top / 2);

    int prevCount = toBeExpanded.top;
    toBeExpanded.top /= 2;

    return Node.createLeaf(newArray, prevCount - prevCount / 2);
  }

  private Node expandLinks(Node toBeExpanded) {
    Object[] newArray = new Object[bucketMaxSize + 1];

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
