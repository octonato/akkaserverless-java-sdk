package org.example.valueentity

import com.akkaserverless.javasdk.impl.Serializer
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.akkaserverless.scalasdk.valueentity.ValueEntityOptions
import com.akkaserverless.scalasdk.valueentity.ValueEntityProvider
import com.google.protobuf.Descriptors
import com.google.protobuf.empty.EmptyProto
import org.example.valueentity

import scala.collection.immutable.Seq

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

object CounterProvider {
  def apply(entityFactory: ValueEntityContext => Counter): CounterProvider =
    new CounterProvider(entityFactory, ValueEntityOptions.defaults)
}

class CounterProvider private(entityFactory: ValueEntityContext => Counter, override val options: ValueEntityOptions)
  extends ValueEntityProvider[CounterState, Counter] {

  def withOptions(newOptions: ValueEntityOptions): CounterProvider =
    new CounterProvider(entityFactory, newOptions)

  override final val serviceDescriptor: Descriptors.ServiceDescriptor =
    CounterApiProto.javaDescriptor.findServiceByName("CounterService")

  override final val entityType = "counter"

  override final def newRouter(context: ValueEntityContext): CounterRouter =
    new CounterRouter(entityFactory(context))

  override final val additionalDescriptors: Seq[Descriptors.FileDescriptor] =
    CounterApiProto.javaDescriptor ::
    EmptyProto.javaDescriptor ::
    CounterDomainProto.javaDescriptor :: Nil

  override def serializer: Serializer =
    Serializer.noopSerializer
}

