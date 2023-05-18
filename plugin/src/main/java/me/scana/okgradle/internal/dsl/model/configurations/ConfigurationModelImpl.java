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
package me.scana.okgradle.internal.dsl.model.configurations;

import me.scana.okgradle.internal.dsl.api.configurations.ConfigurationModel;
import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.model.GradleDslBlockModel;
import me.scana.okgradle.internal.dsl.parser.configurations.ConfigurationDslElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ConfigurationModelImpl extends GradleDslBlockModel implements ConfigurationModel {
  @NonNls private static final String TRANSITIVE = "transitive";
  @NonNls private static final String VISIBLE = "visible";

  public ConfigurationModelImpl(@NotNull ConfigurationDslElement element) {
    super(element);
  }


  @NotNull
  @Override
  public String name() {
    return myDslElement.getName();
  }

  @NotNull
  @Override
  public ResolvedPropertyModel transitive() {
    return getModelForProperty(TRANSITIVE);
  }

  @NotNull
  @Override
  public ResolvedPropertyModel visible() {
    return getModelForProperty(VISIBLE);
  }
}
