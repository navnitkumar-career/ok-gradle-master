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
package me.scana.okgradle.internal.dsl.model.dependencies;

import me.scana.okgradle.internal.dsl.api.dependencies.FileTreeDependencyModel;
import me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel;
import me.scana.okgradle.internal.dsl.api.ext.ResolvedPropertyModel;
import me.scana.okgradle.internal.dsl.model.dependencies.DependenciesModelImpl;
import me.scana.okgradle.internal.dsl.model.dependencies.DependencyModelImpl;
import me.scana.okgradle.internal.dsl.model.ext.GradlePropertyModelBuilder;
import me.scana.okgradle.internal.dsl.model.ext.transforms.MapMethodTransform;
import me.scana.okgradle.internal.dsl.model.ext.transforms.SingleArgToMapTransform;
import me.scana.okgradle.internal.dsl.model.ext.transforms.SingleArgumentMethodTransform;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslMethodCall;
import me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradlePropertiesDslElement;
import java.util.List;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileTreeDependencyModelImpl extends DependencyModelImpl implements FileTreeDependencyModel {
  @NonNls public static final String FILE_TREE = "fileTree";
  @NonNls public static final String DIR = "dir";
  @NonNls private static final String INCLUDE = "include";
  @NonNls private static final String EXCLUDE = "exclude";

  @NotNull private GradleDslMethodCall myDslElement;

  @NotNull
  static FileTreeDependencyModel createNew(@NotNull GradlePropertiesDslElement parent,
                                           @NotNull String configurationName,
                                           @NotNull String dir,
                                           @Nullable List<String> includes,
                                           @Nullable List<String> excludes) {
    GradleDslMethodCall newElement = new GradleDslMethodCall(parent, GradleNameElement.create(configurationName), FILE_TREE);
    FileTreeDependencyModel fileTreeModel = create(newElement, configurationName, DependenciesModelImpl.Maintainers.SINGLE_ITEM_MAINTAINER);
    // Since we just created the method call with the FILE_TREE name, create should never return null.
    assert fileTreeModel != null;
    fileTreeModel.dir().setValue(dir);
    GradlePropertyModel includesModel = fileTreeModel.includes();
    if (includes != null) {
      includesModel.convertToEmptyList();
      includes.forEach(e -> includesModel.addListValue().setValue(e));
    }
    GradlePropertyModel excludesModel = fileTreeModel.excludes();
    if (excludes != null) {
      excludesModel.convertToEmptyList();
      excludes.forEach(e -> excludesModel.addListValue().setValue(e));
    }
    parent.setNewElement(newElement);
    return fileTreeModel;
  }

  @Nullable
  static FileTreeDependencyModel create(@NotNull GradleDslMethodCall methodCall,
                                        @NotNull String configName,
                                        @NotNull Maintainer maintainer) {
    if (!methodCall.getMethodName().equals(FILE_TREE)) {
      return null;
    }
    return new FileTreeDependencyModelImpl(configName, methodCall, maintainer);
  }

  private FileTreeDependencyModelImpl(@NotNull String configurationName,
                                      @NotNull GradleDslMethodCall dslElement,
                                      @NotNull Maintainer maintainer) {
    super(configurationName, maintainer);
    myDslElement = dslElement;
  }

  @Override
  @NotNull
  protected GradleDslElement getDslElement() {
    return myDslElement;
  }

  @Override
  void setDslElement(@NotNull GradleDslElement dslElement) {
    myDslElement = (GradleDslMethodCall)dslElement;
  }

  @Override
  @NotNull
  public ResolvedPropertyModel dir() {
    return GradlePropertyModelBuilder.create(myDslElement).addTransform(new MapMethodTransform(FILE_TREE, DIR))
      .addTransform(new SingleArgumentMethodTransform(FILE_TREE)).asMethod(true).buildResolved();
  }

  @Override
  @NotNull
  public ResolvedPropertyModel includes() {
    return GradlePropertyModelBuilder.create(myDslElement).addTransform(new SingleArgToMapTransform(DIR, INCLUDE))
      .addTransform(new MapMethodTransform(FILE_TREE, INCLUDE)).asMethod(true).buildResolved();
  }

  @Override
  @NotNull
  public ResolvedPropertyModel excludes() {
    return GradlePropertyModelBuilder.create(myDslElement).addTransform(new SingleArgToMapTransform(DIR, EXCLUDE))
      .addTransform(new MapMethodTransform(FILE_TREE, EXCLUDE)).asMethod(true).buildResolved();
  }
}
