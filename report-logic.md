# Report generation logic

This document describes the rules used for generating reports for (Norwegian) tax authorities.

## Requirements

The tax authorities requests the following information:

1. Profit/loss (PNL) report of each transaction
2. The balance of assets (amount of each currency) in the wallet at the end of each year (December
   31st 23:59:59).

The user specifies the home currency (HC), for example: NOK or EUR. Profit and loss (PNL) for each
transaction is calculated in USDT, but at the end of the year it is translated into the home
currency.

## Required transaction information

To calculate the necessary reports, the following transactions must be collected from the Binance
exchange:

1. Executed buy-orders
2. Executed sell-orders
3. Deposits
4. Withdrawals
5. Currency purchase with a credit card
6. Interest on savings
7. _Dust collection_
8. Asset dividends
9. Auto-invest

## Stored information

To calculate the necessary reports, the following information is necessary:

* User's home currency (HC)
* List of all transactions
* Wallet snapshot after each transaction

## Data structures

The necessary information can be stored in the following data structures:

- `Transaction`: Stores information about a transaction: buy order, sell order, etc. A generic base
  class.
- `RawAccountChange`: an atomic change of one account. One transaction can contain several
  RawAccountChange entries. For example, a sell transaction may have one buy-change, one sell-change
  and one fee-change - three changes to the account which are part of the same transaction.
- `Wallet`: stores balances of different assets (currencies) - `AssetBalance` entry for each asset.
- `AssetBalance`: stores the amount and average obtain-price for a specific asset (currency).
- `WalletSnapshot`: stores a wallet situation at a specific time moment, after execution of a
  specific transaction. Stores also the running PNL - the accumulated profit&loss for all
  transactions up until the given time moment.
- `ExtraInfo`: a set of user-provided extra info units. Each unit of info is an `ExtraInfoEntry`
  object for a specific time moment.
- `ExtraInfoUnig` - a unit of extra information provided by the user. For example, price at which a
  deposited coin was obtained (somewhere outside the Binance exchange, for example, on Coinbase).
- `DepositTransaction`, `BuyTransaction`, etc.: child classes for `Transaction`, implement the
  transaction-specific logic - take the `WalletSnapshot` before the transaction and `ExtraInfo`, and
  create a `WalletSnapshot` after the transaction.

Note: to avoid rounding problems, all money-related amounts are represented as `Decimal` - a custom
class wrapping the `BigDecimal` Java class.

## Calculation algorithm

The following general rules apply:

* Raw account changes are collected from the CSV file (downloaded from Binance)
* The raw changes are grouped as transactions
* Some transactions may need user's manual input:
    * Fiat currency exchange - the used exchange rate
    * Deposit - the obtaining price of the deposited currency, in HC
    * Withdrawal - the realised sell-price of the currency, in HC
    * Exchange rate of USDT/HomeCurrency at the end of each year
* After each transaction, the following is re-calculated:
    * Amount and average obtaining price (in USDT) for each asset involved in the transaction
    * Profit/Loss in USDT for this specific transaction (if any)
    * New wallet snapshot - all the assets in the wallet
    * Current "running" Profit/loss in USDT
* At the end of each year the following amounts are calculated:
    * Current running PNL, in HC
    * List of all assets, the value of each asset in the wallet, in HC

In buy-transactions, one currency is purchased (called _base currency_) while another currency is
sold (called _quote currency_). In sell transactions the base currency is sold and quote currency is
purchased.

### Fee calculations

Fee calculations are as follows:

* `feeInUsdt = transaction.fee * (wallet[transaction.feeCurrency].avgObtainPriceUsdt)`
* `wallet[transaction.feeCurrency].amount -= transaction.fee`

### Rules for calculation for each transaction type

Calculations are performed for each transaction, based on the transaction type. Each Transaction
type is represented by a separate class in the code. The class implements
method `WalletSnapshot process(WalletSnapshot oldSnapshot, ExtraInfo extraInfo)`. The following
sections describe the logic of how each transaction type must update the wallet.

#### Buy order

An executed buy order means that:

* A currency has been purchased (base currency)
* A currency has been sold (quote currency)
* A fee has been paid

Wallet changes:

