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

import static me.scana.okgradle.util.AndroidPluginUtils.getBaseDirPath;
import static me.scana.okgradle.internal.dsl.parser.apply.ApplyDslElement.APPLY_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.buildscript.BuildScriptDslElement.BUILDSCRIPT_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.buildscript.SubProjectsDslElement.SUBPROJECTS_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.dependencies.DependenciesDslElement.DEPENDENCIES_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.ext.ExtDslElement.EXT_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.java.JavaDslElement.JAVA_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.repositories.RepositoriesDslElement.REPOSITORIES_BLOCK_NAME;
import static me.scana.okgradle.util.AndroidPluginUtils.getGradleBuildFile;
import static com.intellij.openapi.vfs.VfsUtil.findFileByIoFile;

import me.scana.okgradle.internal.dsl.api.BuildScriptModel;
import me.scana.okgradle.internal.dsl.api.GradleBuildModel;
import me.scana.okgradle.internal.dsl.api.GradleFileModel;
import me.scana.okgradle.internal.dsl.api.GradleSettingsModel;
import me.scana.okgradle.internal.dsl.api.PluginModel;
import me.scana.okgradle.internal.dsl.api.ProjectBuildModel;
import me.scana.okgradle.internal.dsl.api.configurations.ConfigurationsModel;
import me.scana.okgradle.internal.dsl.api.dependencies.DependenciesModel;
import me.scana.okgradle.internal.dsl.api.ext.ExtModel;
import me.scana.okgradle.internal.dsl.api.java.JavaModel;
import me.scana.okgradle.internal.dsl.api.repositories.RepositoriesModel;
import me.scana.okgradle.internal.dsl.api.values.GradleNotNullValue;
import me.scana.okgradle.internal.dsl.model.buildscript.BuildScriptModelImpl;
import me.scana.okgradle.internal.dsl.model.configurations.ConfigurationsModelImpl;
import me.scana.okgradle.internal.dsl.model.dependencies.DependenciesModelImpl;
import me.scana.okgradle.internal.dsl.model.ext.ExtModelImpl;
import me.scana.okgradle.internal.dsl.model.java.JavaModelImpl;
import me.scana.okgradle.internal.dsl.model.repositories.RepositoriesModelImpl;
import me.scana.okgradle.internal.dsl.parser.BuildModelContext;
import me.scana.okgradle.internal.dsl.parser.apply.ApplyDslElement;
import me.scana.okgradle.internal.dsl.parser.buildscript.BuildScriptDslElement;
import me.scana.okgradle.internal.dsl.parser.buildscript.SubProjectsDslElement;
import me.scana.okgradle.internal.dsl.parser.configurations.ConfigurationsDslElement;
import me.scana.okgradle.internal.dsl.parser.dependencies.DependenciesDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslExpressionMap;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslLiteral;
import me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement;
import me.scana.okgradle.internal.dsl.parser.ext.ExtDslElement;
import me.scana.okgradle.internal.dsl.parser.files.GradleBuildFile;
import me.scana.okgradle.internal.dsl.parser.files.GradleDslFile;
import me.scana.okgradle.internal.dsl.parser.files.GradlePropertiesFile;
import me.scana.okgradle.internal.dsl.parser.files.GradleSettingsFile;
import me.scana.okgradle.internal.dsl.parser.java.JavaDslElement;
import me.scana.okgradle.internal.dsl.parser.repositories.RepositoriesDslElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import me.scana.okgradle.util.Constants;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

public class GradleBuildModelImpl extends GradleFileModelImpl implements GradleBuildModel {
  @NonNls private static final String PLUGIN = "plugin";

  /**
   * @deprecated Use {@link ProjectBuildModel#get(Project)} instead.
   */
  @Deprecated
  @Nullable
  public static GradleBuildModel get(@NotNull Project project) {
    VirtualFile file = getGradleBuildFile(getBaseDirPath(project));
    return file != null ? parseBuildFile(file, project, project.getName()) : null;
  }

  /**
   * @deprecated Use {@link ProjectBuildModel#getModuleBuildModel(Module)} instead.
   */
  @Deprecated
  @Nullable
  public static GradleBuildModel get(@NotNull Module module) {
    VirtualFile file = getGradleBuildFile(module);
    return file != null ? parseBuildFile(file, module.getProject(), module.getName()) : null;
  }

