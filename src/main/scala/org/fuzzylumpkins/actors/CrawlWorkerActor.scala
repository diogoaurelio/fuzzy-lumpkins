package org.fuzzylumpkins.actors

import java.sql.Date
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.routing.RoundRobinPool
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.validator.routines.UrlValidator
import org.fuzzylumpkins.actors.CrawlWorkerActor.Product
import org.fuzzylumpkins.crawl.CrawlServiceFactory
import org.fuzzylumpkins.crawl.CrawlServiceFactory.PropertyData
import org.fuzzylumpkins.targets.RealEstate


class CrawlWorkerActor(parallelism: Int = 5) extends Actor with LazyLogging {

  val urlValidator = new UrlValidator()
  var router: ActorRef = _
  val finalResult = mutable.Set.empty[PropertyData]
  val resultsCollected: mutable.Map[String, Int] = mutable.Map.empty[String, Int]
  var loopCount: AtomicInteger = new AtomicInteger(0)

  def receive = {
    case CrawlWorkerActor.Crawl(realestate) =>
      println(s"${this.getClass.getName} - Starting with product gathering activity (to collect all ${realestate.toString} product URLs)")
      collectProducts(realestate, sender())

    case CrawlWorkerActor.Product(realestate, url, count, totalCount, originalSender) =>
      println(s"${this.getClass.getName} - Crawling real estate ${realestate.toString} URL ${url} [count: ${count}, totalCount: ${totalCount}]")
      val propertyData = crawlData(realestate, url)
      sender() ! CrawlWorkerActor
          .PreliminaryResult(realestate, url, count, propertyData, totalCount, originalSender)

    case CrawlWorkerActor.PreliminaryResult(realEstate, url, count, propertyData, totalCount, originalSender) =>
      println(s"${this.getClass.getName} - Received preliminaryResult for real estate ${realEstate}; Current results collected ${resultsCollected.size}; CollectedInfo was: ${propertyData}")
      resultsCollected += (url -> count)
      if (propertyData.isDefined) {
        finalResult += propertyData.get
      }
      if (resultsCollected.size == totalCount) {
        try {
          context.stop(router)
        } catch {
          case e: Exception => println(s"${this.getClass.getName} - Failed to stop router: ${e.getLocalizedMessage}")
        } finally {
          println(s"${this.getClass.getName} - Finished crawling real estate ${realEstate.toString}")
          originalSender ! CrawlWorkerActor.FinalResult(realEstate, finalResult.toSet)
        }
      }
  }

  private def crawlData(realEstate: RealEstate.Company, url: String): Option[PropertyData] = {
    val service = new CrawlServiceFactory
    val date = new Date(System.currentTimeMillis)
    val formater = new SimpleDateFormat("yyyy_MM_dd")
    val result: Set[PropertyData] = service.collectInfo(realEstate, products = Set(url))
    result.headOption
  }

  private def collectProducts(realestate: RealEstate.Company, sender: ActorRef): Unit = {
    val service = new CrawlServiceFactory
    val date = new Date(System.currentTimeMillis)
    val formater = new SimpleDateFormat("yyyy_MM_dd")
    println(s"${this.getClass.getName} - Starting product gathering for ${formater.format(date)}")
    val products: Set[String] = service.gatherProducts(realestate)
    if (products.nonEmpty) {
      router = context.actorOf(Props(new CrawlWorkerActor).withRouter(new RoundRobinPool(parallelism)))
      var productCount = 0
      products.foreach { product =>
        productCount += 1
        router ! Product(realestate, product, productCount, products.size, sender)
      }
    } else {
      println("Received a total of zero products!, nothing to do here!")
      sender ! CrawlWorkerActor.FinalResult(realestate, Set.empty[PropertyData])
    }
  }

}

object CrawlWorkerActor {

  case class PreliminaryResult(realEstate: RealEstate.Company, url: String, count: Int,
                               propertyData: Option[PropertyData], totalCount: Int, sender: ActorRef)

  case class CheckFinalResult(realEstate: RealEstate.Company, totalCount: Int, sender: ActorRef)

  case class FinalResult(realestate: RealEstate.Company, propertyData: Set[PropertyData])

  case class Product(realestate: RealEstate.Company, url: String, count: Int, totalCount: Int,
                     sender: ActorRef)

  case class Crawl(realestate: RealEstate.Company)

}
