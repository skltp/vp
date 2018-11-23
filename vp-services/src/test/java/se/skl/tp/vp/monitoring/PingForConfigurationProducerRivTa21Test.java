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
package se.skl.tp.vp.monitoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import se.skl.tp.vp.vagvalagent.VagvalAgent;

public class PingForConfigurationProducerRivTa21Test {
	private static final Integer ONE = new Integer(1);
	private static final Integer ZERO = new Integer(0);

	@Test
	public void resourcesNeededForVpAvailable() {

		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getNumberOfBehorigheter()).thenReturn(ONE);
		when(vagvalAgent.getNumberOfVagval()).thenReturn(ONE);

		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);

		assertTrue(pingRivTa21.resourcesNeededForVpAvailable());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void virtualiseringsInfoIsMissing() {

		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getNumberOfBehorigheter()).thenReturn(ONE);
		when(vagvalAgent.getNumberOfVagval()).thenReturn(ZERO);

		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);

		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void behorighetsInfoIsMissing() {

		VagvalAgent vagvalAgent = mock(VagvalAgent.class);
		when(vagvalAgent.getNumberOfBehorigheter()).thenReturn(ZERO);
		when(vagvalAgent.getNumberOfBehorigheter()).thenReturn(ONE);

		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(vagvalAgent);

		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}

	@Test
	public void agentIsMissing() {

		PingForConfigurationProducerRivTa21 pingRivTa21 = new PingForConfigurationProducerRivTa21();
		pingRivTa21.setVagvalAgent(null);

		assertFalse(pingRivTa21.resourcesNeededForVpAvailable());
	}

}
