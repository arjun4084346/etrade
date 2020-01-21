# E*TRADE API Java Example Appication 

The example application provides an environment for testing and validating the sandbox and live API.

## Table of Contents

* [Requirements](#requirements)
* [Setup](#setup)
* [Running Code](#running-code)

## Requirements
 - Java 1.8 or later.
 - Gradle
 - An [E*TRADE](https://us.etrade.com) account.
 - E*TRADE consumer key and consumer secret.
	
 ## Setup
 - git clone the repository.
 - Update oauth keys in the oauth.properties file available with source.
 - Run `git update-index --assume-unchanged  src/main/resources/oauth.properties` to not expose your keys accidentally.

## Running Code 

 - Run `./gradlew run`
 - To run in cron mode, pass parameters, e.g. `./gradlew run --args='manage'` `./gradlew run --args='find'`

## Features
 - Sandbox
   * Account List
   * Balance
   * Portfolio
   * Order List
   * Order Preview
   * Quote
 - Live
   * Account List
   * Balance
   * Portfolio
   * Order List
   * Order Preview
   * Quote

## Documentation
 - [Developer Guide](https://developer.etrade.com/home)