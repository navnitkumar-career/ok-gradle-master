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
package me.scana.okgradle.internal.dsl.model;

import me.scana.okgradle.internal.dsl.api.android.BaseCompileOptionsModel;
import me.scana.okgradle.internal.dsl.api.java.LanguageLevelPropertyModel;
import me.scana.okgradle.internal.dsl.model.GradleDslBlockModel;
import me.scana.okgradle.internal.dsl.parser.elements.BaseCompileOptionsDslElement;
import org.jetbrains.annotations.NotNull;

import static me.scana.okgradle.internal.dsl.parser.elements.BaseCompileOptionsDslElement.SOURCE_COMPATIBILITY_ATTRIBUTE_NAME;
import static me.scana.okgradle.internal.dsl.parser.elements.BaseCompileOptionsDslElement.TARGET_COMPATIBILITY_ATTRIBUTE_NAME;

/**
 * Base compile options model that only have sourceCompatibility / targetCompatibility fields.
 */
public abstract class BaseCompileOptionsModelImpl extends GradleDslBlockModel implements BaseCompileOptionsModel {

  public BaseCompileOptionsModelImpl(@NotNull BaseCompileOptionsDslElement dslElement, boolean useAssignment) {
    super(dslElement);
  }

  @NotNull
  @Override
  public LanguageLevelPropertyModel sourceCompatibility() {
    return getLanguageModelForProperty(SOURCE_COMPATIBILITY_ATTRIBUTE_NAME);
  }

  @NotNull
  @Override
  public LanguageLevelPropertyModel targetCompatibility() {
    return getLanguageModelForProperty(TARGET_COMPATIBILITY_ATTRIBUTE_NAME);
  }
}
