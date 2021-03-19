package org.collections.btree;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VectorTest {

  @Test
  public void insertion_test(){
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
  public void removal_test(){
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
  public void immutable_test(){
    Vector<Integer> vect = new Vector<>(4);

    vect = vect.add(1);
    vect = vect.add(2);

    vect.set(0, 0);

    Assertions.assertEquals(1, vect.get(0));
  }

  @Test
  public void setter_test(){
    Vector<Integer> vect = new Vector<>(4);
    vect = vect.add(1);
    vect = vect.add(2);

    vect = vect.set(0, 3);
    vect = vect.set(1, 4);

    Assertions.assertEquals(3, vect.get(0));
    Assertions.assertEquals(4, vect.get(1));
  }
}