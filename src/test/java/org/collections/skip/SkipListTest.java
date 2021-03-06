package org.collections.skip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SkipListTest {

  @Test
  void insertion() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    sklst.add(5);
    sklst.add(3);
    sklst.add(1);
    sklst.add(2);
    sklst.add(2);

    System.out.println(sklst);
    Assertions.assertArrayEquals(new Integer[]{1, 2, 2, 3, 5},
        sklst.toArray(new Integer[0]));
  }

  @Test
  void insertionBig() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    var arr = new Integer[10];
    var rnd = new Random(1);
    for (int i = 0; i < 10; i++) {
      var rand = rnd.nextInt(10);
      arr[i] = rand;
      sklst.add(rand);
    }

    var res = sklst.toArray(new Integer[0]);
    Arrays.sort(arr);

    System.out.println(sklst);
    Assertions.assertArrayEquals(arr, res);
  }

  @Test
  void deletion() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    sklst.add(5);
    sklst.add(3);
    sklst.add(1);
    sklst.add(2);
    sklst.add(2);
    sklst.remove(2);
    sklst.remove(5);
    sklst.remove(1);

    System.out.println(sklst);
    Assertions.assertArrayEquals(new Integer[]{2, 3},
        sklst.toArray(new Integer[0]));
  }

  @Test
  void deletionBig() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    var arr = new ArrayList<Integer>();
    var rnd = new Random(1);
    for (int i = 0; i < 10; i++) {
      var rand = rnd.nextInt(10);
      arr.add(rand);
      sklst.add(rand);
    }
    for (int i = 0; i < 5; i++) {
      var rand = rnd.nextInt(10);
      sklst.remove(rand);
      if (arr.contains(rand)) {
        arr.remove(arr.indexOf(rand));
      }
    }

    var res = sklst.toArray(new Integer[0]);
    var arr2 = arr.toArray(new Integer[0]);
    Arrays.sort(arr2);

    System.out.println(sklst);
    Assertions.assertArrayEquals(arr2, res);
  }

  @Test
  void retainAll() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    sklst.add(5);
    sklst.add(3);
    sklst.add(1);
    sklst.add(2);
    sklst.add(2);

    sklst.retainAll(List.of(2, 2));

    System.out.println(sklst);
    Assertions.assertArrayEquals(new Integer[]{2, 2},
        sklst.toArray(new Integer[0]));
  }

  @Test
  void containsAll() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    sklst.add(5);
    sklst.add(3);
    sklst.add(1);
    sklst.add(2);
    sklst.add(2);

    var result = sklst.containsAll(List.of(2, 3, 5));

    System.out.println(sklst);
    Assertions.assertTrue(result);
  }

  @Test
  void removeAll() {
    var sklst = new SkipList<Integer>(Comparator.naturalOrder());

    sklst.add(5);
    sklst.add(3);
    sklst.add(1);
    sklst.add(2);
    sklst.add(2);

    sklst.removeAll(List.of(3, 2, 2));

    System.out.println(sklst);
    Assertions.assertArrayEquals(new Integer[]{1, 5},
        sklst.toArray(new Integer[0]));
  }

  @Test
  void test_iterator() {
    SkipList<Integer> lst = new SkipList<>();
    lst.add(1);
    lst.add(2);
    lst.add(3);

    int[] arr = new int[3];
    int index = 0;
    for (int i : lst) {
      arr[index++] = i;
    }

    Assertions.assertArrayEquals(new int[]{1, 2, 3}, arr);
  }

  @Test
  void test_first(){
    var lst = new SkipList<Integer>();

    lst.add(1);
    lst.add(2);

    Assertions.assertEquals(1, lst.first());
  }

  @Test
  void test_last(){
    var lst = new SkipList<Integer>();

    lst.add(1);
    lst.add(2);

    Assertions.assertEquals(2, lst.last());
  }
}