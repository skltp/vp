package se.skl.tp.vp.httpheader;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import se.skl.tp.vp.TestBeanConfiguration;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.service.TakCacheService;
import se.skltp.takcache.BehorigheterCache;
import se.skltp.takcache.RoutingInfo;
import se.skltp.takcache.TakCache;
import se.skltp.takcache.VagvalCache;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.skl.tp.vp.util.soaprequests.RoutingInfoUtil.createRoutingInfo;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_UNIT_TEST;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.util.takcache.TakCacheMockUtil.createTakCacheLogOk;
import static se.skl.tp.vp.util.takcache.TestTakDataDefines.RIV20;

@CamelSpringBootTest
@SpringBootTest(classes = TestBeanConfiguration.class, properties = {
        "tp.tls.store.consumer.bundle=cons",
        "tp.tls.store.producer.bundle=prod",
        "tp.tls.allowedOutgoingCipherSuites=*"})
@TestPropertySource("classpath:application.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class XSkltpPrtHeaderTestSslBundles {

    @MockitoBean
    TakCache takCache;

    @MockitoBean
    VagvalCache vagvalCache;

    @MockitoBean
    BehorigheterCache behorigheterCache;

    @Produce("direct:start")
    protected ProducerTemplate template;

    @EndpointInject("mock:result")
    protected MockEndpoint resultEndpoint;

    @Autowired
    CamelContext camelContext;

    @Autowired
    TakCacheService takCacheService;

    @BeforeEach
    public void setUp() throws Exception {
        createRoute(camelContext);
        camelContext.start();
        resultEndpoint.reset();
        Mockito.when(takCache.refresh()).thenReturn(createTakCacheLogOk());
        takCacheService.refresh();
    }

    @Test
    void headerTest() {
        List<RoutingInfo> list = new ArrayList<>();
        list.add(createRoutingInfo("http://localhost:11111/vp",RIV20));
        Mockito.when(vagvalCache.getRoutingInfo("urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", "UnitTest")).thenReturn(list);
        Mockito.when(behorigheterCache.isAuthorized("UnitTest", "urn:riv:insuranceprocess:healthreporting:GetCertificateResponder:1", "UnitTest")).thenReturn(true);

        template.sendBody(createGetCertificateRequest(RECEIVER_UNIT_TEST));

        String header = (String) resultEndpoint.getExchanges().get(0).getIn().getHeader(HttpHeaders.X_SKLTP_PRODUCER_RESPONSETIME);
        assertNotNull(header);
    }


    private void createRoute(CamelContext camelContext) throws Exception {
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").routeId("start")
                        .setHeader(HttpHeaders.X_VP_SENDER_ID, constant("UnitTest"))
                        .setHeader(HttpHeaders.X_VP_INSTANCE_ID, constant("dev_env"))
                        .setHeader("X-Forwarded-For", constant("1.2.3.4"))
                        .to("netty-http:http://localhost:12312/vp")
                        .to("mock:result");

                from("netty-http:http://localhost:11111/vp").routeId("producent")
                        .process((Exchange exchange)-> {});
            }
        });
    }
}
