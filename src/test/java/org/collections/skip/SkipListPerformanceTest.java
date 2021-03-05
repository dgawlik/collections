package org.collections.skip;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class SkipListPerformanceTest {

  private final Random rnd = new Random(1);

  @Test
  public void testAddition() {
    TreeSet<Integer> treeSet = new TreeSet<>();
    SkipList<Integer> skipList = new SkipList<>();

    List<Integer> elements = new ArrayList<>();
    for (int i = 0; i < 1_000_000; i++) {
      elements.add(this.rnd.nextInt());
    }

    var tsStart = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      treeSet.add(elements.get(i));
    }
    var tsEnd = System.currentTimeMillis();

    var slStart = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      skipList.add(elements.get(i));
    }
    var slEnd = System.currentTimeMillis();

    System.out.println(
        "Addition: TreeSet=" + (tsEnd - tsStart) + "ms, SkipList=" + (slEnd
            - slStart) + "ms");
  }

  @Test
  public void testRemoval() {
    TreeSet<Integer> treeSet = new TreeSet<>();
    SkipList<Integer> skipList = new SkipList<>();

    List<Integer> elements = new ArrayList<>();
    for (int i = 0; i < 1_000_000; i++) {
      elements.add(this.rnd.nextInt());
      treeSet.add(elements.get(i));
      skipList.add(elements.get(i));
    }

    var tsStart = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      treeSet.remove(elements.get(i));
    }
    var tsEnd = System.currentTimeMillis();

    var slStart = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      skipList.remove(elements.get(i));
    }
    var slEnd = System.currentTimeMillis();

    System.out.println(
        "Removal: TreeSet=" + (tsEnd - tsStart) + "ms, SkipList=" + (slEnd
            - slStart) + "ms");
  }

}
