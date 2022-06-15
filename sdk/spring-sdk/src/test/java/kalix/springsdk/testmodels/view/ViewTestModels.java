package kalix.springsdk.testmodels.view;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.testmodels.valueentity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class ViewTestModels {


  @Table(value = "users_view")
  // when types are annotated, it's implicitly a transform = false
  @Subscribe.ValueEntity(entityType = "users")
  public class UserByEmailWithGet extends View<User> {

    @Query("SELECT * FROM users_view WHERE name = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      /*
        message GetUserRequest
          string email = 2;
      */
      return null; // TODO: user should not implement this and we need to find a nice API
    }

    @Override
    public User emptyState() {
      return null; // TODO: user should not have to implement this when not transforming
    }
  }


  @Table(value = "users_view")
  @Subscribe.ValueEntity(entityType = "users")
  public class UserByEmailWithPost extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      /*
        message GetUserRequest
          Any json_body =  1;
      */
      return null;
    }

    @Override
    public User emptyState() {
      return null;
    }
  }


  @Table(value = "users_view")
  @Subscribe.ValueEntity(entityType = "users")
  public class UserByNameEmailWithPost extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/{name}")
    public User getUser(@PathVariable String name, @RequestBody ByEmail byEmail) {
       /*
        message GetUserRequest
          Any json_body =  1;
          string name = 2;
      */
      return null;
    }

    @Override
    public User emptyState() {
      return null;
    }
  }

  @Table("users_view")
  public class TransformedUserView extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(entityType = "users")
    @PostMapping("/users/on-change")
    public UpdateEffect<TransformedUser> onChange(@RequestBody User user) {
      return effects().updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-name")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      /*
        message GetUserRequest
          Any json_body =  1;
      */
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
  @Subscribe.ValueEntity(entityType = "users")
  public class ViewWithSubscriptionsInMixedLevels extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(entityType = "users")
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
