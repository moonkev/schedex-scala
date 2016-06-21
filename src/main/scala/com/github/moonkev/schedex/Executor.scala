package com.github.moonkev.schedex

/**
 * Created by Kevin Mooney on 6/10/16.
 */

import java.io.{PrintWriter, StringWriter}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.moonkev.schedex.Util.Str
import com.github.moonkev.schedex.command.JavaCommandBuilder
import com.typesafe.scalalogging.LazyLogging
import org.apache.mesos.{MesosExecutorDriver, ExecutorDriver}
import org.apache.mesos.Protos._
import scala.collection.JavaConversions._
import jnr.process.{Process, ProcessBuilder}
import jnr.constants.platform.Signal

object Executor extends org.apache.mesos.Executor with LazyLogging {

  var process: Option[Process] = None

  val yamlMapper = {
    val mapper = new ObjectMapper(new YAMLFactory()) with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    mapper
  }

  def main(args: Array[String]) {
    //System.out.println(String.format("Got args = %s", args(0).toString))
    sys.addShutdownHook {
      if (process.isDefined) process.get.kill()
    }
    val driver = new MesosExecutorDriver(Executor)
    val status = if (driver.run eq Status.DRIVER_STOPPED) 0 else 1
    sys.exit(status)

  }

  def registered(driver: ExecutorDriver, executor: ExecutorInfo, framework: FrameworkInfo, slave: SlaveInfo) {
    logger.info("[registered] framework:" + Str.framework(framework) + " slave:" + Str.slave(slave))
  }

  def reregistered(driver: ExecutorDriver, slave: SlaveInfo) {
    logger.info("[reregistered] " + Str.slave(slave))
  }

  def disconnected(driver: ExecutorDriver) {
    logger.info("[disconnected]")
  }

  def launchTask(driver: ExecutorDriver, task: TaskInfo) {

    try {
      val config = yamlMapper.readValue[Map[String, Any]](Util.labels(task)("app_config"))
      val command = JavaCommandBuilder.buildCommand(config)
      val processBuilder = new ProcessBuilder(command)
      process = Some(processBuilder.start())
      driver.sendStatusUpdate(TaskStatus.newBuilder().setTaskId(task.getTaskId).setState(TaskState.TASK_RUNNING).build)

      new Thread {
        override def run() {
          setName("Stdout redirect")
          process.get.waitFor()
          driver.sendStatusUpdate(TaskStatus.newBuilder().setTaskId(task.getTaskId).setState(TaskState.TASK_FINISHED).build)
        }
      }.start()

      new Thread {
        override def run() {
          setName("Proc")
          process.get.waitFor()
          driver.sendStatusUpdate(TaskStatus.newBuilder().setTaskId(task.getTaskId).setState(TaskState.TASK_FINISHED).build)
        }
      }.start()

    } catch {
      case t: Throwable =>
        logger.error("", t)
        val stackTrace = new StringWriter()
        t.printStackTrace(new PrintWriter(stackTrace, true))
        driver.sendStatusUpdate(TaskStatus.newBuilder
          .setTaskId(task.getTaskId).setState(TaskState.TASK_FAILED)
          .setMessage(stackTrace.toString)
          .build
        )
    }
  }

  def stopExecutor(driver: ExecutorDriver, async: Boolean = false): Unit = {
    def stop0 {
      if (process.isDefined) {
        process.get.kill(Signal.SIGKILL)
        Thread.sleep(10000)
        if (process.get.exitValue == -1) {
          process.get.kill()
        }
      }
      driver.stop()
    }

    if (async)
      new Thread() {
        override def run(): Unit = {
          setName("ExecutorStopper")
          stop0
        }
      }.start()
    else
      stop0
  }

  def killTask(driver: ExecutorDriver, id: TaskID) {
    logger.info("[killTask] " + id.getValue)
  }

  def frameworkMessage(driver: ExecutorDriver, data: Array[Byte]) {
    logger.info("[frameworkMessage] " + new String(data))
  }

  def shutdown(driver: ExecutorDriver) {
    logger.info("[shutdown]")
  }

  def error(driver: ExecutorDriver, message: String) {
    logger.info("[error] " + message)
  }
}