package org.fuzzylimpkins

import java.io.BufferedWriter
import java.io.FileWriter
import java.sql.Date
import java.text.SimpleDateFormat

import akka.actor.Actor
import com.github.tototoshi.csv.CSVWriter
import org.apache.commons.validator.routines.UrlValidator
import org.fuzzylumpkins.crawl.CrawlServiceFactory
import org.fuzzylumpkins.crawl.RealEstate


class Worker extends Actor {

  val urlValidator = new UrlValidator()
  val na = "N/A"

  def receive = {
    case Worker.Crawl(realestate, depth) =>
      val result = crawl(realestate)
      if (result.isDefined) {
        sender() ! result
      }
  }

  def crawl(realestate: RealEstate.Company): Option[Worker.Result] = {
    val csvSchema = List("real-estate-company", "title", "sales-id", "sales-representative",
      "url", "status", "price", "year-built", "net-area", "raw-area", "num-bedrooms",
      "num-bathrooms", "overall-condition", "energy-certificate", "room-details",
      "text-description"
    )
    val service = new CrawlServiceFactory
    val date = new Date(System.currentTimeMillis)
    val formater = new SimpleDateFormat("yyyy_MM_dd")
    println(s"Starting crawl for ${formater.format(date)}")
    val result: List[List[String]] = service.crawl(realestate)
        .toList
        .map(p =>
          List(p.realestate, p.title.getOrElse(na), p.salesDetails.id.getOrElse(na),
            p.salesDetails.representative.getOrElse(na), p.url, p.status.getOrElse(na), p.overallDetails.price.getOrElse(na),
            p.overallDetails.year.getOrElse(na), p.overallDetails.netArea.getOrElse(na).toString,
            p.overallDetails.rawArea.getOrElse(na).toString, p.overallDetails.numBedRooms.getOrElse(na).toString,
            p.overallDetails.numBathRooms.getOrElse(na).toString, p.overallDetails.conditions.getOrElse(na),
            p.overallDetails.energyCertificate.getOrElse(na), p.roomDetails.details.map { case (k, v) => s"${k}-${v}" }.mkString(" + "),
            p.overallDetails.textDescription.getOrElse(na)
        ))
    val outputFile = new BufferedWriter(new FileWriter(s"century_21_crawl_${formater.format(date)}.csv"))
    val csvWriter = new CSVWriter(outputFile)
    //val csvSchema = DmpPredictionsMap.getParams
    println(s"CSV schema is: [$csvSchema]")
    try {
      csvWriter.writeAll(csvSchema :: result)
    } finally {
      outputFile.close()
    }
    Some(Worker.Result(realestate, "", Set()))
  }
}

object Worker {

  case class Result(realestate: RealEstate.Company, rawBlob: String, links: Set[String])
  case class Crawl(realestate: RealEstate.Company, depth: Integer)

}
