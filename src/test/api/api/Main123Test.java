package api;

import de.sormuras.mainrunner.api.Main;

public class Main123Test {
  @Main({"1", "2", "3"})
  public static void main(String[] args) {
    System.out.println("Main123Test.main");
    System.out.println("args = " + java.util.Arrays.deepToString(args));
  }
}
