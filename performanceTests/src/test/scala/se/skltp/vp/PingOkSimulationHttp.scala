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

class PingOkSimulationHttp extends Simulation {

  val testTimeSecs   = 60
  val rampUpTimeSecs = 10
  val minWaitMs      = 500 milliseconds
  val maxWaitMs      = 1500 milliseconds

  val httpConf = http
    .baseURL("http://localhost:8080")

  //NOTE!
  //
  //HTTP needs correct http headers x-vp-sender-id and x-vp-instance-id, and your
  //ip adress must be in VP config (vp-config-override.properties) whitelist.
  
  val skltp_headers = Map(
    "Accept-Encoding" -> "gzip,deflate",
    "Content-Type" -> "text/xml;charset=UTF-8",
    "SOAPAction" -> "urn:riv:test:PingResponder:1:ping",
    "x-vp-sender-id" -> "tp",
    "x-vp-instance-id" -> "THIS_VP_INSTANCE_ID",
    "Keep-Alive" -> "115")

  val scn = scenario("Ping OK http scenario")
    .during(testTimeSecs) {     
      exec(
        http("Ping")
          .post("/vp/Ping/1/rivtabp20")
          .headers(skltp_headers)
          .body(RawFileBody("data/Ping_ok.xml")).asXML
          .check(status.is(200))
          .check(xpath("soap:Envelope", List("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")).exists)
          .check(xpath("//pr:pingResponse", List("pr" -> "urn:riv:itinfra:tp:PingResponder:1")).count.is(1))
        )
      .pause(minWaitMs, maxWaitMs)
    }
    setUp(scn.inject(rampUsers(Conf.noOfUsers.toInt) over (rampUpTimeSecs seconds)).protocols(httpConf))
}