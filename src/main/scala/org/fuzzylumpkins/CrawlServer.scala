package org.fuzzylimpkins

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import org.fuzzylumpkins.crawl.RealEstate


class CrawlServer extends Actor {
  
  val clients: mutable.Map[RealEstate.Company, Set[ActorRef]] = mutable.Map[RealEstate.Company, Set[ActorRef]]()
  val controllers: mutable.Map[RealEstate.Company, ActorRef] = mutable.Map[RealEstate.Company, ActorRef]()

  def receive = {
    case CrawlServer.CrawlRequest(realestate, depth) =>
      controllers.get(realestate) match {
        case Some(actor) =>
          sender ! CrawlServer.CrawlInProgress(s"Crawl of URL ${realestate} is already in progress, nothing to do")
          clients(realestate) += sender
        case None => 
          val worker = context.actorOf(Props(new Worker))
          worker ! Worker.Crawl(realestate, 1)
          controllers += (realestate -> worker)
          clients += (realestate -> Set(sender))
      }

        case Worker.Result(realestate, rawBlob, links) =>
          context.stop(controllers(realestate))
          clients(realestate) foreach (_ ! CrawlServer.CrawlResponse(realestate, rawBlob, links))
          clients -= realestate
          controllers -= realestate
        
  }

}

object CrawlServer {
  case class CrawlRequest(realestate: RealEstate.Company, depth: Integer)
  case class CrawlInProgress(msg: String)
  case class CrawlResponse(realestate: RealEstate.Company, rawBlob: String, links: Set[String])
}
