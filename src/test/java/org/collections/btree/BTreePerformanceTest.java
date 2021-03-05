package org.collections.btree;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

class BTreePerformanceTest {

  private Random rnd = new Random(1);


  public static void main(String[] args) {
    var perf = new BTreePerformanceTest();
//    perf.vs_treeset();
//    perf.vs_timsort();
    perf.vs_timsort_sorted();
  }


  public void vs_treeset() {
    BTree<Integer> vct = new BTree<>(64);
    TreeSet<Integer> reference = new TreeSet<>();

    int n = 4_000_000;
    int[] arr = new int[n];
    for (int i = 0; i < n; i++) {
      arr[i] = rnd.nextInt(100_000_000);
    }

    long sutStart = System.currentTimeMillis();
    for (Integer i : arr) {
      vct.add(i);
    }
    long sutEnd = System.currentTimeMillis();

    long refStart = System.currentTimeMillis();
    for (Integer i : arr) {
      reference.add(i);
    }
    long refEnd = System.currentTimeMillis();

    System.out.println("TreeSet=" + (refEnd - refStart) + "ms");
    System.out.println("SortedVector=" + (sutEnd - sutStart) + "ms");
  }

  public void vs_timsort() {
    BTree<Integer> vct = new BTree<>(64);

    int n = 4_000_000;
    Integer[] arr = new Integer[n];
    for (int i = 0; i < n; i++) {
      arr[i] = rnd.nextInt(100_000_000);
    }

    long sutStart = System.currentTimeMillis();
    vct.addAll(Arrays.asList(arr));
    Integer[] sorted = vct.toArray(new Integer[0]);
    long sutEnd = System.currentTimeMillis();

    long refStart = System.currentTimeMillis();
    Arrays.sort(arr);
    long refEnd = System.currentTimeMillis();

    System.out.println("Timsort=" + (refEnd - refStart) + "ms");
    System.out.println("SortedVector=" + (sutEnd - sutStart) + "ms");
  }


  public void vs_timsort_sorted() {
    BTree<Integer> vct = new BTree<>(128);

    int n = 4_000_000;
    Integer[] arr = new Integer[n];
    for (int i = 0; i < n; i++) {
      arr[i] = i;
    }

    long sutStart = System.currentTimeMillis();
    vct.addAll(Arrays.asList(arr));
    Integer[] sorted = vct.toArray(new Integer[0]);
    long sutEnd = System.currentTimeMillis();

    long refStart = System.currentTimeMillis();
    Arrays.sort(arr);
    long refEnd = System.currentTimeMillis();

    System.out.println("Timsort=" + (refEnd - refStart) + "ms");
    System.out.println("SortedVector=" + (sutEnd - sutStart) + "ms");
  }
}