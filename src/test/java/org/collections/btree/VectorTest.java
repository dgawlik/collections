package org.collections.btree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VectorTest {

  @Test
  void insertion_test(){
    Vector<Integer> vect = new Vector<>(4);
    vect = vect.add(1);
    vect = vect.add(2);
    vect = vect.add(0, 3);
    vect = vect.add(vect.size()-1, 4);

    Integer[] vectArr = vect.toArray(new Integer[4]);
    Integer[] expected = new Integer[]{3,1,4,2};

    Assertions.assertArrayEquals(expected, vectArr);
  }

  @Test
  void removal_test(){
    Vector<Integer> vect = new Vector<>(4);
    for(int i=0;i<5;i++){
      vect = vect.add(i);
    }

    vect = vect.remove(0);
    vect = vect.removeAtIndex(3);

    Integer[] vectArr = vect.toArray(new Integer[3]);
    Integer[] expected = new Integer[]{1,2,3};

    System.out.println(vect.toString());
    Assertions.assertArrayEquals(expected, vectArr);
  }

  @Test
  void immutable_test(){
    Vector<Integer> vect = new Vector<>(4);

    vect = vect.add(1);
    vect = vect.add(2);

    vect.set(0, 0);

    Assertions.assertEquals(1, vect.get(0));
  }

  @Test
  void setter_test(){
    Vector<Integer> vect = new Vector<>(4);
    vect = vect.add(1);
    vect = vect.add(2);

    vect = vect.set(0, 3);
    vect = vect.set(1, 4);

    Assertions.assertEquals(3, vect.get(0));
    Assertions.assertEquals(4, vect.get(1));
  }

  @Test
  void chaos_test(){
    Vector<Integer> vect = new Vector<>(8);
    ArrayList<Integer> ref = new ArrayList<>();
    Random rnd = new Random(0);

    for(int i=0;i<100;i++){
      var r = rnd.nextInt(1000);
      vect = vect.add(r);
      ref.add(r);
    }

    for(int i=0;i<100;i++){
      var index = rnd.nextInt(100);
      var r = rnd.nextInt(1000);

      vect = vect.add(index, r);
      ref.add(index, r);
    }

    for(int i=0;i<100;i++){
      var index = rnd.nextInt(ref.size());
      vect = vect.removeAtIndex(index);
      ref.remove(index);
    }

    Integer[] expected = ref.toArray(new Integer[0]);
    Integer[] vectArr = vect.toArray(new Integer[0]);

    Assertions.assertArrayEquals(expected, vectArr);
  }


  @Test
  void test_iterator(){
    var vect = new Vector<Integer>(4);

    int[] ref = new int[20];
    int index = 0;
    for(int i=0;i<20;i++){
      ref[index++] = i;
      vect = vect.add(i);
    }

    int[] buf = new int[20];
    index = 0;
    for(int i : vect){
      buf[index++] = i;
    }

    Assertions.assertArrayEquals(ref, buf);
  }

  @Test
  void contains_all(){
    var vect = new Vector<Integer>(4);

    vect = vect.addAll(List.of(1,2,3,4));
    vect = vect.removeAll(List.of(1,2));

    System.out.println(vect);
    Assertions.assertTrue(vect.containsAll(List.of(3,4)));
    Assertions.assertEquals(2, vect.size());
  }
}