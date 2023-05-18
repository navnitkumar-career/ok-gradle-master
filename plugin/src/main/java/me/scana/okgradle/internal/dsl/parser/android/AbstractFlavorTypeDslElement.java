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
package me.scana.okgradle.internal.dsl.parser.android;

import me.scana.okgradle.internal.dsl.model.android.AndroidModelImpl;
import me.scana.okgradle.internal.dsl.parser.android.BuildTypeDslElement;
import me.scana.okgradle.internal.dsl.parser.android.ProductFlavorDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.*;
import org.jetbrains.annotations.NotNull;

/**
 * Common base class for {@link BuildTypeDslElement} and {@link ProductFlavorDslElement}.
 */
public abstract class AbstractFlavorTypeDslElement extends GradleDslBlockElement {
  protected AbstractFlavorTypeDslElement(@NotNull GradleDslElement parent, @NotNull GradleNameElement name) {
    super(parent, name);
  }

  @Override
  public void addParsedElement(@NotNull GradleDslElement element) {
    String property = element.getName();
    if (property.equals("consumerProguardFiles") && element instanceof GradleDslSimpleExpression) {
      addAsParsedDslExpressionList((GradleDslSimpleExpression)element);
      return;
    }

    if (property.equals("setProguardFiles")) {
      // Clear the property since setProguardFiles overwrites these.
      removeProperty("proguardFiles");
      addToParsedExpressionList("proguardFiles", element);
      return;
    }

    if (property.equals("proguardFiles") || property.equals("proguardFile")) {
      addToParsedExpressionList("proguardFiles", element);
      return;
    }

    if (property.equals("resValue")) {
      if (!(element instanceof GradleDslExpressionList)) {
        return;
      }
      GradleDslExpressionList listElement = (GradleDslExpressionList)element;
      if (listElement.getExpressions().size() != 3 || listElement.getLiterals(String.class).size() != 3) {
        return;
      }
    }

    super.addParsedElement(element);
  }

  @Override
  public boolean isInsignificantIfEmpty() {
    // defaultConfig is special in that is can be deleted if it is empty.
    return myName.name().equals(AndroidModelImpl.DEFAULT_CONFIG);
  }
}
