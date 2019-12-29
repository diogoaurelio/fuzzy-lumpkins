package org.fuzzylumpkins.crawl

import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData


class CrawlServiceFactory extends LazyLogging {

  def crawl(realEstate: String): Set[PropertyData] = {
    if (realEstate.equals(RealEstateCompanies.century21.toString)) {
      val crawler = new Century21Crawler
      crawler.gatherProducts()
      println(s"Collected a total of ${crawler.productUrls.size} results")
      val results = mutable.Set.empty[PropertyData]
      crawler.productUrls.map(product => {
        println(s"Collecting data from URL ${product}")
        val res: Option[PropertyData] = crawler.collectInfo(product)
        if (res.isDefined)
          results += res.get
      })

      results.toSet
    } else {
      val error = s"Provided real-estate company '${realEstate}' is not listed in our records, " +
          s"unable to proceed"
      logger.error(error)
      sys.exit(1)
    }
  }

}

object CrawlServiceFactory {
  case class PropertyData(url: String, title: Option[String], status: Option[String],
                          realestate: String, salesDetails: SalesDetails,
                          overallDetails: OverallDetails, roomDetails: RoomDetails)
  case class OverallDetails(price: Option[String], year: Option[String], netArea: Option[Int],
                            rawArea: Option[Int], numBathRooms: Option[Int], numBedRooms: Option[Int],
                            conditions: Option[String], parkingDetails: Option[String],
                            energyCertificate: Option[String], textDescription: Option[String])
  case class RoomDetails(details: Map[String, String])
  case class SalesDetails(id: Option[String], representative: Option[String], phone: Option[String])
}
