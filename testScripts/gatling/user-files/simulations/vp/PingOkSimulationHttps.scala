package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class PingOkSimulationHttps extends Simulation {

  val testTimeSecs   = 60
  val noOfUsers      = 30
  val rampUpTimeSecs = 10
  val minWaitMs      = 500 milliseconds
  val maxWaitMs      = 1500 milliseconds

  val httpConf = httpConfig
    .baseURL("https://localhost:20000")

  //NOTE!
  //
  //HTTPS towards VP needs a valid ssl certificat configured in <gatling_home>/conf/gatling.conf 
  
  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:test:PingResponder:1:ping:ping",
    "Keep-Alive" -> "115")

  val scn = scenario("Ping OK https scenario")
    .during(testTimeSecs) {     
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
          .headers(skltp_headers)
          .fileBody("Ping_ok.xml").asXML
          .check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//pr:pingResponse", List("pr" -> "urn:riv:itinfra:tp:PingResponder:1")).count.is(1))
        )
      .pause(minWaitMs, maxWaitMs)
    }
    setUp(scn.users(noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}