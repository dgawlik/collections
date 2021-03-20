package org.collections.btree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class BTreeTest {

  private Random rnd = new Random(1);

  @Test
  void insert_random_numbers() {
    BTree<Integer> vct = new BTree<>(4);
    Integer[] arr = this.prepare(80, 50);
    for (Integer i : arr) {
      vct.add(i);
    }
    Arrays.sort(arr);
    System.out.println(vct);
    assertArrayEquals(arr, vct.toArray(new Integer[0]));
  }

  @Test
  void delete_random_numbers() {
    TreeSet<Integer> reference = new TreeSet<>();
    BTree<Integer> sut = new BTree<>(4);

    for (int i=0;i<12;i++) {
      reference.add(i);
      sut.add(i);
    }

    System.out.println(sut);
    for (int i=0;i<12;i+=2) {
      reference.remove(i);
      sut.remove(i);
    }

    System.out.println(sut);
    for (Object o : reference.toArray()) {
      System.out.print(o + " ");
    }
    assertArrayEquals(reference.toArray(), sut.toArray());
  }

  @Test
  void unable_to_remove_from_empty(){
    BTree<Integer> vct = new BTree<>(16);
    vct.add(1);
    vct.add(2);
    vct.remove(1);
    vct.remove(2);
    var res = vct.remove(2);
    assertFalse(res);
  }

  @Test
  void contains_is_correct(){
    BTree<Integer> vct = new BTree<>(16);
    vct.add(1);
    vct.add(2);

    var res1 = vct.contains(1);
    assertTrue(res1);
    var res2 = vct.contains(3);
    assertFalse(res2);
  }

  @Test
  void iterator(){
    BTree<Integer> vct = new BTree<>(16);
    vct.add(1);
    vct.add(2);

    StringBuilder sb = new StringBuilder();
    for(Integer i : vct){
      sb.append(i).append(",");
    }

    assertEquals("1,2,", sb.toString());
  }

  private Integer[] prepare(int size, int max) {
    Integer[] arr = new Integer[size];
    for (int i = 0; i < size; i++) {
      arr[i] = this.rnd.nextInt(max);
    }
    return arr;
  }
}