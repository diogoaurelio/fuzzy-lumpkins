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
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver

class EraCrawler extends Crawler with LazyLogging {

  override def collectInfo(url: String,
                           realestateCompany: RealEstate.Company): Option[PropertyData] = {
    println(s"${this.getClass.getName} - Crawling product details for url ${url}")
    System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver")
    val driver = new ChromeDriver()
    try {
      //driver.manage().window().maximize()
      driver.manage().timeouts().implicitlyWait(3l, TimeUnit.SECONDS)
      driver.get(url)

      // overall
      val title = safeGetStringWebElement(
        () => driver.findElement(By.xpath("//div[@class=\"titulos\"]/h2")))
              .map(s => s.trim)

      val state = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_txt_estado\"]")))
          .map(s => s.trim)

      val priceExtract = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_preco_venda\"]")))
          .map(s => s.trim)
      val rawAreaExtract: Option[Double] = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_area_bruta\"]")))
          .map(s => Try(s.trim.split("m")(0).toDouble) match {
            case Success(e) => e
            case Failure(_) => 0
          })
      val textDetails = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_div_texto_imovel\"]")))
          .map(s => s.trim)

      val overallDetails = OverallDetails(price = priceExtract, year = None, netArea = None,
        rawArea = rawAreaExtract, numBathRooms = None, numBedRooms = None, conditions = None,
        energyCertificate = None, textDescription = textDetails)

      println(s"PUTA OVERALL DETAILS: ${overallDetails}")

      val roomDetailsExtract = safeGetStringWebElement(() => driver.findElement(By.xpath("//div[@id=\"ctl00_ContentPlaceHolder1_tabshow1\"]")))
          .map(details => {
            val roomDetailsExtract = details.split("\n").map(str => {
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
            RoomDetails(details = roomDetailsExtract)
          })
          .headOption
      println(s"PUTA ROOM DETAILS ${roomDetailsExtract}")

      // additional details
      val additionalDetails: Option[AdditionalDetails] = safeGetStringWebElement(() => driver.findElement(By.xpath("//div[@id=\"ctl00_ContentPlaceHolder1_tabshow0\"]")))
          .map(details => {
            val additionalDetailsExtract  = details.split("\n").map(str => {
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
            AdditionalDetails(details = additionalDetailsExtract)
          })
          .headOption

      println(s"PUTA Additional Details: ${additionalDetails}")


      // sales
      val salesId = safeGetStringWebElement(
        () => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_ref\"]")))
          .map(elm => {
            val arr = elm.split("/ REF:")
            if (arr.length > 1)
              arr(1).trim
            else
              arr(0).trim
          })

      val salesDetails = SalesDetails(id = salesId, representative = None, phone = None)

      // location
      val locationExtraDetails = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_zona\"]")))
          .map(s => s.trim)
      val parish = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_freguesia\"]")))
          .map(s => s.trim)
      val districtExtract = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_distrito\"]")))
          .map(s => s.trim)
      val cityExtract = safeGetStringWebElement(() => driver.findElement(By.xpath("//span[@id=\"ctl00_ContentPlaceHolder1_lbl_imovel_show_concelho\"]")))
          .map(s => s.trim)

      val locationDetails = LocationDetails(city = cityExtract, district = districtExtract,
        parish = parish, extraDetails = locationExtraDetails)
      println(s"Location details: ${locationDetails}")



      /*

      val details = safeGetStringWebElement(() => driver.findElement(By.xpath("//div[@class=\"main-details\"]/h4")))
          .map(s => s.trim)
      val splitDetails = details.split(" ")
      val numBedRooms = Some(splitDetails(0).toInt)
      val numBathRooms = Some(splitDetails(2).toInt)
      val overallDetailsExtract = driver
          .findElement(By.xpath("//div[@class=\"property-details-list\"]/ul")).getText

      if (overallDetailsExtract.length == 0) {
        println("${this.getClass.getName} - No property details were found!")
        None
      } else {
        val additionalArr = overallDetailsExtract.split("\n")
        val priceExtracted = l1DetailsExtract(additionalArr(0))

        val state = l1DetailsExtract(additionalArr(1))
        val netArea = l2DetailsExtractToInt(additionalArr(2), l2IndexExtract = 0)
        val rawArea = l2DetailsExtractToInt(additionalArr(3), l2IndexExtract = 0)
        val overallCondition = l1DetailsExtract(additionalArr(4))
        val yearBuilt = l1DetailsExtract(additionalArr(7))
        val energyCertificate = l1DetailsExtract(additionalArr(8))

        val textDetails = Some(
          driver.findElement(By.xpath("//p[@class=\"description read-more-100\"]")).getText.trim)

        val overallDetails = OverallDetails(priceExtracted, year = yearBuilt, netArea, rawArea,
          numBathRooms, numBedRooms, overallCondition, parkingDetails = Some(""), energyCertificate,
          textDescription = textDetails)
        logger.info(
          s" ${this.getClass.getName} -Extracted overallDetails for URL ${url}: ${overallDetails}")
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
            println(s"Failed to get room details for URL ${url} - ${t.getMessage}")
            RoomDetails(Map.empty[String, String])
        }
        val salesRepresentative = Some(
          driver.findElement(By.xpath("//div[@class=\"agent-details\"]")).getText)
        val salesDetails = SalesDetails(id = salesId, representative = salesRepresentative,
          phone = Some(""))
        logger.info(s"${this.getClass.getName} - Final sales details: ${salesDetails}")
        */
        Some(PropertyData(url, title, status = state,
          company = realestateCompany.toString,
          salesDetails = salesDetails,
          overallDetails = overallDetails,
          roomDetails = RoomDetails(Map.empty[String, String]),
          locationDetails = locationDetails,
          additionalDetails = AdditionalDetails(Map.empty[String, String])
        ))

    } catch {
      case e: Exception => {
        println(s"${this.getClass.getName} - Failed to crawl products for URL: ${url} --- ${
          e.getMessage
        }")
        None
      }
    } finally {
      Thread.sleep(3)
      if (driver != null) {
        driver.close()
      }
    }
  }

  def gatherProducts(): Set[String] = {
    var pageCount = 0
    var continueNextPage = true
    var countEmptyResults = 0
    while (continueNextPage) {
      pageCount += 1
      println(s"${this.getClass.getName} - Crawling page num ${pageCount}...")
      try {
        val results = crawlForProducts(url = getPageUrl(pageCount))
        println(s"Got a total of ${results.size} results from URL: ${getPageUrl(pageCount)}")
        if (results.isEmpty) {
          countEmptyResults += 1
        }
        if (countEmptyResults >= 20 || pageCount >= 1) {
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
    driver.manage().timeouts().implicitlyWait(3l, TimeUnit.SECONDS)

    driver.get(url)
    try {
      val allLinks = driver.findElements(By.tagName("a"))
          .asScala
          .toSet
      allLinks
          .map(e => e.getAttribute("href"))
          .filter(l => l != null)
          .filter(url => url.startsWith("https://www.era.pt/imoveis"))
          .filter(url => !url.equals("https://www.era.pt/imoveis/comprar") &&
              !url.equals("https://www.era.pt/imoveis/arrendar") &&
              !url.startsWith("https://www.era.pt/imoveis/default.aspx") &&
              !url.startsWith("https://www.era.pt/imoveis/comprar/apartamentos/lisboa/lisboa?pg=")
          )
    } catch {
      case e: Exception => {
        println(s"${this.getClass.getName} - Failed to crawl products for URL --- ${e.getMessage}")
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

  private def getNextPage(driver: ChromeDriver): WebElement = driver
      .findElement(By.xpath("//li[@class=\"page\"]"))

  private def getPageUrl(pageNum: Int): String = s"https://www.era.pt/imoveis/comprar/apartamentos/lisboa/lisboa?pg=${pageNum}"

}
