package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class PingVp004Simulation extends Simulation {

  val testTimeSecs   = 60
  val noOfUsers      = 10
  val rampUpTimeSecs = 10
	val minWaitMs      = 1000 milliseconds
  val maxWaitMs      = 2000 milliseconds

  val httpConf = httpConfig
    .baseURL("http://localhost:8080")

  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:test:PingResponder:1:ping:ping",
    "x-vp-sender-id" -> "tp",
    "x-vp-instance-id" -> "THIS_VP_INSTANCE_ID",
		"Keep-Alive" -> "115")

	val scn = scenario("Ping VP004 scenario")
    .during(testTimeSecs) { 		
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
  				.headers(skltp_headers)
          .fileBody("Ping_VP004.xml").asXML
  				.check(status.is(500))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
        )
      .pause(minWaitMs, maxWaitMs)
    }
  	setUp(scn.users(noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}