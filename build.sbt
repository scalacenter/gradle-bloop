ThisBuild / resolvers ++= List(
  MavenRepository(
    "Gradle releases",
    "https://repo.gradle.org/gradle/libs-releases-local/"
  ),
  MavenRepository("Android plugin", "https://maven.google.com/")
  // TODO I don't think this is needed but let everything pass first to ensure
  // MavenRepository(
  //  "Android dependencies",
  //  "https://repo.spring.io/plugins-release/"
  // )
)

def GitHubDev(handle: String, fullName: String, email: String) =
  Developer(handle, fullName, email, url(s"https://github.com/$handle"))

ThisBuild / organization := "ch.epfl.scala"
ThisBuild / developers := List(
  GitHubDev("jvican", "Jorge Vicente Cantero", "jorge@vican.me"),
  GitHubDev("Duhemm", "Martin Duhem", "martin.duhem@gmail.com")
)
ThisBuild / licenses := Seq(
  "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
)

ThisBuild / scmInfo := Some(
  sbt.ScmInfo(
    sbt.url("https://github.com/scalacenter/gradle-bloop"),
    "scm:git:git@github.com:scalacenter/gradle-bloop.git"
  )
)

lazy val gradleVersion = "5.0"

lazy val plugin = (project in file("."))
  .settings(
    name := "gradle-bloop",
    scalaVersion := "2.13.10",
    crossScalaVersions := Seq("2.11.12", "2.12.17", scalaVersion.value),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
    libraryDependencies ++= List(
      "dev.gradleplugins" % "gradle-api" % gradleVersion % Provided,
      "dev.gradleplugins" % "gradle-test-kit" % gradleVersion % Provided,
      "org.gradle" % "gradle-core" % gradleVersion % Provided,
      "org.gradle" % "gradle-tooling-api" % gradleVersion % Provided,
      "org.codehaus.groovy" % "groovy" % "3.0.13" % Provided,
      "com.android.tools.build" % "gradle" % "4.2.2" % Provided,
      "ch.epfl.scala" %% "bloop-config" % "1.5.5",
      "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
      // TODO is this still needed
      "io.github.classgraph" % "classgraph" % "4.8.151" % Test,
      // TODO is this still needed
      "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0" % Test
    )
  )
