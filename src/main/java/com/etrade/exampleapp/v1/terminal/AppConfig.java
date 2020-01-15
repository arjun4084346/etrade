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
  public static final double highIVRank;
  public static final int noManagementPeriod;
  public static final int sleepTime;
  public static final int maxNumOfPositions;
  public static final double level1Delta;
  public static final double level2Delta;
  public static final double level3Delta;


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
      highIVRank = Double.parseDouble(props.getProperty("highIVRank", "30.0"));
      noManagementPeriod = Integer.parseInt(props.getProperty("noManagementPeriod", "16"));
      sleepTime = Integer.parseInt(props.getProperty("autoMode.sleepTime", "60"));
      maxNumOfPositions = Integer.parseInt(props.getProperty("maxNumOfPositions", "150"));
      level1Delta = Double.parseDouble(props.getProperty("level1Delta", "0.20"));
      level2Delta = Double.parseDouble(props.getProperty("level2Delta", "0.40"));
      level3Delta = Double.parseDouble(props.getProperty("level3Delta", "0.80"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
