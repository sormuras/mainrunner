package test.integration;

import com.github.sormuras.mainrunner.api.Main;

public class AnnotatedProgram {
  @Main
  @Main("a")
  @Main({"a", "b"})
  @Main({"a", "b", "c"})
  public static void main(String[] args) {}
}
