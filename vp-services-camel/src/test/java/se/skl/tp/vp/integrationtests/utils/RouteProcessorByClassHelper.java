package se.skl.tp.vp.integrationtests.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.camel.Navigate;
import org.apache.camel.Processor;
import org.apache.camel.Route;

public class RouteProcessorByClassHelper<T> {

  private Class<T> fClass;

  private Optional<Map<T, Integer>> find(Route source) {
    return internalFind(source.navigate(), 0);
  }

  private Optional<Map<T, Integer>> internalFind(
      Navigate<Processor> navigate, int orderOfApperance) {
    for (Processor processor : navigate.next()) {
      orderOfApperance++;
      if (fClass.isInstance(processor)) {
        return createResult((T) processor, orderOfApperance);
      }
      if (processor instanceof Navigate) {
        Optional<Map<T, Integer>> tmpres = internalFind((Navigate) processor, orderOfApperance);
        if (tmpres.isPresent()) {
          return tmpres;
        }
      }
    }
    return Optional.empty();
  }

  private Optional<Map<T, Integer>> createResult(T processor, int orderOfApperance) {
    Map<T, Integer> result = new HashMap<>();
    result.put(processor, new Integer(orderOfApperance));
    return Optional.ofNullable(result);
  }

  /**
   * Help class for examining the presence of certain processor in  a route, ensure order between
   * processors are as expected in a certain route etc.
   * @param pClass the class of intrest
   */
  private RouteProcessorByClassHelper(Class<T> pClass) {
    fClass = pClass;
  }

  public static int orderOfAppearance(Route source, Class<?> pClass) {
    Optional<Map<?, Integer>> res = new RouteProcessorByClassHelper(pClass).find(source);
    return (res.isPresent()) ? res.get().values().iterator().next() : -1;
  }

  public static <T> Optional<T> find(Route source, Class<T> pClass) {
    Optional<Map<T, Integer>> res = new RouteProcessorByClassHelper<T>(pClass).find(source);
    return (res.isPresent())
        ? Optional.ofNullable(res.get().keySet().iterator().next())
        : Optional.empty();
  }

  public static boolean processorOfClassExistsInRoute(Route source, Class<?> pClass) {
    return find(source, pClass).isPresent();
  }

  public static boolean isOfOrder(Route route, Class<?> before, Class<?> after) {
    return orderOfAppearance(route, before) < orderOfAppearance(route, after);
  }
}
