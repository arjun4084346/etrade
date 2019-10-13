package com.etrade.exampleapp.v1.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfig {
  public static final String[] watchlist;
  public static final double arbitrageStrength;
  public static final double targetGainPercentage;
  public static final int criticalDTE;

  static {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    try(InputStream in = classloader.getResourceAsStream("configs")) {
      Properties props = new Properties();
      props.load(in);
      watchlist = props.getProperty("watchlist").split(",");
      arbitrageStrength = Double.parseDouble(props.getProperty("arbitrageStrength"));
      targetGainPercentage = Double.parseDouble(props.getProperty("targetGainPercentage"));
      criticalDTE = Integer.parseInt(props.getProperty("criticalDTE"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
