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
package se.skl.tp.vp.vagvalrouter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;


public class VagvalRouterUnitTest extends AbstractMuleContextTestCase{

	private static final int DEFAULT_RESPONSETIMEOUT = 30000;

	@Test
	public void testHttpsPropertyIsSet() throws Exception {
		final String url = "https://localhost:20000/vp/PingForConfiguration/1/rivtabp21";
		this.verifyProperty(url, true);
	}
	
	@Test
	public void testHttpsPropertyIsSetToFalse() throws Exception {
		final String url = "http://localhost:20000/vp/PingForConfiguration/1/rivtabp21";
		this.verifyProperty(url, false);
	}
	
	@Test
	public void defaultResponseTimeoutSelectedWhenNotProvided() throws Exception{
		final VagvalRouter router = new VagvalRouter();
		router.setResponseTimeout(DEFAULT_RESPONSETIMEOUT);
		Integer providedResponsetime = null;
		
		assertEquals(DEFAULT_RESPONSETIMEOUT, router.selectResponseTimeout(createMessageWithResponseTimeout(providedResponsetime)));
	}

	@Test
	public void responseTimeoutProvidedIsUsed() {
		final VagvalRouter router = new VagvalRouter();
		router.setResponseTimeout(DEFAULT_RESPONSETIMEOUT);
		int providedResponsetime = 5000;

		assertEquals(providedResponsetime, router.selectResponseTimeout(createMessageWithResponseTimeout(providedResponsetime)));
	}

	private MuleMessage createMessageWithResponseTimeout(Integer providedResponseTimeout) {
		MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
		if (providedResponseTimeout != null) {
			message.setProperty(VPUtil.FEATURE_RESPONSE_TIMOEUT,
					providedResponseTimeout, PropertyScope.INVOCATION);
		}
		return message;
	}
	
	private void verifyProperty(final String url, final boolean expectedResult) throws Exception {

		final DefaultMuleEvent event = Mockito.mock(DefaultMuleEvent.class);	
		final DefaultMuleMessage msg = Mockito.mock(DefaultMuleMessage.class);
		
		final VagvalRouter router = new VagvalRouter();
		
		final AddressingHelper helper = Mockito.mock(AddressingHelper.class);
		Mockito.when(helper.getAddress(msg)).thenReturn(url);
		
		router.setAddressingHelper(helper);
		
		
		Mockito.when(event.getMessage()).thenReturn(msg);
				
		final List<?> receipients = router.getRecipients(event);
		
		assertNotNull(receipients);
		assertEquals(1, receipients.size());
		assertEquals(url, receipients.get(0));
		
		Mockito.verify(helper, Mockito.times(1)).getAddress(msg);
		Mockito.verifyNoMoreInteractions(helper);
		
		Mockito.verifyNoMoreInteractions(msg);
	}
}
