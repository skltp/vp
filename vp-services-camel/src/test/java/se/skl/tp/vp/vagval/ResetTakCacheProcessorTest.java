package se.skl.tp.vp.vagval;

import static org.mockito.Mockito.mock;
import static se.skl.tp.vp.util.JunitUtil.assertStringContains;

import java.util.ArrayList;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.TakCacheLog;

@CamelSpringBootTest
@SpringBootTest(classes = VagvalTestConfiguration.class)
public class ResetTakCacheProcessorTest {
    @Autowired
    private ResetTakCacheProcessor processor;

    @MockBean(name = "takCacheImpl")
    private TakCache takCacheMock;

    private List<String> testLog = new ArrayList<>();
    private String log1 =  "Test log1";
    private String log2 =  "Test log2";

    @BeforeEach
    public void beforeTest() {
        testLog.add(log1);
        testLog.add(log2);

        TakCacheLog takCacheLog = mock(TakCacheLog.class);
        Mockito.when(takCacheLog.getLog()).thenReturn(testLog);
        Mockito.when(takCacheMock.refresh()).thenReturn(takCacheLog);
    }

    @Test
    public void testResetIsOK() throws Exception {
        Exchange ex = createExchange();
        processor.process(ex);
        assertStringContains(ex.getMessage().getBody(String.class), log1);
        assertStringContains(ex.getMessage().getBody(String.class), log2);
    }

    private Exchange createExchange() {
        CamelContext ctx = new DefaultCamelContext();
        return new DefaultExchange(ctx);
    }

}
