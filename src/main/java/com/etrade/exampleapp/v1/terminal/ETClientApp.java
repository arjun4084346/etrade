package com.etrade.exampleapp.v1.terminal;

import com.etrade.exampleapp.v1.clients.accounts.OptionsChainClient;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.etrade.exampleapp.config.OOauthConfig;
import com.etrade.exampleapp.config.SandBoxConfig;
import com.etrade.exampleapp.v1.clients.accounts.AccountListClient;
import com.etrade.exampleapp.v1.clients.accounts.BalanceClient;
import com.etrade.exampleapp.v1.clients.accounts.PortfolioClient;
import com.etrade.exampleapp.v1.clients.market.QuotesClient;
import com.etrade.exampleapp.v1.clients.order.OrderClient;
import com.etrade.exampleapp.v1.clients.order.OrderPreviewClient;
import com.etrade.exampleapp.v1.exception.ApiException;

import static com.etrade.exampleapp.v1.Utils.*;

@Slf4j
public class ETClientApp extends AppCommandLine {
	private AnnotationConfigApplicationContext ctx = null;
	private Map<String, String> acctListMap = new HashMap<>();
	private boolean isLive = false;
	private boolean auto = false;
	public final static String lineSeparator = System.lineSeparator();
	public final static PrintStream out = System.out;
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_RESET = "\u001B[0m";
	private static final String SEPARATOR = "-----------------------------------------\n";

	public ETClientApp(String[] args) {
		super(args);
	}

	private void init(boolean flag){
		try {
			log.debug("Current Thread :"+ Thread.currentThread().getName() + ", Id : "+Thread.currentThread().getId() );
			if (ctx != null) {
				ctx.close();
			}
			if (flag) {
				ctx = new AnnotationConfigApplicationContext();
				ctx.register(OOauthConfig.class);
				ctx.refresh();
			} else {
				ctx = new AnnotationConfigApplicationContext();
				ctx.register(SandBoxConfig.class);
				ctx.refresh();
			}
		} catch (Exception e) {
			out.println( " Sorry we are not able to initiate oauth request at this time..");
			log.error("Oauth Initialization failed ",e);
		}
		log.debug(" Context initialized for "+(isLive ? "Live Environment":" Sandbox Environment"));
	}

/*	private void initOOauth() {
		log.debug("Initializing the oauth " );
		try {
			if( sessionData == null) {
				log.debug(" Re-Initalizing oauth ...");
				OauthController controller = ctx.getBean(OauthController.class);

				controller.fetchToken();

				controller.authorize();
				sessionData = controller.fetchAccessToken();
				log.debug(" Oauth initialized ");
			}
		}catch(Exception e) {
			log.debug(e);
			out.println( " Sorry we are not able to continue at this time, please restart the client..");
		}
	}*/

	public static void main(String[] args) {
		ETClientApp obj = new ETClientApp(args);

		if (obj.hasOption("help")) {
			obj.printHelp();
			return;
		}

		try {
			obj.keyMenu(args);
		} catch (NumberFormatException e) {
			System.out.println(" Main menu : NumberFormatException ");
		} catch (IOException e) {
			System.out.println(" Main menu : System failure ");;
		}
	}

	private void keyMenu(String[] args) throws NumberFormatException, IOException {
		int choice;
		String arg = "";

		do {
			if (args.length > 0) {
				choice = 2;
				arg = args[0];
				auto = true;
			} else {
				//printKeyMenu();
				choice = 2; //KeyIn.getKeyInInteger();
			}
			switch (choice) {
				case 1:
					log.debug(" Initializing sandbox application context..");
					isLive = false;
				case 2:
					log.debug(" Initializing Live application context..");
					isLive = true;
					init(isLive);
					mainMenu(this, arg);
					break;
				case 'x':
					out.println("Goodbye");
					System.exit(0);
				default:
					out.println("Invalid Option :");
					out.println("Goodbye");
			}
		} while (true);
	}

