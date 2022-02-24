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

package akkaserverless.javasdk.valueentity;

import kalix.javasdk.EntityId;

public class Increase {

  @EntityId
  final public String counterId;
  final public long increaseBy;

  public Increase(String counterId, long increaseBy) {
    this.counterId = counterId;
    this.increaseBy = increaseBy;
  }
}
