package test.integration;

import com.github.sormuras.mainrunner.api.Run;

public class AnnotatedProgram {
  @Run
  @Run("a")
  @Run({"a", "b"})
  @Run({"a", "b", "c"})
  public static void main(String[] args) {}
}
