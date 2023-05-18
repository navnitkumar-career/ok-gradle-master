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
package me.scana.okgradle.internal.dsl.parser.elements;

import me.scana.okgradle.internal.dsl.parser.ext.ExtDslElement;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleNameElement {
  /**
   * This regex is used to extract indexes out from a map or list property.
   * Example matches would be:
   *   someListProperty[0]
   *   otherMap['key'][1]
   *   list[0][2]['key'][2]
   *
   * The first group will be the property name, with the index texts in succeeding groups.
   * I.e for the last example property.
   *    1st group -> "list"
   *    2nd group -> "0"
   *    3rd group -> "2"
   *    4th group -> "'key'"
   *    5th group -> "2"
   */
  @NotNull
  public static final Pattern INDEX_PATTERN = Pattern.compile("\\[(.+?)\\]|(.+?)(?=\\[)");

  @NotNull
  private final Pattern SPACES = Pattern.compile("\\s+");

  @Nullable
  private PsiElement myNameElement;
  @Nullable
  private String mySavedName;
  @Nullable
  private String myUnsavedName;
  @Nullable
  private String myFakeName; // Used for names that do not require a file element.
  @Nullable
  private String myName = null; // Cached version of the final name (to be reset on any change of the above fields).


  /**
   * Requires read access.
   */
  @NotNull
  public static GradleNameElement from(@NotNull PsiElement element) {
    return new GradleNameElement(element);
  }

  @NotNull
  public static GradleNameElement empty() {
    return new GradleNameElement((PsiElement)null);
  }

  @NotNull
  public static GradleNameElement create(@NotNull String name) {
    return new GradleNameElement(name, false);
  }

  @NotNull
  public static GradleNameElement fake(@NotNull String name) {
    return new GradleNameElement(name, true);
  }

  @NotNull
  public static GradleNameElement copy(@NotNull GradleNameElement element) { return new GradleNameElement(element); }

  /**
   * Requires read access.
   */
  private GradleNameElement(@Nullable PsiElement element) {
    setUpFrom(element);
  }

  private GradleNameElement(@NotNull String name, boolean isFake) {
    if (isFake) {
      myFakeName = name;
    }
    else {
      myUnsavedName = name;
    }
  }

  private GradleNameElement(@NotNull GradleNameElement element) {
    mySavedName = element.mySavedName;
    myUnsavedName = element.myUnsavedName;
    myFakeName = element.myFakeName;
  }

  /**
   * Changes this element to be backed by the given PsiElement. This method should not be called outside of
   * GradleWriter subclasses.
   */
  public void commitNameChange(@Nullable PsiElement nameElement) {
    setUpFrom(nameElement);
  }

  @NotNull
  public String fullName() {
    List<String> parts = qualifyingParts();
    parts.add(name());
    return createNameFromParts(parts);
  }

  @NotNull
  public List<String> fullNameParts() {
    return Splitter.on(".").splitToList(fullName());
  }

  public static String createNameFromParts(@NotNull List<String> parts) {
    return String.join(".", parts);
  }

  @NotNull
  public List<String> qualifyingParts() {
    String name = findName();
    if (name == null) {
      return Lists.newArrayList();
    }

    List<String> nameSegments = Splitter.on('.').splitToList(name);
    // Remove the last element, which is not a qualifying part;
    return nameSegments.subList(0, nameSegments.size() - 1).stream().map(GradleNameElement::convertNameToKey).collect(Collectors.toList());
  }

  public boolean isQualified() {
    String name = findName();
    if (name == null) {
      return false;
    }

    return name.contains(".");
  }

  @NotNull
  public String name() {
    String name = findName();
    if (name == null) {
      return "";
    }
    int lastDotIndex = name.lastIndexOf('.') + 1;
    return convertNameToKey(name.substring(lastDotIndex));
  }

  @Nullable
  public PsiElement getNamedPsiElement() {
    return myNameElement;
  }

  @Nullable
  public String getUnsavedName() {
    return myUnsavedName;
  }


  public void rename(@NotNull String newName) {
    if (!isFake()) {
      myUnsavedName = newName;
    }
    else {
      myFakeName = newName;
    }
    myName = null;
  }

  public boolean isEmpty() {
    String name = findName();
    return name == null || name.isEmpty();
  }

  public boolean isFake() {
    return myNameElement == null && myFakeName != null;
  }

  @Override
  @NotNull
  public String toString() {
    return fullName();
  }

  public boolean containsPropertyReference(@NotNull String propertyReference) {
    String name = name();
    if (propertyReference.equals(name)) {
      return true;
    }

    Matcher matcher = INDEX_PATTERN.matcher(propertyReference);
    if (matcher.find() && matcher.groupCount() > 0) {
      String indexName = matcher.group(0);
      if (indexName.equals(name)) {
        return true;
      }
    }

    List<String> parts = Arrays.asList(propertyReference.split("\\."));
    if (!parts.isEmpty() && parts.get(0).equals(name)) {
      return true;
    }
    if (parts.size() > 1 && parts.get(0).equals(ExtDslElement.EXT_BLOCK_NAME) && parts.get(1).equals(name)) {
      return true;
    }

    return false;
  }

  @Nullable
  private String findName() {
    if (myName != null) return myName;
    String name = null;
    if (myUnsavedName != null) {
      name = myUnsavedName;
    }
    else if (mySavedName != null) {
      name = mySavedName;
    }

    if (name == null && myFakeName != null) {
      name = myFakeName;
    }

    if (name != null) {
      // Remove whitespace
      name = SPACES.matcher(name).replaceAll("");
    }
    myName = name;
    return name;
  }

  @NotNull
  public static String convertNameToKey(@NotNull String str) {
    return StringUtil.unquoteString(str);
  }

  /**
   * READ ACCESS REQUIRED.
   */
  private void setUpFrom(@Nullable PsiElement element) {
    myNameElement = element;
    if (myNameElement instanceof PsiNamedElement) {
      mySavedName = ((PsiNamedElement)myNameElement).getName();
    }
    else if (myNameElement != null) {
      mySavedName = myNameElement.getText();
    }
    myName = null;
  }
}
