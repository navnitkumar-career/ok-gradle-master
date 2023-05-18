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

import me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel;
import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.api.java.LanguageLevelPropertyModel;
import me.scana.okgradle.internal.dsl.api.util.GradleDslModel;
import me.scana.okgradle.internal.dsl.model.ext.GradlePropertyModelBuilder;
import me.scana.okgradle.internal.dsl.model.ext.GradlePropertyModelImpl;
import me.scana.okgradle.internal.dsl.model.ext.PropertyUtil;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslExpression;
import me.scana.okgradle.internal.dsl.parser.elements.GradlePropertiesDslElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for the models representing block elements.
 */
public abstract class GradleDslBlockModel implements GradleDslModel {
  protected GradlePropertiesDslElement myDslElement;

  protected GradleDslBlockModel(@NotNull GradlePropertiesDslElement dslElement) {
    myDslElement = dslElement;
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return myDslElement.getPsiElement();
  }

  public boolean hasValidPsiElement() {
    PsiElement psiElement = getPsiElement();
    return psiElement != null && psiElement.isValid();
  }

  @Override
  @NotNull
  public Map<String, GradlePropertyModel> getInScopeProperties() {
    return myDslElement.getInScopeElements().entrySet().stream()
                       .collect(Collectors.toMap(e -> e.getKey(), e -> new GradlePropertyModelImpl(e.getValue())));
  }

  @Override
  @NotNull
  public List<GradlePropertyModel> getDeclaredProperties() {
    return myDslElement.getContainedElements(true).stream()
                       .filter(e -> e instanceof GradleDslExpression)
                       .map(e -> new GradlePropertyModelImpl(e)).collect(Collectors.toList());
  }

  @NotNull
  protected ResolvedPropertyModel getModelForProperty(@NotNull String property) {
    return getModelForProperty(property, false);
  }

  @NotNull
  protected LanguageLevelPropertyModel getLanguageModelForProperty(@NotNull String property) {
    return GradlePropertyModelBuilder.create(myDslElement, property).buildLanguage();
  }

  @NotNull
  protected ResolvedPropertyModel getModelForProperty(@NotNull String property, boolean isMethod) {
    return GradlePropertyModelBuilder.create(myDslElement, property).asMethod(isMethod).buildResolved();
  }

  @NotNull
  protected ResolvedPropertyModel getFileModelForProperty(@NotNull String property, boolean isMethod) {
    return GradlePropertyModelBuilder.create(myDslElement, property).asMethod(isMethod).addTransform(PropertyUtil.FILE_TRANSFORM)
                                     .buildResolved();
  }
}
