package org.collections.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import serialization.Pair;

public class SerializationSpeedTest {

  public static void main(String[] args) throws IOException {
    SerializationSpeedTest.test_integers();
    //SerializationSpeedTest.custom_ser();
  }

  public static void test_integers() throws IOException {
    byte[][] serialized = new byte[1_000_000][];

    long start = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ObjectOutputStream out = new ObjectOutputStream(bos)) {
        out.writeObject(Integer.valueOf(i));
        serialized[i] = bos.toByteArray();
      }
    }
    long end = System.currentTimeMillis();

    System.out.println("Java Serialization: " + (end - start) + " ms");
  }

  public static void custom_ser() {
    byte[][] serialized = new byte[1_000_000][];

    long start = System.currentTimeMillis();
    for (int i = 0; i < 1_000_000; i++) {
      serialized[i] = new Pair(i, i).toBytes();
    }
    long end = System.currentTimeMillis();

    System.out.println("Custom Serialization: " + (end - start) + " ms");
  }
}
