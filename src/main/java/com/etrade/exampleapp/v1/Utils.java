package com.etrade.exampleapp.v1;

import com.etrade.exampleapp.v1.clients.accounts.PortfolioClient;
import com.etrade.exampleapp.v1.clients.market.QuotesClient;
import com.etrade.exampleapp.v1.clients.order.OrderTerm;
import com.etrade.exampleapp.v1.clients.order.PriceType;
import com.etrade.exampleapp.v1.exception.ApiException;
import com.etrade.exampleapp.v1.terminal.ETClientApp;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Utils {
  private final static PrintStream out = System.out;
  private final static Logger log = Logger.getLogger(ETClientApp.class);
  private final static long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000L;

  public static void handleApiException(ApiException e) {
    out.println();
    out.println(String.format("HttpStatus: %20s", e.getHttpStatus()));
    out.println(String.format("Message: %23s", e.getMessage()));
    out.println(String.format("Error Code: %20s", e.getCode()));
    out.println();
    out.println();
  }

  public static double getCurrentMidPrice(AnnotationConfigApplicationContext ctx, String symbol) {
    QuotesClient client = ctx.getBean(QuotesClient.class);

    try {
      String response = client.getQuotes(symbol);
      try {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
        JSONObject quoteResponse = (JSONObject) jsonObject.get("QuoteResponse");
        JSONArray quoteData = (JSONArray) quoteResponse.get("QuoteData");

        if (quoteData != null) {
          for (Object quoteDatum : quoteData) {
            JSONObject innerObj = (JSONObject) quoteDatum;
            JSONObject all = (JSONObject) innerObj.get("All");
            return ((Double) all.get("bid") + (Double) all.get("ask")) / 2;
          }
        } else {
          log.error(" Error : Invalid stock symbol.");
          out.println("Error : Invalid Stock Symbol.\n");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch(ApiException e) {
      Utils.handleApiException(e);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0.0;
  }

  // This only works with JSONObject of quote response
  public static double getExtrinsicOfCall(double currentPrice, @NonNull JSONObject call) {
    double strikePrice = (Double) call.get("strikePrice");
    double bid = (Double) call.get("bid");
    double ask = (Double) call.get("ask");

    if (ask - bid > bid/5) {
      return -1.0;
    }
    double callPrice = (bid + ask) / 2;
    double intrinsic = (Double) call.get("strikePrice") > currentPrice ? 0.0 : currentPrice - strikePrice;

    return Math.max(0.0, callPrice - intrinsic) * 100;
  }

  // This only works with JSONObject of quote response
  // currentPrice is the price of underlying stock
  public static double getExtrinsicOfPut(double stockPrice, @NonNull JSONObject put) {
    double strikePrice = (Double) put.get("strikePrice");
    double bid = (Double) put.get("bid");
    double ask = (Double) put.get("ask");

    if (ask - bid > bid/10) {
      return -1.0;
    }
    double putPrice = (bid + ask) / 2;
    double intrinsic = (Double) put.get("strikePrice") < stockPrice ? 0.0 : strikePrice - stockPrice;

    return Math.max(0.0, putPrice - intrinsic) * 100;
  }

  // currentPrice is the price of option
  public static double getExtrinsicValue(double strikePrice, double underlyingPrice, double optionsPrice, OptionsType optionsType) {
    double intrinsic = 0;
    if (optionsType == OptionsType.CALL) {
      intrinsic = underlyingPrice < optionsPrice ? underlyingPrice - strikePrice : 0;
    } else if (optionsType == OptionsType.PUT) {
      intrinsic = strikePrice > underlyingPrice ? strikePrice - underlyingPrice : 0;
    }

    return Math.max(0.0, optionsPrice - intrinsic) * 100;
  }

  public static double findShortArbitrage(AnnotationConfigApplicationContext ctx, JSONObject call, Calendar expiryDate, double arbitrage) {
    String symbol = getSymbolFromQuoteDetails((String) call.get("symbol"));
    QuotesClient client = ctx.getBean(QuotesClient.class);

    try {
      String response = client.getQuotes(symbol);
      JSONParser jsonParser = new JSONParser();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
      JSONObject quoteResponse = (JSONObject) jsonObject.get("QuoteResponse");
      JSONArray quoteData = (JSONArray) quoteResponse.get("QuoteData");

      if (quoteData != null) {
        // not sure why it returns an array of quoteDatum
        JSONObject innerObj = (JSONObject) quoteData.get(0);
        innerObj = (JSONObject) innerObj.get("All");
        double dividend = (double) innerObj.get("dividend");
        long exDividendDate = (long) innerObj.get("exDividendDate");
        exDividendDate -= 20000L; // subtract a few hours to avoid cases where both dates are almost same
        exDividendDate *= 1000L;	// ex dividend date is in seconds, not ms!
        // dividend should not be applied if ex-dividend date is on the next day of the expiry date
        if (System.currentTimeMillis() <= exDividendDate && exDividendDate <= expiryDate.getTimeInMillis()) {
          //out.println("adding dividend");
          arbitrage += dividend*100;
          // exclude amt strikes, they are more likely to get assign
          double callDelta = (Double) ((JSONObject) call.get("OptionGreeks")).get("delta");
          if (callDelta >= .16) {
            return 0;
          }
          //out.println("ex dividend date : " + new Date(exDividendDate * 1000).toString());
        } else {
          // this might be ok in some cases after adding new if condition
          //out.println("not adding dividend");
        }
      } else {
        out.println(ETClientApp.ANSI_RED + "[ERROR] : " + ETClientApp.ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
        return 0;
      }
    } catch (ApiException | ParseException e) {
      log.error(e);
    }
    return arbitrage;
  }

  public static double calculateAnnualArbitrage(double currentPrice, int daysToExpiry, double dividendAdjustageArbitrage) {
    double annualArbitrage = (dividendAdjustageArbitrage / daysToExpiry) * 365;
    double margin = currentPrice * getMarginPercentage();
    // interest = prt/100 => r = interest * 100 / pt
    double annualArbitragePercentage = annualArbitrage * 100 / margin;
    return Math.round(annualArbitragePercentage*100)/100.0;
  }

  public static Map<String, List<JSONObject>> getPositions(AnnotationConfigApplicationContext ctx) {
    Map<String, List<JSONObject>> positionGroups = new HashMap<>();
    JSONParser jsonParser = new JSONParser();

    try {
      PortfolioClient client = ctx.getBean(PortfolioClient.class);
      String response = client.getPortfolio();
      JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
      JSONObject portfolioResponse = (JSONObject) jsonObject.get("PortfolioResponse");
      JSONArray accountPortfolioArr = (JSONArray) portfolioResponse.get("AccountPortfolio");

      for (Object value : accountPortfolioArr) {
        JSONObject acctObj = (JSONObject) value;
        JSONArray positionArr = (JSONArray) acctObj.get("Position");

        for (Object o : positionArr) {
          JSONObject innerObj = (JSONObject) o;
          //percentageGainManagement(innerObj);
          JSONObject product = (JSONObject) innerObj.get("Product");
          String symbol = (String) product.get("symbol");
          List<JSONObject> positions = positionGroups.getOrDefault(symbol, new ArrayList<>());
          positions.add(innerObj);
          positionGroups.put(symbol, positions);
        }
      }
    } catch(ApiException e) {
      Utils.handleApiException(e);
    } catch (Exception e) {
      log.error(" getPortfolio ", e);
      out.println();
      out.println(String.format("Message: %23s", e.getMessage()));
      out.println();
      out.println();
    }
    return positionGroups;
  }

  // TODO : this need a lot of improvement
  // next identify (short strangle + short option)
  // maybe need to return a mapping of strategy->positions
  // one algo to find srategies is to start searching by num of positions.
  // if num of positions is 4, it can only be iron condor or something managed in the same way
  // if it is 6, it can only be butterfly
  public static OptionsStrategy identityPositionType(List<JSONObject> value) {
    int numOfShortCalls = 0;
    int numOfLongCalls = 0;
    int numOfShortPuts = 0;
    int numOfLongPuts = 0;
    int numOfContracts = 0;
    int numOfShares = 0;
    int numOfPositions = 0;

    for(JSONObject position : value) {
      JSONObject product = (JSONObject) position.get("Product");
      SecurityType securityType = SecurityType.valueOf((String) product.get("securityType"));
      PositionType positionType = PositionType.valueOf((String) position.get("positionType"));
      long quantity = Math.abs((long) position.get("quantity"));

      if (securityType == SecurityType.OPTN) {
        numOfContracts++;
        OptionsType optionsType = OptionsType.valueOf((String) product.get("callPut"));

        if (optionsType == OptionsType.CALL && positionType == PositionType.LONG) {
          numOfLongCalls += quantity;
        } else if (optionsType == OptionsType.CALL && positionType == PositionType.SHORT) {
          numOfShortCalls += quantity;
        } else if (optionsType == OptionsType.PUT && positionType == PositionType.LONG) {
          numOfLongPuts += quantity;
        } else if (optionsType == OptionsType.PUT && positionType == PositionType.SHORT) {
          numOfShortPuts += quantity;
        }
      } else {
        numOfShares += quantity;
      }
    }

    if (numOfLongCalls == 0
        && numOfLongPuts == 0
        && numOfShortCalls == numOfShortPuts) {
      return OptionsStrategy.SHORT_STRANGLE;
    } else if (numOfLongPuts > 0
        && numOfShortCalls == numOfLongPuts
        && numOfShares == 100 * numOfLongPuts) {
      return OptionsStrategy.LONG_ARBITRAGE;
    } else if (numOfLongCalls == 0
        && numOfLongPuts == 0
        && numOfShortCalls > 0
        && numOfShortPuts == 0) {
      return OptionsStrategy.SHORT_CALL;
    } else if (numOfLongCalls == 0
        && numOfLongPuts == 0
        && numOfShortPuts > 0
        && numOfShortCalls == 0) {
      return OptionsStrategy.SHORT_CALL;
    } else if ((numOfShortCalls == numOfLongCalls) || (numOfLongPuts == numOfShortPuts)) {
      return OptionsStrategy.SPREAD;
    }
    return OptionsStrategy.UNSUPPORTED;
  }

  public static String getSymbolFromQuoteDetails(String quoteDetail) {
    int index = quoteDetail.lastIndexOf('/');
    return quoteDetail.substring(index + 1);
  }

  public static Calendar getExpiryFromJson(JSONObject object, String key) {
    String[] quoteDetail = ((String) object.get(key)).split(":");
    // e.g. https://api.etrade.com/v1/market/quote/TSLA:2019:11:22:CALL:257.500000
    Calendar expiryDate = Calendar.getInstance();
    expiryDate.set(Integer.parseInt(quoteDetail[2]), Integer.parseInt(quoteDetail[3]) - 1, Integer.parseInt(quoteDetail[4]));
    return expiryDate;
  }

  public static int getDaysToExpiry(Calendar expiryDate) {
    return (int) ((expiryDate.getTimeInMillis() - System.currentTimeMillis()) / MILLIS_IN_A_DAY);
  }

  public static String calendarToDate(Calendar date) {
    return date.toInstant().toString().substring(0, 10);
  }

  // TODO : difficult to calculate,
  //  MARGIN CAN DEPEND ON STRIKE PRICE!!!
  //  maybe just use preview client?
  //  margin = orderpreviewclient.get("currentOrderImpact")
  public static int getMarginPercentage() {
    // assuming 50% margin requirement
    return 50;
  }

  public static String getPrice(PriceType priceType, JSONObject orderDetail) {
    String value;

    if (PriceType.LIMIT == priceType ) {
      value = String.valueOf(orderDetail.get("limitPrice"));
    } else if( PriceType.MARKET == priceType) {
      value = "Mkt";
    } else {
      value = priceType.getValue();
    }
    return value;
  }

  public static String getTerm(OrderTerm orderTerm) {
    String value;

    if (OrderTerm.GOOD_FOR_DAY == orderTerm) {
      value = "Day";
    } else {
      value = orderTerm.getValue();
    }

    return value;
  }

  public static String convertLongToDate(Long ldate) {
    LocalDateTime dte = LocalDateTime.ofInstant(Instant.ofEpochMilli(ldate), ZoneId.systemDefault());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yy");
    return formatter.format(dte);
  }

  public enum OptionsStrategy {
    SHORT_STRANGLE,
    SHORT_STRADDLE,
    LONG_ARBITRAGE,
    SHORT_DIAGONAL,
    LONG_DIAGONAL,
    SHORT_ARBITRAGE,  // TODO : so difficult to find and manage,
    // take dividend into account, notice if drop happens only on one day or gradually?
    // also if the security is hard to borrow? whats the interest rate, if any?
    SPREAD,
    // we are not yet ready to distinguish debit and credit spreads
    CREDIT_SPREAD,
    SHORT_PUT,
    SHORT_CALL,
    SHORT_BROKEN_WING_BUTTERFLY,
    UNSUPPORTED
  }

  public enum SecurityType {
    EQ,
    OPTN
  }

  public enum OptionsType {
    CALL,
    PUT
  }

  public enum PositionType {
    LONG,
    SHORT
  }
}



