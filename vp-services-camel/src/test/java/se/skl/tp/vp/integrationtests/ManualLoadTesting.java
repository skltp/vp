package se.skl.tp.vp.integrationtests;

import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.RECEIVER_HTTPS;
import static se.skl.tp.vp.util.soaprequests.TestSoapRequests.createGetCertificateRequest;
import static se.skl.tp.vp.utils.MemoryUtil.getNettyMemoryJsonString;

import io.netty.channel.ChannelHandlerContext;
import io.undertow.util.FileUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.camel.Exchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import se.skl.tp.vp.constants.HttpHeaders;
import se.skl.tp.vp.integrationtests.errorhandling.connectionResetByPeer.ResetByPeerServer;
import se.skl.tp.vp.integrationtests.errorhandling.connectionResetByPeer.ServerBehavior;
import se.skl.tp.vp.integrationtests.utils.MockProducer;
import se.skl.tp.vp.integrationtests.utils.RandomCollection;
import se.skl.tp.vp.integrationtests.utils.StartTakService;
import se.skl.tp.vp.integrationtests.utils.TestConsumer;
import se.skl.tp.vp.util.LeakDetectionBaseTest;
import se.skl.tp.vp.util.TestLogAppender;

/**
 * This is a simple integrationtest for loadtesting ment to be run manually
 *
 * The load test uses 4 different calls to three different producers that could be weighted
 * 1. Normal payload
 * 2. Large payload
 * 3. Normal payload to producer that just disconnect to connection ("reset by peer" error)
 * 4. Large payload and gets VP007 error
 *
 * The producers use a random timeout value from a weigthed list of timeouts.
 * VPs internal timeout is default set to 4s
 *
 * You can change three variables to manipulate the test
 * 1. CALLS_PER_SECOND - The number of calls per second
 * 2. DURATION_IN_SECONDS - For how long should the test last
 * 3. PRODUCER_WEIGHTED_TIMEOUTS - A weighted list of timeouts for the producers
 * 4. WEIGHTED_CALLS - The weight of call types from enum CallTypes
 */
@CamelSpringBootTest
@SpringBootTest
@StartTakService
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled // Disabled to avoid running in CI/CD pipelines by mistake
@SuppressWarnings({"java:S3577", "java:S2925", "NewClassNamingConvention"}) // Thread.sleep is ok in load test, class name is descriptive
public class ManualLoadTesting extends LeakDetectionBaseTest {

  private enum CallTypes {
    LARGE_PAYLOAD,
    NORMAL_PAYLOAD,
    RESET_BY_PEER,
    VP007
  }

  // CHANGE THIS TO CHANGE BEHAVIOUR OF THE TEST
  public static final int CALLS_PER_SECOND = 25;
  public static final int DURATION_IN_SECONDS = 60 * 60 * 15;
  public static final RandomCollection<Integer> PRODUCER_WEIGHTED_TIMEOUTS = (new RandomCollection())
      .add(10, 50)
      .add(10, 100)
      .add(5, 1000)
      .add(1, 3000)
      .add(1, 5000);// VP default timeout is 4sec so this cause a VP009 timeout error
  public static final RandomCollection<CallTypes> WEIGHTED_CALLS = (new RandomCollection())
      .add(10, CallTypes.LARGE_PAYLOAD)
      .add(5, CallTypes.NORMAL_PAYLOAD)
      .add(2, CallTypes.RESET_BY_PEER)
      .add(2, CallTypes.VP007);


  public static final String HTTPS_PRODUCER_URL = "https://localhost:19001/vardgivare-b/tjanst2";
  public static final String HTTP_PRODUCER_URL = "http://localhost:19000/vardgivare-b/tjanst2";

  @Value("${vp.instance.id}")
  String vpInstanceId;

  @Autowired
  TestConsumer testConsumer;

  @Autowired
  MockProducer mockHttpsProducer;

  @Autowired
  MockProducer mockProducer;

  Map<Integer, Long> responseCodes = new ConcurrentHashMap<>();
  Map<CallTypes, Long> randomCalls = new ConcurrentHashMap<>();

  @BeforeAll
  @SuppressWarnings("java:S5786") // Used both in @BeforeAll and direct calls, so must be public
  public static void startLeakDetection() {
    System.setProperty("spring.profiles.active", "leak");
    LeakDetectionBaseTest.startLeakDetection();
  }

