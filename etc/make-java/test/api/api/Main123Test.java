package api;

import de.sormuras.mainrunner.api.Main;

public class Main123Test {
  @Main({"1", "2", "3"})
  @Main({"4", "5", "6"})
  public static void main(String... args) {
    assert args.length == 3 : "Expected 3 arguments";
  }
}
