# PNL report tool

A command-line tool that generates profit/loss (PNL) report
using [Transaction history exported from Binance](https://www.binance.com/en/my/wallet/history/) in
the format of a CSV file.

The PNL report will be generated in home currency (HC).

## Necessary data

You need to provide the following information to the tool:

1. Home currency (HC) - a currency in which all the profit and loss will be calculated.
2. CSV file downloaded
   from [Binance > Wallet > Transaction History](https://www.binance.com/en/my/wallet/history/) >
   Generate all statements.
3. CSV file with extra information, as required by the tool. To do the calculations properly, the
   tool needs some extra information, for example, currency exchange rates at specific dates, the
   price of a deposited coin in HC on the date of deposit, and some other things. You can run the
   tool first without providing this information. The tool will tell you what information is
   necessary.

## Running the tool

Run the tool from the command line (or from your IDE) and provide the following command-line
arguments:

1. Path to the Binance-exported CSV file
2. Path to a CSV file where the report will be written
3. Home currency
4. Path to the CSV file with extra information

## Extra information CSV

The CSV file with extra information must contain the following columns:

1. Unix timestamp, including milliseconds. This timestamp must match the timestamp of the
   transaction to which you want to attach this extra information.
2. The type of the extra information,
   see [ExtraInfoType](src/main/java/org/compilers/data/ExtraInfoType.java)
3. The value of the transaction. The meaning of it depends on the extra info type. For example, 
   a price at which the currency was purchased (measured in the Home currency).

## Report generation logic

See [report-logic.md](report-logic.md).