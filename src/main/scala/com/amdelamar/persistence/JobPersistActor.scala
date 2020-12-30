package com.amdelamar.persistence

import akka.actor.{Actor, Props}
import akka.persistence.{DeleteMessagesFailure, DeleteMessagesSuccess, PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

case class ExecuteJob(seqNr: Long, job: Job)
case class ConfirmJob(seqNr: Long, job: Job)

/**
 * Main Actor that persists (saves) the incoming messages.
 *
 * Once persisted, it forwards to the JobActor to do the actual work.
 */
class JobPersistActor(implicit ec: ExecutionContext) extends PersistentActor {
  private val logger = Logger(getClass.getName)
  private val jobActorRef = context.system.actorOf(Props(classOf[JobActor], ec))

  override def persistenceId: String = "job-actor-1.0"

  override def receiveRecover: Receive = {
    case job: Job =>
      // On start, the journal is checked first for messages and fed here.
      logger.info(s"Got job during recovery: $job")
      jobActorRef ! ExecuteJob(lastSequenceNr, job)

    case RecoveryCompleted =>
      // Received after all messages in journal are recovered.
      logger.info(s"Recovery completed!")
  }

  override def receiveCommand: Receive = {
    case job: Job =>
      // Persist job to the journal immediately.
      persist(job) { _ =>
        logger.info(s"Successfully persisted job: $job")

        // Send to sub Actor for actual processing
        // only after we persist the message first.
        jobActorRef ! ExecuteJob(lastSequenceNr, job)
      }

    case c: ConfirmJob =>
      // Job finished, so we can un-persist it from the journal
      logger.info(s"Deleting job from journal: $c")
      deleteMessages(c.seqNr)

    case DeleteMessagesSuccess(seqNr) =>
      logger.info(s"Deleted from journal: $seqNr")

    case DeleteMessagesFailure(e, seqNr) =>
      logger.error(s"Failed to delete from journal: $seqNr", e)
  }
}

/**
 * Sub Actor that does the actual work.
 *
 * Only JobPersistActor should be sending this JobActor messages.
 * Nothing else should be sending messages to this actor, ensuring
 * that the JobPersistActor persists messages first before
 * processing them.
 */
class JobActor(implicit ec: ExecutionContext) extends Actor {
  private val logger = Logger(getClass.getName)

  override def receive: Receive = {
    case e: ExecuteJob =>
      executeJobNow(e).map { confirmJob =>
        logger.info(s"Successfully executed job: ${confirmJob.job}")
        // Inform parent actor the job has finished.
        // Its important that we do this, otherwise we keep
        // building up the journal with tons of messages
        // that are already processed. And a restart would
        // have us reprocess everything again from the
        // beginning of time. By confirming the job is done
        // we can safely remove it from the journal.
        sender ! confirmJob
      }.recover {
        case ex: Exception =>
          logger.error(s"Exception when executing job: ${e.job}", ex)
          // Optional retry logic.
          // Resend to parent actor to be re-processed in case
          // of exceptions or timeouts.
          sender ! e.job
      }
  }

  /** Simulate work by doing something that takes some time. */
  def executeJobNow(executeJob: ExecuteJob): Future[ConfirmJob] = {
    Thread.sleep(5.seconds.toMillis)
    Future.successful {
      ConfirmJob(executeJob.seqNr, executeJob.job)
    }
  }
}