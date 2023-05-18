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

import me.scana.okgradle.internal.dsl.api.BuildModelNotification;
import me.scana.okgradle.internal.dsl.api.GradleFileModel;
import me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel;
import me.scana.okgradle.internal.dsl.model.ext.GradlePropertyModelImpl;
import me.scana.okgradle.internal.dsl.parser.files.GradleDslFile;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GradleFileModelImpl implements GradleFileModel {
  @NotNull protected GradleDslFile myGradleDslFile;

  public GradleFileModelImpl(@NotNull GradleDslFile gradleDslFile) {
    myGradleDslFile = gradleDslFile;
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return myGradleDslFile.getPsiElement();
  }

  @NotNull
  @Override
  public Project getProject() {
    return myGradleDslFile.getProject();
  }

  @Override
  public void reparse() {
    myGradleDslFile.reparse();
  }

  @Override
  public boolean isModified() {
    return myGradleDslFile.isModified();
  }

  @Override
  public void resetState() {
    myGradleDslFile.resetState();
  }

  @NotNull
  @Override
  public VirtualFile getVirtualFile() {
    return myGradleDslFile.getFile();
  }

  @Override
  @NotNull
  public Map<String, GradlePropertyModel> getInScopeProperties() {
    return myGradleDslFile.getInScopeElements().entrySet().stream()
      .collect(Collectors.toMap(e -> e.getKey(), e -> new GradlePropertyModelImpl(e.getValue())));
  }

  @NotNull
  @Override
  public List<GradlePropertyModel> getDeclaredProperties() {
    return myGradleDslFile.getContainedElements(false).stream().map(e -> new GradlePropertyModelImpl(e))
      .collect(Collectors.toList());
  }

  @NotNull
  public Set<GradleDslFile> getAllInvolvedFiles() {
    Set<GradleDslFile> files = new HashSet<>();
    files.add(myGradleDslFile);
    // Add all parent dsl files.
    files.addAll(getParentFiles());

    List<GradleDslFile> currentFiles = new ArrayList<>();
    currentFiles.add(myGradleDslFile);
    // TODO: Generalize cycle detection in GradleDslSimpleExpression and reuse here.
    // Attempting to parse a cycle of applied files will fail in GradleDslFile#mergeAppliedFiles;
    while (!currentFiles.isEmpty()) {
      GradleDslFile currentFile = currentFiles.remove(0);
      files.addAll(currentFile.getApplyDslElement());
      currentFiles.addAll(currentFile.getApplyDslElement());
    }

    // Get all the properties files.
    for (GradleDslFile file : new ArrayList<>(files)) {
      GradleDslFile sibling = file.getSiblingDslFile();
      if (sibling != null) {
        files.add(sibling);
      }
    }

    return files;
  }

  private Set<GradleDslFile> getParentFiles() {
    Set<GradleDslFile> files = new HashSet<>();
    GradleDslFile file = myGradleDslFile.getParentModuleDslFile();
    while (file != null) {
      files.add(file);
      file = file.getParentModuleDslFile();
    }
    return files;
  }

  private void saveAllRelatedFiles() {
    getAllInvolvedFiles().forEach(GradleDslFile::saveAllChanges);
  }

  @Override
  public void applyChanges() {
    myGradleDslFile.applyChanges();

    saveAllRelatedFiles();
  }

  @TestOnly
  @NotNull
  public GradleDslFile getDslFile() {
    return myGradleDslFile;
  }

  @Override
  @NotNull
  public Map<String, List<BuildModelNotification>> getNotifications() {
    return getAllInvolvedFiles().stream().filter(e -> !e.getPublicNotifications().isEmpty())
      .collect(Collectors.toMap(e -> e.getFile().getPath(), e -> e.getPublicNotifications()));
  }
}
