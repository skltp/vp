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

class LoadTestVP extends Simulation {

    val httpConf_RC = http
       .baseURL("http://ine-tit-app04.sth.basefarm.net:23000") 
       .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") 

    setUp(
		Scenarios.scn_SendMedicalCertificateAnswerHttps.inject(rampUsers(Conf.noOfUsers.toInt) over (Scenarios.rampUpTimeSecs seconds)).protocols(Conf.httpConf),
		Scenarios.scn_GetSubjectOfCareScheduleHttps_2.inject(rampUsers(Conf.noOfUsers.toInt) over (Scenarios.rampUpTimeSecs seconds)).protocols(Conf.httpConf),
		Scenarios.scn_PingOkSimulationHttps.inject(rampUsers(Conf.noOfUsers.toInt) over (Scenarios.rampUpTimeSecs seconds)).protocols(Conf.httpConf),
		Scenarios.scn_GetSubjectOfCareScheduleHttps.inject(rampUsers(Conf.noOfUsers.toInt) over (Scenarios.rampUpTimeSecs seconds)).protocols(Conf.httpConf),
		Scenarios.scn_ResetCache.inject(nothingFor(7 seconds),atOnceUsers(1)).protocols(httpConf_RC) 
	)
}