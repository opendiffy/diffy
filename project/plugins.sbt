//resolvers ++= Seq(
//  "Twitter's Repository" at "https://maven.twttr.com/",
//  "jgit-repo" at "http://download.eclipse.org/jgit/maven"
//)
resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Resolver.sonatypeRepo("snapshots")
)

val releaseVersion = "19.8.0"

addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % releaseVersion)
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "2.0.0-RC2-1")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")