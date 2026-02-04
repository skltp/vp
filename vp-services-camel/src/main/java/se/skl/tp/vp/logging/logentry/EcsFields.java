package se.skl.tp.vp.logging.logentry;

/**
 * Constants for ECS (Elastic Common Schema) field names used in logging.
 * @see <a href="https://www.elastic.co/guide/en/ecs/current/index.html">Elastic Common Schema (ECS) Reference</a>
 */
public class EcsFields {
    private EcsFields() { /* Static utility class */ }
    /** Name of the host machine */
    public static final String HOST_HOSTNAME = "host.hostname";
    /** IP address of the host machine */
    public static final String HOST_IP = "host.ip";
    /** Architecture of the host (e.g., x86_64, arm64) */
    public static final String HOST_ARCHITECTURE = "host.architecture";
    /** OS family (e.g., windows, unix, darwin) */
    public static final String HOST_OS_FAMILY = "host.os.family";
    /** Operating system name (e.g., Linux, Windows Server 2019) */
    public static final String HOST_OS_NAME = "host.os.name";
    /** Operating system version */
    public static final String HOST_OS_VERSION = "host.os.version";
    /** Operating system platform (e.g., linux, windows, darwin) */
    public static final String HOST_OS_PLATFORM = "host.os.platform";
    /** Type of host (e.g., container, vm, physical) */
    public static final String HOST_TYPE = "host.type";
    /** Correlation ID for tracing, used to tie related events together */
    public static final String TRACE_ID = "trace.id";
    /** Span ID for tracing, used to identify a specific operation within a trace */
    public static final String SPAN_ID = "span.id";
    /** Camel exchange ID */
    public static final String TRANSACTION_ID = "transaction.id";

    /** Unique identifier for this event. */
    public static final String EVENT_ID = "event.id";
    /** The action captured by the event. */
    public static final String EVENT_ACTION = "event.action";
    /** Name of the module this data is coming from. */
    public static final String EVENT_MODULE = "event.module";
    /** Duration of the event in nanoseconds. */
    public static final String EVENT_DURATION = "event.duration";
    /** First level in the ECS category hierarchy. */
    public static final String EVENT_KIND = "event.kind";
    /** Second level in the ECS category hierarchy. */
    public static final String EVENT_CATEGORY = "event.category";
    /** Third level in the ECS category hierarchy. */
    public static final String EVENT_TYPE = "event.type";

    // Source and destination fields
    /** Source address (e.g., IP address or hostname) */
    public static final String SOURCE_ADDRESS = "source.address";
    /** Source IP address */
    public static final String SOURCE_IP = "source.ip";
    /** Destination address (e.g., IP address or hostname) */
    public static final String DESTINATION_ADDRESS = "destination.address";
    /** Destination domain name */
    public static final String DESTINATION_DOMAIN = "destination.domain";
    /** Destination port number */
    public static final String DESTINATION_PORT = "destination.port";

    /** Error type, e.g., exception class name */
    public static final String ERROR_TYPE = "error.type";
    /** Error message describing the error */
    public static final String ERROR_MESSAGE = "error.message";
    /** Stack trace of the error */
    public static final String ERROR_STACK_TRACE = "error.stack_trace";

    /** Key-value pairs of HTTP request headers */
    public static final String HTTP_REQUEST_HEADERS = "http.request.headers";
    /** Size of the HTTP request body in bytes */
    public static final String HTTP_REQUEST_BODY_BYTES = "http.request.body.bytes";
    /** Content of the HTTP request body (not traced in production) */
    public static final String HTTP_REQUEST_BODY_CONTENT = "http.request.body.content";
    /** HTTP request method (e.g., GET, POST) */
    public static final String HTTP_REQUEST_METHOD = "http.request.method";
    /** Key-value pairs of HTTP response headers */
    public static final String HTTP_RESPONSE_HEADERS = "http.response.headers";
    /** Size of the HTTP response body in bytes */
    public static final String HTTP_RESPONSE_BODY_BYTES = "http.response.body.bytes";
    /** Content of the HTTP response body (not traced in production) */
    public static final String HTTP_RESPONSE_BODY_CONTENT = "http.response.body.content";
    /** HTTP response status code */
    public static final String HTTP_RESPONSE_STATUS_CODE = "http.response.status_code";

    /** Name of the service */
    public static final String SERVICE_NAME = "service.name";

    /** Complete URL of the incoming request to the service */
    public static final String URL_FULL = "url.full";
    /** Complete URL of the outgoing request to the backend (forwarding destination) */
    public static final String URL_ORIGINAL = "url.original";

    // TLS/SSL fields
    /** Normalized lowercase protocol name parsed from original string (e.g., tls, ssl) */
    public static final String TLS_VERSION_PROTOCOL = "tls.version_protocol";
    /** Numeric part of the version parsed from the original string (e.g., 1.2, 1.3) */
    public static final String TLS_VERSION = "tls.version";
    /** TLS cipher suite used in the connection */
    public static final String TLS_CIPHER = "tls.cipher";
    /** Whether the TLS connection was successfully established */
    public static final String TLS_ESTABLISHED = "tls.established";

    // Server fields (for TLS handshake logging)
    /** Server address (hostname or IP) */
    public static final String SERVER_ADDRESS = "server.address";

    /** Labels for additional metadata as key-value pairs */
    public static final String LABELS = "labels.";

    /** The log message */
    public static final String MESSAGE = "message";
}

