package org.fuzzylumpkins.actors

import java.io.BufferedWriter
import java.io.FileWriter
import java.sql.Date
import java.text.SimpleDateFormat

import akka.actor.Actor
import com.github.tototoshi.csv.CSVWriter
import com.typesafe.scalalogging.LazyLogging
import org.fuzzylumpkins.actors.ExporterActor.CsvExport
import org.fuzzylumpkins.actors.ExporterActor. ExportResult

class ExporterActor extends Actor with LazyLogging {

  override def receive: Receive = {
    case CsvExport(name, headers, data) =>
      logger.info(s"Exporting ${name} data")
      val fileName = exportCsv(name, headers, data)
      sender() ! ExportResult(fileName)
  }

  private def exportCsv(name: String, headers: List[String], data: List[List[String]]): Option[String] = {

    val date = new Date(System.currentTimeMillis)
    val formater = new SimpleDateFormat("yyyy_MM_dd")
    val fileName = s"${name}_crawl_${formater.format(date)}.csv"
    val outputFile = new BufferedWriter(new FileWriter(fileName))
    val csvWriter = new CSVWriter(outputFile)
    //val csvSchema = DmpPredictionsMap.getParams
    println(s"CSV schema is: [${headers}]")

    try {
      csvWriter.writeAll(headers :: data)
      Some(fileName)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to export ${fileName}: ${e.getMessage}")
        None
    } finally {
      outputFile.close()
    }
  }
}

object ExporterActor {
  case class CsvExport(name: String, headers: List[String], data: List[List[String]])
  case class ExportResult(fileName: Option[String])
}
