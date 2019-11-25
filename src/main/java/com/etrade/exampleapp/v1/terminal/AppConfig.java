package com.etrade.exampleapp.v1.terminal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfig {
  public static final String[] watchlist;
  public static final String[] noDividendWatchlist;
  public static final double arbitrageStrength;
  public static final double arbitrageStrengthDollars;
  public static final double targetGainPercentage;
  public static final int criticalDTE;
  public static final int noManagementPeriod;
  public static final int sleepTime;

  static {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    try(InputStream in = classloader.getResourceAsStream("configs")) {
      Properties props = new Properties();
      props.load(in);
      watchlist = props.getProperty("watchlist").split(",");
      noDividendWatchlist = props.getProperty("noDividendWatchlist").split(",");
      arbitrageStrength = Double.parseDouble(props.getProperty("arbitrageStrength", "12"));
      arbitrageStrengthDollars = Double.parseDouble(props.getProperty("arbitrageStrengthDollars", "20"));
      targetGainPercentage = Double.parseDouble(props.getProperty("targetGainPercentage", "66"));
      criticalDTE = Integer.parseInt(props.getProperty("criticalDTE", "14"));
      noManagementPeriod = Integer.parseInt(props.getProperty("noManagementPeriod", "16"));
      sleepTime = Integer.parseInt(props.getProperty("autoMode.sleepTime", "60"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
