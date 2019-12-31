package org.fuzzylumpkins.crawl

import scala.collection.mutable
import scala.util.Success
import scala.util.Failure
import scala.util.Try

import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData
import org.fuzzylumpkins.targets.RealEstate
import org.openqa.selenium.WebElement

trait Crawler {
  val productUrls: mutable.Set[String] = mutable.Set.empty[String]
  def gatherProducts: Set[String]
  def collectInfo(url: String, realestateCompany: RealEstate.Company): Option[PropertyData]

  def safeGetWebElement(getElm: () => WebElement): Option[WebElement] = {
    Try(getElm()) match {
      case Success(elm) => Some(elm)
      case Failure(_) => None
    }
  }

  def safeGetStringWebElement(getElm: () => WebElement): Option[String] = {
    safeGetWebElement(getElm) match {
      case Some(elm) => Try(elm.getText) match {
        case Success(e) => Some(e)
        case Failure(_) => None
      }
      case _ => None
    }
  }

  def l1DetailsExtract(str: String, splitter: String = ":",
                               indexExtract: Int = 1): Option[String] = {
    val extract = str.split(splitter)
    if (extract.isEmpty) {
      None
    }
    if (extract.length <= 1) {
      Some(extract(0).trim)
    }
    Some(extract(indexExtract).trim)
  }

  def l2DetailsExtract(str: String, splitterL1: String = ":",
                               splitterL2: String = "\\s",
                               l1IndexExtract: Int = 1,
                               l2IndexExtract: Int = 1): Option[String] = {
    l1DetailsExtract(str, splitterL1, l1IndexExtract) match {
      case Some(newStr) => l1DetailsExtract(newStr.trim, splitterL2, l2IndexExtract)
      case None => None
    }
  }

  def l2DetailsExtractToInt(str: String, splitterL1: String = ":",
                                    splitterL2: String = "\\s",
                                    l1IndexExtract: Int = 1,
                                    l2IndexExtract: Int = 1
                                   ): Option[Int] = {
    l2DetailsExtract(str, splitterL1, splitterL2, l1IndexExtract, l2IndexExtract) match {
      case Some(value) => Some(value.toInt)
      case _ => None
    }
  }
}
