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
package me.scana.okgradle.internal.dsl.model.repositories;

import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.api.repositories.RepositoryModel;
import me.scana.okgradle.internal.dsl.model.ext.GradlePropertyModelBuilder;
import me.scana.okgradle.internal.dsl.model.ext.transforms.RepositoryClosureTransform;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradlePropertiesDslElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all the repository models.
 */
public abstract class RepositoryModelImpl implements RepositoryModel {
  @NonNls private static final String NAME = "name";

  @NotNull private final String myDefaultRepoName;

  @NotNull protected final GradlePropertiesDslElement myHolder;
  @NotNull protected final GradleDslElement myDslElement;

  protected RepositoryModelImpl(@NotNull GradlePropertiesDslElement holder,
                                @NotNull GradleDslElement dslElement,
                                @NotNull String defaultRepoName) {
    myHolder = holder;
    myDslElement = dslElement;
    myDefaultRepoName = defaultRepoName;
  }

  @NotNull
  @Override
  public ResolvedPropertyModel name() {
    return GradlePropertyModelBuilder.create(myDslElement).asMethod(true)
                                     .addTransform(new RepositoryClosureTransform(myHolder, NAME, myDefaultRepoName)).buildResolved();
  }
}
