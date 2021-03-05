# collections

![photo](photo.jpg "Photo")


### Skip List

A sorted multiset.

Operation | Performance
---|---
Insert | O(logN)
Delete | O(logN)
Search | O(logN)

### BTree

A sorted multiset.
Exponential search within the bucket.

Operation | Performance
---|---
Insert | O(logN)
Delete | O(logN)
Search | O(logN)

### Persistent Vector

Indexable BTree.
Mutable but easy to convert to immutable.

Operation | Performance
---|---
Insert | ~O(1)
Delete | ~O(1)
Search | O(N)
Index | ~O(1)
