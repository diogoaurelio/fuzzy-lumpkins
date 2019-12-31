package org.fuzzylumpkins.actors

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData
import org.fuzzylumpkins.targets.RealEstate


class CrawlMasterActor extends Actor {

  val clients: mutable.Map[RealEstate.Company, Set[ActorRef]] = mutable
      .Map[RealEstate.Company, Set[ActorRef]]()
  val controllers: mutable.Map[RealEstate.Company, ActorRef] = mutable
      .Map[RealEstate.Company, ActorRef]()

  def receive = {
    case CrawlMasterActor.CrawlRequest(realestate) =>
      controllers.get(realestate) match {
        case Some(actor) =>
          sender ! CrawlMasterActor
              .CrawlInProgress(s"Crawl of URL ${realestate} is already in progress, nothing to do")
          clients(realestate) += sender
        case None =>
          val worker = context.actorOf(Props(new CrawlWorkerActor))
          worker ! CrawlWorkerActor.Crawl(realestate)
          controllers += (realestate -> worker)
          clients += (realestate -> Set(sender))
      }

    case CrawlWorkerActor.FinalResult(realestate, propertyData) =>
      println(s"Received final result for real estate ${realestate.toString} - a total of ${
        propertyData.size
      } results")
      context.stop(controllers(realestate))
      clients(realestate) foreach (_ ! CrawlMasterActor.CrawlResponse(realestate, propertyData))
      clients -= realestate
      controllers -= realestate

  }

}

object CrawlMasterActor {

  case class CrawlRequest(realestate: RealEstate.Company)

  case class CrawlInProgress(msg: String)

  case class CrawlResponse(realestate: RealEstate.Company, propertyData: Set[PropertyData])

}
