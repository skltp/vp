package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class PingVp009Simulation extends Simulation {

  val testTimeSecs   = 60
  val noOfUsers      = 1
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
    //"x-vp-sender-id" -> "HSASERVICES-100M",
    //"x-vp-instance-id" -> "NTjP_HSASERVICES-100S",
		"Keep-Alive" -> "115")

	val scn = scenario("Ping VP009 scenario")
    .during(testTimeSecs) { 		
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
  				.headers(skltp_headers)
          .fileBody("Ping_VP009.xml").asXML
  				.check(status.is(500))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//faultstring", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//faultstring/text()", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).is("VP009 Error connecting to service producer at adress http://localhost:9090/non-existing-service"))
        )
      .pause(minWaitMs, maxWaitMs)
    }
  	setUp(scn.users(noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}