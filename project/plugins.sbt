resolvers ++= Seq(
  "Twitter's Repository" at "https://maven.twttr.com/",
  "jgit-repo" at "http://download.eclipse.org/jgit/maven"
)

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.7.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
