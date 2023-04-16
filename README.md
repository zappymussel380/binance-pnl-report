# PNL report tool

A command-line tool that generates profit/loss (PNL) report
using [Transaction history exported from Binance](https://www.binance.com/en/my/wallet/history/) in
the format of a CSV file.

PNL for each transaction is calculated in USD, because that is the most widely used quote currency -
coins are bought and sold with the USDT (or BUSD) coin representing USD. The annual report is
translated to home currency (HC).

## Necessary data

You need to provide the following information to the tool:

1. Home currency (HC) - a currency in which all the profit and loss will be calculated.
2. CSV file downloaded
   from [Binance > Wallet > Transaction History](https://www.binance.com/en/my/wallet/history/) >
   Generate all statements. Note: for the report to be correct, you must download the whole history
   of transaction - from the first day you started trading at Binance, up until the moment you want
   the report for. For example, if you started in Binance in 2019, and you want a report for 2023,
   you need to download CSV files for 2019, 2020, 2021, 2022 and 2023, and combine them manually in
   one large CSV file, then give that large CSV file as input for this tool.
3. CSV file with extra information, as required by the tool. To do the calculations properly, the
   tool needs some extra information, for example, currency exchange rates at specific dates, the
   price of a deposited coin in HC on the date of deposit, and some other things. You can run the
   tool first without providing this information. The tool will tell you what information is
   necessary.

There is no automatic retrieval of exchange rates, therefore the user must supply the USD/HC
exchange rates at the end of each year manually.

## Running the tool

Run the tool from the command line (or from your IDE) and provide the following command-line
arguments:

1. Path to the Binance-exported CSV file
2. Home currency (for example, NOK)
3. Path to the CSV file with extra information

## Output files
The reports are written to the following files:
- Transaction log (what was bought or sold, at what price) is written to file `transactions.csv`
- Asset balances in the wallet after each transaction - file `balances.csv`
- Annual PNL report - file `profits.csv`

## Extra information CSV

The CSV file with extra information must contain the following columns:

1. Unix timestamp, including milliseconds. This timestamp must match the timestamp of the
   transaction to which you want to attach this extra information.
2. Human-readable timestamp. Not used by the tool, for human debugging only.
3. The type of the extra information,
   see [ExtraInfoType](src/main/java/no/strazdins/data/ExtraInfoType.java)
4. The asset in question. For example, "NOK" or "LTC".
5. The value of the transaction. The meaning of it depends on the extra info type. For example, a
   price at which the currency was purchased (in USDT).

Note: you can run the report generator tool, and it will tell you what kind of extra information it
needs. Copy that output (the right timestamps will be there), find out the necessary values
(using your bank transcripts, currency exchange rates at specific dates, etc.). You can convert the
integer values of unix timestamps to human-readable time using sites such
as [unixtimestamp. com](https://www.unixtimestamp.com/).

## Report generation logic

See [report-logic.md](report-logic.md).