/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.scana.okgradle.internal.dsl.parser.elements;

import me.scana.okgradle.internal.dsl.parser.elements.GradleDslBlockElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for representing compileOptions block or others blocks which have sourceCompatibility / targetCompatibility fields.
 */
public abstract class BaseCompileOptionsDslElement extends GradleDslBlockElement {
  @NonNls public static final String COMPILE_OPTIONS_BLOCK_NAME = "compileOptions";

  @NonNls public static final String SOURCE_COMPATIBILITY_ATTRIBUTE_NAME = "sourceCompatibility";
  @NonNls public static final String TARGET_COMPATIBILITY_ATTRIBUTE_NAME = "targetCompatibility";

  protected BaseCompileOptionsDslElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement parent, @NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement name) {
    super(parent, name);
  }

  public BaseCompileOptionsDslElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement parent) {
    super(parent, GradleNameElement.create(COMPILE_OPTIONS_BLOCK_NAME));
  }

  @Override
  public void addParsedElement(@NotNull GradleDslElement element) {
    setParsedElement(element);
  }
}
