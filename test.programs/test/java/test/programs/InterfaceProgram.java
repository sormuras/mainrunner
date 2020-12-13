package test.programs;

public interface InterfaceProgram {
  static void main(String[] args) {
    assert args.length == 0;
  }

  class VarArgsProgram {
    public static void main(String... args) {}
  }

  interface Nested {
    class NestedProgram {
      public static void main(String[] args) {}
    }
  }
}