```
* wallet[quote].amount -= quote.amount
* usdtSpentInTransaction = quote.amount * wallet[quote].avgObtainPriceUsdt + feeInUsdt
* totalUsdtSpent = usdtSpentInTransaction + (wallet[base.asset].avgObtainPriceUsdt *
  wallet[base.asset].amount)
* wallet[base.asset].amount += baseAmount
* wallet[base.asset].avgObtainPriceUsdt = 
  totalUsdtSpent / wallet[base.asset].amount
* pnlInUsdt is unchanged
```

#### Auto-invest

An auto-invest transaction is handled the same way as a buy-transaction - a coin is purchased at
scheduled moments.

#### Sell order

An executed sell order means that:

* A currency has been sold (base currency)
* A currency has been purchased (quote currency)
* A fee has been paid

If the quote currency is not USDT, treat it as a buy transaction instead, where quote currency was
bought and base currency was sold!

Wallet changes:

```
* wallet[USDT].amount += quote.amount
* wallet[base.asset].amount -= base.amount
* wallet[base.asset].avgObtainPriceUsdt is unchanged
* receivedUsdt = quote.amount - feeInUsdt
* investedUsdt = base.amount * wallet[base.asset].avgObtainPriceUsdt
* pnlInUsdt += receivedUsdtValue - investedUsdtValue
```

#### Deposit

The user has deposited a cryptocurrency in the exchange.

It is not known where and how the currency was obtained (mining, or purchase on another platform).

User must enter manually enter the transaction.averagePrice - the obtain-price for the
cryptocurrency, in HC.

Wallet changes:

```
* deposit.avgObtainPriceUsdt = [manualUserInput]
* usdtSpentInTransaction = deposit.amount * deposit.avgObtainPriceUsdt
* usdtSpentPreviously = wallet[deposit.asset].avgObtainPriceUsdt * wallet[deposit.asset].amount
* totalUsdtSpent = usdtSpentInTransaction + usdtSpentPreviously
* wallet[deposit.asset].amount += base.amount
* wallet[deposit.asset].avgObtainPriceUsdt = totalUsdtSpent / wallet[deposit.asset].amount
* pnlInUsdt is unchanged
```

#### Withdrawal

The user has withdrawn a cryptocurrency from the exchange.

It is now know what happens to the currency after the withdrawal, hence

* We assume that it was converted to the users home-currency (HC) immediately after withdrawal
* The user must manually specify the realised price (in HC) at which the withdrawn currency was
  converted

Wallet changes:

```
* quoteCurrency = homeCurrency
* transaction.averagePrice = [manualUserInput]
* wallet[baseCurrency].amount -= baseAmount
* hcObtainedInTransaction = baseAmount * transaction.averagePrice
* wallet[baseCurrency].averageObtainPriceHC is unchanged
* priceDifferenceInHC = transaction.averagePrice - wallet[baseCurrency].averageObtainPrice
* transaction.profitLossInHC = transaction.amount * priceDifferenceInHC
```

#### Credit-card purchase

The user has purchased a currency, using credit card.

Wallet changes:

```
* quoteCurrency = homeCurrency
* wallet[baseCurrency].amount += baseAmount
* hcSpentInTransaction = baseAmount * transaction.averagePrice
* totalHcSpent = hcSpentInTransaction + (wallet[baseCurrency].averageObtainPriceHC *
  wallet[baseCurrency].amount)
* wallet[baseCurrency].averageObtainPriceHC = totalHcSpent / wallet[baseCurrency].amount
* transaction.profitLossInHC is unchanged
```

#### Savings interest

Interest on savings generates new currency where the "average obtaining price" for that new currency
is zero.

Wallet changes:

```
* totalHCSpent = (wallet[baseCurrency].averageObtainPriceHc * wallet[baseCurrency].amount)
* wallet[baseCurrency].amount += baseAmount
* wallet[baseCurrency].averageObtainPriceHC = totalHCSpent / wallet[baseCurrency].amount
* transaction.profitLossInHC is unchanged
```

#### Dust collection

Dust collection means that the user converted a small amount of a coin into BNB coin. It is
essentially a sell-order in the coin/BNB market.

#### Asset dividends

Asset dividend means that the user gets a "free token" because of her holding a specific asset in
the wallet for a specific amount of time. The algorithm for wallet updates is the same as for the
savings interest, because we get an extra amount of asset (coin) for free.

#### Other transactions

Binance transaction export includes the following additional transaction types:

* Commission Rebate
* Cashback Voucher
* Distribution
* Simple Earn Flexible Redemption
* Simple Earn Flexible Subscription
* Auto-invest transactions (new in 2022)

These transactions also must be handled.