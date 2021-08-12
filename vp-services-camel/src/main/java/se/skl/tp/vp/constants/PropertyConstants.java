package se.skl.tp.vp.constants;

public class PropertyConstants {

    private PropertyConstants() {
        //To hide implicit public constructor. Sonar suggestion.
    }

    public static final String CERTIFICATE_SENDERID_SUBJECT_PATTERN = "certificate.senderid.subject.pattern";
    public static final String IP_WHITELIST = "ip.whitelist";
    public static final String SENDER_ID_ALLOWED_LIST = "sender.id.allowed.list";
    public static final String VP_INSTANCE_ID = "vp.instance.id";

    public static final String VAGVALROUTER_SENDER_IP_ADRESS_HTTP_HEADER = "http.forwarded.header.xfor";

    public static final String HSA_FILES = "hsa.files";

    public static final String TIMEOUT_JSON_FILE = "timeout.json.file";
    public static final String TIMEOUT_JSON_FILE_DEFAULT_TJANSTEKONTRAKT_NAME = "timeout.json.file.default.tjanstekontrakt.name";
    public static final String WSDL_JSON_FILE = "wsdl.json.file";
    public static final String WSDLFILES_DIRECTORY = "wsdlfiles.directory";

    public static final String THROW_VP013_WHEN_ORIGNALCONSUMER_NOT_ALLOWED = "throw.vp013.when.originalconsumer.not.allowed";
    public static final String PROPAGATE_CORRELATION_ID_FOR_HTTPS = "propagate.correlation.id.for.https";
    public static final String VP_HEADER_USER_AGENT = "vp.header.user.agent";
    public static final String VP_HEADER_CONTENT_TYPE = "vp.header.content.type";
    public static final String VP_USE_ROUTING_HISTORY = "vp.use.routing.history";

    public static final String VP_HTTP_ROUTE_URL = "vp.http.route.url";
    public static final String VP_HTTPS_ROUTE_URL = "vp.https.route.url";
    public static final String VP_HTTP_GET_ROUTE = "vp.status.url";

    public static final String PRODUCER_CHUNKED_ENCODING = "producer.chunked.encoding";
    public static final String HAWTIO_AUTHENTICATION_ENABLED = "hawtio.authentication.enabled";
    public static final String HAWTIO_EXTERNAL_LOGINFILE = "hawtio.external.loginfile";

    public static final String MEMORY_LOG_PERIOD = "memory.logger.period.seconds";
}
