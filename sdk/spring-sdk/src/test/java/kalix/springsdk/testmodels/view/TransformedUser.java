package kalix.springsdk.testmodels.view;

import com.fasterxml.jackson.annotation.JsonCreator;

public class TransformedUser {
  public final String name;
  public final String email;

  @JsonCreator
  public TransformedUser(String name, String email) {
    this.name = name;
    this.email = email;
  }
}
