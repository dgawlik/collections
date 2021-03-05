package org.collections.btree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BTreeArrayTest {

  @Test
  public void add_elements() {
    var vct = new BTreeArray<Integer>(4);
    var arr = new Integer[10];
    for (int i = 0; i < 10; i++) {
      vct.add(i);
      arr[i] = i;
    }

    assertArrayEquals(arr, vct.toArray(new Integer[0]));
  }

  @Test
  public void remove_first() {
    var vct = new BTreeArray<Integer>(4);
    var arr = new ArrayList<Integer>();
    for (int i = 0; i < 10; i++) {
      vct.add(i);
      arr.add(i);
    }

    arr.remove(0);
    vct.remove(0);

    System.out.println(vct);
    assertArrayEquals(arr.toArray(new Integer[0]), vct.toArray(new Integer[0]));
  }

  @Test
  public void remove_last() {
    var vct = new BTreeArray<Integer>(4);
    var arr = new ArrayList<Integer>();
    for (int i = 0; i < 10; i++) {
      vct.add(i);
      arr.add(i);
    }

    arr.remove(9);
    vct.remove(9);

    System.out.println(vct);
    assertArrayEquals(arr.toArray(new Integer[0]), vct.toArray(new Integer[0]));
  }

  @Test
  public void chaos_test() {
    var vct = new BTreeArray<Integer>(4);
    var arr = new ArrayList<Integer>();

    var rnd = new Random(1);

    for (int i = 0; i < 20; i++) {
      vct.add(i);
      arr.add(i);
    }

    for (int i = 0; i < 100; i++) {
      int size = arr.size();
      int off = rnd.nextInt(size);
      if (rnd.nextDouble() < .5) {
        var val = rnd.nextInt(100);
        arr.add(off, val);
        vct.add(off, val);
      } else {
        arr.remove(off);
        vct.remove(off);
      }
    }

    assertArrayEquals(arr.toArray(new Integer[0]), vct.toArray(new Integer[0]));
  }

  @Test
  public void contains(){
    var vct = new BTreeArray<Integer>(4);
    for (int i = 0; i < 10; i++) {
      vct.add(i);
    }

    var res = vct.contains(5);
    assertTrue(res);
  }

  @Test
  public void index_of(){
    var vct = new BTreeArray<Integer>(4);
    for (int i = 0; i < 10; i++) {
      vct.add(i);
    }

    var res = vct.indexOf(5);
    assertEquals(5,res);
  }

  @Test
  public void insert_first() {
    var vct = new BTreeArray<Integer>(16);
    vct.add(1);
    vct.add(2);
    vct.add(0, 3);

    assertEquals("[3,1,2,]", vct.toString());
  }

}