package controllers

import akka.actor.{Props, Actor}
import akka.stream.{MaterializerSettings, FlowMaterializer}
import akka.util.Timeout
import org.reactivestreams.{Subscription, Subscriber, Publisher}
import play.api._
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current
import scala.concurrent.{Future, Promise}

import scala.concurrent.duration._

import akka.stream.scaladsl.Flow


object Application extends Controller {
  //curl -X POST -d @Skype_5.8.0.1027.dmg http://127.0.0.1:9000

  //val bodyParser = BodyParser( request => Iteratee.fold[Array[Byte],Int](0)((c,_) => c+1 ).map(Right(_)))

  def bodyParser(consumer: Array[Byte] => Future[Int]) = BodyParser( rh => iteratee(rh, consumer))

  def iteratee(rh: RequestHeader, consumer: Array[Byte] => Future[Int]): Iteratee[Array[Byte], Either[Result, Int]] = {

    implicit val _ = play.api.libs.concurrent.Akka.system
    implicit val m = FlowMaterializer(MaterializerSettings())

    val publisher = new OneToOnePublisher
    Flow.apply(publisher).mapFuture(consumer)

    publisher.iteratee()
  }


  val consumer: Array[Byte] => Future[Int] = (bytes) => play.api.libs.concurrent.Promise.timeout(1, 200 milliseconds)

  def index = Action(bodyParser(consumer)) { rq =>
    Ok("got " + rq.body + " chunks")
  }

}



object ProducerActor {

  case class Request(count: Int)

  case class Offer(bytes: Array[Byte])

  case class Subscribe(subscriber: Subscriber[Array[Byte]])

  def props = Props(new ProducerActor)
}

class ProducerActor extends Actor {

  import ProducerActor._
  import akka.pattern._

  var pendingRequests = 0
  var counter = 0
  var sub1: Option[Subscriber[Array[Byte]]] = None
  var pendingPromise: Option[(Offer, Promise[Int])] = None

  override def receive: Receive = {
    case Request(count) =>
      pendingRequests += count
      if(pendingPromise.isDefined) {
        val (offer, p) = pendingPromise.get
        self ! offer
        counter += 1
        p.success(counter)
        pendingPromise = None
      }
    case o@Offer(bytes) =>
      if(pendingRequests > 0 && sub1.isDefined) {
         sub1.get.onNext(bytes)
         counter += 1
         pendingRequests -= 1
         sender() ! counter
      } else {
         val p = Promise[Int]
         pendingPromise = Some((o, p))
         p.future pipeTo sender()
      }

    case Subscribe(sub) =>
      sub1 = Some(sub)
      sub.onSubscribe(new Subscription {
        override def cancel(): Unit = ???

        override def request(p1: Int): Unit = self ! Request(p1)
      })

  }

}


class OneToOnePublisher extends Publisher[Array[Byte]] {

  import akka.pattern._
  import scala.concurrent.duration._

  implicit val timeout = Timeout(5 seconds)

  val ref = play.api.libs.concurrent.Akka.system.actorOf(ProducerActor.props)

  override def subscribe(sub: Subscriber[Array[Byte]]): Unit = {
    ref ! ProducerActor.Subscribe(sub)
  }

  def offer(bytes: Array[Byte]): Future[Int] = {
    (ref ? ProducerActor.Offer(bytes)).mapTo[Int]
  }


  def iteratee() = {
    Iteratee.foldM[Array[Byte], Int](0)((c, bytes) => offer(bytes)).map(Right(_))
  }


}