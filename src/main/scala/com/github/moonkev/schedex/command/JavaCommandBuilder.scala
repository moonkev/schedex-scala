package com.github.moonkev.schedex.command

/**
 * Created by digitalx on 6/14/16.
 */
object JavaCommandBuilder {

  def buildCommand(config: Map[String, Any]) = {
    val command = scala.collection.mutable.ListBuffer.empty[String]

    if (config.contains("java_home")) command += config("java_home") + "/bin/java"
    else if (sys.env.contains("JAVA_HOME")) command += sys.env("JAVA_HOME") + "/bin/java"
    else command += "java"

    if (config.contains("app_name")) command += "-Dapp.name=" + config("app_name")

    if (config.contains("min_heap")) command += "-Xms" + config("min_heap")

    if (config.contains("max_heap")) command += "-Xmx" + config("max_heap")

    if (config.contains("xx_options")) {
      for ((name, value) <- config("xx_options").asInstanceOf[Map[String, Either[Boolean, Any]]]) {
        value match {
          case Left(value) => command += "-XX:" + (if (value) "+" else "-") + name
          case Right(value) => command += "-XX:" + value.toString + name
        }
      }
    }

    if (config.contains("system_properties")) {
      for ((name, value) <- config("system_properties").asInstanceOf[Map[String, Any]]) {
        command += "-D" + name + "=" + value
      }
    }

    if (config.contains("jvm_args")) config("jvm_args").asInstanceOf[List[Any]].foreach(command += _.toString)

    if (config.contains("classpath")) {
      command += "-cp"
      command += config("classpath").asInstanceOf[List[String]].mkString(":")
    }

    if (config.contains("class")) command += config("class").toString
    else if (config.contains("jar")) {
      command += "-jar"
      command += config("jar").toString
    }
    else throw new CommandBuilderException("You must specify either a class or a jar for java type application")

    if (config.contains("args")) config("args").asInstanceOf[List[Any]].foreach(command += _.toString)

    command
  }
}

