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

package de.sormuras.mainrunner.engine;

import java.nio.file.Path;
import java.util.Set;

enum OverlaySingleton implements Overlay {
  INSTANCE {

    private final Set<String> systemPropertyNames =
        Set.of(
            "jdk.module.path", // The application module path
            "jdk.module.upgrade.path", // The upgrade module path
            "jdk.module.main", // The module name of the initial/main module
            "jdk.module.main.class" // The main class name of the initial module
            );

    @Override
    public String display() {
      return "Mainrunner (Java 11)";
    }

    @Override
    public Path java() {
      return ProcessHandle.current().info().command().map(Path::of).orElseThrow();
    }

    @Override
    public Set<String> systemPropertyNames() {
      return systemPropertyNames;
    }
  }
}
