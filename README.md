# collections

**Exotic data structures in Java!**

![photo](photo.jpg "Photo")

The project is collection of less known data structures. The focus has been made
on balancing performance with simplicity and ease
of understanding. I tried to document all major
parts in implementation.

Currently there are:
* Skip list
* Mutable BTree
* Immutable Persistent Vector


### Skip List

Skip lists in performance are much like binary trees,
but they offer better locality of data. They are frequently
used as substitute for binary trees when it comes to concurrency.
They are randomized datastructures.

Implementation is different from typical skip list as
each node has some number of level nodes and they point to
nodes some hops ahead. This way some space requirements has been
reduced. It is a multiset holding sorted values, implementing
java Collection interface and resembling closely interface of
TreeSet.

Operation | Performance
---|---
Insert | O(logN)
Delete | O(logN)
Search | O(logN)

### BTree

BTrees are frequently used to implement functional
data structures because they strike good balance between
tree (which is immutable) and array (which is mutable).

They are also a good way to sort large arrays. You can
find benchmarks [here](https://github.com/dgawlik/collections/blob/main/src/test/java/org/collections/btree/BTreePerformanceTest.java).
What is really interesting that similar to TimSort they are
adaptive (make use of natural order in data) so inserting ordered
values is much faster than average.

Implementation is multiset implementing Collection java interface,
and resembling SortedSet interface. The branching factor is configurable.
I found that branches=64 is optimal for ~4 000 000 elements.

Further optimization is exponential search rather than plain binary
search for intra-node searches, which makes it more robust for partially
sorted data (for which leaves are hot on borders)

Operation | Performance
---|---
Insert | O(logN)
Delete | O(logN)
Search | O(logN)

### Persistent Vector

Immutable data structure that is more performance than
CopyOnWriteArray. It's very similar to BTree except that
data is not sorted. Constant operations in table are in fact
always limited by O(depth*branching factor).

Performance is depending on branching factor. For small
branching modifications are fast at expense of reads. For big
branching factors modifications are slow.


Operation | Performance
---|---
Insert | ~O(1)
Delete | ~O(1)
Search | O(N)
Index | ~O(1)