	private void mainMenu(ETClientApp obj, String arg) throws NumberFormatException, IOException {
		int choice = 0;

		do {
			if (arg.length() > 0) {
				if (arg.equals("find")) {
					choice = 5;
				} else if (arg.equals("manage")) {
					choice = 6;
				}
				out.println(SEPARATOR);
			} else {
				printMainMenu();
				choice = KeyIn.getKeyInInteger();
			}

			switch (choice) {
				case 1:
					out.println(" Input selected for main menu : "+ choice);
					getAccountList();
					obj.subMenu(obj);
					choice = 99;
					break;
				case 2:
					out.println(" Input selected for main menu : "+ choice);
					out.print("Please enter Stock Symbol: ");
					String symbol = KeyIn.getKeyInString();
					getQuotes(symbol);
					break;
				case 3:
					out.println("Back to previous menu");
					keyMenu(new String[]{});
					break;
				case 4:
					out.println("Please enter stock symbol: ");
					symbol = KeyIn.getKeyInString();
					List<Pair> queryParams = Collections.singletonList(Pair.of("symbol", symbol));
					getOptionsChain(queryParams);
					break;
				case 5:
					findArbitrageOpportunities();
					if (auto) {
						try {
							Thread.sleep(60 * 1000L);
						} catch (InterruptedException e) {
							out.println(ANSI_RED + "[ERROR] : " + ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
						}
					}
					break;
				case 6:
					managePortfolio();
					if (auto) {
						try {
							Thread.sleep(60 * 1000L);
						} catch (InterruptedException e) {
							out.println(ANSI_RED + "[ERROR] : " + ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
						}
					}
					break;
				default:
					out.println("Invalid Option :");
					choice = 'x';
					out.println("Goodbye");
					System.exit(0);
					break;
			}
		} while (!"x".equals(choice) || choice != 99);
	}

	private void subMenu(ETClientApp obj) throws NumberFormatException, IOException {
		int choice;
		String acctKey;

		do {
			printSubMenu();
			choice = KeyIn.getKeyInInteger();
			out.println(" Input selected for submenu : "+ choice);
			switch (choice) {
				case 1:
					out.print("Please select an account index for which you want to get balances: ");
					acctKey = KeyIn.getKeyInString();
					getBalance(acctKey);
					break;
				case 2:
					out.print("Please select an account index for which you want to get Portfolio: ");
					acctKey = KeyIn.getKeyInString();
					getPortfolio(acctKey);
					break;
				case 3:
					printMenu(orderMenu);
					int orderChoice = KeyIn.getKeyInInteger();
					switch(orderChoice) {
						case 1:
							out.print("Please select an account index for which you want to get Orders: ");
							acctKey = KeyIn.getKeyInString();
							getOrders(acctKey);
							break;
						case 2:
							previewOrder();
							break;
						case 3:
							out.println("Back to previous menu");
							subMenu(this);
							break;
						default:
							printMenu(orderMenu);
							break;
					}
					break;
				case 4:
					out.println("Going to main menu");
					obj.mainMenu(obj, "");
					break;
				default:
					printSubMenu();
					break;
			}
		} while (choice != 4);
	}

	public void previewOrder(){
		OrderPreviewClient client = ctx.getBean(OrderPreviewClient.class);
		Map<String, String> inputs = client.getOrderDataMap();
		String accountIdKey;
		out.print("Please select an account index for which you want to preview Order: ");
		String acctKeyIndex = KeyIn.getKeyInString();

		try {
			accountIdKey = getAccountIdKeyForIndex(acctKeyIndex);
		} catch(ApiException e) {
			log.error(e.toString());
			return;
		}
		out.print(" Enter Symbol : ");
		String symbol = KeyIn.getKeyInString();
		inputs.put("SYMBOL", symbol);

		/* Shows Order Action Menu */
		printMenu(orderActionMenu);
		/* Accept OrderAction choice*/
		int choice = isValidMenuItem("Please select valid index for Order Action", orderActionMenu);
		/* Fills data to service*/
		client.fillOrderActionMenu(choice,inputs);
		out.print(" Enter Quantity : ");
		int qty = KeyIn.getKeyInInteger();
		inputs.put("QUANTITY", String.valueOf(qty));
		/* Shows Order PriceType  Menu */
		printMenu(orderPriceTypeMenu);
		/* Accept PriceType choice */
		choice = isValidMenuItem("Please select valid index for price type", orderPriceTypeMenu);
		/* Fills data to service*/
		client.fillOrderPriceMenu(choice,inputs);

		if(choice == 2) {
			out.print(" Enter limit price : ");
			double limitPrice = KeyIn.getKeyInDouble();
			inputs.put("LIMIT_PRICE", String.valueOf(limitPrice));
		}

		/* Shows Order Duration  Menu */
		printMenu(durationTypeMenu);
		choice = isValidMenuItem("Please select valid index for Duration type", durationTypeMenu);
		client.fillDurationMenu(choice,inputs);
		client.previewOrder(accountIdKey, inputs);
	}

	private void getAccountList() {
		AccountListClient client = ctx.getBean(AccountListClient.class);

		try {
			String response = client.getAccountList();
			out.println(String.format("\n%20s %25s %25s %25s %25s\n", "Number", "AccountId", "AccountIdKey", "AccountDesc", "InstitutionType"));

			try {
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
				JSONObject acctLstResponse = (JSONObject) jsonObject.get("AccountListResponse");
				JSONObject accounts = (JSONObject) acctLstResponse.get("Accounts");
				JSONArray acctsArr = (JSONArray) accounts.get("Account");
				Iterator itr = acctsArr.iterator();
				long count = 1;

				while (itr.hasNext()) {
					JSONObject innerObj = (JSONObject) itr.next();
					String acctIdKey = (String) innerObj.get("accountIdKey");
					String acctStatus = (String) innerObj.get("accountStatus");
					if (acctStatus != null && !acctStatus.equals("CLOSED")) {
						acctListMap.put(String.valueOf(count), acctIdKey);
						out.println(String.format("%20s %25s %25s %25s %25s\n", count, innerObj.get("accountId"), acctIdKey, innerObj.get("accountDesc"), innerObj.get("institutionType")));
						count++;
					}
				}
			} catch (Exception e) {
				log.error(" Exception on get accountList : " + e.getMessage());
				e.printStackTrace();
			}
		} catch (ApiException e) {
			handleApiException(e);
		} catch (Exception e) {
			log.error(" getAccountList : GenericException ", e);
		}
	}

	private void getBalance(String acctIndex) {
		BalanceClient client = ctx.getBean(BalanceClient.class);
		String response = "";
		String accountIdKey;
		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		} catch(ApiException e) {
			handleApiException(e);
			return;
		}

		try {
			log.debug(" Response String : " + response);
			response = client.getBalance(accountIdKey);
			log.debug(" Response String : " + response);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
			log.debug(" JSONObject : " + jsonObject);

			JSONObject balanceResponse = (JSONObject) jsonObject.get("BalanceResponse");
			String accountId = (String) balanceResponse.get("accountId");
			out.println(String.format("%s\t\t\tBalances for %s %s%s", lineSeparator, accountId, lineSeparator, lineSeparator));

			JSONObject computedRec = (JSONObject) balanceResponse.get("Computed");
			JSONObject realTimeVal = (JSONObject) computedRec.get("RealTimeValues");

			if (computedRec.get("accountBalance") != null) {
				if (Double.class.isAssignableFrom(computedRec.get("accountBalance").getClass())) {
					Double accountBalance = (Double)computedRec.get("accountBalance");
					out.println("\t\tCash purchasing power:   $" + accountBalance);
				} else if( Long.class.isAssignableFrom(computedRec.get("accountBalance").getClass())){
					Long accountBalance = (Long)computedRec.get("accountBalance");
					out.println("\t\tCash purchasing power:   $" + accountBalance);
				}
			}

			if (realTimeVal.get("totalAccountValue") != null) {
				if (Double.class.isAssignableFrom(realTimeVal.get("totalAccountValue").getClass())) {
					Double totalAccountValue = (Double)realTimeVal.get("totalAccountValue");
					out.println("\t\tLive Account Value:      $" + totalAccountValue);
				} else if( Long.class.isAssignableFrom(realTimeVal.get("totalAccountValue").getClass())){
					Long totalAccountValue = (Long)realTimeVal.get("totalAccountValue");
					out.println("\t\tLive Account Value:      $" + totalAccountValue);
				}
			}

			if (computedRec.get("marginBuyingPower") != null) {
				if (Double.class.isAssignableFrom(computedRec.get("marginBuyingPower").getClass())) {
					Double marginBuyingPower = (Double)computedRec.get("marginBuyingPower");
					out.println("\t\tMargin Buying Power:     $" + marginBuyingPower);
				} else if( Long.class.isAssignableFrom(computedRec.get("marginBuyingPower").getClass())){
					Long totalAccountValue = (Long)computedRec.get("marginBuyingPower");
					out.println("\t\tMargin Buying Power:     $" + totalAccountValue);
				}
			}

			if (computedRec.get("cashBuyingPower") != null) {
				if (Double.class.isAssignableFrom(computedRec.get("cashBuyingPower").getClass())) {
					Double cashBuyingPower = (Double)computedRec.get("cashBuyingPower");
					out.println("\t\tCash Buying Power:       $" + cashBuyingPower);
				} else if( Long.class.isAssignableFrom(computedRec.get("cashBuyingPower").getClass())){
					Long cashBuyingPower = (Long)computedRec.get("cashBuyingPower");
					out.println("\t\tCash Buying Power:       $" + cashBuyingPower);
				}
			}

			System.out.println("\n");
		} catch(ApiException e) {
			handleApiException(e);
		} catch (Exception e) {
			log.error(" getBalance : GenericException " ,e);
		}
	}

	private void getPortfolio(String acctIndex) {
		PortfolioClient client = ctx.getBean(PortfolioClient.class);
		String accountIdKey;

		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		} catch(ApiException e) {
			return;
		}

		try {
			String response = client.getPortfolio(accountIdKey);
			log.debug(" Response String : " + response);
			JSONParser jsonParser = new JSONParser();

			JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
			out.println("*************************************");
			JSONObject portfolioResponse = (JSONObject) jsonObject.get("PortfolioResponse");
			out.println("*************************************");
			JSONArray accountPortfolioArr = (JSONArray) portfolioResponse.get("AccountPortfolio");
			Object[] responseData = new Object[8];
			Iterator acctItr = accountPortfolioArr.iterator();

			StringBuilder sbuf = new StringBuilder();
			Formatter fmt = new Formatter(sbuf);
			StringBuilder acctIdBuf = new StringBuilder();

			while (acctItr.hasNext()) {
				JSONObject acctObj = (JSONObject) acctItr.next();
				String accountId = (String) acctObj.get("accountId");
				acctIdBuf.append(lineSeparator).append("\t\t Portfolios for ").append(accountId).append(lineSeparator).append(
						lineSeparator);
				JSONArray positionArr = (JSONArray) acctObj.get("Position");
				Iterator itr = positionArr.iterator();

				while (itr.hasNext()) {
					StringBuilder formatString = new StringBuilder("");
					JSONObject innerObj = (JSONObject) itr.next();

					JSONObject prdObj = (JSONObject) innerObj.get("Product");
					responseData[0] = prdObj.get("symbol");
					formatString.append("%25s");

					responseData[1] = innerObj.get("quantity");
					formatString.append(" %25s");

					responseData[2] = prdObj.get("securityType");
					formatString.append(" %25s");

					JSONObject quickObj = (JSONObject) innerObj.get("Complete");

					if(Double.class.isAssignableFrom(quickObj.get("lastTrade").getClass())) {
						responseData[3] =  quickObj.get("lastTrade");
						formatString.append(" %25f");
					} else {
						responseData[3] =  quickObj.get("lastTrade");
						formatString.append(" %25d");
					}

					if (Double.class.isAssignableFrom(innerObj.get("pricePaid").getClass())) {
						responseData[4] = innerObj.get("pricePaid");
						formatString.append(" %25f");
					} else {
						responseData[4] = innerObj.get("pricePaid");
						formatString.append(" %25d");
					}
					if (Double.class.isAssignableFrom(innerObj.get("totalGain").getClass())) {
						responseData[5] = innerObj.get("totalGain");
						formatString.append(" %25f");
					} else {
						responseData[5] = innerObj.get("totalGain");
						formatString.append(" %25d");
					}
					if (Double.class.isAssignableFrom(innerObj.get("marketValue").getClass())) {
						responseData[6] =  innerObj.get("marketValue");
						formatString.append(" %25f").append(lineSeparator);
					} else {
						responseData[6] =  innerObj.get("marketValue");
						formatString.append(" %25d").append(lineSeparator);;
					}
					fmt.format(formatString.toString(), responseData[0], responseData[1],responseData[2],responseData[3],responseData[4],responseData[5],responseData[6]);
				}
			}
			out.println(acctIdBuf.toString());
			String titleFormat = "%25s %25s %25s %25s %25s %25s %25s" + System.lineSeparator();
			out.printf(titleFormat, "Symbol","Quantity", "Type", "LastPrice", "PricePaid", "TotalGain","Value");
			out.println(sbuf);
			out.println();
			out.println();
		} catch(ApiException e) {
			handleApiException(e);
		} catch (Exception e) {
			log.error(" getPortfolio ", e);
			out.println();
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println();
			out.println();
		}
	}

