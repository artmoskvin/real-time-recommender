logLevel := Level.Warn

// This will allow you to download the plugin directly from Maven Central.
// It's simply instructing SBT what path to use to retrieve it from there.
def outworkersPattern: Patterns = {
  val pattern = "[organisation]/[module](_[scalaVersion])(_[sbtVersion])/[revision]/[artifact]-[revision](-[classifier]).[ext]"

  Patterns(
    pattern :: Nil,
    pattern :: Nil,
    isMavenCompatible = true
  )
}

resolvers ++= Seq(
  // whatever is already in here..,
  Resolver.bintrayRepo("websudos", "oss-releases"),
  Resolver.url(
    "Maven Ivy Websudos",
    url(Resolver.DefaultMavenRepositoryRoot)
  )(outworkersPattern),
  Resolver.url(
    "Outworkers OSS",
    url("http://dl.bintray.com/websudos/oss-releases")
  )(Resolver.ivyStylePatterns)
)

// And finally the plugin dependency itself
addSbtPlugin("com.websudos" %% "phantom-sbt" % "1.22.0")