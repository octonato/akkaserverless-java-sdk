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

package com.akkaserverless.javasdk.impl.view

import com.akkaserverless.javasdk.view.UpdateHandlerContext

/**
 * INTERNAL API
 */
private[impl] final case class ViewException(viewId: String,
                                             commandName: String,
                                             message: String,
                                             cause: Option[Throwable])
    extends RuntimeException(message, cause.orNull)

/**
 * INTERNAL API
 */
private[impl] object ViewException {
  def apply(message: String): ViewException =
    ViewException(viewId = "", commandName = "", message, None)

  def apply(context: UpdateHandlerContext, message: String, cause: Option[Throwable]): ViewException =
    ViewException(context.viewId, context.commandName, message, cause)

}
