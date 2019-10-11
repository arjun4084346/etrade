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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Utils {
  private final static PrintStream out = System.out;
  private final static Logger log = Logger.getLogger(ETClientApp.class);

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
    double callPrice = ((Double) call.get("bid") + (Double) call.get("ask")) / 2;
    double intrinsic = (Double) call.get("strikePrice") > currentPrice ? 0.0 : currentPrice - strikePrice;

    return Math.max(0.0, callPrice - intrinsic) * 100;
  }

  // This only works with JSONObject of quote response
  public static double getExtrinsicOfPut(double currentPrice, @NonNull JSONObject put) {
    double strikePrice = (Double) put.get("strikePrice");
    double putPrice = ((Double) put.get("bid") + (Double) put.get("ask")) / 2;
    double intrinsic = (Double) put.get("strikePrice") < currentPrice ? 0.0 : strikePrice - currentPrice;

    return Math.max(0.0, putPrice - intrinsic) * 100;
  }

  public static double getExtrinsicValue(double strikePrice, double currentPrice, OptionsType optionsType) {
    double intrinsic = 0;
    if (optionsType == OptionsType.CALL) {
      intrinsic = strikePrice < currentPrice ? currentPrice - strikePrice : 0;
    } else if (optionsType == OptionsType.PUT) {
      intrinsic = strikePrice > currentPrice ? strikePrice - currentPrice : 0;
    }

    return Math.max(0.0, currentPrice - intrinsic) * 100;
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
  // maybe need to return a mapping of strategy->positions
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

    if (numOfShortCalls == numOfShortPuts) {
      return OptionsStrategy.SHORT_STRANGLE;
    } else if (numOfShortCalls == numOfLongPuts && numOfShares == 100 * numOfLongPuts) {
      return OptionsStrategy.LONG_ARBITRAGE;
    }

    return OptionsStrategy.CREDIT_SPREAD;
  }

  public static String getSymbolFromQuoteDetails(String quoteDetail) {
    int index = quoteDetail.lastIndexOf('/');
    return quoteDetail.substring(index);
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
    CREDIT_SPREAD,
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



