/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skltp.vp 

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import scala.concurrent.duration._

object Scenarios {

    val testTimeSecs   = 60
    val rampUpTimeSecs = 10
    val minWaitMs      = 500 milliseconds
    val maxWaitMs      = 1500 milliseconds

	/*
	 *	HTTP scenarios
     */	
	
	// Ping OK
	val scn_PingOkSimulationHttp = scenario("Ping OK http scenario")
      .during(testTimeSecs) {     
        exec(
          http("Ping")
            .post("/vp/Ping/1/rivtabp20")
            .headers(Headers.pingHttp_header)
            .body(RawFileBody("data/Ping_ok.xml")).asXML
            .check(status.is(200))
            .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
            .check(xpath("//pr:pingResponse", List("pr" -> "urn:riv:itinfra:tp:PingResponder:1")).count.is(1))
          )
        .pause(minWaitMs, maxWaitMs)
    }

	// Resetcache var 10:e sekund
	val scn_ResetCache = scenario("RC_Scenario")
    	.during(testTimeSecs) {     
			forever {
				pace(10 seconds)
				.exec(
		          http("ResetCache")
			      .get("/resetcache")
			      .check(status.is(200))
		        )
			}
		}

	/*
	 *	HTTPS scenarios
     */	
	
	// Ping OK
    val scn_PingOkSimulationHttps = scenario("Ping OK https scenario")
      .during(testTimeSecs) {     
        exec(
          http("Ping")
            .post("/vp/Ping/1/rivtabp20")
            .headers(Headers.pingHttps_header)
            .body(RawFileBody("data/Ping_ok.xml")).asXML
            .check(status.is(200))
            .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
            .check(xpath("//pr:pingResponse", List("pr" -> "urn:riv:itinfra:tp:PingResponder:1")).count.is(1))
          )
        .pause(minWaitMs, maxWaitMs)
    }
	  
	// GetSubjectOfCareSchedule 1
	val scn_GetSubjectOfCareScheduleHttps = scenario("GetSubjectOfCareSchedule OK scenario 12:an, fall 1")
	  .during(testTimeSecs) { 		
	    exec(
	      http("GetAggregatedSubjectOfCareSchedule")
	        .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
			 .headers(Headers.getSubjectOfCareSchedule_header)
		     .body(RawFileBody("data/GetSubjectOfCareSchedule_Mock_121212121212.xml")).asXML
	  		 .check(status.is(200))
	         .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
	         .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(1))
	      )
	    .pause(minWaitMs, maxWaitMs)
	}

	// GetSubjectOfCareSchedule 2
	val scn_GetSubjectOfCareScheduleHttps_2 = scenario("GetSubjectOfCareSchedule OK scenario 12:an, fall 2")
	  .during(testTimeSecs) { 		
	    exec(
	      http("GetAggregatedSubjectOfCareSchedule")
		    .post("/vp/GetSubjectOfCareSchedule/1/rivtabp21")
			 .headers(Headers.getSubjectOfCareSchedule_header)
		     .body(RawFileBody("data/GetSubjectOfCareSchedule_Mock_121212121212.xml")).asXML
	  		 .check(status.is(200))
	         .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
	         .check(xpath("//resp:timeslotDetail", List("resp" -> "urn:riv:crm:scheduling:GetSubjectOfCareScheduleResponder:1")).count.is(1))
	      )
	    .pause(minWaitMs, maxWaitMs)
	}
	  
	// SendMedicalCertificateAnswer	  
	val scn_SendMedicalCertificateAnswerHttps = scenario("SendMedicalCertificateAnswer OK https scenario")
      .during(testTimeSecs) {     
	    exec(
	      http("SendMedicalCertificateAnswer") 
            .post("/vp/SendMedicalCertificateAnswer/1/rivtabp20")
	        .headers(Headers.sendMedicalCertificateAnswer_headers)
	        .body(RawFileBody("data/SendMedicalCertificateAnswer.xml")).asXML
			.check(status.is(200))
	        .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
	        .check(xpath("//resp:resultCode", List("resp" -> "urn:riv:insuranceprocess:healthreporting:2")).count.is(1))
	    )
	  .pause(minWaitMs, maxWaitMs)
    }	  
}