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
package me.scana.okgradle.internal.dsl.api.ext;

import me.scana.okgradle.internal.dsl.api.android.SigningConfigModel;
import me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel;
import me.scana.okgradle.internal.dsl.api.ext.RawText;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a reference to another property or variable.
 */
public final class ReferenceTo extends RawText {
  @NotNull private static final String SIGNING_CONFIGS = "signingConfigs";

  public ReferenceTo(@NotNull String text) {
    super(text);
  }

  public ReferenceTo(@NotNull GradlePropertyModel model) {
    super(model.getFullyQualifiedName());
  }

  public ReferenceTo(@NotNull SigningConfigModel model) { super(SIGNING_CONFIGS + "." + model.name());
  }

  public static ReferenceTo createForSigningConfig(@NotNull String signingConfigName) {
    return new ReferenceTo(SIGNING_CONFIGS + "." + signingConfigName);
  }
}
