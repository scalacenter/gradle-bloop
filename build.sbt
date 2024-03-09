ThisBuild / resolvers ++= List(
  MavenRepository(
    "Gradle releases",
    "https://repo.gradle.org/gradle/libs-releases-local/"
  ),
  MavenRepository("Android plugin", "https://maven.google.com/"),
  MavenRepository("Android dependencies", "https://repo.spring.io/plugins-release/")
)

def GitHubDev(handle: String, fullName: String, email: String) =
  Developer(handle, fullName, email, url(s"https://github.com/$handle"))

ThisBuild / organization := "ch.epfl.scala"
ThisBuild / homepage := Some(url("https://github.com/scalacenter/gradle-bloop"))
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

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
ThisBuild / semanticdbEnabled := true

lazy val V = new {
  val androidGradle = "4.2.2"
  val bloopConfig = "1.5.5"
  val classgraph = "4.8.168"
  val gradle = "5.0"
  val groovy = "3.0.21"
  val junitInterface = "0.13.3"

  val scala211 = "2.11.12"
  val scala212 = "2.12.19"
  val scala213 = "2.13.13"
}

lazy val plugin = (project in file("."))
  .settings(
    name := "gradle-bloop",
    scalaVersion := V.scala213,
    crossScalaVersions := Seq(V.scala211, V.scala212, V.scala213),
    scalacOptions += (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => "-Ywarn-unused-import"
      case Some((2, 12)) => "-Ywarn-unused"
      case Some((2, 13)) => "-Wunused"
      case _ => ""
    }),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-a"),
    libraryDependencies ++= List(
      "dev.gradleplugins" % "gradle-api" % V.gradle % Provided,
      "dev.gradleplugins" % "gradle-test-kit" % V.gradle % Provided,
      "org.gradle" % "gradle-core" % V.gradle % Provided,
      "org.gradle" % "gradle-tooling-api" % V.gradle % Provided,
      "org.codehaus.groovy" % "groovy" % V.groovy % Provided,
      "com.android.tools.build" % "gradle" % V.androidGradle % Provided,
      "ch.epfl.scala" %% "bloop-config" % V.bloopConfig,
      "com.github.sbt" % "junit-interface" % V.junitInterface % Test,
      "io.github.classgraph" % "classgraph" % V.classgraph % Test
    )
  )
