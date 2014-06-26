package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class PingVp004SimulationHttps extends Simulation {

  val testTimeSecs   = 60
  val noOfUsers      = 1
  val rampUpTimeSecs = 10
  val minWaitMs      = 3000 milliseconds
  val maxWaitMs      = 5000 milliseconds

  val httpConf = httpConfig
    .baseURL("https://localhost:20000")

  //NOTE!
  //
  //HTTPS towards VP needs a valid ssl certificat configured in <gatling_home>/conf/gatling.conf 
  
  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:test:PingResponder:1:ping",
    "x-vp-sender-id" -> "<CONSUMER-HSAID>",
    "x-vp-instance-id" -> "<VP_INSTANCE_ID>",
    "Keep-Alive" -> "115")

  val scn = scenario("Ping VP004 http scenario")
    .during(testTimeSecs) {     
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
          .headers(skltp_headers)
          .fileBody("Ping_VP004.xml").asXML
          .check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//faultstring", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//faultstring/text()", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).is("VP004 No Logical Adress found for serviceNamespace:urn:riv:itinfra:tp:Ping:1:rivtabp20, receiverId:ping-vp004"))
        )
      .pause(minWaitMs, maxWaitMs)
    }
    setUp(scn.users(noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}