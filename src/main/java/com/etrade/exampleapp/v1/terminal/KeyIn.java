package com.etrade.exampleapp.v1.terminal	;

import static com.etrade.exampleapp.v1.terminal.ETClientApp.out;
import java.util.Scanner;

public class KeyIn {
	 private static Scanner scanner = new Scanner( System.in );

	 static int getKeyInInteger() throws RuntimeException{
		 String input = scanner.nextLine();
		 int choice;

		 if (input.equalsIgnoreCase("x")) {
			 choice = 'x';
		 } else if (input.length() == 0) {
			 out.println("Invalid input, please enter valid number");
			 choice = getKeyInInteger();
		 } else {
			 try {
				 choice = Integer.parseInt(input);
			 } catch (Exception e) {
				 out.println("Invalid input, please enter valid number");
				 choice = getKeyInInteger();
			 }
		 }
		 return choice;
	 }
	 static double getKeyInDouble() throws RuntimeException{
		 String input = scanner.nextLine();
		 double value;

		 try {
			 value = Double.parseDouble(input);
		 } catch (Exception e) {
			 out.println("Invalid input, please enter valid number");
			 value = getKeyInDouble();
		 }
		 return value;
	 }

	 public static String getKeyInString() throws RuntimeException{
		 return scanner.nextLine();
	 }

	 public static void close() {
		 scanner.close();
	 }
}
