/*
 * Copyright (C) 2016 The Android Open Source Project
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
package me.scana.okgradle.internal.dsl.parser.android.splits;

import me.scana.okgradle.internal.dsl.parser.elements.GradleDslBlockElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslMethodCall;
import me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class AbiDslElement extends GradleDslBlockElement {
  @NonNls public static final String ABI_BLOCK_NAME = "abi";

  public AbiDslElement(@NotNull GradleDslElement parent) {
    super(parent, GradleNameElement.create(ABI_BLOCK_NAME));
  }

  @Override
  public void addParsedElement(@NotNull GradleDslElement element) {
    String property = element.getName();
    if (property.equals("include") || property.equals("exclude")) {
      addToParsedExpressionList(property, element);
      return;
    }

    if (property.equals("reset") && element instanceof GradleDslMethodCall) {
      addParsedResettingElement(element, "include");
      return;
    }

    super.addParsedElement(element);
  }
}
