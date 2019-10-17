package com.etrade.exampleapp.v1.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfig {
  public static final String[] watchlist;
  public static final double arbitrageStrength;
  public static final double targetGainPercentage;
  public static final int criticalDTE;
  public static final int noManagementPeriod;

  static {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    try(InputStream in = classloader.getResourceAsStream("configs")) {
      Properties props = new Properties();
      props.load(in);
      watchlist = props.getProperty("watchlist").split(",");
      arbitrageStrength = Double.parseDouble(props.getProperty("arbitrageStrength", "12"));
      targetGainPercentage = Double.parseDouble(props.getProperty("targetGainPercentage", "66"));
      criticalDTE = Integer.parseInt(props.getProperty("criticalDTE", "14"));
      noManagementPeriod = Integer.parseInt(props.getProperty("noManagementPeriod", "16"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
