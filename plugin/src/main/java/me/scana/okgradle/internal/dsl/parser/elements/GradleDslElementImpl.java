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
package me.scana.okgradle.internal.dsl.parser.elements;

import me.scana.okgradle.internal.dsl.api.BuildModelNotification;
import me.scana.okgradle.internal.dsl.api.ext.PropertyType;
import me.scana.okgradle.internal.dsl.model.notifications.NotificationTypeReference;
import me.scana.okgradle.internal.dsl.parser.GradleReferenceInjection;
import me.scana.okgradle.internal.dsl.parser.ModificationAware;
import me.scana.okgradle.internal.dsl.parser.buildscript.BuildScriptDslElement;
import me.scana.okgradle.internal.dsl.parser.ext.ExtDslElement;
import me.scana.okgradle.internal.dsl.parser.files.GradleDslFile;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.scana.okgradle.internal.dsl.api.ext.PropertyType.DERIVED;
import static me.scana.okgradle.internal.dsl.model.ext.PropertyUtil.isNonExpressionPropertiesElement;
import static me.scana.okgradle.internal.dsl.parser.buildscript.BuildScriptDslElement.*;
import static me.scana.okgradle.internal.dsl.parser.ext.ExtDslElement.EXT_BLOCK_NAME;

public abstract class GradleDslElementImpl implements me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement, ModificationAware {
  @NotNull protected me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement myName;

  @Nullable protected me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement myParent;

  @NotNull protected List<GradlePropertiesDslElement> myHolders = new ArrayList<>();

  @NotNull private final GradleDslFile myDslFile;

  @Nullable private PsiElement myPsiElement;

  @Nullable private me.scana.okgradle.internal.dsl.parser.elements.GradleDslClosure myClosureElement;
  @Nullable private me.scana.okgradle.internal.dsl.parser.elements.GradleDslClosure myUnsavedClosure;

  private long myLastCommittedModificationCount;
  private long myModificationCount;

  // Whether or not that DslElement should be represented with the assignment syntax i.e "name = 'value'" or
  // the method call syntax i.e "name 'value'". This is needed since on some element types as we do not carry
  // the information to make this distinction. GradleDslElement will set this to a default of false.
  protected boolean myUseAssignment;

  @NotNull private PropertyType myElementType;

  @NotNull protected final List<GradleReferenceInjection> myDependencies = new ArrayList<>();
  @NotNull protected final List<GradleReferenceInjection> myDependents = new ArrayList<>();

  /**
   * Creates an in stance of a {@link me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement}
   *
   * @param parent     the parent {@link me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement} of this element. The parent element should always be a not-null value except if
   *                   this element is the root element, i.e a {@link GradleDslFile}.
   * @param psiElement the {@link PsiElement} of this dsl element.
   * @param name       the name of this element.
   */
  protected GradleDslElementImpl(@Nullable me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement parent, @Nullable PsiElement psiElement, @NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement name) {
    assert parent != null || this instanceof GradleDslFile;

    myParent = parent;
    myPsiElement = psiElement;
    myName = name;


    if (parent == null) {
      myDslFile = (GradleDslFile)this;
    }
    else {
      myDslFile = parent.getDslFile();
    }

    myUseAssignment = false;
    // Default to DERIVED, this is overwritten in the parser if required for the given element type.
    myElementType = DERIVED;
  }