  /**
   * @deprecated Use {@link ProjectBuildModel#getModuleBuildModel(Module)} instead.
   */
  @Deprecated
  @NotNull
  public static GradleBuildModel parseBuildFile(@NotNull VirtualFile file, @NotNull Project project) {
    return parseBuildFile(file, project, "<Unknown>");
  }

  /**
   * @deprecated Use {@link ProjectBuildModel#getModuleBuildModel(Module)} instead.
   */
  @Deprecated
  @NotNull
  public static GradleBuildModel parseBuildFile(@NotNull VirtualFile file,
                                                @NotNull Project project,
                                                @NotNull String moduleName) {
    return new GradleBuildModelImpl(parseBuildFile(file, project, moduleName, BuildModelContext.create(project), false));
  }

  @Deprecated
  @NotNull
  @Override
  public List<GradleNotNullValue<String>> appliedPlugins() {
    return plugins().stream().map(plugin -> new GradleNotNullValue<String>() {
      @NotNull
      @Override
      public VirtualFile getFile() {
        return plugin.name().getGradleFile();
      }

      @NotNull
      @Override
      public String getPropertyName() {
        return plugin.name().getName();
      }

      @Nullable
      @Override
      public String getDslText() {
        return ApplicationManager.getApplication().runReadAction((Computable<String>)() -> {
          PsiElement e = plugin.name().getPsiElement();
          return e == null ? null : e.getText();
        });
      }

      @NotNull
      @Override
      public Map<String, GradleNotNullValue<Object>> getResolvedVariables() {
        return ImmutableMap.of();
      }

      @NotNull
      @Override
      public String value() {
        return plugin.name().forceString();
      }

      @Nullable
      @Override
      public PsiElement getPsiElement() {
        return plugin.name().getPsiElement();
      }
    }).collect(Collectors.toList());
  }

  /**
   *  Parses a build file and produces the {@link GradleBuildFile} that represents it.
   *
   * @param file the build file that should be parsed, this must be a gradle build file
   * @param project the project that the build file belongs to
   * @param moduleName the name of the module
   * @param context the context that should be used for this parse
   * @param isApplied whether or not the file should be parsed as if it was applied, if true we do not populate the
   *                  file with the properties found in the subprojects block. This should be true for any file that is not part of the
   *                  main build.gradle structure (i.e project and module files) otherwise we might attempt to parse the file we are parsing
   *                  again leading to a stack overflow.
   * @return the model of the given Gradle file.
   */
  @NotNull
  public static GradleBuildFile parseBuildFile(@NotNull VirtualFile file,
                                               @NotNull Project project,
                                               @NotNull String moduleName,
                                               @NotNull BuildModelContext context,
                                               boolean isApplied) {
    GradleBuildFile buildDslFile = new GradleBuildFile(file, project, moduleName, context);
    ApplicationManager.getApplication().runReadAction(() -> {
      if (!isApplied) {
        populateWithParentModuleSubProjectsProperties(buildDslFile, context);
      }
      populateSiblingDslFileWithGradlePropertiesFile(buildDslFile, context);
      buildDslFile.parse();
    });
    return buildDslFile;
  }

  public static void populateWithParentModuleSubProjectsProperties(@NotNull GradleBuildFile buildDslFile,
                                                                   @NotNull BuildModelContext context) {
    VirtualFile maybeSettingsFile = buildDslFile.tryToFindSettingsFile();
    if (maybeSettingsFile == null) {
      return;
    }
    GradleSettingsFile settingsFile = context.getOrCreateSettingsFile(maybeSettingsFile);

    GradleSettingsModel gradleSettingsModel = new GradleSettingsModelImpl(settingsFile);
    String modulePath = gradleSettingsModel.moduleWithDirectory(buildDslFile.getDirectoryPath());
    if (modulePath == null) {
      return;
    }

    GradleBuildModel parentModuleModel = gradleSettingsModel.getParentModuleModel(modulePath);
    if (!(parentModuleModel instanceof GradleBuildModelImpl)) {
      return;
    }

    GradleBuildModelImpl parentModuleModelImpl = (GradleBuildModelImpl)parentModuleModel;

    GradleDslFile parentModuleDslFile = parentModuleModelImpl.myGradleDslFile;
    buildDslFile.setParentModuleDslFile(parentModuleDslFile);

    SubProjectsDslElement subProjectsDslElement =
      parentModuleDslFile.getPropertyElement(SUBPROJECTS_BLOCK_NAME, SubProjectsDslElement.class);
    if (subProjectsDslElement == null) {
      return;
    }

    buildDslFile.setParsedElement(subProjectsDslElement);
    for (Map.Entry<String, GradleDslElement> entry : subProjectsDslElement.getPropertyElements().entrySet()) {
      // TODO: This should be applied.
      buildDslFile.setParsedElement(entry.getValue());
    }
  }

