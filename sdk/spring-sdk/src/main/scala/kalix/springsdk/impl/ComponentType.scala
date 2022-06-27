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

package kalix.springsdk.impl

import kalix.springsdk.annotations.Entity
import kalix.springsdk.annotations.Query
import kalix.springsdk.annotations.Subscribe
import kalix.springsdk.annotations.Table
import kalix.springsdk.impl.reflection.DynamicMethodInfo
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.RestServiceIntrospector

object ComponentType {

  def selectMethods[T](component: Class[T], nameGenerator: NameGenerator): Seq[DynamicMethodInfo] =
    inferComponent(component, nameGenerator).methodsInfo

  private def inferComponent[T](component: Class[T], nameGenerator: NameGenerator): ComponentType[T] = {
    if (component.getAnnotation(classOf[Entity]) != null)
      EntityComponent(component, nameGenerator)
    else if (component.getAnnotation(classOf[Table]) != null)
      ViewComponent(component, nameGenerator)
    else
      ActionComponent(component, nameGenerator)

  }
}
sealed trait ComponentType[T] {
  def component: Class[T]
  def nameGenerator: NameGenerator
  def methodsInfo: Seq[DynamicMethodInfo]
}

case class ActionComponent[T](component: Class[T], nameGenerator: NameGenerator) extends ComponentType[T] {
  override def methodsInfo: Seq[DynamicMethodInfo] = {
    val restService = RestServiceIntrospector.inspectService(component)
    restService.methods.map { restMethod =>

      val hasSubscriptionAnnotations =
        restMethod.javaMethod.getAnnotation(classOf[Subscribe.ValueEntity]) != null

      if (hasSubscriptionAnnotations)
        DynamicMethodInfo.buildSubscriptionMethod(restMethod, nameGenerator)
      else
        DynamicMethodInfo.buildSyntheticMessageDescriptor(restMethod, nameGenerator)
    }
  }
}

case class EntityComponent[T](component: Class[T], nameGenerator: NameGenerator) extends ComponentType[T] {
  val entityKeys: Seq[String] = component.getAnnotation(classOf[Entity]).entityKey()

  override def methodsInfo: Seq[DynamicMethodInfo] = {
    val restService = RestServiceIntrospector.inspectService(component)
    restService.methods.map { restMethod =>
      DynamicMethodInfo.buildSyntheticMessageDescriptor(restMethod, nameGenerator, entityKeys)
    }
  }
}
case class ViewComponent[T](component: Class[T], nameGenerator: NameGenerator) extends ComponentType[T] {

  val tableName: String = component.getAnnotation(classOf[Table]).value()

  val hasTypeLevelValueEntitySubs = component.getAnnotation(classOf[Subscribe.ValueEntity]) != null

  override def methodsInfo: Seq[DynamicMethodInfo] = {
    val restService = RestServiceIntrospector.inspectService(component)

    restService.methods.map { restMethod =>
      // TODO: validate - all update and query methods must have same return type
      // is query method?
      if (restMethod.javaMethod.getAnnotation(classOf[Query]) != null) {
        // TODO generate TableType
        DynamicMethodInfo.buildSyntheticMessageDescriptor(restMethod, nameGenerator)
      } else {

        val hasMethodLevelValueEntitySubs =
          restMethod.javaMethod.getAnnotation(classOf[Subscribe.ValueEntity]) != null

        if (hasTypeLevelValueEntitySubs && hasMethodLevelValueEntitySubs)
          throw new IllegalArgumentException(
            "Mixed usage of @Subscribe.ValueEntity annotations. " +
            "You should either use it at type level or at method level, not both.")
        else
          DynamicMethodInfo.buildSubscriptionMethod(restMethod, nameGenerator)
      }

    }
  }
}
