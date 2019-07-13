package api;

public interface InterfaceTests {
  static void main(String... args) {}

  interface InterfaceNestedTests {
    static void main(String... args) {}

    interface InterfaceNestedAlphaTest {
      static void main(String... args) {}
    }

    interface InterfaceNestedOmegaTest {
      static void main(String... args) {}
    }
  }
}
