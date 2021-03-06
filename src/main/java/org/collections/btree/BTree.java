package org.collections.btree;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.NoSuchElementException;


/**
 * Sorted multiset implemented as tree with high branching factor.
 * Number of branches in nodes is configurable.
 *
 * Each bucket holds sorted values and children are values nested between
 * consecutive two values. So children of top level nodes form sorted ranges
 * to which inserted | deleted values fall, all the way down to leaf where
 * instead of ranges there are plain elements.
 *
 * Invariants:
 *
 * (1) forall i: node.pivots[i] <= node.pivots[i+1]
 * (2) all nodes have at least one child
 * (3) forall i: node.pivots[i] <= node.links[i].pivots[0..n] <= node.pivots.[i+1]
 *
 * D - depth, B - branching factor
 * Insertion: O(D*B) ~ O(1)
 * Removal: O(D*B) ~ O(1)
 * Contains: O(D*logB) ~ O(1)
 *
 * @param <T> type that this collection holds
 */
@SuppressWarnings("unchecked")
public class BTree<T extends Comparable<T>> implements
    org.collections.Multiset<T> {

  @SuppressWarnings("unchecked")
  private static class Iterator<T> implements java.util.Iterator<T> {

    private final Object[] arr;
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
      if(this.top >= this.arr.length){
        throw new NoSuchElementException("Iterator reached the end.");
      }
      return (T) this.arr[this.top++];
    }
  }

  /** BTree single node can be leaf or internal.
   */
  private static class Node {

    //if leaf holds sorted values
    //and if node pivots[i] holds minimum value
    //held by subtree at links[i]
    Object[] pivots;

    //links to subtrees if leaf this field is null
    Object[] links;

    //is leaf or node?
    boolean isLeaf;

    //upper exclusive bound of elements contained
    //in pivots and links
    int top;

    public Node(Object[] array, int top, boolean isLeaf, int bucketMaxSize) {
      this.isLeaf = isLeaf;
      this.top = top;

      if (isLeaf) {
        this.pivots = array;
      } else {
        this.links = array;
        this.pivots = new Object[bucketMaxSize + 1];
        for (int i = 0; i < top; i++) {
          this.pivots[i] = ((Node) this.links[i]).pivots[0];
        }
      }
    }

    public static Node createLeaf(Object[] values, int top,
        int bucketMaxSize) {
      return new Node(values, top, true, bucketMaxSize);
    }

    public static Node createNode(Object[] links, int top,
        int bucketMaxSize) {
      return new Node(links, top, false, bucketMaxSize);
    }

    public static Node createSingleton(Object value, int bucketMaxSize) {
      Object[] arr = new Object[bucketMaxSize + 1];
      arr[0] = value;
      return new Node(arr, 1, true, bucketMaxSize);
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

  private final int bucketMaxSize;

  private Node root;

  private int size;


  public BTree(Comparator<T> comparator, int bucketMaxSize) {
    this.comparator = comparator;
    this.size = 0;
    this.bucketMaxSize = bucketMaxSize;
  }

  public BTree(int bucketMaxSize) {
    this(Comparator.naturalOrder(), bucketMaxSize);
  }

  @Override
  public Comparator<T> comparator() {
    return this.comparator;
  }


  @Override
  public T first() {
    var node = this.root;
    while (!node.isLeaf) {
      node = (Node) node.links[0];
    }
    return (T) node.pivots[0];
  }

  @Override
  public T last() {
    var node = this.root;
    while (!node.isLeaf) {
      node = (Node) node.links[node.top - 1];
    }
    return (T) node.pivots[node.top - 1];
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
    Node drillDownNode = root;
    while (!drillDownNode.isLeaf) {
      int atIndex = this
          .searchNode(drillDownNode.pivots, (T) value, drillDownNode.top - 1);
      drillDownNode = (Node) drillDownNode.links[atIndex];
    }
    int atPivotIndex = this
        .searchLeaf(drillDownNode.pivots, (T) value, drillDownNode.top - 1);
    return this.comparator
        .compare((T) value, (T) drillDownNode.pivots[atPivotIndex]) == 0;
  }


  @Override
  public Iterator<T> iterator() {
    T[] arr = (T[]) Array
        .newInstance(this.root.pivots[0].getClass(), this.size);
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
      this.root = Node.createSingleton(value, bucketMaxSize);
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
    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();
    Node drillDownNode = this.root;

    Node carry = insertGrowLeavesAtBottom(value, visitedStack,
        visitedIndicesStack, drillDownNode);

    rollUpGrowingLinks(visitedStack, visitedIndicesStack, carry);
  }

  /** Top down matching bucket is found by checking range [min(bi), min(bi+1)
   * and then when procedure reaches the leaf value is inserted, optionally
   * overflown node is split
   *
   * @param value to be inserted
   * @param visitedStack matching buckets on each level
   * @param visitedIndicesStack corresponding indices
   * @param root top level bucket
   * @return new node after split or null
   */
  private Node insertGrowLeavesAtBottom(T value, LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack, Node root) {

    Node currentNode = root;
    while (!currentNode.isLeaf) {
      int foundIndex = this
          .searchNode(currentNode.pivots, value, currentNode.top - 1);
      visitedStack.offerLast(currentNode);
      visitedIndicesStack.offerLast(foundIndex);
      currentNode = (Node) currentNode.links[foundIndex];
    }

    int leafFoundIndex = this
        .searchLeaf(currentNode.pivots, value, currentNode.top - 1);

    //index must be on left of first greater value
    if (this.comparator.compare(value, (T) currentNode.pivots[leafFoundIndex])
        > 0) {
      leafFoundIndex++;
    }

    this.insertInArray(currentNode.pivots, value, leafFoundIndex,
        currentNode.top);
    currentNode.top++;

    if (currentNode.top > bucketMaxSize) {
      return expand(currentNode, true);
    } else {
      return null;
    }
  }

  /** During insertion value has been added to particular leaf.
   * If count > max branching factor node is split into two
   * and right node is passed to upper level.
   *
   * @param visitedStack stack to be consumed on way up
   * @param visitedIndicesStack corresponding indices
   * @param insertedOnTheBottom node inserted on previous level
   */
  private void rollUpGrowingLinks(LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack, Node insertedOnTheBottom) {

    Node rollUpNode;
    Node insertedOnCurrentLevel = insertedOnTheBottom;

    while (!visitedStack.isEmpty() && insertedOnCurrentLevel != null) {
      rollUpNode = visitedStack.pollLast();
      @SuppressWarnings("ConstantConditions") int rollUpNodeIndex = visitedIndicesStack
          .pollLast();

      this.insertInArray(rollUpNode.links, insertedOnCurrentLevel,
          rollUpNodeIndex + 1, rollUpNode.top);
      this.insertInArray(rollUpNode.pivots, insertedOnCurrentLevel.pivots[0],
          rollUpNodeIndex + 1, rollUpNode.top);

      rollUpNode.top++;
      if (rollUpNode.top > bucketMaxSize) {
        insertedOnCurrentLevel = expand(rollUpNode, false);
      } else {
        insertedOnCurrentLevel = null;
      }
    }

    if (insertedOnCurrentLevel != null) {
      Object[] newLinks = new Object[bucketMaxSize + 1];
      newLinks[0] = this.root;
      newLinks[1] = insertedOnCurrentLevel;
      this.root = Node.createNode(newLinks, 2, this.bucketMaxSize);
    }
  }

  private boolean delete(T value) {
    Node drillDownNode = this.root;

    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();

    boolean found = drillDownDeleteFromLeaf(value, drillDownNode, visitedStack,
        visitedIndicesStack);

    rollUpMergingSparseNodes(visitedStack, visitedIndicesStack);

    return found;
  }

  /** If value belongs to bucket i, value < min(bucket i+1)
   * checking this procedure drills down to leaf and value
   * is deleted if present.
   *
   * @param value value to be deleted
   * @param root top level node
   * @param visitedStack stack filled by node on each level down
   * @param visitedIndicesStack corresponding indices
   * @return isDeleted
   */
  private boolean drillDownDeleteFromLeaf(T value, Node root,
      LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack) {

    Node currentNode = root;
    while (!currentNode.isLeaf) {
      int foundAtIndex = this
          .searchNode(currentNode.pivots, value, currentNode.top - 1);
      visitedStack.offerLast(currentNode);
      visitedIndicesStack.offerLast(foundAtIndex);
      currentNode = (Node) currentNode.links[foundAtIndex];
    }

    int leafFoundAtIndex = this
        .searchLeaf(currentNode.pivots, value, currentNode.top - 1);
    if (leafFoundAtIndex > 0 &&
        this.comparator.compare(
            value,
            (T) currentNode.pivots[leafFoundAtIndex]) < 0) {

      leafFoundAtIndex--;
    }

    if (this.comparator.compare(
        value,
        (T) currentNode.pivots[leafFoundAtIndex]) == 0) {

      Object[] array = currentNode.pivots;
      @SuppressWarnings("UnnecessaryLocalVariable") int i = leafFoundAtIndex;
      System.arraycopy(array, i + 1, array, i, currentNode.top - 1 - i);

      currentNode.top--;
      return true;
    } else {
      return false;
    }
  }

  /** Deleting elements makes the node children sparse.
   * If two adjacent children are less than half of max branching
   * factor they are compacted to single node, and procedure
   * repeats on higher level.
   *
   * @param visitedStack nodes visited during delete
   * @param visitedIndicesStack corresponding indices
   */
  private void rollUpMergingSparseNodes(LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack) {

    Node rollUpNode;
    while (!visitedStack.isEmpty()) {
      rollUpNode = visitedStack.pollLast();
      @SuppressWarnings("ConstantConditions") int ind = visitedIndicesStack
          .pollLast();

      rollUpNode.pivots[ind] = ((Node) rollUpNode.links[ind]).pivots[0];

      if (ind >= 1
          && ((Node) rollUpNode.links[ind - 1]).top
          + ((Node) rollUpNode.links[ind]).top
          < bucketMaxSize / 2) {

        Object[] toBeFilled = new Object[bucketMaxSize + 1];
        int newTop;
        if (hasLeaves(rollUpNode)) {
          newTop = concatAdjacentArrays(rollUpNode, ind, toBeFilled, true);
        } else {
          newTop = concatAdjacentArrays(rollUpNode, ind, toBeFilled, false);
        }
        insertCompactingNode(rollUpNode, ind, toBeFilled, newTop);
      }
    }
  }

  /** Tests if one level below there are leaves
   *
   * @param evaluated node under question
   * @return result
   */
  private boolean hasLeaves(Node evaluated) {
    return ((Node) evaluated.links[0]).isLeaf;
  }

  /** Compacts adjacent children of given node
   *
   * @param selected node whose children are being compacted
   * @param forIndex (forIndex-1, forIndex) are merged
   * @param toBeFilled new destination
   * @param usePivots are pivots or links being compacted
   * @return new Top
   */
  private int concatAdjacentArrays(Node selected, int forIndex,
      Object[] toBeFilled, boolean usePivots) {
    int filledInIndex = 0;

    int firstTop = ((Node) selected.links[forIndex - 1]).top;
    int secondTop = ((Node) selected.links[forIndex]).top;

    Object[] firstSource;
    Object[] secondSource;
    if (usePivots) {
      firstSource = ((Node) selected.links[forIndex - 1]).pivots;
      secondSource = ((Node) selected.links[forIndex]).pivots;
    } else {
      firstSource = ((Node) selected.links[forIndex - 1]).links;
      secondSource = ((Node) selected.links[forIndex]).links;
    }

    System
        .arraycopy(firstSource, 0, toBeFilled, 0, firstTop);
    filledInIndex += firstTop;

    System.arraycopy(secondSource, 0, toBeFilled, filledInIndex, secondTop);
    filledInIndex += secondTop;

    return filledInIndex;
  }


  /** Deletes two sparse nodes and inserts compact one
   *
   * @param parent node that should contain compacting node
   * @param forIndex (forIndex-1, forIndex) are compacted
   * @param compactArray new array
   * @param compactArrayTop new array top
   */
  private void insertCompactingNode(Node parent, int forIndex,
      Object[] compactArray, int compactArrayTop) {

    Node newCompactNode;
    if (hasLeaves(parent)) {
      newCompactNode = Node
          .createLeaf(compactArray, compactArrayTop, this.bucketMaxSize);
    } else {
      newCompactNode = Node
          .createNode(compactArray, compactArrayTop, this.bucketMaxSize);
    }

    //move elements [i+1..top] one hop left
    System
        .arraycopy(parent.links, forIndex + 1, parent.links, forIndex,
            parent.top - 1 - forIndex);
    System
        .arraycopy(parent.pivots, forIndex + 1, parent.pivots, forIndex,
            parent.top - 1 - forIndex);

    parent.top--;
    parent.links[forIndex - 1] = newCompactNode;
    parent.pivots[forIndex - 1] = newCompactNode.pivots[0];
  }


  /** Splits node's pivots | links in half and returns new node
   * with right half
   *
   * @param it node under operations
   * @param usePivots are pivots the subject of split
   * @return new node to be inserted
   */
  private Node expand(Node it, boolean usePivots) {
    Object[] expandedArray = new Object[bucketMaxSize + 1];
    Object[] source;
    if (usePivots) {
      source = it.pivots;
    } else {
      source = it.links;
    }

    System.arraycopy(source, it.top / 2, expandedArray, 0,
        it.top - it.top / 2);

    int prevTop = it.top;
    it.top /= 2;

    return usePivots ?
        Node.createLeaf(expandedArray, prevTop - prevTop / 2,
            this.bucketMaxSize)
        : Node.createNode(expandedArray, prevTop - prevTop / 2,
            this.bucketMaxSize);
  }

  private <U> int fillArray(U[] arr, int ind, Node node) {
    for (int i = 0; i < node.top; i++) {
      if (node.isLeaf) {
        arr[ind++] = (U) node.pivots[i];
      } else {
        ind = fillArray(arr, ind, (Node) node.links[i]);
      }
    }
    return ind;
  }

  private <U> void insertInArray(U[] arr, U value, int ind, int top) {
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
   * @return index in pivots
   */
  private int searchLeaf(Object[] arr, T value, int end) {
    int middle = end / 2;

    int offset = 1;
    int checkpoint;
    int nextCheckpoint;
    int sign;
    if (this.comparator.compare(value, (T) arr[middle]) < 0) {
      checkpoint = 0;
      nextCheckpoint = 0;
      sign = 1;
    } else {
      checkpoint = end;
      nextCheckpoint = end;
      sign = -1;
    }

    while (true) {
      if (
          (sign == 1
              ? (checkpoint + offset <= end)
              : (checkpoint - offset >= 0)
          ) && comparator.compare(
              value,
              (T) arr[checkpoint + offset * sign]
          ) * sign >= 0
      ) {
        nextCheckpoint = checkpoint + offset * sign;
        offset <<= 1;
      } else {
        checkpoint = nextCheckpoint;
        offset >>= 1;
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
   * @return index in links
   */
  private int searchNode(Object[] arr, T value, int end) {
    int middle = end / 2;
    int offset = 1;
    int checkpoint;
    int nextCheckpoint;
    int sign;
    if (this.comparator.compare(value, (T) arr[middle]) < 0) {
      checkpoint = 0;
      nextCheckpoint = 0;
      sign = 1;
    } else {
      checkpoint = end;
      nextCheckpoint = end;
      sign = -1;
    }
    while (true) {
      if (
          (sign == 1
              ? (checkpoint + offset + 1 <= end) :
              (checkpoint - offset >= 0)
          ) && (sign == 1
              ? comparator.compare(value, (T) arr[checkpoint + offset + 1]) >= 0
              : comparator.compare(value, (T) arr[checkpoint - offset + 1]) < 0)
      ) {
        nextCheckpoint = checkpoint + offset * sign;
        offset <<= 1;
      } else {
        checkpoint = nextCheckpoint;
        offset >>= 1;
      }
      if (checkpoint == nextCheckpoint) {
        return checkpoint;
      }
    }
  }
}
