/*
 * Copyright (C) 2018 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.mainrunner.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.commons.support.AnnotationSupport;

class Java11Tests {

  @Test
  void accessJava11() {
    assertEquals("Java11", Java11.class.getSimpleName());
  }

  @Test
  @Main
  void defaultMainValues(TestInfo info) {
    var method = info.getTestMethod().orElseThrow(Error::new);
    var mains = AnnotationSupport.findRepeatableAnnotations(method, Main.class);

    assertEquals(1, mains.size());
    var main = mains.get(0);

    assertEquals("[]", Arrays.toString(main.value()));
    assertEquals("main(${ARGS})", main.displayName());
    assertEquals(0, main.java().expectedExitValue());
    assertEquals("[]", Arrays.toString(main.java().options()));
  }
}
