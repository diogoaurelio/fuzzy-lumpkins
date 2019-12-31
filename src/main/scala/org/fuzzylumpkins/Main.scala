package org.fuzzylimpkins

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import org.fuzzylumpkins.actors.CrawlMasterActor
import org.fuzzylumpkins.actors.CrawlMasterActor.CrawlInProgress
import org.fuzzylumpkins.actors.CrawlMasterActor.CrawlRequest
import org.fuzzylumpkins.actors.CrawlMasterActor.CrawlResponse
import org.fuzzylumpkins.actors.ExporterActor
import org.fuzzylumpkins.actors.ExporterActor.CsvExport
import org.fuzzylumpkins.targets.RealEstate


object Main extends App {

  println(s"Booting Fuzzy lumpkins Get off my property App at ${System.currentTimeMillis}")
  val system = ActorSystem("fuzzy")
  val managerActor = system.actorOf(Props[CrawlMasterActor], "CrawlMaster")
  val exporterActor = system.actorOf(Props[ExporterActor], "ExporterActor")
  val mainApp = system.actorOf(Props(new Main(managerActor, exporterActor)), name = "MainApp")
}

class Main(managerActor: ActorRef, exporterActor: ActorRef) extends Actor {

  val na = "N/A"

  //managerActor ! CrawlRequest(RealEstate.Century21)
  managerActor ! CrawlRequest(RealEstate.Era)

  def receive = {
    case CrawlResponse(company, propertyData) => {
      if (propertyData.size > 0) {

        println(
          s"Crawl finished for company ${company}, current time is ${System.currentTimeMillis}")
        val csvSchema = List("real-estate-company", "title", "sales-id", "sales-representative",
          "url", "status", "price", "year-built", "net-area", "raw-area", "num-bedrooms",
          "num-bathrooms", "overall-condition", "energy-certificate", "room-details",
          "text-description"
        )

        val data: List[List[String]] = propertyData
            .toList
            .map(p =>
              List(p.realestate, p.title.getOrElse(na), p.salesDetails.id.getOrElse(na),
                p.salesDetails.representative.getOrElse(na), p.url, p.status.getOrElse(na),
                p.overallDetails.price.getOrElse(na),
                p.overallDetails.year.getOrElse(na),
                p.overallDetails.netArea.getOrElse(na).toString,
                p.overallDetails.rawArea.getOrElse(na).toString,
                p.overallDetails.numBedRooms.getOrElse(na).toString,
                p.overallDetails.numBathRooms.getOrElse(na).toString,
                p.overallDetails.conditions.getOrElse(na),
                p.overallDetails.energyCertificate.getOrElse(na),
                p.roomDetails.details.map { case (k, v) => s"${k}-${v}" }.mkString(" + "),
                p.overallDetails.textDescription.getOrElse(na)
              ))
        exporterActor ! CsvExport(company.toString, csvSchema, data)
      } else {
        println(s"${this.getClass.getName} - Crawl size is empty, thus nothing to do!")
      }
    }
    case CrawlInProgress(msg) => 
      println(s"Crawl Master Actor replied: ${msg}")
  }

}