  public static void populateSiblingDslFileWithGradlePropertiesFile(@NotNull GradleBuildFile buildDslFile,
                                                                    @NotNull BuildModelContext context) {
    File propertiesFilePath = new File(buildDslFile.getDirectoryPath(), Constants.GRADLE_PROPERTIES);
    VirtualFile propertiesFile = findFileByIoFile(propertiesFilePath, false);
    if (propertiesFile == null) {
      return;
    }

    GradlePropertiesFile parsedProperties = context.getOrCreatePropertiesFile(propertiesFile, buildDslFile.getName());
    if (parsedProperties == null) {
      return;
    }
    GradlePropertiesModel propertiesModel = new GradlePropertiesModel(parsedProperties);

    GradleDslFile propertiesDslFile = propertiesModel.myGradleDslFile;
    buildDslFile.setSiblingDslFile(propertiesDslFile);
    propertiesDslFile.setSiblingDslFile(buildDslFile);
  }

  GradleBuildModelImpl(@NotNull GradleBuildFile buildDslFile) {
    super(buildDslFile);
  }

  @Override
  @NotNull
  public List<PluginModel> plugins() {
    ApplyDslElement applyDslElement = myGradleDslFile.getPropertyElement(APPLY_BLOCK_NAME, ApplyDslElement.class);
    if (applyDslElement == null) {
      return ImmutableList.of();
    }

    return new ArrayList<>(me.scana.okgradle.internal.dsl.model.PluginModelImpl.deduplicatePlugins(me.scana.okgradle.internal.dsl.model.PluginModelImpl.create(applyDslElement)).values());
  }

  @NotNull
  @Override
  public PluginModel applyPlugin(@NotNull String plugin) {
    ApplyDslElement applyDslElement = myGradleDslFile.getPropertyElement(APPLY_BLOCK_NAME, ApplyDslElement.class);
    if (applyDslElement == null) {
      applyDslElement = new ApplyDslElement(myGradleDslFile);
      // The apply element should be added at the start.
      myGradleDslFile.addNewElementAt(0, applyDslElement);
    }

    Map<String, PluginModel> models = me.scana.okgradle.internal.dsl.model.PluginModelImpl.deduplicatePlugins(me.scana.okgradle.internal.dsl.model.PluginModelImpl.create(applyDslElement));
    if (models.containsKey(plugin)) {
      return models.get(plugin);
    }

    GradleDslExpressionMap applyMap = new GradleDslExpressionMap(myGradleDslFile, GradleNameElement.create(APPLY_BLOCK_NAME));
    GradleDslLiteral literal = new GradleDslLiteral(applyMap, GradleNameElement.create(PLUGIN));
    literal.setValue(plugin.trim());
    applyMap.setNewElement(literal);
    applyDslElement.setNewElement(applyMap);

    return new me.scana.okgradle.internal.dsl.model.PluginModelImpl(applyMap, literal);
  }

  @Override
  public void removePlugin(@NotNull String plugin) {
    ApplyDslElement applyDslElement = myGradleDslFile.getPropertyElement(APPLY_BLOCK_NAME, ApplyDslElement.class);
    if (applyDslElement == null) {
      return;
    }

    me.scana.okgradle.internal.dsl.model.PluginModelImpl.removePlugins(PluginModelImpl.create(applyDslElement), plugin);
  }