	private void getQuotes(String symbol) {
		DecimalFormat format = new DecimalFormat("#.00");
		QuotesClient client = ctx.getBean(QuotesClient.class);

		try {
			String response = client.getQuotes(symbol);
			log.debug(" Response String : " + response);
			try {
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
				log.debug(" JSONObject : " + jsonObject);

				JSONObject quoteResponse = (JSONObject) jsonObject.get("QuoteResponse");
				JSONArray quoteData = (JSONArray) quoteResponse.get("QuoteData");

				if (quoteData != null) {

					for (Object quoteDatum : quoteData) {
						out.println();
						JSONObject innerObj = (JSONObject) quoteDatum;
						if (innerObj != null && innerObj.get("dateTime") != null) {
							String dateTime = (String) (innerObj.get("dateTime"));
							out.println("Date Time: " + dateTime);
						}

						JSONObject product = (JSONObject) innerObj.get("Product");
						if (product != null && product.get("symbol") != null) {
							String symbolValue = product.get("symbol").toString();
							out.println("Symbol: " + symbolValue);
						}

						if (product != null && product.get("securityType") != null) {
							String securityType = product.get("securityType").toString();
							out.println("Security Type: " + securityType);
						}

						JSONObject all = (JSONObject) innerObj.get("All");
						if (all != null && all.get("lastTrade") != null) {
							String lastTrade = all.get("lastTrade").toString();
							out.println("Last Price: " + lastTrade);
						}

						if (all != null && all.get("changeClose") != null && all.get("changeClosePercentage") != null) {
							String changeClose = format.format(all.get("changeClose"));
							String changeClosePercentage = (String) (all.get("changeClosePercentage")).toString();
							out.println("Today's Change: " + changeClose + " (" + changeClosePercentage + "%)");
						}

						if (all != null && all.get("open") != null) {
							String open = all.get("open").toString();
							out.println("Open: " + open);
						}

						if (all != null && all.get("previousClose") != null) {
							String previousClose = format.format(all.get("previousClose"));
							out.println("Previous Close: " + previousClose);
						}

						if (all != null && all.get("bid") != null && all.get("bidSize") != null) {
							String bid = format.format(all.get("bid"));
							String bidSize = all.get("bidSize").toString();
							out.println("Bid (Size): " + bid + "x" + bidSize);
						}

						if (all != null && all.get("ask") != null && all.get("askSize") != null) {
							String ask = format.format(all.get("ask"));
							String askSize = all.get("askSize").toString();
							out.println("Ask (Size): " + ask + "x" + askSize);
						}

						if (all != null && all.get("low") != null && all.get("high") != null) {
							String low = format.format(all.get("low"));
							String high = all.get("high").toString();
							out.println("Day's Range: " + low + "-" + high);
						}

						if (all != null && all.get("totalVolume") != null) {
							String totalVolume = all.get("totalVolume").toString();
							out.println("Volume: " + totalVolume);
						}

						JSONObject mutualFund = (JSONObject) innerObj.get("MutualFund");
						if (mutualFund != null && mutualFund.get("netAssetValue") != null) {
							String netAssetValue = (String) (mutualFund.get("netAssetValue")).toString();
							out.println("Net Asset Value: " + netAssetValue);
						}

						if (mutualFund != null && mutualFund.get("changeClose") != null
								&& mutualFund.get("changeClosePercentage") != null) {
							String changeClose = format.format(mutualFund.get("changeClose"));
							String changeClosePercentage = (String) (mutualFund.get("changeClosePercentage")).toString();
							out.println("Today's Change: " + changeClose + " (" + changeClosePercentage + "%)");
						}

						if (mutualFund != null && mutualFund.get("publicOfferPrice") != null) {
							String publicOfferPrice = (String) (mutualFund.get("publicOfferPrice")).toString();
							out.println("Public Offer Price: " + publicOfferPrice);
						}

						if (mutualFund != null && mutualFund.get("previousClose") != null) {
							String previousClose = mutualFund.get("previousClose").toString();
							out.println("Previous Close: " + previousClose);
						}
					}
					out.println();
				} else {
					log.error(" Error : Invalid stock symbol.");
					out.println("Error : Invalid Stock Symbol.\n");
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		} catch(ApiException e) {
			handleApiException(e);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JSONObject getOptionsChain(List<Pair> queryParams) {
		OptionsChainClient client = ctx.getBean(OptionsChainClient.class);

		try {
			String response = client.getOptionsChain(queryParams);
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(response);
			return (JSONObject) jsonObject.get("OptionChainResponse");
		} catch (ApiException e) {
			System.err.println(e);
			handleApiException(e);
		} catch (ParseException e) {
			System.err.println(e);
			e.printStackTrace();
		}
		return new JSONObject();
	}

	private void findArbitrageOpportunities() {
		Calendar cal = Calendar.getInstance();
		String expiryMonth = String.valueOf(cal.get(Calendar.MONTH) + 1);
		String optionCategory = "ALL";
		String includeWeekly = "true";
		List<Pair> queryParams = new ArrayList<>();
		// definitely required
		queryParams.add(Pair.of("includeWeekly", "true"));
		queryParams.add(Pair.of("optionCategory", "ALL"));

		// include?
		queryParams.add(Pair.of("priceType", "ALL"));

		//queryParams.add(Pair.of("expiryDay", "13"));
		queryParams.add(Pair.of("expiryYear", "2020"));
		queryParams.add(Pair.of("expiryMonth", "02"));
		queryParams.add(Pair.of("noOfStrikes", "8"));

    String[] symbols = AppConfig.watchlist;
    //symbols = new String[]{"SHOP"};

		for (String symbol : symbols) {
			queryParams.add(Pair.of("symbol", symbol));
			double underlyingPrice = getCurrentMidPrice(ctx, symbol);
			String noOfStrikes = String.valueOf(Math.round(underlyingPrice / 10));
			//queryParams.add(Pair.of("noOfStrikes", noOfStrikes));
			JSONObject optionsChain = getOptionsChain(queryParams);
			processOptionsChain(underlyingPrice, optionsChain);
			queryParams.remove(Pair.of("symbol", symbol));
		}
	}

	private void processOptionsChain(double underlyingPrice, JSONObject optionsChain) {
		JSONArray optionPair = (JSONArray) optionsChain.get("OptionPair");
		// option chain might be not available for that expiry for that symbol
		if (optionPair == null) {
			out.println("No option pair received.");
			return;
		}

		for (Object o : optionPair) {
			JSONObject callPutPair = (JSONObject) o;
			JSONObject call = (JSONObject) callPutPair.get("Call");
			JSONObject put = (JSONObject) callPutPair.get("Put");
			processOnePair(underlyingPrice, call, put);
		}
	}

	private void processOnePair(double underlyingPrice, JSONObject call, JSONObject put) {
		double strikePrice = (Double) call.get("strikePrice");
		boolean shortArbitrage = false;
		boolean upcomingDividend = false;
		double arbitrage;
		double dividendAdjustedArbitrage;

		Calendar expiryDate = getExpiryFromJson(call, "quoteDetail");
		int daysToExpiry = getDaysToExpiry(expiryDate);

		double extrinsicCall = getExtrinsicValue(underlyingPrice, call, OptionsType.CALL);
		double extrinsicPut = getExtrinsicValue(underlyingPrice, put, OptionsType.PUT);
		// liquidity check
		if (extrinsicCall < 0 || extrinsicPut < 0) {
			return;
		}

		arbitrage = extrinsicCall - extrinsicPut;
		arbitrage = Math.round(arbitrage * 100) / 100.0;

		// todo : either check if ex dividend date is b/w now and expiry, or check ex dividend date + 3 months are b/w now and expiry
		if (arbitrage < 0) {
			// shortArbitrage = !shortArbitrage;
			// arbitrage = -arbitrage;
			dividendAdjustedArbitrage = findShortArbitrage(ctx, call, expiryDate, arbitrage);
			if (dividendAdjustedArbitrage > 0) {
				// if it became +ve, it must be because of dividend
				upcomingDividend = true;
			}
		} else {
			dividendAdjustedArbitrage = arbitrage;
		}

		if (dividendAdjustedArbitrage < 0) {
			// TODO : verify if security is hard-to-borrow
			// not available in quote response or optionchains response
			// maybe create a list of hard-to-borrow securities and print that
			shortArbitrage = true;
		}
		dividendAdjustedArbitrage = Math.abs(dividendAdjustedArbitrage);
		// TODO : how much to subtract so it get filled?
		//  again difficult, may depend upon moneyness!
		//  is the option penny incremental?
		dividendAdjustedArbitrage -= 3;

		// exclude '0 arbitrage minus commission' cases.
		if (dividendAdjustedArbitrage <= AppConfig.arbitrageStrengthDollars) {
			return;
		}

		double annualArbitragePercentage = calculateAnnualArbitrage(underlyingPrice, daysToExpiry, dividendAdjustedArbitrage);

		// this method return time in GMT
		String expiry = calendarToDate(expiryDate);

		// SHORT ARBITRAGE means dividend information might be missing!
		if (Math.abs(annualArbitragePercentage) >= AppConfig.arbitrageStrength) {
			out.println(call.get("optionRootSymbol") + " :: " + expiry + " :: " + strikePrice + " :: " +
					annualArbitragePercentage + "%" + " (" + dividendAdjustedArbitrage + ") " +
					(shortArbitrage ? ANSI_GREEN + "[SHORT ARBITRAGE] " + ANSI_RESET : " ") +
					(upcomingDividend ? ANSI_GREEN + "[UPCOMING DIVIDEND]" + ANSI_RESET : ""));
		}
	}

	private void managePortfolio() {
		Map<String, List<JSONObject>> positionGroups = getPositions(ctx);
		Set<String> managedPositions = new HashSet<>();

		for (Map.Entry<String, List<JSONObject>> entry : positionGroups.entrySet()) {
//			if (!entry.getKey().equalsIgnoreCase("WBA")) {
//				continue;
//			}
			OptionsStrategy optionsStrategy = identifyPositionType(entry.getValue());
			switch (optionsStrategy) {
				case SHORT_PUT:
				case SHORT_CALL:
				case SHORT_STRANGLE:
					manageDelta(entry.getValue(), managedPositions, OptionsStrategy.SHORT_STRANGLE);
          manageShortStrangle(entry.getValue(), managedPositions);
					break;
				case COVERED_CALL:
					manageDelta(entry.getValue(), managedPositions, OptionsStrategy.COVERED_CALL, 0.30, 0.50, .90);
					break;
				case LONG_ARBITRAGE:
					manageLongArbitrage(entry.getKey(), entry.getValue(), managedPositions);
					break;
				case SHORT_STRADDLE:
				// TODO : add more management strategies
					break;
				case OTHERS:
					manageDelta(entry.getValue(), managedPositions, OptionsStrategy.OTHERS);
				default:
					break;
			}
		}
	}

	private void manageDelta(List<JSONObject> positions, Set<String> managedPositions, OptionsStrategy strategy) {
		manageDelta(positions, managedPositions, strategy, AppConfig.level1Delta, AppConfig.level2Delta, AppConfig.level3Delta);
	}

	private void manageDelta(List<JSONObject> positions, Set<String> managedPositions, OptionsStrategy strategy,
			double level1Delta, double level2Delta, double level3Delta) {
		double callDelta = 0;
		double putDelta = 0;
		double equityDelta = 0;
		double totalDelta = 0;
		double netDeltaPerContract = 0;
		double maxPutDelta = Double.MIN_VALUE;
		double maxCallDelta = Double.MAX_VALUE;
		long numOfCalls = 0;	//spreads should results in 0
		long numOfPuts = 0;
		int numOfITMcalls = 0;
		int numOfITMPuts = 0;
		int highDeltaCalls = 0;
		int highDeltaPuts = 0;
		long numOfShares = 0;
		boolean adjustmentNeeded = false;
		String underlyingSymbol = (String) ((JSONObject) positions.get(0).get("Product")).get("symbol");

		if (managedPositions.contains(underlyingSymbol)) {
			return;
		}

		for (JSONObject position : positions) {
			JSONObject positionProductData = (JSONObject) position.get("Product");
			JSONObject positionCompleteData = (JSONObject) position.get("Complete");
			SecurityType securityType = SecurityType.valueOf((String) positionProductData.get("securityType"));
			long quantity = Math.abs((long) position.get("quantity"));
			double delta = 0;

			if (securityType.equals(SecurityType.EQ)) {
				equityDelta = quantity/100;
				numOfShares = quantity;
			} else {
				delta = (double) positionCompleteData.get("delta");
				OptionsType optionsType = OptionsType.valueOf((String) positionProductData.get("callPut"));
				PositionType positionType = PositionType.valueOf((String) position.get("positionType"));
				boolean itm = isITM(positionCompleteData);

				if (OptionsType.CALL == optionsType) {
					if (itm) {
						numOfITMcalls++;
					}
					numOfCalls += quantity;
					delta = positionType.equals(PositionType.LONG) ? delta : -1 * delta;
					callDelta += delta * quantity;
					if (delta < -level1Delta) {
						highDeltaCalls++;
					}
					maxCallDelta = Math.min(maxCallDelta, delta);
				} else {
					if (itm) {
						numOfITMPuts++;
					}
					numOfPuts += quantity;
					delta = positionType.equals(PositionType.LONG) ? delta : -1 * delta;
					putDelta += delta * quantity;
					if (delta > level1Delta) {
						highDeltaPuts++;
					}
					maxPutDelta = Math.max(maxPutDelta, delta);
				}
			}
		}

		totalDelta = putDelta + callDelta + equityDelta;
		netDeltaPerContract = totalDelta / (Math.min(numOfCalls, numOfPuts));

		switch (strategy) {
			case COVERED_CALL:
				if (Math.abs(callDelta) < level1Delta * numOfShares / 100) {
					adjustmentNeeded = true;
				}
				break;
			case CREDIT_SPREAD:
				if (maxCallDelta > 2) {
					adjustmentNeeded = true;
				}
			default:
				if (numOfITMcalls > highDeltaPuts ||
						numOfITMPuts > highDeltaCalls ||
						netDeltaPerContract < -level2Delta ||
						netDeltaPerContract > level2Delta ||
						maxCallDelta < -level3Delta ||
						maxPutDelta > level3Delta) {
					adjustmentNeeded = true;
				}
		}



		if (adjustmentNeeded) {
			out.println(underlyingSymbol + " needs delta adjustment.");
		}
	}

	private void manageShortStrangle(List<JSONObject> positions, Set<String> managedPositions) {
		// keep in mind that eligibility for managing is a different thing and being able to roll is different
		// the later part involves data about IV earnings, to decide roll/close/strike/expiry - out of scope

		String underlyingSymbol = (String) ((JSONObject) positions.get(0).get("Product")).get("symbol");

		if (managedPositions.contains(underlyingSymbol)) {
			return;
		}

		for (JSONObject position : positions) {
      percentageGainManagement(position, managedPositions);
      // todo if 14 days are left, time to manage.
      //  if cant be rolled => if can be rolled roll, otherwise close if both legs are in profit
      timeManagement(position, managedPositions);
      // TODO manage one itm leg
    }
  }

//	private void manageShortStrangle(Pair<JSONObject, JSONObject> positions) {
//		//boolean manage = profitManagement(positions.getLeft());
//		List<JSONObject> pos = new ArrayList<>();
//		pos.add(positions.getLeft());
//		pos.add(positions.getRight());
//		JSONObject pos1 = positions.getLeft();
//		JSONObject pos2 = positions.getRight();
//		double profit1 = profit(pos1);
//		double profit1Percentage = profitPercentage(pos1);
//		double profit2 = profit(pos2);
//		double profit2Percentage = profitPercentage(pos2);
//		double totalProfit = profit1 + profit2;
//		double totalCost = (double) pos1.get("totalCost") + (double) pos2.get("totalCost");
//		double totalProfitPercentage = totalProfit * 100 / totalCost;
//		boolean manage1 = false;
//		boolean manage2 = false;
//
//		if ((profitManagement(pos1) || (gammaManagement(pos1) && profit1 > 0))
//				&& totalProfitPercentage >= 50) {
//			// roll or close based on iv earnings
//		}
//		if ((profitManagement(pos2) || (gammaManagement(pos2) && profit2 > 0))
//				&& totalProfitPercentage >= 50) {
//			// roll or close based on iv earnings
//		}
//
//		if ((profitManagement(pos1) || profitManagement(pos2)) && totalProfitPercentage >= 50) {
//			// roll or close based on iv earnings
//		}
//		if (gammaManagement(pos1) && profit1 > 0) {
//			// roll or close based on iv earnings
//		}
//		if (gammaManagement(pos2) && profit2 > 0) {
//			// roll or close based on iv earnings
//		}
//
//		// TODO : cases to cover,
//		//  0) no roll despite 66% profit if noManagementPeriod has started, increase this period maybe? think. consider IV/rank into account?
//		//  0.4) manage the complete strangle if one leg is 66% profitable other leg is in loss, but overall it is 50% profit (ANY DTE)
//		//  0.5) manage the complete strangle if one leg is 66% profitable other leg is in loss, but overall it is profit and DTE <= criticalDTE
//		//  1) cant close one leg if the other leg is in loss and this leg cannot be rolled (e.g. due to earnings)
//		//  2) when stable, turn the loop on
//		//  3) when more stable, turn the sms on
//		// moneynessManagement(positions);
//	}

	private void manageLongArbitrage(String symbol, List<JSONObject> positions, Set<String> managedPositions) {
		double shortCallPrice = 0;
		double longPutPrice = 0;
		double strikePrice = 0;
		double underlyingPrice = getCurrentMidPrice(ctx, symbol);

		if (managedPositions.contains(symbol)) {
			return;
		}

		for (JSONObject position : positions) {
			PositionType positionType = PositionType.valueOf((String) position.get("positionType"));

			if (((JSONObject) position.get("Product")).get("securityType").equals(SecurityType.EQ.name())) {
				continue;
			}
			if (positionType == PositionType.LONG) {
				String putSymbol = getSymbolFromQuoteDetails((String) position.get("quoteDetails"));
				longPutPrice = getCurrentMidPrice(ctx, putSymbol);
				strikePrice = (Long) ((JSONObject) position.get("Product")).get("strikePrice");
			} else if (positionType == PositionType.SHORT) {
				String callSymbol = getSymbolFromQuoteDetails((String) position.get("quoteDetails"));
				shortCallPrice = getCurrentMidPrice(ctx, callSymbol);
			}
		}

		// TODO : do we want to close out this position in favour of other better opportunity?
		// take into account the free capital
		if (getExtrinsicValue(strikePrice, underlyingPrice, longPutPrice, OptionsType.PUT)
				>= getExtrinsicValue(strikePrice, underlyingPrice, shortCallPrice, OptionsType.CALL)) {
			out.println("adjust arbitrage on " + symbol);
		}
	}

  private void percentageGainManagement(JSONObject position, Set<String> managedPositions) {
		double totalPercentageGain = 0;
		double ivRank = 0.0;
    if (Double.class.isAssignableFrom(position.get("totalGainPct").getClass())) {
      totalPercentageGain = (double) position.get("totalGainPct");
    } else if (Long.class.isAssignableFrom(position.get("totalGainPct").getClass())) {
			totalPercentageGain = (long) position.get("totalGainPct");
		} else {
			out.println(ANSI_RED + "[ERROR] : " + ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
		}

		if (totalPercentageGain > AppConfig.targetGainPercentage) {
			// TODO : how to find iv rank???
			// && ivRank > AppConfig.highIVRank) {
			out.println("Target achieved for : " + position.get("symbolDescription"));
			managedPositions.add((String) position.get("symbolDescription"));
		}
  }

	private  double profit(JSONObject position) {
		if (Double.class.isAssignableFrom(position.get("totalGain").getClass())) {
			return (double) position.get("totalGain");
		} else {
			out.println(ANSI_RED + "[ERROR] : " + ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
			return 0.0;
		}
	}

	private void timeManagement(JSONObject position, Set<String> managedPositions) {
    Calendar expiryDate = getExpiryFromJson(position, "quoteDetails");
    int daysToExpiry = getDaysToExpiry(expiryDate);
    if (daysToExpiry < AppConfig.criticalDTE && profit(position) > 0) {
			// time management does not depend upon IV
    	//long earningsDate = (long) position.get("earnings");
			//earningsDate -= 20000;
			//earningsDate *= 1000;

			//if (System.currentTimeMillis() < earningsDate // earnings coming
			//		&& earningsDate - expiryDate.getTimeInMillis() < 7 * 24 * 60 * 60 *1000L	// earnings scheduled before next expiry
			//		&& other leg is in loss	) {
			// todo : if (earnings are near or iv is low) and total position is in net loss, do not print
				out.println("Position " + position.get("symbolDescription") + " too close to expiry.");
				managedPositions.add((String) position.get("symbolDescription"));
			//}
		}
  }

  private void moneynessManagement(List<JSONObject> positions) {
		// TODO manage one itm leg
		JSONObject pos1 = positions.get(0);
		JSONObject pos2 = positions.get(1);
		String symbol1 = getSymbolFromQuoteDetails((String) pos1.get("quoteDetails"));
		String symbol2 = getSymbolFromQuoteDetails((String) pos2.get("quoteDetails"));
		JSONObject innerObj1;
		JSONObject innerObj2;
		QuotesClient client = ctx.getBean(QuotesClient.class);

		try {
			String response1 = client.getQuotes(symbol1);
			JSONParser jsonParser1 = new JSONParser();
			JSONObject jsonObject1 = (JSONObject) jsonParser1.parse(response1);
			JSONObject quoteResponse1 = (JSONObject) jsonObject1.get("QuoteResponse");
			JSONArray quoteData1 = (JSONArray) quoteResponse1.get("QuoteData");
			String response2 = client.getQuotes(symbol2);
			JSONParser jsonParser2 = new JSONParser();
			JSONObject jsonObject2 = (JSONObject) jsonParser2.parse(response2);
			JSONObject quoteResponse2 = (JSONObject) jsonObject2.get("QuoteResponse");
			JSONArray quoteData2 = (JSONArray) quoteResponse2.get("QuoteData");

			if (quoteData1 != null) {
				// not sure why it returns an array of quoteDatum
				innerObj1 = (JSONObject) ((JSONObject) quoteData1.get(0)).get("All");
			} else {
				out.println(ETClientApp.ANSI_RED + "[ERROR] : " + ETClientApp.ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
			}
			if (quoteData2 != null) {
				// not sure why it returns an array of quoteDatum
				innerObj2 = (JSONObject) ((JSONObject) quoteData2.get(0)).get("All");
			} else {
				out.println(ETClientApp.ANSI_RED + "[ERROR] : " + ETClientApp.ANSI_RESET + Thread.currentThread().getStackTrace()[2].getLineNumber());
			}
		} catch (ApiException | ParseException e) {
				log.error(e.toString());
		}




		String symbol = (String) ((JSONObject) pos1.get("Product")).get("symbol");
//		if ((pos1.get("itm").toString().equalsIgnoreCase("itm")
//				&& (double) pos2.get("delta") <= .20) ||
//				(pos2.get("itm").toString().equalsIgnoreCase("itm")
//						&& (double) pos1.get("delta") <= .20)) {
//			out.println("One lef of strangle on " + symbol + " is ITM. Roll the other leg.");
//		}
	}

	private void getOrders(final String acctIndex) {
		OrderClient client = ctx.getBean(OrderClient.class);
		String accountIdKey;

		try {
			accountIdKey = getAccountIdKeyForIndex(acctIndex);
		} catch(ApiException e) {
			return;
		}

		try {
			String response = client.getOrders(accountIdKey);
			log.debug(" Get Order response : " + response);
			if (response != null) {
				StringBuilder acctIdBuf = new StringBuilder();
				acctIdBuf.append(lineSeparator).append("\t\t Orders for selected account index : ").append(acctIndex).append(
						lineSeparator).append(lineSeparator);
				out.println(acctIdBuf.toString());
				client.parseResponse(response);
			} else {
				out.println("No records...");
			}

		} catch(ApiException e) {
			handleApiException(e);
		} catch (Exception e) {
			log.error(" getBalance : GenericException " ,e);
			out.println();
			out.println(String.format("Message: %23s", e.getMessage()));
			out.println();
			out.println();
		}
	}

	private String getAccountIdKeyForIndex(String acctIndex) throws ApiException {
		String accountIdKey = "";

		try {
			accountIdKey = acctListMap.get(acctIndex);
			if (accountIdKey == null) {
				out.println(" Error : !!! Invalid account index selected !!! ");
			}
		} catch (Exception e) {
			log.error(" getAccountIdKeyForIndex ", e);
		}

		if (accountIdKey == null) {
			throw new ApiException(0, "0","Invalid selection for accountId index");
		}
		return accountIdKey;
	}
}
