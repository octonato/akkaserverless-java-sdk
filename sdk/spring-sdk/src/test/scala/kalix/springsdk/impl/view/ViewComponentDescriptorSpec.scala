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

package kalix.springsdk.impl.view

import kalix.springsdk.impl.ComponentDescriptorSuite
import kalix.springsdk.testmodels.view.ViewTestModels.TransformedUserView
import org.scalatest.wordspec.AnyWordSpec

class ViewComponentDescriptorSpec extends AnyWordSpec with ComponentDescriptorSuite {

  "View introspector" should {

    "generate proto for a View using POST request with explicit update method" in {
      assertDescriptor[TransformedUserView] { desc =>

        val methodOptions = this.findKalixMethodOptions(desc, "OnChange")
        val entityType = methodOptions.getEventing.getIn.getValueEntity
        entityType shouldBe "user"

        methodOptions.getView.getUpdate.getTable shouldBe "users_view"
        methodOptions.getView.getUpdate.getTransformUpdates shouldBe true
        // check json input schema:  ByEmail

        val queryMethodOptions = this.findKalixMethodOptions(desc, "GetUser")
        queryMethodOptions.getView.getQuery.getQuery shouldBe "SELECT * FROM users_view WHERE email = :email"

      // check json output schema:  TransformedUser

      }
    }

    "generate proto for a View using POST request" in {
      // TODOs:
      // * check virtual update method
      //   * has table
      //   * is transform = false
      // * check query method
      // * check table type descriptor
      // * check request type descriptor
      pending
    }

    "generate proto for a View using POST request with path param " in {
      // TODOs:
      // * check virtual update method
      //   * has table
      //   * is transform = false
      // * check query method
      // * check table type descriptor
      // * check request type descriptor (json_body + path params)
      pending
    }

    "generate proto for a View using GET request with path param" in {
      // TODOs:
      // * check virtual update method
      //   * has table
      //   * is transform = false
      // * check query method
      // * check table type descriptor
      // * check request type descriptor (json_body + path params)
      pending
    }

    "not allow @Subscribe annotations in mixed levels" in {
      // it should be annotated either on type or on method level
      pending
    }

  }
}
