package se.skl.tp.vp.vagval;

import static org.apache.camel.test.junit4.TestSupport.assertStringContains;
import static org.mockito.ArgumentMatchers.any;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheInitializationException;


@RunWith( CamelSpringBootRunner.class )
@SpringBootTest(classes = VagvalTestConfiguration.class)
public class ResetHsaCacheProcessorTest {
    @Autowired
    ResetHsaCacheProcessor processor;

    @MockBean(name="hsaCache")
    HsaCache hsaCacheMock;

    @Before
    public void beforeTest() {
        Mockito.when(hsaCacheMock.init(any())).thenReturn(hsaCacheMock);
    }

    @Test
    public void testResetIsOK() throws Exception {
        Exchange ex = createExchange();
        Mockito.when(hsaCacheMock.getHSACacheSize()).thenReturn(5).thenReturn(10);
        processor.process(ex);
        assertStringContains(ex.getOut().getBody(String.class), "Successfully reset HSA cache");
    }

    @Test
    public void testResetWarning() throws Exception {
        Exchange ex = createExchange();
        Mockito.when(hsaCacheMock.getHSACacheSize()).thenReturn(10).thenReturn(1);
        processor.process(ex);
        assertStringContains(ex.getOut().getBody(String.class), "Warning: HSA cache reset to");
    }

    @Test
    public void testResetException() throws Exception {
        Exchange ex = createExchange();
        Mockito.when(hsaCacheMock.init(any())).thenThrow(HsaCacheInitializationException.class);
        processor.process(ex);
        assertStringContains(ex.getOut().getBody(String.class), "Reset HSA cache failed.");
    }


    private Exchange createExchange() {
        CamelContext ctx = new DefaultCamelContext();
        return new DefaultExchange(ctx);
    }
}
