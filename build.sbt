name := "fuzzy-lumpkins"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.13.1"


libraryDependencies ++= {
    val akkaVersion = "2.6.1"
    Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion
        ,"org.jsoup" % "jsoup" % "1.8+"
        ,"commons-validator" % "commons-validator" % "1.5+"
        ,"net.ruippeixotog" %% "scala-scraper" % "2.2.0"
        ,"org.seleniumhq.selenium" % "selenium-java" % "3.14.0"
        ,"com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"
        ,"com.github.tototoshi" %% "scala-csv" % "1.3.6"
        )
}
