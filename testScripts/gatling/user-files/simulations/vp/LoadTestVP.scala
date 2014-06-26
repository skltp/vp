package vp

import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.excilys.ebi.gatling.jdbc.Predef._
import com.excilys.ebi.gatling.http.Headers.Names._
import akka.util.duration._
import bootstrap._

class LoadTestVP extends Simulation {

  //Global settings for time of seconds to run test and ramp up time
  val testTimeSecs   = 30
  val rampUpTimeSecs = 10

  val httpConf = httpConfig.baseURL("https://localhost:20000")

  val skltp_getsubjectofcareschedule_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1:GetSubjectOfCareSchedule",
		"Keep-Alive" -> "115")

  	//
  	//Lokal teststubbe GetAggregatedSubjectOfCareSchedule som svarar på ADAM med en post
  	//
  	val adam_noOfUsers      = 10
  	val adam_minWaitMs      = 500 milliseconds
  	val adam_maxWaitMs      = 1500 milliseconds

	val scnAdam = scenario("GetSubjectOfCareSchedule OK scneario ADAM")
    .during(testTimeSecs) { 		
      exec(
        http("GetAggregatedSubjectOfCareSchedule")
          .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
  				.headers(skltp_getsubjectofcareschedule_headers)
          .fileBody("GetSubjectOfCareSchedule_ADAM.xml").asXML
  				.check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(1)
        )
      )
      .pause(adam_minWaitMs, adam_maxWaitMs)
    }

  	//
  	//Lokal teststubbe GetAggregatedSubjectOfCareSchedule som svarar på ERIK med en post
  	//
  	val erik_noOfUsers      = 10
  	val erik_minWaitMs      = 500 milliseconds
  	val erik_maxWaitMs      = 1500 milliseconds

	val scnErik = scenario("GetSubjectOfCareSchedule OK scneario ERIK")
    .during(testTimeSecs) { 		
      exec(
        http("GetAggregatedSubjectOfCareSchedule")
          .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
  				.headers(skltp_getsubjectofcareschedule_headers)
          .fileBody("GetSubjectOfCareSchedule_ERIK.xml").asXML
  				.check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(1)
        )
      )
      .pause(erik_minWaitMs, erik_maxWaitMs)
    }

    //
  	//Kalendercentralen GetAggregatedSubjectOfCareSchedule
  	//
  	val kc_noOfUsers      = 10
  	val kc_minWaitMs      = 500 milliseconds
  	val kc_maxWaitMs      = 1500 milliseconds

	val scnKc = scenario("GetSubjectOfCareSchedule OK scneario Kalendercentralen")
    .during(testTimeSecs) { 		
      exec(
        http("GetAggregatedSubjectOfCareSchedule")
          .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
  				.headers(skltp_getsubjectofcareschedule_headers)
          .fileBody("GetSubjectOfCareSchedule_KC.xml").asXML
  				.check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(1)
        )
      )
      .pause(kc_minWaitMs, kc_maxWaitMs)
    }

  	//
  	//Lokal Ping tjänst VP
  	//

  	val skltp_ping_headers = Map(
      "Accept-Encoding" -> "gzip,deflate",
      "Content-Type" -> "text/xml;charset=UTF-8",
      "SOAPAction" -> "urn:riv:itinfra:tp:PingResponder:1:Ping",
		  "Keep-Alive" -> "115")

  	val ping_noOfUsers      = 10
  	val ping_minWaitMs      = 500 milliseconds
  	val ping_maxWaitMs      = 1500 milliseconds

  	val scnPing = scenario("Ping OK http scenario")
    .during(testTimeSecs) {     
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
          .headers(skltp_ping_headers)
          .fileBody("Ping_ok.xml").asXML
          .check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//pr:pingResponse", List("pr" -> "urn:riv:itinfra:tp:PingResponder:1")).count.is(1))
        )
      .pause(ping_minWaitMs, ping_maxWaitMs)
    }

    //
    //SendMedicalCertificateAnswer testtubbe i FkAdapter
    //

    val skltp_SendMedicalCertificateAnswer_headers = Map(
      "Accept-Encoding" -> "gzip,deflate",
      "Content-Type" -> "text/xml;charset=UTF-8",
      "SOAPAction" -> "urn:riv:insuranceprocess:healthreporting:SendMedicalCertificateAnswerResponder:1:SendMedicalCertificateAnswer",
      "Keep-Alive" -> "115")

    val sendMedicalCertificateAnswer_noOfUsers      = 10
    val sendMedicalCertificateAnswer_minWaitMs      = 500 milliseconds
    val sendMedicalCertificateAnswer_maxWaitMs      = 1500 milliseconds

    val scnSendMedicalCertificateAnswer = scenario("SendMedicalCertificateAnswer OK http scenario")
    .during(testTimeSecs) {     
      exec(
        http("SendMedicalCertificateAnswer")
          .post("/vp/SendMedicalCertificateAnswer/1/rivtabp20")
          .headers(skltp_SendMedicalCertificateAnswer_headers)
          .fileBody("SendMedicalCertificateAnswer.xml").asXML
          .check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//resp:resultCode", List("pr" -> "urn:riv:insuranceprocess:healthreporting:SendMedicalCertificateAnswerResponder:1")).count.is(1))
        )
      .pause(sendMedicalCertificateAnswer_minWaitMs, sendMedicalCertificateAnswer_maxWaitMs)
    }

    setUp(scnAdam.users(adam_noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
    setUp(scnErik.users(erik_noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
    setUp(scnKc.users(erik_noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
    setUp(scnPing.users(ping_noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
    setUp(scnSendMedicalCertificateAnswer.users(sendMedicalCertificateAnswer_noOfUsers).ramp(rampUpTimeSecs).protocolConfig(httpConf))
}