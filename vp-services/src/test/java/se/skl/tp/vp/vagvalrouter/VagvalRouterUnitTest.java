package se.skl.tp.vp.vagvalrouter;

import java.util.List;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.mule.api.MuleMessage;

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
		
		final MuleMessage msg = Mockito.mock(MuleMessage.class);
		
		final List receipients = router.getRecipients(msg);
		assertNotNull(receipients);
		assertEquals(1, receipients.size());
		assertEquals(url, receipients.get(0));
		
		Mockito.verify(helper, Mockito.only()).getAddress();
		Mockito.verify(helper, Mockito.times(1)).getAddress();
		
		Mockito.verify(msg, Mockito.only()).setBooleanProperty(VPUtil.IS_HTTPS, expectedResult);
		Mockito.verify(msg, Mockito.times(1)).setBooleanProperty(VPUtil.IS_HTTPS, expectedResult);
	}
}
