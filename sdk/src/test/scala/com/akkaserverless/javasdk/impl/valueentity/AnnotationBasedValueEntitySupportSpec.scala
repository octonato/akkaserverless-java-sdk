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

package com.akkaserverless.javasdk.impl.valueentity

import com.akkaserverless.javasdk.impl.reply.MessageReplyImpl
import com.akkaserverless.javasdk.valueentity._
import com.akkaserverless.javasdk.impl.{AnySupport, ResolvedServiceMethod, ResolvedType}
import com.akkaserverless.javasdk.{Reply, EntityContext => _, _}
import com.example.valueentity.shoppingcart.ShoppingCartApi
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{ByteString, Any => JavaPbAny}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import java.util.Optional
import scala.compat.java8.OptionConverters._

class AnnotationBasedValueEntitySupportSpec extends AnyWordSpec with Matchers {
  trait BaseContext extends Context {
    override def serviceCallFactory(): ServiceCallFactory = new ServiceCallFactory {
      override def lookup[T](serviceName: String, methodName: String, messageType: Class[T]): ServiceCallRef[T] =
        throw new NoSuchElementException
    }
  }

  object MockContext extends ValueEntityContext with BaseContext {
    override def entityId(): String = "foo"
  }

  class MockCommandContext(override val commandName: String = "AddItem", state: Option[JavaPbAny] = None)
      extends CommandContext[JavaPbAny]
      with BaseContext {
    var currentState: Option[JavaPbAny] = state
    override def commandId(): Long = 20
    override def getState(): Optional[JavaPbAny] = currentState.asJava
    override def updateState(newState: JavaPbAny): Unit = currentState = Some(newState)
    override def deleteState(): Unit = currentState = None
    override def entityId(): String = "foo"
    override def metadata(): Metadata = ???
    override def fail(errorMessage: String): RuntimeException = ???
    override def forward(to: ServiceCall): Unit = ???
    override def effect(effect: ServiceCall, synchronous: Boolean): Unit = ???
  }

  object WrappedResolvedType extends ResolvedType[Wrapped] {
    override def typeClass: Class[Wrapped] = classOf[Wrapped]
    override def typeUrl: String = AnySupport.DefaultTypeUrlPrefix + "/wrapped"
    override def parseFrom(bytes: ByteString): Wrapped = Wrapped(bytes.toStringUtf8)
    override def toByteString(value: Wrapped): ByteString = ByteString.copyFromUtf8(value.value)
  }

  object StringResolvedType extends ResolvedType[String] {
    override def typeClass: Class[String] = classOf[String]
    override def typeUrl: String = AnySupport.DefaultTypeUrlPrefix + "/string"
    override def parseFrom(bytes: ByteString): String = bytes.toStringUtf8
    override def toByteString(value: String): ByteString = ByteString.copyFromUtf8(value)
  }

  case class Wrapped(value: String)

  val anySupport = new AnySupport(Array(ShoppingCartApi.getDescriptor), this.getClass.getClassLoader)
  val serviceDescriptor = ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService")

  def method(name: String = "AddItem"): ResolvedServiceMethod[String, Wrapped] =
    ResolvedServiceMethod(serviceDescriptor.findMethodByName(name), StringResolvedType, WrappedResolvedType)

  def create(behavior: AnyRef, methods: ResolvedServiceMethod[_, _]*): ValueEntityHandler =
    new AnnotationBasedEntitySupport(behavior.getClass,
                                     anySupport,
                                     methods.map(m => m.descriptor.getName -> m).toMap,
                                     Some(_ => behavior)).create(MockContext)

  def create(clazz: Class[_]): ValueEntityHandler =
    new AnnotationBasedEntitySupport(clazz, anySupport, Map.empty, None).create(MockContext)

  def command(str: String) =
    ScalaPbAny.toJavaProto(ScalaPbAny(StringResolvedType.typeUrl, StringResolvedType.toByteString(str)))

  def decodeWrapped(reply: Reply[JavaPbAny]): Wrapped =
    reply match {
      case MessageReplyImpl(any, _, _) =>
        decodeWrapped(any)
    }

  def decodeWrapped(any: JavaPbAny): Wrapped = {
    any.getTypeUrl should ===(WrappedResolvedType.typeUrl)
    WrappedResolvedType.parseFrom(any.getValue)
  }

  def state(any: Any): JavaPbAny = anySupport.encodeJava(any)

