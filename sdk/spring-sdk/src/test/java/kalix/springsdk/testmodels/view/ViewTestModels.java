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

package kalix.springsdk.testmodels.view;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.testmodels.valueentity.User;
import kalix.springsdk.testmodels.valueentity.UserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class ViewTestModels {

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class) // when types are annotated, it's implicitly a transform = false
  public static class UserByEmailWithGet extends View<User> {

    @Query("SELECT * FROM users_view WHERE name = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      return null; // TODO: user should not implement this. we need to find a nice API for this
    }

    @Override
    public User emptyState() {
      return null; // TODO: user should not have to implement this when not transforming
    }
  }


  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByEmailWithPost extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }

    @Override
    public User emptyState() {
      return null;
    }
  }


  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByNameEmailWithPost extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/{name}")
    public User getUser(@PathVariable String name, @RequestBody ByEmail byEmail) {
      return null;
    }

    @Override
    public User emptyState() {
      return null;
    }
  }

  @Table("users_view")
  public static class TransformedUserView extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    @PostMapping("/users/on-change")
    public UpdateEffect<TransformedUser> onChange(@RequestBody User user) {
      return effects().updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-name")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }

    @Override
    public TransformedUser emptyState() {
      return null;
    }
  }

  /**
   * This should be illegal.
   * Either we subscribe at type level, and it's a transform = false.
   * Or we subscribe at method level, and it's a transform = true.
   */
  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithSubscriptionsInMixedLevels extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    @PostMapping("/users/on-change")
    public UpdateEffect<TransformedUser> onChange(@RequestBody User user) {
      return effects().updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-name")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }

    @Override
    public TransformedUser emptyState() {
      return null;
    }
  }
}
