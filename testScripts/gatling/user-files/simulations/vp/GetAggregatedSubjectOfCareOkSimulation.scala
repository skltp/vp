package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class GetAggregatedSubjectOfCareOkSimulation extends Simulation {

  val testTimeSecs   = 30
  val noOfUsers      = 1
  val rampUpTimeSecs = 10
	val minWaitMs      = 500 milliseconds
  val maxWaitMs      = 1500 milliseconds

	val httpConf = httpConfig
	  .baseURL("https://localhost:20000")

  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1:GetSubjectOfCareSchedule",
		"Keep-Alive" -> "115")

	val scn = scenario("GetSubjectOfCareSchedule OK scneario")
    .during(testTimeSecs) { 		
      exec(
        http("GetAggregatedSubjectOfCareSchedule")
          .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
  				.headers(skltp_headers)
          .fileBody("GetSubjectOfCareSchedule_121212121212_ok.xml").asXML
  				.check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(2)
        )
      )
      .pause(minWaitMs, maxWaitMs)
    }
  	setUp(scn.users(noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}