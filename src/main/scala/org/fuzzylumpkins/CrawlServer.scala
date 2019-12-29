package org.fuzzylimpkins

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props


class CrawlServer extends Actor {
  
  val clients: mutable.Map[String, Set[ActorRef]] = mutable.Map[String, Set[ActorRef]]()
  val controllers: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  def receive = {
    case CrawlServer.CrawlRequest(url, depth) => 
      controllers.get(url) match {
        case Some(actor) =>
          sender ! CrawlServer.CrawlInProgress(s"Crawl of URL ${url} is already in progress, nothing to do")
          clients(url) += sender
        case None => 
          val worker = context.actorOf(Props(new Worker))
          worker ! Worker.Crawl(url, 1)
          controllers += (url -> worker) 
          clients += (url -> Set(sender))
      }

        case Worker.Result(url, rawBlob, links) =>
          context.stop(controllers(url))
          clients(url) foreach (_ ! CrawlServer.CrawlResponse(url, rawBlob, links))
          clients -= url
          controllers -= url
        
  }

}

object CrawlServer {
  case class CrawlRequest(url: String, depth: Integer)
  case class CrawlInProgress(msg: String)
  case class CrawlResponse(url: String, rawBlob: String, links: Set[String])
}
