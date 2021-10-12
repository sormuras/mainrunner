package com.github.sormuras.mainrunner.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RunRepeatable.class)
public @interface Run {

  /**
   * Argument array to be passed to the test run.
   *
   * @return arguments
   */
  String[] value() default {};

  /**
   * Display name of the test run.
   *
   * @return the display name
   */
  String displayName() default "main(${ARGS})";

  /**
   * Fork a new Java VM instance and launch the main class.
   *
   * @return the Java runtime configuration
   */
  Java java() default @Java;
}
