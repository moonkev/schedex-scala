name := "schedex"

version := "1.0"

scalaVersion := "2.11.8"

val jacksonVersion = "2.7.4"

//libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.2"
libraryDependencies += "org.apache.mesos" % "mesos" % "0.28.2"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.7.4"
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
libraryDependencies += "com.github.jnr" % "jnr-process" % "0.1"


