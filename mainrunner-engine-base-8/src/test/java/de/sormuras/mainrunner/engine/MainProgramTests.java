package de.sormuras.mainrunner.engine;

import static de.sormuras.mainrunner.engine.MainProgram.isSingleFileSourceCodeProgram;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainProgramTests {

  @Test
  void nonStaticMainMethodDoesNotMatch() {
    String program = "class A{public void main(String[]a){}}";
    assertFalse(isSingleFileSourceCodeProgram(program));
  }

  @Test
  void minimalClassProgramMatchesPattern() {
    String program = "class A{public static void main(String[]a){}}";
    assertTrue(isSingleFileSourceCodeProgram(program));
  }

  @Test
  void minimalInterfaceProgramMatchesPattern() {
    String program = "interface A{static void main(String[]a){}}";
    assertTrue(isSingleFileSourceCodeProgram(program));
  }

  @Test
  void minimalEnumProgramMatchesPattern() {
    String program = "enum A{;public static void main(String[]a){}}";
    assertTrue(isSingleFileSourceCodeProgram(program));
  }

  @Test
  void multiLineProgramMatchesPattern() {
    String program = "class A{public   static\n void\r main   (\r\n\nString[] args){}}";
    assertTrue(isSingleFileSourceCodeProgram(program));
  }
}
