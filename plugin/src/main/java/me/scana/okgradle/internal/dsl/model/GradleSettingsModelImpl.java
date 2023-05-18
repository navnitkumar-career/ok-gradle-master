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

import static me.scana.okgradle.internal.dsl.parser.settings.ProjectPropertiesDslElement.BUILD_FILE_NAME;
import static me.scana.okgradle.util.AndroidPluginUtils.getGradleBuildFile;
import static me.scana.okgradle.util.AndroidPluginUtils.getGradleSettingsFile;
import static com.intellij.openapi.util.io.FileUtil.filesEqual;
import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;
import static com.intellij.openapi.vfs.VfsUtilCore.virtualToIoFile;
import static me.scana.okgradle.util.AndroidPluginUtils.getBaseDirPath;

import me.scana.okgradle.internal.dsl.api.GradleBuildModel;
import me.scana.okgradle.internal.dsl.api.GradleSettingsModel;
import me.scana.okgradle.internal.dsl.api.ProjectBuildModel;
import me.scana.okgradle.internal.dsl.parser.BuildModelContext;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslExpressionList;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslSimpleExpression;
import me.scana.okgradle.internal.dsl.parser.files.GradleBuildFile;
import me.scana.okgradle.internal.dsl.parser.files.GradleSettingsFile;
import me.scana.okgradle.internal.dsl.parser.settings.ProjectPropertiesDslElement;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.scana.okgradle.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleSettingsModelImpl extends GradleFileModelImpl implements GradleSettingsModel {
  public static final String INCLUDE = "include";
  private static final String INCLUDE_BUILD = "includeBuild";

  /**
   * @deprecated Use {@link ProjectBuildModel#getProjectSettingsModel()} instead.
   */
  @Deprecated
  @Nullable
  public static GradleSettingsModel get(@NotNull Project project) {
    VirtualFile file = getGradleSettingsFile(getBaseDirPath(project));
    return file != null ? parseBuildFile(file, project, "settings") : null;
  }

  /**
   * @deprecated Use {@link ProjectBuildModel#getProjectSettingsModel()} instead.
   */
  @Deprecated
  @NotNull
  public static GradleSettingsModel parseBuildFile(@NotNull VirtualFile file, @NotNull Project project, @NotNull String moduleName) {
    GradleSettingsFile settingsFile = new GradleSettingsFile(file, project, moduleName, BuildModelContext.create(project));
    settingsFile.parse();
    return new GradleSettingsModelImpl(settingsFile);
  }

  public GradleSettingsModelImpl(@NotNull GradleSettingsFile parsedModel) {
    super(parsedModel);
  }

  /**
   * Returns the module paths specified by the include statements. Note that these path are not file paths, but instead specify the
   * location of the modules in the project hierarchy. As such, the paths use the ':' character as separator.
   *
   * <p>For example, the path a:b or :a:b represents the module in the directory $projectDir/a/b.
   */
  @NotNull
  @Override
  public List<String> modulePaths() {
    List<String> result = Lists.newArrayList();
    result.add(":"); // Indicates the root module.

    GradleDslExpressionList includePaths = myGradleDslFile.getPropertyElement(INCLUDE, GradleDslExpressionList.class);
    if (includePaths == null) {
      return result;
    }

    for (GradleDslSimpleExpression includePath : includePaths.getSimpleExpressions()) {
      String value = includePath.getValue(String.class);
      if (value != null) {
        result.add(standardiseModulePath(value));
      }
    }
    return result;
  }

  @Override
  public void addModulePath(@NotNull String modulePath) {
    modulePath = standardiseModulePath(modulePath);
    myGradleDslFile.addToNewLiteralList(INCLUDE, modulePath);
  }

  @Override
  public void removeModulePath(@NotNull String modulePath) {
    // Try to remove the module path whether it has ":" prefix or not.
    if (!modulePath.startsWith(":")) {
      myGradleDslFile.removeFromExpressionList(INCLUDE, ":" + modulePath);
    }
    myGradleDslFile.removeFromExpressionList(INCLUDE, modulePath);
  }

  @Override
  public void replaceModulePath(@NotNull String oldModulePath, @NotNull String newModulePath) {
    // Try to replace the module path whether it has ":" prefix or not.
    if (!newModulePath.startsWith(":")) {
      newModulePath = ":" + newModulePath;
    }
    if (!oldModulePath.startsWith(":")) {
      myGradleDslFile.replaceInExpressionList(INCLUDE, ":" + oldModulePath, newModulePath);
    }
    myGradleDslFile.replaceInExpressionList(INCLUDE, oldModulePath, newModulePath);
  }

  @Nullable
  @Override
  public File moduleDirectory(String modulePath) {
    modulePath = standardiseModulePath(modulePath);
    if (!modulePaths().contains(modulePath)) {
      return null;
    }
    return moduleDirectoryNoCheck(modulePath);
  }

  @Nullable
  private File moduleDirectoryNoCheck(String modulePath) {
    File rootDirPath = virtualToIoFile(myGradleDslFile.getFile().getParent());
    if (modulePath.equals(":")) {
      return rootDirPath;
    }

    String projectKey = "project('" + modulePath + "')";
    ProjectPropertiesDslElement projectProperties = myGradleDslFile.getPropertyElement(projectKey, ProjectPropertiesDslElement.class);
    if (projectProperties != null) {
      File projectDir = projectProperties.projectDir();
      if (projectDir != null) {
        return projectDir;
      }
    }

    File parentDir;
    if (modulePath.lastIndexOf(':') == 0) {
      parentDir = rootDirPath;
    }
    else {
      String parentModule = parentModuleNoCheck(modulePath);
      if (parentModule == null) {
        return null;
      }
      parentDir = moduleDirectoryNoCheck(parentModule);
    }
    String moduleName = modulePath.substring(modulePath.lastIndexOf(':') + 1);
    return new File(parentDir, moduleName);
  }

  @Nullable
  @Override
  public String moduleWithDirectory(@NotNull File moduleDir) {
    for (String modulePath : modulePaths()) {
      if (filesEqual(moduleDir, moduleDirectory(modulePath))) {
        return modulePath;
      }
    }
    return null;
  }

  @Nullable
  @Override
  public GradleBuildModel moduleModel(@NotNull String modulePath) {
    File buildFilePath = buildFile(modulePath);
    if (buildFilePath == null) {
      return null;
    }
    VirtualFile buildFile = findFileByIoFile(buildFilePath, false);
    if (buildFile == null) {
      return null;
    }
    GradleBuildFile dslFile =
      myGradleDslFile.getContext().getOrCreateBuildFile(buildFile, modulePath.substring(modulePath.lastIndexOf(':') + 1), false);
    return new GradleBuildModelImpl(dslFile);
  }

  @Nullable
  @Override
  public String parentModule(@NotNull String modulePath) {
    modulePath = standardiseModulePath(modulePath);
    List<String> allModulePaths = modulePaths();
    if (!allModulePaths.contains(modulePath)) {
      return null;
    }
    String currentPath = modulePath;
    do {
      currentPath = parentModuleNoCheck(currentPath);
      if (allModulePaths.contains(currentPath)) {
        return currentPath;
      }
    }
    while (currentPath != null && !currentPath.equals(":"));
    return null;
  }

  @Nullable
  private static String parentModuleNoCheck(@NotNull String modulePath) {
    modulePath = standardiseModulePath(modulePath);
    if (modulePath.equals(":")) {
      return null;
    }
    int lastPathElementIndex = modulePath.lastIndexOf(':');
    return lastPathElementIndex == 0 ? ":" : modulePath.substring(0, lastPathElementIndex);
  }

  @Nullable
  @Override
  public GradleBuildModel getParentModuleModel(@NotNull String modulePath) {
    String parentModule = parentModule(modulePath);
    if (parentModule == null) {
      return null;
    }
    return moduleModel(parentModule);
  }

  @Nullable
  @Override
  public File buildFile(@NotNull String modulePath) {
    File moduleDirectory = moduleDirectory(modulePath);
    if (moduleDirectory == null) {
      return null;
    }

    String buildFileName = null;
    String projectKey = "project('" + modulePath + "')";
    ProjectPropertiesDslElement projectProperties = myGradleDslFile.getPropertyElement(projectKey, ProjectPropertiesDslElement.class);
    if (projectProperties != null) {
      buildFileName =  projectProperties.getLiteral(BUILD_FILE_NAME, String.class);
    }

    if (buildFileName == null) {
      buildFileName = Constants.BUILD_GRADLE;
    }

    return new File(moduleDirectory, buildFileName);
  }

  @NotNull
  @Override
  public List<VirtualFile> includedBuilds() {
    List<GradleDslElement> properties = myGradleDslFile.getPropertyElementsByName(INCLUDE_BUILD);
    return properties.stream().map((element) -> {
      String value = extractValueFromElement(element);
      if (value == null) {
        return null;
      }
      return attemptToFindIncludedBuildRoot(value);
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }

  private static String standardiseModulePath(@NotNull String modulePath) {
    return modulePath.startsWith(":") ? modulePath : ":" + modulePath;
  }

  @Nullable
  private static String extractValueFromElement(@Nullable GradleDslElement element) {
    if (!(element instanceof GradleDslSimpleExpression)) {
      return null;
    }

    return ((GradleDslSimpleExpression)element).getValue(String.class);
  }

  @Nullable
  private VirtualFile attemptToFindIncludedBuildRoot(@NotNull String fileName) {
    File realFile = new File(fileName);
    VirtualFile file;
    if (realFile.isAbsolute()) {
      file = LocalFileSystem.getInstance().findFileByIoFile(realFile);
    } else {
      file = myGradleDslFile.getFile().getParent().findFileByRelativePath(fileName);
    }

    // Make sure the composite root is a directory and that it contains a build.gradle or settings.gradle file.
    if (file != null && file.isDirectory()) {
      File compositeRoot = virtualToIoFile(file);
      VirtualFile buildFile = getGradleBuildFile(compositeRoot);
      if (buildFile != null) {
        return file;
      }
      VirtualFile settingsFile = getGradleSettingsFile(compositeRoot);
      if (settingsFile != null) {
        return file;
      }
    }

    return null;
  }
}
