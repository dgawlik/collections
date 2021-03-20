import java.util.ArrayList;
import java.util.function.Supplier;
import org.collections.btree.Vector;

class VectorPerformanceTest {

  public static void main(String[] args) {
    VectorPerformanceTest.append();
  }

  public static void append() {
    final ArrayList<Integer> arr = new ArrayList<>();
    measure(() -> {
      for (int i = 0; i < 1_000_000; i++) {
        arr.add(i);
      }
      return null;
    }, () -> {
      Vector<Integer> vct = new Vector<>(255);
      for (int i = 0; i < 1_000_000; i++) {
        vct = vct.add(i);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public static void insert_in_the_middle() {
    final ArrayList<Integer> arr = new ArrayList<>();
    measure(() -> {
      for (int i = 0; i < 1_00_000; i++) {
        arr.add(i / 2, i);
      }
      return null;
    }, () -> {
      Vector<Integer> vct = new Vector<>(255);
      for (int i = 0; i < 1_00_000; i++) {
        vct = vct.add(i / 2, i);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public static void pop_first() {
    final ArrayList<Integer> arr = new ArrayList<>();
    Vector<Integer> vct = new Vector<>(255);

    for (int i = 0; i < 1_000; i++) {
      arr.add(i);
      vct = vct.add(i);
    }

    final Vector<Integer> vct3 = vct;
    measure(() -> {
      for (int i = 0; i < 1_000; i++) {
        arr.remove(0);
      }
      return null;
    }, () -> {
      Vector<Integer> vct2 = vct3;
      for (int i = 0; i < 1_000; i++) {
        vct2 = vct2.remove(0);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public static void index_of() {
    final ArrayList<Integer> arr = new ArrayList<>();
    Vector<Integer> vct = new Vector<>(255);

    for (int i = 0; i < 1_00_000; i++) {
      arr.add(i);
      vct = vct.add(i);
    }

    final Vector<Integer> vct3 = vct;
    measure(() -> {
      for (int i = 0; i < 1_000; i++) {
        arr.indexOf(arr.get(arr.size() - 1));
      }
      return null;
    }, () -> {
      Vector<Integer> vct2 = vct3;
      for (int i = 0; i < 1_000; i++) {
        vct2.indexOf(vct2.get(vct2.size() - 1));
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public static void get_set() {
    final ArrayList<Integer> arr = new ArrayList<>();
    Vector<Integer> vct = new Vector<>(255);

    for (int i = 0; i < 1_000_000; i++) {
      arr.add(i);
      vct = vct.add(i);
    }

    Vector<Integer> vct3 = vct;
    measure(() -> {
      for (int i = 0; i < 1_000_000; i++) {
        arr.set(i / 2, arr.get(i / 2) + 1);
      }
      return null;
    }, () -> {
      Vector<Integer> vct2 = vct3;
      for (int i = 0; i < 1_000_000; i++) {
        vct2 = vct2.set(i / 2, vct2.get(i / 2) + 1);
      }
      return null;
    }, "ArrayList", "Vector");
  }

  public static void measure(Supplier<Void> func1, Supplier<Void> func2,
      String name1,
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