package org.collections.btree;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class BTreeArrayPerformanceTest {


  @Test
  public void append() {
    final ArrayList<Integer> arr = new ArrayList<>();
    final BTreeArray<Integer> vct = new BTreeArray<>(255);
    measure(() -> {
      for (int i = 0; i < 1_000_000; i++) {
        arr.add(i);
      }
      return null;
    }, () -> {
      for (int i = 0; i < 1_000_000; i++) {
        vct.add(i);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  @Test
  public void insert_in_the_middle() {
    final ArrayList<Integer> arr = new ArrayList<>();
    final BTreeArray<Integer> vct = new BTreeArray<>(255);
    measure(() -> {
      for (int i = 0; i < 1_00_000; i++) {
        arr.add(i / 2, i);
      }
      return null;
    }, () -> {
      for (int i = 0; i < 1_00_000; i++) {
        vct.add(i / 2, i);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  @Test
  public void pop_first() {
    final ArrayList<Integer> arr = new ArrayList<>();
    final BTreeArray<Integer> vct = new BTreeArray<>(255);

    for (int i = 0; i < 1_00_000; i++) {
      arr.add(i);
      vct.add(i);
    }

    measure(() -> {
      for(int i=0;i< 1_00_000;i++){
        arr.remove(0);
      }
      return null;
    }, () -> {
      for(int i=0;i< 1_00_000;i++){
        vct.remove(0);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  @Test
  public void index_of() {
    final ArrayList<Integer> arr = new ArrayList<>();
    final BTreeArray<Integer> vct = new BTreeArray<>(255);

    for (int i = 0; i < 1_00_000; i++) {
      arr.add(i);
      vct.add(i);
    }

    measure(() -> {
      for(int i=0;i< 1_000;i++){
        arr.indexOf(arr.get(arr.size()-1));
      }
      return null;
    }, () -> {
      for(int i=0;i< 1_000;i++){
       vct.indexOf(vct.get(vct.size()-1));
      }
      return null;
    }, "ArrayList", "Vector");
  }

  @Test
  public void get_set() {
    final ArrayList<Integer> arr = new ArrayList<>();
    final BTreeArray<Integer> vct = new BTreeArray<>(255);

    for (int i = 0; i < 1_000_000; i++) {
      arr.add(i);
      vct.add(i);
    }

    measure(() -> {
      for(int i=0;i< 1_000_000;i++){
        arr.set(i/2, arr.get(i/2)+1);
      }
      return null;
    }, () -> {
      for(int i=0;i< 1_000_000;i++){
        vct.set(i/2, vct.get(i/2)+1);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public void measure(Supplier<Void> func1, Supplier<Void> func2, String name1,
      String name2) {
    long start = System.currentTimeMillis();
    func1.get();
    long end = System.currentTimeMillis();

    long start2 = System.currentTimeMillis();
    func2.get();
    long end2 = System.currentTimeMillis();

    System.out.println(name1 + ": " + (end - start) + " ms");
    System.out.println(name2 + ": " + (end2 - start2) + " ms");
    System.out.println("---");
  }
}