package org.fuzzylumpkins.crawl

import java.util.concurrent.TimeUnit

import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import org.fuzzylumpkins.crawl.CrawlServiceFactory.AdditionalDetails
import org.fuzzylumpkins.crawl.CrawlServiceFactory.LocationDetails
import org.fuzzylumpkins.crawl.CrawlServiceFactory.OverallDetails
import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData
import org.fuzzylumpkins.crawl.CrawlServiceFactory.RoomDetails
import org.fuzzylumpkins.crawl.CrawlServiceFactory.SalesDetails
import org.fuzzylumpkins.targets.RealEstate
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver

class Century21Crawler extends Crawler with LazyLogging {

  override def collectInfo(url: String, realestateCompany: RealEstate.Company): Option[PropertyData] = {
    println(s"${this.getClass.getName} Crawling product details for url ${url}")
    System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
    val driver = new ChromeDriver()
    try {
      driver.manage().window().maximize()
      driver.manage().timeouts().implicitlyWait(10l, TimeUnit.SECONDS)
      driver.get(url)

      val title = safeGetStringWebElement(() => driver.findElement(By.xpath("//div[@class=\"property-name\"]/h3")))
      val salesId = safeGetStringWebElement(() => driver.findElement(By.xpath("//div[@class=\"property-name\"]/p")))
      val details = driver.findElement(By.xpath("//div[@class=\"main-details\"]/h4")).getText
      val splitDetails = details.split(" ")
      val numBedRooms = Some(splitDetails(0).toInt)
      val numBathRooms = Some(splitDetails(2).toInt)
      val overallDetailsExtract = driver
          .findElement(By.xpath("//div[@class=\"property-details-list\"]/ul")).getText

      if (overallDetailsExtract.length == 0) {
        println(s"${this.getClass.getName} No property details were found!")
        None
      } else {
        val additionalArr = overallDetailsExtract.split("\n")
        val priceExtracted = l1DetailsExtract(additionalArr(0))

        val state = l1DetailsExtract(additionalArr(1))
        val netArea = l2DetailsExtractToDouble(additionalArr(2), l2IndexExtract = 0)
        val rawArea = l2DetailsExtractToDouble(additionalArr(3), l2IndexExtract = 0)
        val overallCondition = l1DetailsExtract(additionalArr(4))
        val yearBuilt = l1DetailsExtract(additionalArr(7))
        val energyCertificate = l1DetailsExtract(additionalArr(8))

        val textDetails = Some(
          driver.findElement(By.xpath("//p[@class=\"description read-more-100\"]")).getText.trim)

        val overallDetails = OverallDetails(priceExtracted, year = yearBuilt, netArea = netArea,
          rawArea = rawArea, numBathRooms = numBathRooms, numBedRooms = numBedRooms,
          conditions = overallCondition, parkingDetails = None, energyCertificate = energyCertificate,
          textDescription = textDetails)
        logger.info(s"${this.getClass.getName} - Extracted overallDetails for URL ${url}: ${overallDetails}")
        val roomDetails = Try(driver
              .findElement(By.xpath("//div[@class=\"property-details-list\"][2]/ul")).getText) match {
          case Success(extract) =>
            val roomDetailsMap = extract.split("\n").map(str => {
              val kv = str.split("-")
              if (kv.isEmpty) {
                "" -> ""
              } else if (kv.length == 1) {
                kv(0).trim -> ""
              } else {
                kv(0).trim -> kv(1).trim
              }
            })
                .toMap
                .filter(kv => kv != null)
                .filter(kv => kv._1.length > 0)
            RoomDetails(roomDetailsMap)
          case Failure(t) =>
            println(s"${this.getClass.getName} Failed to get room details for URL ${url} - ${t.getMessage}")
            RoomDetails(Map.empty[String, String])
          }
        val salesRepresentative = Some(
          driver.findElement(By.xpath("//div[@class=\"agent-details\"]")).getText)
        val salesDetails = SalesDetails(id = salesId, representative = salesRepresentative,
          phone = Some(""))
        logger.info(s"Final sales details: ${salesDetails}")
        val locationDetails = LocationDetails(city = Some("Lisboa"), district = Some("Lisboa"))
        Some(PropertyData(url, title, status = state,
          company = realestateCompany.toString,
          salesDetails = salesDetails,
          overallDetails = overallDetails,
          roomDetails = roomDetails,
          locationDetails = locationDetails,
          additionalDetails = AdditionalDetails(Map.empty[String, String])
        ))
      }
    } catch {
      case e: Exception => {
        println(s"${this.getClass.getName} - Failed to crawl products for URL: ${url} --- ${e.getMessage}")
        None
      }
    } finally {
      Thread.sleep(3)
      if (driver != null) {
        driver.close()
      }
    }

  }

  override def gatherProducts: Set[String] = {
    var pageCount = 0
    var continueNextPage = true
    var countEmptyResults = 0
    while (continueNextPage) {
      pageCount += 1
      println(s"Crawling page num ${pageCount}...")
      try {
        val results = crawlForProducts(url = getPageUrl(pageCount))
        println(s"Got a total of ${results.size} results from URL: ${getPageUrl(pageCount)}")
        if (results.isEmpty) {
          countEmptyResults += 1
        }
        if (countEmptyResults >= 20 || pageCount >= 2) {
          continueNextPage = false
        }
        productUrls ++= results
      }
      catch {
        case e: Exception => println(s"Failed to get next page: ${e.getMessage}")
      }
    }
    productUrls.toSet
  }

  def crawlForProducts(url: String): Set[String] = {
    println(s"Starting product gathering in URL: ${url}")
    System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
    val driver = new ChromeDriver()

    driver.manage().window().maximize()
    driver.manage().timeouts().implicitlyWait(10l, TimeUnit.SECONDS)

    driver.get(url)
    try {
      val allLinks = driver.findElements(By.tagName("a"))
          .asScala
          .toSet
      val linksFiltered = allLinks.map(e => e.getAttribute("href"))
          .filter(l => l != null)
      if (linksFiltered.nonEmpty) {
        val linksFiltered2 = linksFiltered
            .filter(
              link => link.startsWith("https://www.century21.pt/comprar/apartamento/lisboa"))
        linksFiltered2
      } else {
        Set.empty[String]
      }
    } catch {
      case e: Exception => {
        println(s"Failed to crawl products for URL --- ${e.getMessage}")
        Set.empty[String]
      }
    } finally {
      Thread.sleep(3)
      if (driver != null) {
        driver.close()
      }
      Thread.sleep(3)
    }
  }

  private def getPageUrl(pageNum: Int): String = s"https://www.century21.pt/comprar/apartamento/todos-os-concelhos/?v=c&ord=date-desc&page=${pageNum}&numberOfElements=12&q=lisboa&ptd=Apartamento&be=2"

}
