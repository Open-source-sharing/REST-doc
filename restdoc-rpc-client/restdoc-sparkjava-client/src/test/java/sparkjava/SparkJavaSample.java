package sparkjava;


import spark.Service;
import spark.Spark;
import spark.routematch.RouteMatch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static spark.Spark.get;

public class SparkJavaSample {

  public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    get("/hello", (req, res) -> "Hello World");

    Method method = Spark.class.getDeclaredMethod("getInstance");
    method.setAccessible(true);
    Service service = (Service) method.invoke(null);

    List<RouteMatch> routes = service.routes();

    for (RouteMatch route : routes) {
      System.err.println(route.getMatchUri());
    }
  }
}