  @Override
  public void setParsedClosureElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslClosure closureElement) {
    myClosureElement = closureElement;
  }

  @Override
  public void setNewClosureElement(@Nullable me.scana.okgradle.internal.dsl.parser.elements.GradleDslClosure closureElement) {
    myUnsavedClosure = closureElement;
    setModified();
  }

  @Override
  @Nullable
  public me.scana.okgradle.internal.dsl.parser.elements.GradleDslClosure getUnsavedClosure() {
    return myUnsavedClosure;
  }

  @Override
  @Nullable
  public GradleDslClosure getClosureElement() {
    return myUnsavedClosure == null ? myClosureElement : myUnsavedClosure;
  }

  @Override
  @NotNull
  public String getName() {
    return myName.name();
  }

  @Override
  @NotNull
  public String getQualifiedName() {
    // Don't include the name of the parent if this element is a direct child of the file.
    if (myParent == null || myParent instanceof GradleDslFile) {
      return getName();
    }

    String ourName = getName();
    return myParent.getQualifiedName() + (ourName.isEmpty() ? "" : "." + getName());
  }

  @Override
  @NotNull
  public String getFullName() {
    return myName.fullName();
  }

  @Override
  @NotNull
  public GradleNameElement getNameElement() {
    return myName;
  }

  @Override
  public void rename(@NotNull String newName) {
    myName.rename(newName);
    setModified();

    // If we are a GradleDslSimpleExpression we need to ensure our dependencies are correct.
    if (!(this instanceof GradleDslSimpleExpression)) {
      return;
    }

    List<GradleReferenceInjection> dependents = getDependents();
    unregisterAllDependants();

    reorder();

    // The property we renamed could have been shadowing another one. Attempt to re-resolve all dependents.
    dependents.forEach(e -> e.getOriginElement().resolve());

    // The new name could also create new dependencies, we need to make sure to resolve them.
    getDslFile().getContext().getDependencyManager().resolveWith(this);
  }

  @Override
  @Nullable
  public me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement getParent() {
    return myParent;
  }

  @Override
  public void setParent(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement parent) {
    myParent = parent;
  }

  @Override
  @NotNull
  public List<GradlePropertiesDslElement> getHolders() {
    return myHolders;
  }

  @Override
  public void addHolder(@NotNull GradlePropertiesDslElement holder) {
    myHolders.add(holder);
  }

  @Override
  @Nullable
  public PsiElement getPsiElement() {
    return myPsiElement;
  }

  @Override
  public void setPsiElement(@Nullable PsiElement psiElement) {
    myPsiElement = psiElement;
  }

  @Override
  public boolean shouldUseAssignment() {
    return myUseAssignment;
  }

  @Override
  public void setUseAssignment(boolean useAssignment) {
    myUseAssignment = useAssignment;
  }

  @Override
  @NotNull
  public PropertyType getElementType() {
    return myElementType;
  }

  @Override
  public void setElementType(@NotNull PropertyType propertyType) {
    myElementType = propertyType;
  }

  @Override
  @NotNull
  public GradleDslFile getDslFile() {
    return myDslFile;
  }

  @Override
  @NotNull
  public List<GradleReferenceInjection> getResolvedVariables() {
    ImmutableList.Builder<GradleReferenceInjection> resultBuilder = ImmutableList.builder();
    for (me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement child : getChildren()) {
      resultBuilder.addAll(child.getResolvedVariables());
    }
    return resultBuilder.build();
  }

  @Override
  @Nullable
  public me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement requestAnchor(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement element) {
    return null;
  }

  @Override
  @Nullable
  public me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement getAnchor() {
    return myParent == null ? null : myParent.requestAnchor(this);
  }

  @Override
  @Nullable
  public PsiElement create() {
    return myDslFile.getWriter().createDslElement(this);
  }

  @Override
  @Nullable
  public PsiElement move() {
    return myDslFile.getWriter().moveDslElement(this);
  }

  @Override
  public void delete() {
    for (me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement element : getChildren()) {
      element.delete();
    }

    this.getDslFile().getWriter().deleteDslElement(this);
  }

  @Override
  public void setModified() {
    modify();
    if (myParent != null) {
      myParent.setModified();
    }
  }

  @Override
  public boolean isModified() {
    return getLastCommittedModificationCount() != getModificationCount();
  }

  @Override
  public boolean isBlockElement() {
    return false;
  }

  @Override
  public boolean isInsignificantIfEmpty() {
    return true;
  }

  @Override
  @NotNull
  public abstract Collection<me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement> getChildren();

  @Override
  public final void applyChanges() {
    ApplicationManager.getApplication().assertWriteAccessAllowed();
    apply();
    commit();
  }

  protected abstract void apply();

  @Override
  public final void resetState() {
    reset();
    commit();
  }

  protected abstract void reset();

  @Override
  @NotNull
  public List<me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement> getContainedElements(boolean includeProperties) {
    return Collections.emptyList();
  }

  @Override
  @NotNull
  public Map<String, me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement> getInScopeElements() {
    Map<String, me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement> results = new LinkedHashMap<>();

    if (isNonExpressionPropertiesElement(this)) {
      GradlePropertiesDslElement thisElement = (GradlePropertiesDslElement)this;
      results.putAll(thisElement.getVariableElements());
    }

    // Trace parents finding any variable elements present.
    me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement currentElement = this;
    while (currentElement != null && currentElement.getParent() != null) {
      currentElement = currentElement.getParent();
      if (isNonExpressionPropertiesElement(currentElement)) {
        GradlePropertiesDslElement element = (GradlePropertiesDslElement)currentElement;
        results.putAll(element.getVariableElements());
      }
    }

    // Get Ext properties from the GradleDslFile, and the EXT properties from the buildscript.
    if (currentElement instanceof GradleDslFile) {
      GradleDslFile file = (GradleDslFile)currentElement;
      while (file != null) {
        ExtDslElement ext = file.getPropertyElement(EXT_BLOCK_NAME, ExtDslElement.class);
        if (ext != null) {
          results.putAll(ext.getPropertyElements());
        }
        // Add BuildScriptExt properties.
        BuildScriptDslElement buildScriptElement = file.getPropertyElement(BUILDSCRIPT_BLOCK_NAME, BuildScriptDslElement.class);
        if (buildScriptElement != null) {
          ExtDslElement buildScriptExt = buildScriptElement.getPropertyElement(EXT_BLOCK_NAME, ExtDslElement.class);
          if (buildScriptExt != null) {
            results.putAll(buildScriptExt.getPropertyElements());
          }
        }

        file = file.getParentModuleDslFile();
      }
    }

    return results;
  }

  @Override
  @NotNull
  public <T extends BuildModelNotification> T notification(@NotNull NotificationTypeReference<T> type) {
    return getDslFile().getContext().getNotificationForType(myDslFile, type);
  }

  @Override
  public void registerDependent(@NotNull GradleReferenceInjection injection) {
    assert injection.isResolved() && injection.getToBeInjected() == this;
    myDependents.add(injection);
  }

  @Override
  public void unregisterDependent(@NotNull GradleReferenceInjection injection) {
    assert injection.isResolved() && injection.getToBeInjected() == this;
    assert myDependents.contains(injection);
    myDependents.remove(injection);
  }

  @Override
  public void unregisterAllDependants() {
    // We need to create a new array to avoid concurrent modification exceptions.
    myDependents.forEach(e -> {
      // Break the dependency.
      e.resolveWith(null);
      // Register with DependencyManager
      getDslFile().getContext().getDependencyManager().registerUnresolvedReference(e);
    });
    myDependents.clear();
  }

  @Override
  @NotNull
  public List<GradleReferenceInjection> getDependents() {
    return new ArrayList<>(myDependents);
  }

  @Override
  @NotNull
  public List<GradleReferenceInjection> getDependencies() {
    return new ArrayList<>(myDependencies);
  }

  @Override
  public void updateDependenciesOnAddElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement newElement) {
    newElement.resolve();
    newElement.getDslFile().getContext().getDependencyManager().resolveWith(newElement);
  }

  @Override
  public void updateDependenciesOnReplaceElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement oldElement, @NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement newElement) {
    // Switch dependents to point to the new element.
    List<GradleReferenceInjection> injections = oldElement.getDependents();
    oldElement.unregisterAllDependants();
    injections.forEach(e -> e.resolveWith(newElement));
    // Register all the dependents with this new element.
    injections.forEach(newElement::registerDependent);

    // Go though our dependencies and unregister us as a dependent.
    oldElement.getResolvedVariables().forEach(e -> {
      me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement toBeInjected = e.getToBeInjected();
      if (toBeInjected != null) {
        toBeInjected.unregisterDependent(e);
      }
    });
  }

  @Override
  public void updateDependenciesOnRemoveElement(@NotNull me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement oldElement) {
    List<GradleReferenceInjection> dependents = oldElement.getDependents();
    oldElement.unregisterAllDependants();

    // The property we remove could have been shadowing another one. Attempt to re-resolve all dependents.
    dependents.forEach(e -> e.getOriginElement().resolve());

    // Go though our dependencies and unregister us as a dependent.
    oldElement.getResolvedVariables().forEach(e -> {
      GradleDslElement toBeInjected = e.getToBeInjected();
      if (toBeInjected != null) {
        toBeInjected.unregisterDependent(e);
      }
    });
  }

  @Override
  public void resolve() {
  }

  protected void reorder() {
    if (myParent instanceof ExtDslElement) {
      ((ExtDslElement)myParent).reorderAndMaybeGetNewIndex(this);
    }
  }

  @Override
  public long getModificationCount() {
    return myModificationCount;
  }

  public long getLastCommittedModificationCount() {
    return myLastCommittedModificationCount;
  }

  @Override
  public void modify() {
    myModificationCount++;
    myDependents.forEach(e -> e.getOriginElement().modify());
  }

  public void commit() {
    myLastCommittedModificationCount = myModificationCount;
  }

  @Nullable
  public static String getPsiText(@NotNull PsiElement psiElement) {
    return ApplicationManager.getApplication().runReadAction((Computable<String>)() -> psiElement.getText());
  }
}
