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
package me.scana.okgradle.internal.dsl.model.android;

import me.scana.okgradle.internal.dsl.api.android.BuildTypeModel;
import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.model.android.FlavorTypeModelImpl;
import me.scana.okgradle.internal.dsl.parser.android.BuildTypeDslElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class BuildTypeModelImpl extends FlavorTypeModelImpl implements BuildTypeModel {
  @NonNls private static final String DEBUGGABLE = "debuggable";
  @NonNls private static final String EMBED_MICRO_APP = "embedMicroApp";
  @NonNls private static final String JNI_DEBUGGABLE = "jniDebuggable";
  @NonNls private static final String MINIFY_ENABLED = "minifyEnabled";
  @NonNls private static final String PSEUDO_LOCALES_ENABLED = "pseudoLocalesEnabled";
  @NonNls private static final String RENDERSCRIPT_DEBUGGABLE = "renderscriptDebuggable";
  @NonNls private static final String RENDERSCRIPT_OPTIM_LEVEL = "renderscriptOptimLevel";
  @NonNls private static final String SHRINK_RESOURCES = "shrinkResources";
  @NonNls private static final String TEST_COVERAGE_ENABLED = "testCoverageEnabled";
  @NonNls private static final String ZIP_ALIGN_ENABLED = "zipAlignEnabled";

  public BuildTypeModelImpl(@NotNull BuildTypeDslElement dslElement) {
    super(dslElement);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel debuggable() {
    return getModelForProperty(DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel embedMicroApp() {
    return getModelForProperty(EMBED_MICRO_APP);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel jniDebuggable() {
    return getModelForProperty(JNI_DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel minifyEnabled() {
    return getModelForProperty(MINIFY_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel pseudoLocalesEnabled() {
    return getModelForProperty(PSEUDO_LOCALES_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel renderscriptDebuggable() {
    return getModelForProperty(RENDERSCRIPT_DEBUGGABLE);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel renderscriptOptimLevel() {
    return getModelForProperty(RENDERSCRIPT_OPTIM_LEVEL);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel shrinkResources() {
    return getModelForProperty(SHRINK_RESOURCES);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel testCoverageEnabled() {
    return getModelForProperty(TEST_COVERAGE_ENABLED);
  }

  @Override
  @NotNull
  public ResolvedPropertyModel zipAlignEnabled() {
    return getModelForProperty(ZIP_ALIGN_ENABLED);
  }

}
