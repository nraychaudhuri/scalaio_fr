package controllers

import akka.actor.{Props, Actor}
import akka.dispatch.RequiresMessageQueue
import play.api.Play
import play.api.mvc._
import play.api.libs.concurrent.Akka
import nbbmq.akka.mailbox._

object RealtimeTrafficData extends Controller {

  val analyzer = Akka.system(Play.current).actorOf(TrafficDataAnalyzer.props, "analyzer")
  def trafficData = Action { implicit request =>
    analyzer ! TrafficDataAnalyzer.Analyze(request.body)
    Ok("Done")
  }

}



object TrafficDataAnalyzer {
  case class Analyze(rawData: Any)

  def props = Props(new TrafficDataAnalyzer)
}

class TrafficDataAnalyzer extends Actor with RequiresMessageQueue[NonBlockingBoundedMailbox] {

  import TrafficDataAnalyzer._

  override def receive = {
    case Analyze(raw) =>
      //This is where you push work to worker actors
      doTimeConsumingWork(raw)
  }

  //cpu bound
  private def doTimeConsumingWork(raw: Any) = {
    pi(200 * 500)
  }

  private def pi(m: Long) = {
    def gregoryLeibnitz(n: Long) = 4.0 * (1 - (n % 2) * 2) / (n * 2 + 1)
    var n = 0
    var acc = BigDecimal(0.0)
    while (n < m) {
      acc += gregoryLeibnitz(n)
      n += 1
    }
    acc
  }

}
