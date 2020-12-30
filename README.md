# Akka Persistence Demo

A simple persistent actor demo with Akka Persistence. It uses LevelDB to write an actor's mailbox of messages
to the local disk before processing them. In case of sudden restarts, JVM crashes, or a power outage for the
host, the PersistentActor can recover incomplete messages from the journal and reprocess them on next start.

Requires [JDK 11](https://adoptopenjdk.net/) and [sbt 1.3+](https://www.scala-sbt.org/).

## Start

1. Start the Akka Http server:

```bash
$ cd akka-persistence-demo
$ sbt run
```

## Send some requests

2. Open a new terminal and send a few HTTP calls to the REST API:

```bash
$ curl -X POST "127.0.0.1:8080/jobs/hello-world"
```

## View the logs

3. Monitor the logs to see jobs be persisted and slowly executed, thus simulating some long process an Actor might do.

```bash
[info] running com.amdelamar.App 
13:53:52.063 [INFO ] Recovery completed!
13:53:52.456 [INFO ] App is running at http://localhost:8080/
13:54:28.540 [INFO ] Received job: Job(hello-world,13065a91-7c27-4a23-8750-841c4b3eb906)
13:54:28.600 [INFO ] Successfully persisted job: Job(hello-world,13065a91-7c27-4a23-8750-841c4b3eb906)
13:54:33.605 [INFO ] Successfully executed job: Job(hello-world,13065a91-7c27-4a23-8750-841c4b3eb906)
13:54:33.606 [INFO ] Deleting job from journal: ConfirmJob(2,Job(hello-world,13065a91-7c27-4a23-8750-841c4b3eb906))
13:54:33.624 [INFO ] Deleted from journal: 2
```

The job execution simply waits for 5 seconds, but you can imagine real work being done in production.

After a job is executed its deleted from the journal.

## Interrupt & Resume

4. Send a few HTTP calls to the REST API:

Its easier to see the job execution order if you use incrementing numbers.

```bash
$ curl -X POST "127.0.0.1:8080/jobs/hello-1"
$ curl -X POST "127.0.0.1:8080/jobs/hello-2"
$ curl -X POST "127.0.0.1:8080/jobs/hello-3"
```

5. Kill the server with `Ctrl+C` before the jobs are finished, thus simulating a power outage or JVM crash.

```bash
13:55:50.083 [INFO ] Received job: Job(hello-1,04b72dae-4a6a-40b2-bef5-58ad73e6ebc3)
13:55:50.098 [INFO ] Successfully persisted job: Job(hello-1,04b72dae-4a6a-40b2-bef5-58ad73e6ebc3)
13:55:51.491 [INFO ] Received job: Job(hello-2,1ae450f4-f0c4-4f8e-9306-334a3b60810d)
13:55:51.493 [INFO ] Successfully persisted job: Job(hello-2,1ae450f4-f0c4-4f8e-9306-334a3b60810d)
13:55:52.826 [INFO ] Received job: Job(hello-3,e6ce6606-78bc-427d-af38-ab380529ff38)
13:55:52.839 [INFO ] Successfully persisted job: Job(hello-3,e6ce6606-78bc-427d-af38-ab380529ff38)
^C
[warn] Canceling execution...
[error] Total time: 129 s (02:09), completed Dec 30, 2020, 1:55:54 PM
[warn] Run canceled.
```

On the next start we should see the jobs are recovered from the journal and continue processing.

6. Start the Akka Http server with `sbt run`.

```bash
[info] running com.amdelamar.App 
13:56:34.210 [INFO ] Got job during recovery: Job(hello-1,04b72dae-4a6a-40b2-bef5-58ad73e6ebc3)
13:56:34.214 [INFO ] Got job during recovery: Job(hello-2,1ae450f4-f0c4-4f8e-9306-334a3b60810d)
13:56:34.214 [INFO ] Got job during recovery: Job(hello-3,e6ce6606-78bc-427d-af38-ab380529ff38)
13:56:34.214 [INFO ] Recovery completed!
13:56:34.555 [INFO ] App is running at http://localhost:8080/
13:56:39.217 [INFO ] Successfully executed job: Job(hello-1,04b72dae-4a6a-40b2-bef5-58ad73e6ebc3)
13:56:39.221 [INFO ] Deleting job from journal: ConfirmJob(3,Job(hello-1,04b72dae-4a6a-40b2-bef5-58ad73e6ebc3))
13:56:39.247 [INFO ] Deleted from journal: 3
13:56:44.219 [INFO ] Successfully executed job: Job(hello-2,1ae450f4-f0c4-4f8e-9306-334a3b60810d)
13:56:44.220 [INFO ] Deleting job from journal: ConfirmJob(4,Job(hello-2,1ae450f4-f0c4-4f8e-9306-334a3b60810d))
13:56:44.234 [INFO ] Deleted from journal: 4
13:56:49.221 [INFO ] Successfully executed job: Job(hello-3,e6ce6606-78bc-427d-af38-ab380529ff38)
```

All jobs are recovered and resumed processing after the interruption.
