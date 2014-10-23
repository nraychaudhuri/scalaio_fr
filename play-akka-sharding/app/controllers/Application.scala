package controllers

import akka.actor.ActorRef
import global.AppGlobal
import play.api._
import play.api.mvc._
import actors.JobController

object Application extends Controller {

  val jobController: ActorRef = AppGlobal.system.actorOf(JobController.props, "controller")

  def index = Action {
    Ok("game node is up")
  }

  def createJob(jobId: String) = Action {
    jobController ! JobController.CreateNewJob(jobId)
    Ok("new job is created")
  }

  def updateJobState(jobId: String) = Action { request =>
    jobController ! JobController.UpdateJobState(jobId, state = request.getQueryString("newState"))
    Ok("user state is updated")
  }

  def removeJob(jobId: String) = Action {
    jobController ! JobController.RemoveJob(jobId)
    Ok(s"job ${jobId} is removed")

  }

}