  @NotNull
  @Override
  public BuildScriptModel buildscript() {
    BuildScriptDslElement buildScriptDslElement = myGradleDslFile.getPropertyElement(BUILDSCRIPT_BLOCK_NAME, BuildScriptDslElement.class);
    if (buildScriptDslElement == null) {
      buildScriptDslElement = new BuildScriptDslElement(myGradleDslFile);
      myGradleDslFile.setNewElement(buildScriptDslElement);
    }
    return new BuildScriptModelImpl(buildScriptDslElement);
  }

  @NotNull
  @Override
  public ConfigurationsModel configurations() {
    ConfigurationsDslElement configurationsDslElement =
      myGradleDslFile.getPropertyElement(ConfigurationsDslElement.CONFIGURATIONS_BLOCK_NAME, ConfigurationsDslElement.class);
    if (configurationsDslElement == null) {
      configurationsDslElement = new ConfigurationsDslElement(myGradleDslFile);
      myGradleDslFile.addNewElementBeforeAllOfClass(configurationsDslElement, DependenciesDslElement.class);
    }
    return new ConfigurationsModelImpl(configurationsDslElement);
  }

  @NotNull
  @Override
  public DependenciesModel dependencies() {
    DependenciesDslElement dependenciesDslElement =
      myGradleDslFile.getPropertyElement(DEPENDENCIES_BLOCK_NAME, DependenciesDslElement.class);
    if (dependenciesDslElement == null) {
      dependenciesDslElement = new DependenciesDslElement(myGradleDslFile);
      myGradleDslFile.setNewElement(dependenciesDslElement);
    }
    return new DependenciesModelImpl(dependenciesDslElement);
  }

  @Override
  @NotNull
  public ExtModel ext() {
    ExtDslElement extDslElement = myGradleDslFile.getPropertyElement(EXT_BLOCK_NAME, ExtDslElement.class);
    if (extDslElement == null) {
      extDslElement = new ExtDslElement(myGradleDslFile);
      // Place the Ext element just after the applied plugins, if present.
      List<GradleDslElement> elements = myGradleDslFile.getAllElements();
      int index = (!elements.isEmpty() && elements.get(0) instanceof ApplyDslElement) ? 1 : 0;
      myGradleDslFile.addNewElementAt(index, extDslElement);
    }
    return new ExtModelImpl(extDslElement);
  }

  @NotNull
  @Override
  public JavaModel java() {
    JavaDslElement javaDslElement = myGradleDslFile.getPropertyElement(JAVA_BLOCK_NAME, JavaDslElement.class);
    if (javaDslElement == null) {
      javaDslElement = new JavaDslElement(myGradleDslFile);
      myGradleDslFile.setNewElement(javaDslElement);
    }
    return new JavaModelImpl(javaDslElement);
  }

  @NotNull
  @Override
  public RepositoriesModel repositories() {
    RepositoriesDslElement repositoriesDslElement =
      myGradleDslFile.getPropertyElement(REPOSITORIES_BLOCK_NAME, RepositoriesDslElement.class);
    if (repositoriesDslElement == null) {
      repositoriesDslElement = new RepositoriesDslElement(myGradleDslFile);
      myGradleDslFile.setNewElement(repositoriesDslElement);
    }
    return new RepositoriesModelImpl(repositoriesDslElement);
  }

  @Override
  @NotNull
  public Set<GradleFileModel> getInvolvedFiles() {
    return getAllInvolvedFiles().stream().distinct().map(e -> getFileModel(e)).collect(Collectors.toSet());
  }

  @NotNull
  private static GradleFileModel getFileModel(@NotNull GradleDslFile file) {
    if (file instanceof GradleBuildFile) {
      return new GradleBuildModelImpl((GradleBuildFile)file);
    }
    else if (file instanceof GradleSettingsFile) {
      return new GradleSettingsModelImpl((GradleSettingsFile)file);
    }
    else if (file instanceof GradlePropertiesFile) {
      return new GradlePropertiesModel(file);
    }
    throw new IllegalStateException("Unknown GradleDslFile type found!");
  }

  /**
   * Removes property {@link RepositoriesDslElement#REPOSITORIES_BLOCK_NAME}.
   */
  @Override
  @TestOnly
  public void removeRepositoriesBlocks() {
    myGradleDslFile.removeProperty(REPOSITORIES_BLOCK_NAME);
  }
}
