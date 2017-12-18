crossScalaVersions := Seq("2.12.3", "2.11.11")

scalaVersion := crossScalaVersions.value.head

lazy val `password-recovery-service` = project in file(".") enablePlugins Raml2Hyperbus settings (
    name := "password-recovery-service",
    version := "0.2-SNAPSHOT",
    organization := "com.hypertino",
    resolvers ++= Seq(
      Resolver.sonatypeRepo("public")
    ),
    libraryDependencies ++= Seq(
      "com.hypertino" %% "hyperbus" % "0.3-SNAPSHOT",
      "com.hypertino" %% "hyperbus-t-inproc" % "0.3-SNAPSHOT",
      "com.hypertino" %% "service-control" % "0.3.0",
      "com.hypertino" %% "service-config" % "0.2.0" % "test",
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
      "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % "test",
      "org.jvnet.mock-javamail" % "mock-javamail" % "1.9" % "test",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    ),
    ramlHyperbusSources := Seq(
      ramlSource(
        path = "api/password-recovery-service-api/password-recovery.raml",
        packageName = "com.hypertino.services.passwordrecovery.api",
        isResource = false
      ),
      ramlSource(
        path = "api/auth-pin-service-api/auth-pin.raml",
        packageName = "com.hypertino.services.passwordrecovery.apiref.authpin",
        isResource = false
      ),
      ramlSource(
        path = "api/email-service-api/email.raml",
        packageName = "com.hypertino.services.passwordrecovery.apiref.email",
        isResource = false
      ),
      ramlSource(
        path = "api/user-service-api/user.raml",
        packageName = "com.hypertino.services.passwordrecovery.apiref.user",
        isResource = false
      )
    )
)
