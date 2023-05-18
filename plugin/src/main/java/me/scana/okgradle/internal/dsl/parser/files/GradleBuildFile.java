/*
 * Copyright (C) 2017 The Android Open Source Project
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


import static me.scana.okgradle.internal.dsl.parser.apply.ApplyDslElement.APPLY_BLOCK_NAME;
import static me.scana.okgradle.internal.dsl.parser.elements.BaseCompileOptionsDslElement.SOURCE_COMPATIBILITY_ATTRIBUTE_NAME;
import static me.scana.okgradle.internal.dsl.parser.elements.BaseCompileOptionsDslElement.TARGET_COMPATIBILITY_ATTRIBUTE_NAME;
import static me.scana.okgradle.internal.dsl.parser.java.JavaDslElement.JAVA_BLOCK_NAME;

import me.scana.okgradle.internal.dsl.parser.BuildModelContext;
import me.scana.okgradle.internal.dsl.parser.apply.ApplyDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslExpressionMap;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslLiteral;
import me.scana.okgradle.internal.dsl.parser.files.GradleDslFile;
import me.scana.okgradle.internal.dsl.parser.java.JavaDslElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class GradleBuildFile extends GradleDslFile {
  public GradleBuildFile(@NotNull VirtualFile file,
                         @NotNull Project project,
                         @NotNull String moduleName,
                         @NotNull BuildModelContext context) {
    super(file, project, moduleName, context);
  }

  @Override
  public void addParsedElement(@NotNull GradleDslElement element) {
    if (APPLY_BLOCK_NAME.equals(element.getFullName()) && element instanceof GradleDslExpressionMap) {
      ApplyDslElement applyDslElement = getPropertyElement(APPLY_BLOCK_NAME, ApplyDslElement.class);
      if (applyDslElement == null) {
        applyDslElement = new ApplyDslElement(this);
        super.addParsedElement(applyDslElement);
      }
      applyDslElement.addParsedElement(element);
      return;
    }
    super.addParsedElement(element);
  }

  @Override
  public void setParsedElement(@NotNull GradleDslElement element) {
    if ((SOURCE_COMPATIBILITY_ATTRIBUTE_NAME.equals(element.getName()) || TARGET_COMPATIBILITY_ATTRIBUTE_NAME.equals(element.getName())) &&
        (element instanceof GradleDslLiteral)) {
      JavaDslElement javaDslElement = getPropertyElement(JAVA_BLOCK_NAME, JavaDslElement.class);
      if (javaDslElement == null) {
        javaDslElement = new JavaDslElement(this);
        super.setParsedElement(javaDslElement);
      }
      javaDslElement.setParsedElement(element);
      return;
    }
    super.setParsedElement(element);
  }
}
