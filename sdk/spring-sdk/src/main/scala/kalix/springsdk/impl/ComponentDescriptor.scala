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

import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.springsdk.impl.reflection.DynamicMethodInfo
import kalix.springsdk.impl.reflection.NameGenerator
import kalix.springsdk.impl.reflection.ParameterExtractors.HeaderExtractor
import kalix.springsdk.impl.reflection.RestServiceIntrospector.HeaderParameter
import kalix.springsdk.impl.reflection.RestServiceIntrospector.UnhandledParameter

object ComponentDescriptor {
  def apply[T](component: Class[T]): ComponentDescriptor = {
    val nameGenerator = new NameGenerator
    val methodInfos = ComponentType.selectMethods(component, nameGenerator)
    ComponentDescriptor(component, nameGenerator, methodInfos)
  }
}
case class ComponentDescriptor(component: Class[_], nameGenerator: NameGenerator, methodsInfo: Seq[DynamicMethodInfo]) {

  private val grpcService = ServiceDescriptorProto.newBuilder()
  grpcService.setName(nameGenerator.getName(component.getSimpleName))

  private val messageDescriptors = methodsInfo.flatMap { methodInfo =>
    grpcService.addMethod(methodInfo.grpcMethod)
    // Input message descriptor can be a None. That happens when the desired type is Any.
    // In such case, we should not add an `Any` as a message to the generated proto file
    // And we can't properly filter it out because that's a DescriptorProto and it has no package information.
    // Therefore, our best option is to use a None to exclude it.
    methodInfo.inputMessageDescriptor
  }

  def serviceDescriptor: Descriptors.ServiceDescriptor =
    fileDescriptor.findServiceByName(grpcService.getName)

  val fileDescriptor: Descriptors.FileDescriptor =
    ProtoDescriptorGenerator.genFileDescriptor(
      component.getName,
      component.getPackageName,
      grpcService.build(),
      messageDescriptors)

  val methods: Map[String, ComponentMethod] = methodsInfo.map { method =>

    val message = method.inputMessageDescriptor
      .map { inputDescriptor =>
        fileDescriptor.findMessageTypeByName(inputDescriptor.getName)
      }
      // when empty descriptor, we should fallback to Any
      // see explanation why it can be a None
      .getOrElse(ScalaPbAny.javaDescriptor)

    val extractors = method.restMethod.params.zipWithIndex.map { case (param, idx) =>
      // First, see if we have an extractor for it to extract from the dynamic message
      method.extractors.find(_._1 == idx) match {
        case Some((_, creator)) =>
          creator(message)
        case None =>
          // Yet to resolve this parameter, resolve now
          param match {
            case hp: HeaderParameter =>
              new HeaderExtractor[AnyRef](hp.name, identity)
            case UnhandledParameter(param) =>
              throw new RuntimeException("Unhandled parameter: " + param)
          }
      }
    }
    method.grpcMethod.getName -> ComponentMethod(
      method.restMethod.javaMethod,
      method.grpcMethod.getName,
      extractors.toArray,
      message)
  }.toMap
}
