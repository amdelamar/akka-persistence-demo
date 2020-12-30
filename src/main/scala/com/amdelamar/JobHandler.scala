package com.amdelamar

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.amdelamar.persistence.{Job, JobPersistActor}
import com.typesafe.scalalogging.Logger

import java.util.UUID
import scala.concurrent.ExecutionContext

class JobHandler(implicit system: ActorSystem, ec: ExecutionContext) {
  private val logger = Logger(getClass.getName)
  private val jobPersistActorRef = system.actorOf(Props(classOf[JobPersistActor], ec))

  val routes: Route = {
    pathEndOrSingleSlash {
      complete("App is up.\n")
    } ~
    post {
      path("jobs" / Segment) { name =>

        val job = Job(name, UUID.randomUUID)
        logger.info(s"Received job: $job")

        // Send to Actor for later processing
        jobPersistActorRef ! job

        complete(StatusCodes.OK, s"Received job: $job\n")
      }
    }
  }

}
