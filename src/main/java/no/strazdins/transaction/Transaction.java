package no.strazdins.transaction;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.OperationMultiSet;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.Converter;

/**
 * Contains one financial asset transaction, consisting of several AccountChanges.
 * For example, purchasing a cryptocurrency may consist of three changes: buy + sell + fee.
 */
public class Transaction {
  Map<Operation, List<RawAccountChange>> atomicAccountChanges = new EnumMap<>(Operation.class);
  protected final long utcTime;

  // These values must be set inside the child classes
  protected String baseCurrency;
  protected Decimal baseCurrencyAmount = Decimal.ZERO;
  // Base currency obtaining price in Home Currency
  protected Decimal baseObtainPriceInHc = Decimal.ZERO;
  protected String quoteCurrency = "";
  protected Decimal fee = Decimal.ZERO;
  protected String feeCurrency = "";
  protected Decimal feeInHomeCurrency = Decimal.ZERO;

  protected Decimal pnl = Decimal.ZERO;
  private Decimal quoteAmount = Decimal.ZERO;


  /**
   * Create a new transaction.
   *
   * @param utcTime UTC timestamp of the transaction.
   */
  public Transaction(long utcTime) {
    this.utcTime = utcTime;
  }

  public Transaction(Transaction t) {
    this.atomicAccountChanges = t.atomicAccountChanges;
    this.utcTime = t.utcTime;
  }

  /**
   * Append a change to the transaction.
   *
   * @param change An atomic account change
   */
  public final void append(RawAccountChange change) {
    if (!atomicAccountChanges.containsKey(change.getOperation())) {
      atomicAccountChanges.put(change.getOperation(), new LinkedList<>());
    }
    List<RawAccountChange> changeList = atomicAccountChanges.get(change.getOperation());
    changeList.add(change);
  }

  @Override
  public String toString() {
    return "Transaction@" + Converter.utcTimeToString(utcTime);
  }

  /**
   * Look at the registered raw account changes, find out what kind of transaction this is:
   * deposit, withdrawal, buy, savings interest, dust transfer, etc.
   *
   * @return A transaction with specific type, with the same atomic operations
   */
  public final Transaction clarifyTransactionType() {
    if (consistsOf(Operation.DEPOSIT)) {
      return new DepositTransaction(this);
    } else if (consistsOf(Operation.BUY, Operation.FEE, Operation.TRANSACTION_RELATED)) {
      if (looksLikeReversedBuy()) {
        return new SellTransaction(this);
      } else {
        return new BuyTransaction(this);
      }
    }
    // TODO - implement other transaction types
    return null;
  }

  protected boolean looksLikeReversedBuy() {
    RawAccountChange bought = getFirstChangeOfType(Operation.BUY);
    if (isTypicalQuoteCurrency(bought.getAsset())) {
      // Sold in Coin/USDT or similar market
      return true;
    } else {
      RawAccountChange sold = getFirstChangeOfType(Operation.TRANSACTION_RELATED);
      if (isTypicalQuoteCurrency(sold.getAsset())) {
        // Bought in Coin/USDT or similar market
        return false;
      } else if (bought.getAsset().equals("BTC")) {
        // Bought in BTC/USDT or similar market
        return false;
      } else {
        throw new IllegalArgumentException("Can't understand the operation, bought: "
            + bought + ", sold: " + sold);
      }
    }
  }

  private boolean isTypicalQuoteCurrency(String asset) {
    return asset.equals("USDT") || asset.equals("BUSD") || asset.equals("TUSD");
  }

  private boolean consistsOf(Operation... operations) {
    return getOperationMultiSet().equals(new OperationMultiSet(operations));
  }

  private OperationMultiSet getOperationMultiSet() {
    OperationMultiSet operationMultiSet = new OperationMultiSet();
    for (Map.Entry<Operation, List<RawAccountChange>> entry : atomicAccountChanges.entrySet()) {
      operationMultiSet.add(entry.getKey(), entry.getValue().size());
    }
    return operationMultiSet;
  }

  /**
   * Get the first account change with the given type.
   *
   * @param operation Operation type
   * @return The first change or null if no change of this type is found.
   */
  protected final RawAccountChange getFirstChangeOfType(Operation operation) {
    List<RawAccountChange> changes = atomicAccountChanges.get(operation);
    return changes != null && !changes.isEmpty() ? changes.get(0) : null;
  }

  /**
   * Get necessary extra information needed to process this transaction. The information
   * must be provided by the user.
   *
   * @return Necessary extra information or null if no extra information is necessary.
   */
  public ExtraInfoEntry getNecessaryExtraInfo() {
    return null;
  }

  /**
   * Get timestamp of the transaction, as a UTC timestamp.
   *
   * @return UTC timestamp of the transaction, containing milliseconds.
   */
  public final long getUtcTime() {
    return utcTime;
  }

  /**
   * Consider the current wallet snapshot, and return a new wallet snapshot which is the result
   * of processing this transaction.
   * The logic must be implemented in the child classes.
   *
   * @param walletSnapshot The current wallet snapshot before the transaction
   * @param extraInfo      Extra info provided by the user, if any
   * @return The new wallet snapshot after processing this transaction
   */
  public WalletSnapshot process(WalletSnapshot walletSnapshot, ExtraInfoEntry extraInfo) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the type of the transaction.
   *
   * @return A human-readable type of the transaction.
   */
  public String getType() {
    return "Unknown";
  }

  /**
   * Get the base currency of the transaction.
   *
   * @return The base currency.
   */
  public final String getBaseCurrency() {
    return baseCurrency;
  }

  /**
   * Get the quote currency of the transaction.
   *
   * @return The quote currency
   */
  public final String getQuoteCurrency() {
    return quoteCurrency;
  }


  /**
   * Get the amount of base currency of the transaction.
   *
   * @return The amount of base currency
   */
  public final Decimal getBaseCurrencyAmount() {
    return baseCurrencyAmount;
  }

  /**
   * Get the fee paid in this transaction.
   *
   * @return The fee, in the fee-currency
   */
  public final Decimal getFee() {
    return fee;
  }

  /**
   * Get the nominal currency of the fee.
   *
   * @return The currency in which the fee was paid.
   */
  public final String getFeeCurrency() {
    return feeCurrency;
  }

  /**
   * Get the amount of fee, converted to the Home Currency.
   *
   * @return The fee converted to the Home Currency
   */
  public final Decimal getFeeInHomeCurrency() {
    return feeInHomeCurrency;
  }

  /**
   * Get obtain-price of the base currency, calculated in the Home Currency.
   *
   * @return The obtain-price of the main asset, in Home Currency
   */
  public final Decimal getObtainPrice() {
    return baseObtainPriceInHc;
  }

  /**
   * Get Profit & Loss (PNL) of this single transaction.
   *
   * @return The PNL, in Home currency
   */
  public final Decimal getPnl() {
    return pnl;
  }

  /**
   * Get the amount of quote currency change in this transaction.
   *
   * @return The amount of quote currency change in this transaction
   */
  public final Decimal getQuoteAmount() {
    return quoteAmount;
  }
}
