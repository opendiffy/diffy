import sbt.util

import scala.language.reflectiveCalls

logLevel := util.Level.Info

val diffyVersion = "2.0.0-SNAPSHOT"
val finagleVersion = "19.8.0"

lazy val buildSettings = Seq(
  version := diffyVersion,
  scalaVersion := "2.12.8",
  crossScalaVersions := Seq("2.11.12", "2.12.8"),
  scalaModuleInfo := scalaModuleInfo.value.map(_.withOverrideScalaVersion(true)),
  fork in Test := true,
  javaOptions in Test ++= travisTestJavaOptions
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  // sbt-pgp's publishSigned task needs this defined even though it is not publishing.
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo")))
)

def travisTestJavaOptions: Seq[String] = {
  // When building on travis-ci, we want to suppress logging to error level only.
  val travisBuild = sys.env.getOrElse("TRAVIS", "false").toBoolean
  if (travisBuild) {
    Seq(
      "-DSKIP_FLAKY=true",
      "-Dsbt.log.noformat=true",
      "-Dorg.slf4j.simpleLogger.defaultLogLevel=error",
      "-Dcom.twitter.inject.test.logging.disabled",
      // Needed to avoid cryptic EOFException crashes in forked tests
      // in Travis with `sudo: false`.
      // See https://github.com/sbt/sbt/issues/653
      // and https://github.com/travis-ci/travis-ci/issues/3775
      "-Xmx3G")
  } else {
    Seq(
      "-DSKIP_FLAKY=true")
  }
}

lazy val versions = new {
  // When building on travis-ci, querying for the branch name via git commands
  // will return "HEAD", because travis-ci checks out a specific sha.
  val travisBranch = sys.env.getOrElse("TRAVIS_BRANCH", "")

  // All Twitter library releases are date versioned as YY.MM.patch
  val twLibVersion = finagleVersion

  val agrona = "0.9.22"
  val bijectionCore = "0.9.5"
  val commonsCodec = "1.9"
  val commonsFileupload = "1.4"
  val commonsLang = "2.6"
  val fastutil = "8.1.1"
  val guice = "4.0"
  val jackson = "2.9.9"
  val jodaConvert = "1.2"
  val jodaTime = "2.5"
  val junit = "4.12"
  val kafka = "2.2.0"
  val libThrift = "0.10.0"
  val logback = "1.1.7"
  val mockito = "1.9.5"
  val mustache = "0.8.18"
  val nscalaTime = "2.14.0"
  val rocksdbjni = "5.14.2"
  val scalaCheck = "1.13.4"
  val scalaGuice = "4.1.0"
  val scalaTest = "3.0.0"
  val slf4j = "1.7.21"
  val snakeyaml = "1.24"
  val specs2 = "2.4.17"
}

lazy val scalaCompilerOptions = scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xlint",
  "-Ywarn-unused-import"
)

lazy val finatraDependencies = Seq(
  "com.twitter" %% "finatra-http" % finagleVersion,
  "com.twitter" %% "finatra-http" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "inject-app" % finagleVersion % "test",
  "com.twitter" %% "inject-app" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "inject-core" % finagleVersion % "test",
  "com.twitter" %% "inject-core" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "inject-modules" % finagleVersion % "test",
  "com.twitter" %% "inject-modules" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "inject-server" % finagleVersion % "test",
  "com.twitter" %% "inject-server" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "twitter-server-logback-classic" % finagleVersion,
  "io.netty" % "netty-tcnative-boringssl-static" % "2.0.25.Final",
  "ch.qos.logback" % "logback-classic" % versions.logback
)

lazy val scroogeDependencies = Seq(
  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finagle-thriftmux" % finagleVersion,
  "com.twitter" %% "scrooge-generator" % finagleVersion,
  "com.twitter" %% "scrooge-generator" % finagleVersion % "test" classifier "tests",
  "com.twitter" %% "scrooge-core" % finagleVersion,
  "com.twitter" %% "scrooge-core" % finagleVersion % "test" classifier "tests"
)
lazy val testDependencies = Seq(
  "org.mockito" % "mockito-core" %  versions.mockito % Test,
  "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test,
  "org.scalatest" %% "scalatest" %  versions.scalaTest % Test,
  "org.specs2" %% "specs2-core" % versions.specs2 % Test,
  "org.specs2" %% "specs2-junit" % versions.specs2 % Test,
  "org.specs2" %% "specs2-mock" % versions.specs2 % Test
)

