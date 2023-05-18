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
package me.scana.okgradle.internal.dsl.model.ext.transforms;

import me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslElement;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslExpression;
import me.scana.okgradle.internal.dsl.parser.elements.GradleDslMethodCall;
import me.scana.okgradle.internal.dsl.parser.elements.GradleNameElement;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.HashSet;

import me.scana.okgradle.util.AndroidPluginUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static me.scana.okgradle.internal.dsl.api.ext.GradlePropertyModel.ValueType;
import static me.scana.okgradle.internal.dsl.model.ext.PropertyUtil.*;

/**
 * <p>This transform used for single argument method calls which have a preceding property name.</p>
 * <p>
 * <p>For example this transforms will allow a {@link GradlePropertyModel} to work on the &lt;value&gt; within:</p>
 * <code>storeFile file(&lt;value&gt;)</code><br>
 * or<br>
 * <code>storePassword System.console().readLine(&lt;value&gt;)</code><br>
 * <p>
 * <p>Note: It does not work when there is no preceding property name such as:</p>
 * <code>jcenter()</code><br>
 * <p>
 * <p>When no arguments are present the resulting {@link ValueType} of the model will be {@link ValueType#NONE}.</p>
 */
public class SingleArgumentMethodTransform extends PropertyTransform {
  @NotNull
  private final Set<String> myRecognizedNames = new HashSet<>();
  @NotNull
  private final String myWriteBackName;

  public SingleArgumentMethodTransform(@NotNull String methodName) {
    myRecognizedNames.add(methodName);
    myWriteBackName = methodName;
  }

  public SingleArgumentMethodTransform(@NotNull String methodName, @NotNull String... methodNames) {
    myRecognizedNames.addAll(Arrays.asList(methodNames));
    myRecognizedNames.add(methodName);
    myWriteBackName = methodName;
  }

  public SingleArgumentMethodTransform(@NotNull String methodName, @NotNull GradleDslElement object) {
    myWriteBackName = methodName;
    List<String> splitName = Lists.newArrayList(Splitter.on(".").split(object.getQualifiedName()));
    splitName.add(methodName);
    for (int i = 0; i < splitName.size(); i++) {
      myRecognizedNames.add(AndroidPluginUtils.join(splitName.subList(i, splitName.size()), "."));
    }
  }

  @Override
  public boolean test(@Nullable GradleDslElement e) {
    // We can deal with a null element, we will just create one.
    if (e == null) {
      return true;
    }

    if (e instanceof GradleDslMethodCall) {
      GradleDslMethodCall methodCall = (GradleDslMethodCall)e;
      if (!myRecognizedNames.contains(methodCall.getMethodName()) ||
          methodCall.getArguments().isEmpty()) {
        return false;
      }
      return true;
    }

    return false;
  }

  @Nullable
  @Override
  public GradleDslElement transform(@Nullable GradleDslElement e) {
    if (e == null) {
      return null;
    }

    // This cast is safe, we are guaranteed to have test(e) return true.
    GradleDslMethodCall methodCall = (GradleDslMethodCall)e;
    return methodCall.getArguments().get(0);
  }

  /**
   * @param holder     the parent of the property being represented by the {@link GradlePropertyModel}
   * @param oldElement the old element being represented by the {@link GradlePropertyModel}, if this is {@code null} then the
   *                   {@link GradleDslElement} returned will have to be created, otherwise it may be possible to reuse some elements
   * @param value      the new value that the property should be set to.
   * @param name       the name of the property, this is ONLY used when creating new properties,
   *                   all other names are kept the same.
   * @return the new element to be bound to the property.
   */
  @NotNull
  @Override
  public GradleDslExpression bind(@NotNull GradleDslElement holder,
                                  @Nullable GradleDslElement oldElement,
                                  @NotNull Object value,
                                  @NotNull String name) {
    return createBasicExpression(holder, value, GradleNameElement.empty());
  }

  @Override
  @NotNull
  public GradleDslExpression replace(@NotNull GradleDslElement holder,
                                     @Nullable GradleDslElement oldElement,
                                     @NotNull GradleDslExpression newElement,
                                     @NotNull String name) {
    GradleDslMethodCall methodCall;
    if (oldElement instanceof GradleDslMethodCall) {
      // This cast is safe, we are guaranteed to have test(e) return true.
      methodCall = (GradleDslMethodCall)oldElement;
      if (myRecognizedNames.contains(methodCall.getMethodName())) {
        GradleDslElement baseElement = transform(oldElement);
        replaceElement(methodCall, baseElement, newElement);
        return methodCall;
      }
    }

    GradleNameElement nameElement = GradleNameElement.create(name);
    methodCall = new GradleDslMethodCall(holder, nameElement, myWriteBackName);
    methodCall.addNewArgument(newElement);
    replaceElement(holder, oldElement, methodCall);
    return methodCall;
  }

  @Override
  @Nullable
  public GradleDslElement delete(@NotNull GradleDslElement holder, @NotNull GradleDslElement oldElement,
                                 @NotNull GradleDslElement transformedElement) {
    removeElement(oldElement);
    return null;
  }
}
