package se.skl.tp.vp;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@CamelSpringBootTest
@SpringBootTest(classes = VpServicesApplication.class)
@org.apache.camel.test.spring.junit5.EnableRouteCoverage
@DirtiesContext
public class ApplicationTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void contextLoads(){

    }

}