  "Value based entity annotation support" should {
    "support entity construction" when {

      "there is a noarg constructor" in {
        create(classOf[NoArgConstructorTest])
      }

      "there is a constructor with an EntityId annotated parameter" in {
        create(classOf[EntityIdArgConstructorTest])
      }

      "there is a constructor with a EventSourcedEntityCreationContext parameter" in {
        create(classOf[CreationContextArgConstructorTest])
      }

      "there is a constructor with multiple parameters" in {
        create(classOf[MultiArgConstructorTest])
      }

      "fail if the constructor contains an unsupported parameter" in {
        a[RuntimeException] should be thrownBy create(classOf[UnsupportedConstructorParameter])
      }

    }

    "support command handlers" when {

      "no arg command handler" in {
        val handler = create(new {
          @CommandHandler
          def addItem() = Wrapped("blah")
        }, method())
        decodeWrapped(handler.handleCommand(command("nothing"), new MockCommandContext)) should ===(Wrapped("blah"))
      }

      "no arg command handler with Reply" in {
        val handler = create(new {
          @CommandHandler
          def addItem(): Reply[Wrapped] = Reply.message(Wrapped("blah"))
        }, method())
        decodeWrapped(handler.handleCommand(command("nothing"), new MockCommandContext)) should ===(Wrapped("blah"))
      }

      "single arg command handler" in {
        val handler = create(new {
          @CommandHandler
          def addItem(msg: String) = Wrapped(msg)
        }, method())
        decodeWrapped(handler.handleCommand(command("blah"), new MockCommandContext)) should ===(Wrapped("blah"))
      }

      "multi arg command handler" in {
        val handler = create(
          new {
            @CommandHandler
            def addItem(msg: String, @EntityId eid: String, ctx: CommandContext[JavaPbAny]): Wrapped = {
              eid should ===("foo")
              ctx.commandName() should ===("AddItem")
              Wrapped(msg)
            }
          },
          method()
        )
        decodeWrapped(handler.handleCommand(command("blah"), new MockCommandContext)) should ===(Wrapped("blah"))
      }

      "read state" in {
        val handler = create(
          new {
            @CommandHandler
            def getCart(msg: String, ctx: CommandContext[JavaPbAny]): Wrapped = {
              ctx.getState().asScala.get.asInstanceOf[String] should ===("state")
              ctx.commandName() should ===("GetCart")
              Wrapped(msg)
            }
          },
          method("GetCart")
        )
        val ctx = new MockCommandContext("GetCart", Some(state("state")))
        decodeWrapped(handler.handleCommand(command("blah"), ctx)) should ===(Wrapped("blah"))
      }

      "update state" in {
        val handler = create(
          new {
            @CommandHandler
            def addItem(msg: String, ctx: CommandContext[JavaPbAny]): Wrapped = {
              ctx.updateState(state(msg + " state"))
              ctx.commandName() should ===("AddItem")
              Wrapped(msg)
            }
          },
          method()
        )
        val ctx = new MockCommandContext
        decodeWrapped(handler.handleCommand(command("blah"), ctx)) should ===(Wrapped("blah"))
        ctx.currentState.get should ===(state("blah state"))
      }

      "delete state" in {
        val handler = create(
          new {
            @CommandHandler
            def removeCart(msg: String, ctx: CommandContext[JavaPbAny]): Wrapped = {
              ctx.deleteState()
              ctx.commandName() should ===("RemoveCart")
              Wrapped(msg)
            }
          },
          method("RemoveCart")
        )
        val ctx = new MockCommandContext("RemoveCart")
        decodeWrapped(handler.handleCommand(command("blah"), ctx)) should ===(Wrapped("blah"))
        ctx.currentState should ===(None)
      }

      "fail if there's a bad context type" in {
        a[RuntimeException] should be thrownBy create(new {
          @CommandHandler
          def addItem(msg: String, ctx: BaseContext) =
            Wrapped(msg)
        }, method())
      }

      "fail if there's two command handlers for the same command" in {
        a[RuntimeException] should be thrownBy create(new {
          @CommandHandler
          def addItem(msg: String, ctx: CommandContext[JavaPbAny]) =
            Wrapped(msg)
          @CommandHandler
          def addItem(msg: String) =
            Wrapped(msg)
        }, method())
      }

      "fail if there's no command with that name" in {
        a[RuntimeException] should be thrownBy create(new {
          @CommandHandler
          def wrongName(msg: String) =
            Wrapped(msg)
        }, method())
      }

      "unwrap exceptions" in {
        val handler = create(new {
          @CommandHandler
          def addItem(): Wrapped = throw new RuntimeException("foo")
        }, method())
        val ex = the[RuntimeException] thrownBy handler.handleCommand(command("nothing"), new MockCommandContext)
        ex.getStackTrace()(0)
          .toString should include regex """.*AnnotationBasedValueEntitySupportSpec.*addItem.*AnnotationBasedValueEntitySupportSpec\.scala:\d+"""
        ex.toString should ===("java.lang.RuntimeException: foo")
      }

      "fail if there's a ReplicatedEntity command handler" in {
        val ex = the[RuntimeException] thrownBy create(new {
            @com.akkaserverless.javasdk.replicatedentity.CommandHandler
            def addItem(msg: String) =
              Wrapped(msg)
          }, method())
        ex.getMessage should include("Did you mean")
        ex.getMessage should include(classOf[CommandHandler].getName)
      }

      "fail if there's an EventSourcedEntity command handler" in {
        val ex = the[RuntimeException] thrownBy create(new {
            @com.akkaserverless.javasdk.eventsourcedentity.CommandHandler
            def addItem(msg: String) =
              Wrapped(msg)
          }, method())
        ex.getMessage should include("Did you mean")
        ex.getMessage should include(classOf[CommandHandler].getName)
      }

    }

  }
}

import org.scalatest.matchers.should.Matchers._

@ValueEntity(entityType = "NoArgConstructorTest")
private class NoArgConstructorTest() {}

@ValueEntity(entityType = "EntityIdArgConstructorTest")
private class EntityIdArgConstructorTest(@EntityId entityId: String) {
  entityId should ===("foo")
}

@ValueEntity(entityType = "CreationContextArgConstructorTest")
private class CreationContextArgConstructorTest(ctx: ValueEntityCreationContext) {
  ctx.entityId should ===("foo")
}

@ValueEntity(entityType = "MultiArgConstructorTest")
private class MultiArgConstructorTest(ctx: ValueEntityContext, @EntityId entityId: String) {
  ctx.entityId should ===("foo")
  entityId should ===("foo")
}

@ValueEntity(entityType = "UnsupportedConstructorParameter")
private class UnsupportedConstructorParameter(foo: String)
