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
@SuppressWarnings("unchecked")
public class BTree<T extends Comparable<? super T>> implements
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

    public Node(Object[] array, int top, boolean isLeaf, int BUCKET_MAX_SIZE) {
      this.isLeaf = isLeaf;
      this.top = top;

      if (isLeaf) {
        this.pivots = array;
      } else {
        this.links = array;
        this.pivots = new Object[BUCKET_MAX_SIZE + 1];
        for (int i = 0; i < top; i++) {
          this.pivots[i] = ((Node) this.links[i]).pivots[0];
        }
      }
    }

    public static Node createLeaf(Object[] values, int top, int BUCKET_MAX_SIZE) {
      return new Node(values, top, true, BUCKET_MAX_SIZE);
    }

    public static Node createNode(Object[] links, int top, int BUCKET_MAX_SIZE) {
      return new Node(links, top, false, BUCKET_MAX_SIZE);
    }

    public static Node createSingleton(Object value, int BUCKET_MAX_SIZE) {
      Object[] arr = new Object[BUCKET_MAX_SIZE + 1];
      arr[0] = value;
      return new Node(arr, 1, true, BUCKET_MAX_SIZE);
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

  private Node root;

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
    T[] arr = (T[])Array.newInstance(this.root.pivots[0].getClass(), this.size);
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
      this.root = Node.createSingleton(value, BUCKET_MAX_SIZE);
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

    Node carry = insertGrowLeafsAtBottom(value, visitedStack,
        visitedIndicesStack, drillDownNode);

    bubbleUpGrowLinks(visitedStack, visitedIndicesStack, carry);
  }

  private Node insertGrowLeafsAtBottom(T value, LinkedList<Node> stack,
      LinkedList<Integer> stackInd, Node currentNode) {
    while (!currentNode.isLeaf) {
      int foundIndex = this
          .searchNode(currentNode.pivots, value, currentNode.top - 1);
      stack.offerLast(currentNode);
      stackInd.offerLast(foundIndex);
      currentNode = (Node) currentNode.links[foundIndex];
    }

    int leafFoundIndex = this
        .searchLeaf(currentNode.pivots, value, currentNode.top - 1);

    //index must be on left of first greater value
    if (this.comparator.compare(value, (T) currentNode.pivots[leafFoundIndex])
        > 0) {
      leafFoundIndex++;
    }

    this.insertInArray(currentNode.pivots, value, leafFoundIndex, currentNode.top);
    currentNode.top++;

    if (currentNode.top > BUCKET_MAX_SIZE) {
      return growPivots(currentNode);
    } else {
      return null;
    }
  }

  private void bubbleUpGrowLinks(LinkedList<Node> stack,
      LinkedList<Integer> stackInd, Node insertedOnTheBottom) {

    Node rollUpNode;
    Node insertedOnCurrentLevel = insertedOnTheBottom;

    while (!stack.isEmpty() && insertedOnCurrentLevel != null) {
      rollUpNode = stack.pollLast();
      @SuppressWarnings("ConstantConditions") int rollUpNodeIndex = stackInd.pollLast();

      this.insertInArray(rollUpNode.links, insertedOnCurrentLevel, rollUpNodeIndex + 1,
          rollUpNode.top);
      this.insertInArray(rollUpNode.pivots, insertedOnCurrentLevel.pivots[0],
          rollUpNodeIndex + 1, rollUpNode.top);

      rollUpNode.top++;
      if (rollUpNode.top > BUCKET_MAX_SIZE) {
        insertedOnCurrentLevel = growLinks(rollUpNode);
      } else {
        insertedOnCurrentLevel = null;
      }
    }

    if (insertedOnCurrentLevel != null) {
      Object[] newLinks = new Object[BUCKET_MAX_SIZE + 1];
      newLinks[0] = this.root;
      newLinks[1] = insertedOnCurrentLevel;
      this.root = Node.createNode(newLinks, 2, this.BUCKET_MAX_SIZE);
    }
  }

  private boolean delete(T value) {
    Node drillDownNode = this.root;

    LinkedList<Node> visitedStack = new LinkedList<>();
    LinkedList<Integer> visitedIndicesStack = new LinkedList<>();

    boolean found = deleteFromLeaf(value, drillDownNode, visitedStack,
        visitedIndicesStack);

    rollUpMergingSparseNodes(visitedStack, visitedIndicesStack);

    return found;
  }

  private boolean deleteFromLeaf(T value, Node currentNode,
      LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack) {

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
        this.comparator.compare(value, (T) currentNode.pivots[leafFoundAtIndex])
            < 0) {
      leafFoundAtIndex--;
    }

    if (this.comparator.compare(value, (T) currentNode.pivots[leafFoundAtIndex])
        == 0) {
      System
          .arraycopy(currentNode.pivots, leafFoundAtIndex + 1,
              currentNode.pivots, leafFoundAtIndex,
              currentNode.top - 1 - leafFoundAtIndex);

      currentNode.top--;
      return true;
    } else {
      return false;
    }
  }

  private void rollUpMergingSparseNodes(LinkedList<Node> visitedStack,
      LinkedList<Integer> visitedIndicesStack) {
    Node rollUpNode;
    while (!visitedStack.isEmpty()) {
      rollUpNode = visitedStack.pollLast();
      @SuppressWarnings("ConstantConditions") int ind = visitedIndicesStack.pollLast();

      rollUpNode.pivots[ind] = ((Node)rollUpNode.links[ind]).pivots[0];

      if (ind >= 1
          && ((Node)rollUpNode.links[ind - 1]).top + ((Node)rollUpNode.links[ind]).top
          < BUCKET_MAX_SIZE / 2) {
        if (((Node)rollUpNode.links[ind]).isLeaf) {
          Object[] mergedArr = new Object[BUCKET_MAX_SIZE + 1];

          int ind2 = shrinkPivots(rollUpNode, ind, mergedArr);
          insertMergeNode(rollUpNode, ind, mergedArr, ind2);
        } else {
          Object[] mergedArr = new Object[BUCKET_MAX_SIZE + 1];
          int ind2 = shrinkLinks(rollUpNode, ind, mergedArr);
          insertMergeNode(rollUpNode, ind, mergedArr, ind2);
        }
      }
    }
  }

  private int shrinkPivots(Node it, int ind, Object[] mergedArr) {
    int ind2 = 0;
    System.arraycopy(((Node)it.links[ind - 1]).pivots, 0, mergedArr, 0,
        ((Node)it.links[ind - 1]).top);
    ind2 += ((Node)it.links[ind - 1]).top;
    System.arraycopy(((Node)it.links[ind]).pivots, 0, mergedArr, ind2,
        ((Node)it.links[ind]).top);
    ind2 += ((Node)it.links[ind]).top;
    return ind2;
  }

  private int shrinkLinks(Node it, int ind, Object[] mergedArr) {
    int ind2 = 0;
    System.arraycopy(((Node)it.links[ind - 1]).links, 0, mergedArr, 0,
        ((Node)it.links[ind - 1]).top);
    ind2 += ((Node)it.links[ind - 1]).top;
    System.arraycopy(((Node)it.links[ind]).links, 0, mergedArr, ind2,
        ((Node)it.links[ind]).top);
    ind2 += ((Node)it.links[ind]).top;
    return ind2;
  }

  private void insertMergeNode(Node it, int ind, Object[] mergedArr,
      int ind2) {
    Node mergedN;
    if (((Node)it.links[0]).isLeaf) {
      mergedN = Node.createLeaf(mergedArr, ind2, this.BUCKET_MAX_SIZE);
    } else {
      mergedN = Node.createNode(mergedArr, ind2, this.BUCKET_MAX_SIZE);
    }

    System
        .arraycopy(it.links, ind + 1, it.links, ind, it.top - 1 - ind);
    System
        .arraycopy(it.pivots, ind + 1, it.pivots, ind, it.top - 1 - ind);
    it.top--;
    it.links[ind - 1] = mergedN;
    it.pivots[ind - 1] = mergedN.pivots[0];
  }

  /** Splits node's pivots in half and returns new node
   * with right half
   *
   * @param it node under operations
   * @return new node to be inserted
   */
  private Node growPivots(Node it) {
    Object[] rightArr =  new Object[BUCKET_MAX_SIZE + 1];

    System.arraycopy(it.pivots, it.top / 2, rightArr, 0,
        it.top - it.top / 2);

    int prevTop = it.top;
    it.top /= 2;

    return Node.createLeaf(rightArr, prevTop - prevTop / 2, this.BUCKET_MAX_SIZE);
  }

  /** Splits node's links in half and returns new node
   * with right half
   *
   * @param it node under operations
   * @return new node to be inserted
   */
  private Node growLinks(Node it) {
    Object[] rightArr = new Object[BUCKET_MAX_SIZE + 1];

    System.arraycopy(it.links, it.top / 2, rightArr, 0,
        it.top - it.top / 2);

    int prevTop = it.top;
    it.top /= 2;

    return Node.createNode(rightArr, prevTop - prevTop / 2, this.BUCKET_MAX_SIZE);
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
    int checkpoint, nextCheckpoint, sign;
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
    int checkpoint, nextCheckpoint, sign;
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
