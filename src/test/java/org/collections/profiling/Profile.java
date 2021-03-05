package org.collections.profiling;

import org.collections.btree.BTree;
import java.util.Random;

public class Profile {
  static Random rnd = new Random(1);

  public static void main(String... args){
    int n = 4_000_000;
    int[] arr = new int[n];
    for (int i = 0; i < n; i++) {
//      arr[i] = rnd.nextInt(100_000_000);
      arr[i] = i;
    }

    BTree<Integer> vct = new BTree<>(64);
    for (Integer i : arr) {
      vct.add(i);
    }
  }
}
