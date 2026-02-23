package se.skl.tp.vp.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.Message;
import se.skl.tp.vp.logging.MessageLogger;

// note: class name need not match the @Plugin name.
@Plugin(name = "TestLogAppender", category = "Core", elementType = "appender", printObject = true)
public class TestLogAppender extends AbstractAppender {

  private static TestLogAppender instance;

  public static TestLogAppender getInstance() {
    if(instance != null && !instance.isStarted()){
      instance.start();
    }

    TestLogAppender.clearEvents();

    return instance;
  }

  public static TestLogAppender getInstance(String name, Filter filter, Layout<? extends Serializable> layout) {
    if (instance == null) {
      instance = new TestLogAppender(name, filter, layout, true);
    }
    return instance;
  }

  private static final List<LogEvent> events = new ArrayList<>();
  @Getter
  private static final List<LogEvent> leakEvents = new ArrayList<>();

  protected TestLogAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
    super(name, filter, layout, true, Property.EMPTY_ARRAY);
  }

  protected TestLogAppender(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
  }

  @Override
  public void append(final LogEvent event) {

    // Clear events if start if new incmming message to VP
    if(event.getLoggerName().equalsIgnoreCase(MessageLogger.REQ_IN)) {
      clearEvents();
    }

    if(event.getLoggerName().equalsIgnoreCase("io.netty.util.ResourceLeakDetector")){
     leakEvents.add(event.toImmutable());
    }else {
      events.add(event.toImmutable());
    }
  }

  public static void clearEvents(){
    events.clear();
  }

  public static void clearLeakEvents(){
    leakEvents.clear();
  }

  public static String getEventMessage(String loggerName, int index) {

    List<LogEvent> newEvents = getEvents(loggerName);

    if(newEvents.size() < index)
      return null;

    return newEvents.get(index).getMessage().getFormattedMessage();
  }
  public static Message getEventMessageObject(String loggerName, int index) {

    List<LogEvent> newEvents = getEvents(loggerName);

    if(newEvents.size() < index)
      return null;

    return newEvents.get(index).getMessage();
  }

  public static LogEvent getEvent(String loggerName, int index) {

    List<LogEvent> newEvents = getEvents(loggerName);

    if(newEvents.size() < index)
      return null;

    return newEvents.get(index);
  }

  public static List<LogEvent> getEvents(String loggerName) {
    return events.stream().filter(lg -> loggerName.equals(lg.getLoggerName())).toList();
  }

  public static long getNumEvents(String loggerName) {
    return events.stream().filter(lg -> loggerName.equals(lg.getLoggerName())).count();
  }


  // Your custom appender needs to declare a factory method
  // annotated with `@PluginFactory`. Log4j will parse the configuration
  // and call this factory method to construct an appender instance with
  // the configured attributes.
  @PluginFactory
  public static TestLogAppender createAppender(
      @PluginAttribute("name") String name,
      @PluginElement("Layout") Layout<? extends Serializable> layout,
      @PluginElement("Filter") final Filter filter,
      @PluginAttribute("otherAttribute") String otherAttribute) {

    if (name == null) {
      LOGGER.error("No name provided for TestLogAppender");
      return null;
    }
    if (layout == null) {
      layout = PatternLayout.createDefaultLayout();
    }

    return getInstance(name, filter, layout);
  }

  public static void assertLogMessage(String loggerName, String receiver, String trace, String logMessage, String endpointUrl, String senderId) {
    String respOutLogMsg = TestLogAppender.getEventMessage(loggerName, 0);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "event.action=\"([^\"]+)\"", logMessage, 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "labels.senderid=\"([^\"]+)\"", senderId, 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "url.original=\"([^\"]+)\"", endpointUrl, 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "labels.receiverid=\"([^\"]+)\"",  receiver, 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "labels.routerVagvalTrace=\"([^\"]+)\"", trace, 1);
    JunitUtil.assertMatchRegexGroup(respOutLogMsg, "labels.routerBehorighetTrace=\"([^\"]+)\"", trace, 1);
  }

}