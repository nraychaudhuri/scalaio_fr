package controllers

import controllers.Application._
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.Iteratee
import play.api.mvc._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._

object Uploader extends Controller {

  //curl -X POST -d @QConLondon2013-GilTene-HowNOTtoMeasureLatency.pdf http://127.0.0.1:9000

  val bodyParser = BodyParser { request =>
    Iteratee.foldM[Array[Byte], Int](0)(uploadToS3).map(Right(_))
  }

  def up = Action(bodyParser) { rq =>
    Ok("got " + rq.body + " chunks")
  }


  private def uploadToS3(count: Int, bytes: Array[Byte]): Future[Int] = ???
}
