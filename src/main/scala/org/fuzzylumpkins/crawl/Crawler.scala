package org.fuzzylumpkins.crawl

import scala.collection.mutable

import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData

trait Crawler {
  val productUrls: mutable.Set[String] = mutable.Set.empty[String]
  def gatherProducts: Set[String]
  def collectInfo(url: String, realestateCompany: RealEstate.Company): Option[PropertyData]
}
