
package actors

import akka.actor._
import akka.persistence.{SnapshotOffer, PersistentActor}
import play.api.Logger
import akka.contrib.pattern.ShardRegion
import scala.concurrent.duration._

case class FieldInformation(state: Any)


class TractorJobStream extends PersistentActor {
  import TractorJobStream._

  var state: FieldInformation = _

  // passivate the entity when no activity
  context.setReceiveTimeout(2.minutes)

  override def receiveRecover: Receive = {
    case js: JobStateChanged => updateState(js)

  }

  override def persistenceId: String = s"tractor-jobs-${self.path.name}"

  override def receiveCommand: Receive = {
  	case Start(jobId) =>
      Logger.info("New job is created")
      loadLastSavedStateFromStorage()
  	case Stop(jobId) =>
      Logger.info("Stopping the actor as job finished")
      context.stop(self)
  	case ChangeState(jobId, newState) => persist(JobStateChanged(jobId, newState))(updateState)

    case ReceiveTimeout => context.parent ! ShardRegion.Passivate(stopMessage = PoisonPill)
  }



  private def updateState(gs: JobStateChanged) = {
    Logger.info(s"Applying the delta change for the jobId = ${gs.jobId}")
    state = state.copy(state = gs.newState)
  }

  private def loadLastSavedStateFromStorage() = {
    state = FieldInformation("initial state")
  }

}

object TractorJobStream {

  val shardName = "tractors"

  val idExtractor: ShardRegion.IdExtractor = {
    case m: JobMessage => (m.jobId, m)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case m:JobMessage => m.jobId.toString
  }

  def props: Props = Props(classOf[TractorJobStream])

  trait JobMessage { val jobId: String }


  private case object TakeSnapshot

  //commands
  case class Start(jobId: String) extends JobMessage
  case class Stop(jobId: String) extends JobMessage
  case class ChangeState(jobId: String, newState: Any) extends JobMessage

  //events
  case class JobStateChanged(jobId: String, newState: Any) extends JobMessage

}