/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
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

import java.util.List;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.transport.PropertyScope;

import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;


public class VagvalRouterUnitTest extends TestCase {

	public void testHttpsPropertyIsSet() throws Exception {
		final String url = "https://localhost:20000/vp/PingForConfiguration/1/rivtabp21";
		this.verifyProperty(url, true);
	}
	
	public void testHttpsPropertyIsSetToFalse() throws Exception {
		final String url = "http://localhost:20000/vp/PingForConfiguration/1/rivtabp21";
		this.verifyProperty(url, false);
	}
	
	private void verifyProperty(final String url, final boolean expectedResult) throws Exception {
		
		final VagvalRouter router = new VagvalRouter();
		
		final AddressingHelper helper = Mockito.mock(AddressingHelper.class);
		Mockito.when(helper.getAddress()).thenReturn(url);
		
		router.setAddressingHelper(helper);
		
		final DefaultMuleEvent event = Mockito.mock(DefaultMuleEvent.class);	
		final DefaultMuleMessage msg = Mockito.mock(DefaultMuleMessage.class);
		
		Mockito.when(event.getMessage()).thenReturn(msg);
		Mockito.when(helper.getMuleMessage()).thenReturn(msg);
				
		final List<?> receipients = router.getRecipients(event);
		
		assertNotNull(receipients);
		assertEquals(1, receipients.size());
		assertEquals(url, receipients.get(0));
		
		Mockito.verify(helper, Mockito.times(1)).getMuleMessage();
		Mockito.verify(helper, Mockito.times(1)).getAddress();
		Mockito.verifyNoMoreInteractions(helper);
		
		Mockito.verifyNoMoreInteractions(msg);
	}
}
