/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.springsdk.valueentity;

import com.google.protobuf.Descriptors;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.valueentity.ValueEntityOptions;
import kalix.javasdk.valueentity.ValueEntityProvider;
import kalix.springsdk.annotations.Entity;
import kalix.springsdk.impl.ComponentDescriptor;
import kalix.springsdk.impl.SpringSdkMessageCodec;
import kalix.springsdk.impl.valueentity.ReflectiveValueEntityRouter;

import java.util.Optional;
import java.util.function.Function;

public class ReflectiveValueEntityProvider<S, E extends ValueEntity<S>>
    implements ValueEntityProvider<S, E> {

  private final String entityType;
  private final Function<ValueEntityContext, E> factory;
  private final ValueEntityOptions options;
  private final Descriptors.FileDescriptor fileDescriptor;
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final ComponentDescriptor componentDescriptor;

  public static <S, E extends ValueEntity<S>> ReflectiveValueEntityProvider<S, E> of(
      Class<E> cls, Function<ValueEntityContext, E> factory) {
    return new ReflectiveValueEntityProvider<>(cls, factory, ValueEntityOptions.defaults());
  }

  public ReflectiveValueEntityProvider(
      Class<E> entityClass, Function<ValueEntityContext, E> factory, ValueEntityOptions options) {

    Entity annotation = entityClass.getAnnotation(Entity.class);
    if (annotation == null)
      throw new IllegalArgumentException(
          "Value Entity [" + entityClass.getName() + "] is missing '@Entity' annotation");

    this.entityType = annotation.entityType();

    this.factory = factory;
    this.options = options;

    this.componentDescriptor = ComponentDescriptor.apply(entityClass);

    this.fileDescriptor = componentDescriptor.fileDescriptor();
    this.serviceDescriptor = componentDescriptor.serviceDescriptor();
  }

  @Override
  public ValueEntityOptions options() {
    return options;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return serviceDescriptor;
  }

  @Override
  public String entityType() {
    return entityType;
  }

  @Override
  public ValueEntityRouter<S, E> newRouter(ValueEntityContext context) {
    E entity = factory.apply(context);
    return new ReflectiveValueEntityRouter<>(entity, componentDescriptor.methods());
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {fileDescriptor};
  }

  @Override
  public Optional<MessageCodec> alternativeCodec() {
    return Optional.of(SpringSdkMessageCodec.instance());
  }
}