  @BeforeEach
  void before() throws Exception {
    mockProducer.start(HTTP_PRODUCER_URL);
    mockProducer.setResponseResourceXml("testfiles/GetSubjectOfCareLargeResponse.xml");
    mockHttpsProducer.start(HTTPS_PRODUCER_URL);
    mockHttpsProducer.setResponseResourceXml("testfiles/GetSubjectOfCareLargeResponse.xml");
    TestLogAppender.clearEvents();
  }


  @Test
  @SuppressWarnings("java:S2699") // No assertions as this is a manual load test
  void justLoad() throws InterruptedException {

    startResetByPeerServer();

    responseCodes.clear();
    randomCalls.clear();
    mockProducer.setWeightedTimeouts(PRODUCER_WEIGHTED_TIMEOUTS);
    mockHttpsProducer.setWeightedTimeouts(PRODUCER_WEIGHTED_TIMEOUTS);

    load(this::callWithRandomContract, CALLS_PER_SECOND, DURATION_IN_SECONDS);
    System.out.println(getNettyMemoryJsonString());

    TimeUnit.SECONDS.sleep(10);
    System.out.println("ResponseCodes:");
    responseCodes.forEach((key, value) -> System.out.println(key + ":" + value));
    System.out.println("RandomNumbers:");
    randomCalls.forEach((key, value) -> System.out.println(key + ":" + value));
    System.gc();
    TimeUnit.SECONDS.sleep(60);

    System.out.println(getNettyMemoryJsonString());
    stopResetByPeerServer();
  }

  private void startResetByPeerServer() throws InterruptedException {
    ServerBehavior b = (ChannelHandlerContext ctx) -> {
      ctx.channel().close();
    };
    ResetByPeerServer.startServer(19007, b);
  }

  private void stopResetByPeerServer() {
    ResetByPeerServer.stopServer();
  }


  private void load(Runnable myTask, int ratePerSecond, int durationInSeconds) {

    final ExecutorService es = Executors.newCachedThreadPool();
    ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    ses.scheduleAtFixedRate(() -> es.submit(myTask)
        , 0, 1000 / ratePerSecond, TimeUnit.MILLISECONDS);

    try {
      TimeUnit.SECONDS.sleep(durationInSeconds);
      ses.shutdown();
      es.shutdown();
      ses.awaitTermination(2, TimeUnit.SECONDS);
      es.awaitTermination(2, TimeUnit.SECONDS);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  private void callWithRandomContract() {
    final CallTypes next = WEIGHTED_CALLS.next();
    switch (next) {

      case LARGE_PAYLOAD:
        callVpLargePayloads("tp");
        break;

      case NORMAL_PAYLOAD:
        callVpSmallPayloads();
        break;

      case RESET_BY_PEER:
        callVPResetByPeer();
        break;

      case VP007:
        callVpLargePayloads("notKnownSenderId");
        break;

      default:
        callVpLargePayloads("tp");
        break;

    }

    final Integer responseCode = testConsumer.getResultEndpoint().getExchanges()
        .get(0).getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
    responseCodes.merge(responseCode, 1L, Long::sum);
    randomCalls.merge(next, 1L, Long::sum);
  }


  private void callVpLargePayloads(String senderId ) {
    String largeRequest = FileUtils
        .readFile(getClass().getClassLoader().getResource("testfiles/ProcessNotificationLargePayload.xml"));
    testConsumer.sendHttpRequestToVP(largeRequest, createHeaders(senderId), true);
  }

  private void callVpSmallPayloads() {
    testConsumer.sendHttpRequestToVP(createGetCertificateRequest(RECEIVER_HTTPS), createHeaders("tp"));
  }

  private void callVPResetByPeer() {
    String request = createGetCertificateRequest("HttpProducerResetByPeer");
    testConsumer.sendHttpRequestToVP(request, createHeaders("tp"));
  }

  private Map<String, Object> createHeaders(String senderId) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HttpHeaders.X_VP_INSTANCE_ID, vpInstanceId);
    headers.put(HttpHeaders.X_VP_SENDER_ID, senderId);
    return headers;
  }

}
