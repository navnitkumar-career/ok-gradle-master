/*
 * Copyright (C) 2018 The Android Open Source Project
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
package me.scana.okgradle.internal.dsl.model.android.productFlavors;

import me.scana.okgradle.internal.dsl.api.android.productFlavors.VectorDrawablesOptionsModel;
import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.model.GradleDslBlockModel;
import me.scana.okgradle.internal.dsl.parser.android.productFlavors.VectorDrawablesOptionsDslElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class VectorDrawablesOptionsModelImpl extends GradleDslBlockModel implements VectorDrawablesOptionsModel {
  @NonNls private static final String GENERATED_DENSITIES = "generatedDensities";
  @NonNls private static final String USE_SUPPORT_LIBRARY = "useSupportLibrary";

  // This block element has requires no special handling.
  public VectorDrawablesOptionsModelImpl(@NotNull VectorDrawablesOptionsDslElement element) {
    super(element);
  }

  @NotNull
  @Override
  public ResolvedPropertyModel generatedDensities() {
    return getModelForProperty(GENERATED_DENSITIES, true);
  }

  @NotNull
  @Override
  public ResolvedPropertyModel useSupportLibrary() {
    return getModelForProperty(USE_SUPPORT_LIBRARY, true);
  }
}
