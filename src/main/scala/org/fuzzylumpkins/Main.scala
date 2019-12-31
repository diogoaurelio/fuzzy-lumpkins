package org.fuzzylimpkins

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.fuzzylimpkins.CrawlServer.{CrawlInProgress, CrawlRequest, CrawlResponse}
import org.fuzzylumpkins.crawl.RealEstate


object Main extends App {

  println(s"Booting Fuzzy lumpkins Get off my property App at ${System.currentTimeMillis}")
  val system = ActorSystem("fuzzy")
  val managerActor = system.actorOf(Props[CrawlServer], "CrawlServer")
  val century21 = system.actorOf(Props(new Main(managerActor, 2)), name = "Century21")
}

class Main(managerActor: ActorRef, depth: Integer) extends Actor {
//  managerActor ! CrawlRequest(RealEstateCompanies.Century21, depth)
  managerActor ! CrawlRequest(RealEstate.Era, depth)

  def receive = {
    case CrawlResponse(root, rawBlob, links) => {
      println(s"Current time is ${System.currentTimeMillis}, and root is ${root}")
      println(s"Links: ${links.toList.sortWith(_.length < _.length).mkString("\n")}")
    }
    case CrawlInProgress(msg) => 
      println(s"Crawl Actor replied: ${msg}")
  }

}

