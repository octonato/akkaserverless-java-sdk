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

package kalix.springsdk.impl.valueentity

import com.google.protobuf.Descriptors.FieldDescriptor.JavaType
import kalix.springsdk.impl.ComponentDescriptorSuite
import kalix.springsdk.testmodels.valueentity.ValueEntitiesTestModels.PostWithEntityKeys
import org.scalatest.wordspec.AnyWordSpec

class ValueEntityComponentDescriptorSpec extends AnyWordSpec with ComponentDescriptorSuite {

  "ValueEntity introspector" should {
    "generate mappings for a Value Entity with entity keys in path" in {
      assertDescriptor[PostWithEntityKeys] { desc =>
        val method = desc.methods("CreateEntity")
        assertMessage(method, "json_body", JavaType.MESSAGE)

        assertMessage(method, "userId", JavaType.STRING)
        assertEntityKeyField(method, "userId")

        assertMessage(method, "cartId", JavaType.STRING)
        assertEntityKeyField(method, "cartId")
      }
    }
  }

}
