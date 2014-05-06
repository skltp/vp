package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class PingVp004SimulationHttp extends Simulation {

  val testTimeSecs   = 60
  val noOfUsers      = 1
  val rampUpTimeSecs = 10
  val minWaitMs      = 3000 milliseconds
  val maxWaitMs      = 5000 milliseconds

  val httpConf = httpConfig
    .baseURL("http://localhost:8080")

  //NOTE!
  //
  //HTTP needs correct http headers x-vp-sender-id and x-vp-instance-id, and your
  //ip adress must be in VP config (vp-config-override.properties) whitelist.
  
  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:test:PingResponder:1:ping:ping",
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