lazy val baseSettings = Seq(
  libraryDependencies ++= finatraDependencies,
  libraryDependencies ++= scroogeDependencies,
  libraryDependencies ++= testDependencies,
  libraryDependencies ++= Seq(
    "javax.mail" % "mail" % "1.4.7",
    "org.jsoup" % "jsoup" % "1.7.2",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases")
  ),
  scalaCompilerOptions,
  javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint:unchecked"),
  javacOptions in doc ++= Seq("-source", "1.8"),
  // -a: print stack traces for failing asserts
  testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v"),
  // broken in 2.12 due to: https://issues.scala-lang.org/browse/SI-10134
  scalacOptions in (Compile, doc) ++= {
    if (scalaVersion.value.startsWith("2.12")) Seq("-no-java-comments")
    else Nil
  }
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Compile := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/opendiffy/diffy")),
  autoAPIMappings := true,
  apiURL := Some(url("https://opendiffy.github.io/diffy/scaladocs/")),
  excludeFilter in (Compile, managedSources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, unmanagedSources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, managedResources) := HiddenFileFilter || "BUILD",
  excludeFilter in (Compile, unmanagedResources) := HiddenFileFilter || "BUILD",
  pomExtra :=
    <scm>
      <url>git://github.com/opendiffy/diffy.git</url>
      <connection>scm:git://github.com/opendiffy/diffy.git</connection>
    </scm>
      <developers>
        <developer>
          <id>diffy</id>
          <name>Diffy AI</name>
          <url>https://www.diffy.ai/</url>
        </developer>
      </developers>,
  pomPostProcess := { (node: scala.xml.Node) =>
    val rule = new scala.xml.transform.RewriteRule {
      override def transform(n: scala.xml.Node): scala.xml.NodeSeq =
        n.nameToString(new StringBuilder()).toString() match {
          case "dependency" if (n \ "groupId").text.trim == "org.scoverage" => Nil
          case _ => n
        }
    }

    new scala.xml.transform.RuleTransformer(rule).transform(node).head
  },
  resourceGenerators in Compile += Def.task {
    val dir = (resourceManaged in Compile).value
    val file = dir / "com" / "twitter" / name.value / "build.properties"
    val buildRev = scala.sys.process.Process("git" :: "rev-parse" :: "HEAD" :: Nil).!!.trim
    val buildName = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date)
    val contents = s"name=${name.value}\nversion=${version.value}\nbuild_revision=$buildRev\nbuild_name=$buildName"
    IO.write(file, contents)
    Seq(file)
  }.taskValue
)

lazy val slf4jSimpleTestDependency = Seq(
  libraryDependencies ++= Seq(
    "org.slf4j" % "slf4j-simple" % versions.slf4j % Test
  )
)

lazy val projectSettings = baseSettings ++ buildSettings ++ publishSettings ++ slf4jSimpleTestDependency ++ Seq(
  organization := "ai.diffy"
)

lazy val diffy = project.in(file("."))
  .settings(
    moduleName := "diffy",
    assemblyJarName := "diffy-server.jar",
    excludeFilter in unmanagedResources := HiddenFileFilter || "BUILD",
    unmanagedResourceDirectories in Compile +=
      baseDirectory.value / "src" / "main" / "webapp",
    assemblyMergeStrategy in assembly := {
      case "BUILD" => MergeStrategy.discard
      case "META-INF/io.netty.versions.properties" => MergeStrategy.last
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case _ => MergeStrategy.last

    }
  )
  .settings(buildSettings ++ baseSettings ++ publishSettings)

def aggregatedProjects = Seq[sbt.ProjectReference](diffy)

def mappingContainsAnyPath(mapping: (File, String), paths: Seq[String]): Boolean = {
  paths.foldLeft(false)(_ || mapping._1.getPath.contains(_))
}