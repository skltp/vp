package se.skl.tp.vp.logging.logentry;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.StringMapMessage;

/**
 * Base class for ECS-compliant log entries.
 * Provides common functionality for structured logging with standard ECS fields.
 */
@Slf4j
public abstract class BaseEcsLogEntry extends StringMapMessage {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String EVENT_KIND_VALUE = "event";

    protected static final EcsSystemProperties SYSTEM_PROPERTIES = EcsSystemProperties.getInstance();
    /**
     * Base builder class for ECS log entries.
     * Provides common functionality for building structured log entries.
     * 
     * @param <T> The type of log entry being built
     * @param <B> The type of builder (for method chaining with correct return type)
     */
    public abstract static class BaseBuilder<T extends BaseEcsLogEntry, B extends BaseBuilder<T, B>> {
        protected final StringMapMessage data = new StringMapMessage();
        /**
         * Constructor that initializes common ECS fields.
         * 
         * @param eventAction The event action (e.g., "req-in", "ssl.context.register")
         * @param eventKind The event kind (typically "event")
         * @param eventCategory JSON-encoded array of event categories
         * @param eventModule The module name (e.g., "skltp-messages", "skltp-tls")
         */
        protected BaseBuilder(String eventAction, String eventKind, String eventCategory, String eventModule) {
            putData(EcsFields.EVENT_ACTION, eventAction);
            putData(EcsFields.EVENT_KIND, eventKind);
            putData(EcsFields.EVENT_CATEGORY, eventCategory);
            putData(EcsFields.EVENT_MODULE, eventModule);
            // Add host information - common to all log entries
            addHostInformation();
        }
        /**
         * Adds host information fields to the log entry.
         * This includes hostname, IP, architecture, OS details, and host type.
         */
        protected void addHostInformation() {
            putData(EcsFields.HOST_HOSTNAME, SYSTEM_PROPERTIES.getHostName());
            putData(EcsFields.HOST_IP, SYSTEM_PROPERTIES.getHostIp());
            putData(EcsFields.HOST_ARCHITECTURE, SYSTEM_PROPERTIES.getHostArchitecture());
            putData(EcsFields.HOST_OS_FAMILY, SYSTEM_PROPERTIES.getHostOsFamily());
            putData(EcsFields.HOST_OS_NAME, SYSTEM_PROPERTIES.getHostOsName());
            putData(EcsFields.HOST_OS_VERSION, SYSTEM_PROPERTIES.getHostOsVersion());
            putData(EcsFields.HOST_OS_PLATFORM, SYSTEM_PROPERTIES.getHostOsPlatform());
            putData(EcsFields.HOST_TYPE, SYSTEM_PROPERTIES.getHostType());
        }
        /**
         * Adds a label field to the log entry.
         * Labels are stored with the "labels." prefix as per ECS standards.
         * 
         * @param labelKey The label key (without "labels." prefix)
         * @param value The label value
         */
        protected void putLabel(String labelKey, String value) {
            putData(EcsFields.LABELS + labelKey, value);
        }
        /**
         * Adds a data field to the log entry.
         * Only adds non-null values to keep the log entry clean.
         * 
         * @param key The field key
         * @param value The field value (ignored if null)
         */
        protected void putData(String key, String value) {
            if (value != null) {
                data.put(key, value);
            }
        }
        /**
         * Returns this builder instance with the correct type for method chaining.
         * This enables fluent API style with proper type safety.
         * 
         * @return This builder instance
         */
        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }
        /**
         * Builds the log entry.
         * Subclasses must implement this to create the appropriate log entry type.
         * 
         * @return The constructed log entry
         */
        public abstract T build();
    }
}