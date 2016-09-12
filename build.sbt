lazy val root = (project in file(".")).
  settings(
    name := "multilang",
    version := "0.1_2016-09-12",
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies += "com.lihaoyi" %% "upickle" % "0.4.1",
    mainClass in Compile := Some("Multilang"),
    packAutoSettings
  )
