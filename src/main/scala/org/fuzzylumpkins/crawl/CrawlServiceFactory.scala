package org.fuzzylumpkins.crawl

import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData
import org.fuzzylumpkins.targets.RealEstate


class CrawlServiceFactory extends LazyLogging {

  def collectInfo(realEstate: RealEstate.Company, products: Set[String]): Set[PropertyData] = {
    val crawler = getCrawler(realEstate)
    val results = mutable.Set.empty[PropertyData]
    products.foreach(product => {
      println(s"${this.getClass.getName} - Collecting data from URL ${product}")
      val res: Option[PropertyData] = crawler.collectInfo(product, realEstate)
      if (res.isDefined) {
        results += res.get
      }
    })
    results.toSet
  }

  def gatherProducts(realEstate: RealEstate.Company): Set[String] = {
    val crawler: Crawler = getCrawler(realEstate)
    println(s"${this.getClass.getName} - Collected a total of ${crawler.productUrls.size} results")
    crawler.gatherProducts
  }

  private def getCrawler(realEstate: RealEstate.Company): Crawler = {
    realEstate match {
      case RealEstate.Century21 => new Century21Crawler
      case RealEstate.Era => new EraCrawler
      case _ =>
        val error = s"${
          this.getClass.getName
        } - Provided real-estate company '${realEstate}' is not listed in our records, " +
            s"unable to proceed"
        logger.error(error)
        sys.exit(1)
    }
  }
}

object CrawlServiceFactory {

  case class PropertyData(url: String, title: Option[String], status: Option[String],
                          purpose: String = "sell", company: String,
                          salesDetails: SalesDetails,
                          overallDetails: OverallDetails,
                          roomDetails: RoomDetails,
                          locationDetails: LocationDetails,
                          additionalDetails: AdditionalDetails,
                         )

  case class OverallDetails(price: Option[String],
                            year: Option[String],
                            netArea: Option[Double],
                            rawArea: Option[Double],
                            numBathRooms: Option[Int],
                            numBedRooms: Option[Int],
                            conditions: Option[String],
                            parkingDetails: Option[String] = None,
                            energyCertificate: Option[String] = None,
                            textDescription: Option[String])

  case class LocationDetails(city: Option[String], district: Option[String],
                             parish: Option[String] = None,
                             extraDetails: Option[String] = None)

  case class RoomDetails(details: Map[String, String])

  case class AdditionalDetails(details: Map[String, String])

  case class SalesDetails(id: Option[String], representative: Option[String], phone: Option[String])

}
