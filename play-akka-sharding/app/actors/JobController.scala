package actors

import akka.actor._
import akka.contrib.pattern.ClusterSharding
import global.AppGlobal

class JobController extends Actor {

  import JobController._

  val region: ActorRef = ClusterSharding.apply(AppGlobal.system).shardRegion(TractorJobStream.shardName)

  def receive: Receive = {
    case CreateNewJob(jobId) =>
      region ! TractorJobStream.Start(jobId)
    case RemoveJob(jobId) =>
      region ! TractorJobStream.Stop(jobId)
    case UpdateJobState(jobId, state) =>
      region ! TractorJobStream.ChangeState(jobId, state)
  }
}


object JobController {

  def props = Props[JobController]

  case class UpdateJobState(jobId: String, state: Any)
  case class RemoveJob(jobId: String)
  case class CreateNewJob(jobId: String)

}
