import sbt._

object Dependencies {
  object AkkaServerless {
    val ProtocolVersionMajor = 0
    val ProtocolVersionMinor = 7
    val FrameworkVersion = "0.7.0-beta.9"
  }

  // changing the Scala version of the Java SDK affects end users
  val ScalaVersion = "2.13.5"

  // Note: sync with Protobuf version in Akka gRPC and ScalaPB
  // https://github.com/akka/akka-grpc/blob/v1.1.1/project/Dependencies.scala#L77
  val ProtobufVersion = "3.15.1"

  val AkkaVersion = "2.6.14"
  val AkkaHttpVersion = "10.2.4" // Note: should at least the Akka HTTP version required by Akka gRPC
  val ScalaTestVersion = "3.2.7"
  val JacksonDatabindVersion = "2.11.4" // Akka 2.6.14: 2.11.4, google-http-client-jackson2 1.34.0: 2.10.1
  val DockerBaseImageVersion = "adoptopenjdk/openjdk11:debianslim-jre"
  val LogbackVersion = "1.2.3"
  val LogbackContribVersion = "0.1.5"
  val TestContainersVersion = "1.15.3"
  val JUnitVersion = "4.13.2"
  val JUnitInterfaceVersion = "0.11"
  val JUnitJupiterVersion = "5.7.1"

  val excludeTheseDependencies: Seq[ExclusionRule] = Seq(
    // exclusion rules can be added here
  )

  def akkaDependency(name: String, excludeThese: ExclusionRule*) =
    "com.typesafe.akka" %% name % AkkaVersion excludeAll ((excludeTheseDependencies ++ excludeThese): _*)

  def akkaHttpDependency(name: String, excludeThese: ExclusionRule*) =
    "com.typesafe.akka" %% name % AkkaHttpVersion excludeAll ((excludeTheseDependencies ++ excludeThese): _*)

}
