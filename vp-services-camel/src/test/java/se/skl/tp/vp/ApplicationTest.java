package se.skl.tp.vp;

import org.apache.camel.CamelContext;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.EnableRouteCoverage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = VpServicesApplication.class)
@EnableRouteCoverage
@DirtiesContext
public class ApplicationTest {

    @Autowired
    private CamelContext camelContext;

    @Test
    public void contextLoads(){

    }

}