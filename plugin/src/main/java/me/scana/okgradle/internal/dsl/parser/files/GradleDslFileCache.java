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
package me.scana.okgradle.internal.dsl.parser.files;

import static me.scana.okgradle.util.AndroidPluginUtils.getBaseDirPath;
import static me.scana.okgradle.util.AndroidPluginUtils.getGradleSettingsFile;
import static com.intellij.internal.psiView.stubtree.StubViewerPsiBasedTree.LOG;

import me.scana.okgradle.internal.dsl.model.GradleBuildModelImpl;
import me.scana.okgradle.internal.dsl.parser.BuildModelContext;
import me.scana.okgradle.internal.dsl.parser.files.GradleBuildFile;
import me.scana.okgradle.internal.dsl.parser.files.GradleDslFile;
import me.scana.okgradle.internal.dsl.parser.files.GradlePropertiesFile;
import me.scana.okgradle.internal.dsl.parser.files.GradleSettingsFile;
import com.google.common.base.Charsets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Cache to store a mapping between file paths and their respective {@link GradleDslFileCache} objects, its main purpose it to
 * prevent the parsing of a file more than once. In large projects without caching the parsed file we can end up parsing the same
 * file hundreds of times.
 */
public class GradleDslFileCache {
  @NotNull private Project myProject;
  @NotNull private Map<String, me.scana.okgradle.internal.dsl.parser.files.GradleDslFile> myParsedBuildFiles = new HashMap<>();

  public GradleDslFileCache(@NotNull Project project) {
    myProject = project;
  }

  public void clearAllFiles() {
    myParsedBuildFiles.clear();
  }

  @NotNull
  public me.scana.okgradle.internal.dsl.parser.files.GradleBuildFile getOrCreateBuildFile(@NotNull VirtualFile file,
                                                                                             @NotNull String name,
                                                                                             @NotNull BuildModelContext context,
                                                                                             boolean isApplied) {
    me.scana.okgradle.internal.dsl.parser.files.GradleDslFile dslFile = myParsedBuildFiles.get(file.getUrl());
    if (dslFile == null) {
      dslFile = GradleBuildModelImpl.parseBuildFile(file, myProject, name, context, isApplied);
      myParsedBuildFiles.put(file.getUrl(), dslFile);
    }
    else if (!(dslFile instanceof me.scana.okgradle.internal.dsl.parser.files.GradleBuildFile)) {
      throw new IllegalStateException("Found wrong type for build file in cache!");
    }

    return (GradleBuildFile)dslFile;
  }

  public void putBuildFile(@NotNull String name, @NotNull me.scana.okgradle.internal.dsl.parser.files.GradleDslFile buildFile) {
    myParsedBuildFiles.put(name, buildFile);
  }

  @Nullable
  public GradleSettingsFile getSettingsFile(@NotNull Project project) {
    VirtualFile file = getGradleSettingsFile(getBaseDirPath(project));
    if (file == null) {
      return null;
    }

    me.scana.okgradle.internal.dsl.parser.files.GradleDslFile dslFile = myParsedBuildFiles.get(file.getUrl());
    if (dslFile != null && !(dslFile instanceof GradleSettingsFile)) {
      throw new IllegalStateException("Found wrong type for settings file in cache!");
    }
    return (GradleSettingsFile)dslFile;
  }

  @NotNull
  public GradleSettingsFile getOrCreateSettingsFile(@NotNull VirtualFile settingsFile, @NotNull BuildModelContext context) {
    me.scana.okgradle.internal.dsl.parser.files.GradleDslFile dslFile = myParsedBuildFiles.get(settingsFile.getUrl());
    if (dslFile == null) {
      dslFile = new GradleSettingsFile(settingsFile, myProject, "settings", context);
      dslFile.parse();
      myParsedBuildFiles.put(settingsFile.getUrl(), dslFile);
    }
    else if (!(dslFile instanceof GradleSettingsFile)) {
      throw new IllegalStateException("Found wrong type for settings file in cache!");
    }
    return (GradleSettingsFile)dslFile;
  }

  @Nullable
  public GradlePropertiesFile getOrCreatePropertiesFile(@NotNull VirtualFile file, @NotNull String moduleName, @NotNull BuildModelContext context) {
    me.scana.okgradle.internal.dsl.parser.files.GradleDslFile dslFile = myParsedBuildFiles.get(file.getUrl());
    if (dslFile == null) {
      try {
        Properties properties = getProperties(file);
        dslFile = new GradlePropertiesFile(properties, file, myProject, moduleName, context);
        myParsedBuildFiles.put(file.getUrl(), dslFile);
      } catch (IOException e) {
        LOG.warn("Failed to process properties file " + file.getPath(), e);
        return null;
      }
    }
    else if (!(dslFile instanceof GradlePropertiesFile)) {
      throw new IllegalStateException("Found wrong type for properties file in cache!");
    }
    return (GradlePropertiesFile)dslFile;
  }

  private static Properties getProperties(@NotNull VirtualFile file) throws IOException {
    Properties properties = new Properties();
    properties.load(new InputStreamReader(file.getInputStream(), Charsets.UTF_8));
    return properties;
  }

  @NotNull
  public List<GradleDslFile> getAllFiles() {
    return new ArrayList<>(myParsedBuildFiles.values());
  }
}
