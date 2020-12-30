package com.amdelamar

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.Logger

import scala.util.Try

object App {

  private val logger = Logger("App")
  val HOST = Try(sys.env("HOST")).getOrElse("localhost")
  val PORT = Try(sys.env("PORT").toInt).getOrElse(8080)

  implicit val system = ActorSystem("my-app")
  implicit val executionContext = system.dispatchers.lookup("custom-dispatcher")

  def main(args: Array[String]): Unit = {

    val jobHandler = new JobHandler()

    // This Akka Http setup is not really important. Check JobPersistActor for persistence.
    Http().newServerAt(HOST, PORT)
      .bind(jobHandler.routes)
      .map { _ =>
        logger.info(s"App is running at http://$HOST:$PORT/")
      } recover {
      case ex =>
        logger.error(s"Failed to bind to $HOST:$PORT.", ex.getMessage)
    }
  }
}
