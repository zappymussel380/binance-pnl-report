package no.strazdins.transaction;

import static no.strazdins.data.Operation.EARN_SUBSCRIPTION;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.Operation;
import no.strazdins.data.OperationMultiSet;
import no.strazdins.data.RawAccountChange;
import no.strazdins.data.Wallet;
import no.strazdins.data.WalletDiff;
import no.strazdins.data.WalletSnapshot;
import no.strazdins.tool.TimeConverter;

/**
 * Contains one financial asset transaction, consisting of several AccountChanges.
 * For example, purchasing a cryptocurrency may consist of three changes: buy + sell + fee.
 */
public class Transaction {
  // All PNL is calculated in this currency
  public static final String QUOTE_CURR = "USDT";

  Map<Operation, List<RawAccountChange>> atomicAccountChanges = new EnumMap<>(Operation.class);
  protected final long utcTime;

  // These values must be set inside the child classes
  protected String baseCurrency;
  protected Decimal baseCurrencyAmount = Decimal.ZERO;
  // Base currency obtaining price in Home Currency
  protected Decimal baseObtainPriceInUsdt = Decimal.ZERO;
  protected Decimal avgPriceInUsdt = Decimal.ZERO;
  protected String quoteCurrency = "";
  protected Decimal fee = Decimal.ZERO;
  protected String feeCurrency = "";
  protected Decimal feeInUsdt = Decimal.ZERO;

  protected Decimal pnl = Decimal.ZERO;
  protected Decimal quoteAmount = Decimal.ZERO;


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
    return "Transaction[" + getType() + "]@" + TimeConverter.utcTimeToString(utcTime);
  }

  /**
   * Look at the registered raw account changes, find out what kind of transaction this is:
   * deposit, withdrawal, buy, savings interest, dust transfer, etc.
   *
   * @return A transaction with specific type, with the same atomic operations
   */
  public final Transaction clarifyTransactionType() {
    if (consistsOfMultipleBuySellOperations()) {
      mergeRawChangesByType();
    }

    Transaction t = tryToConvertToBuyOrSell();
    if (t == null) {
      t = tryToConvertToDepositOrWithdraw();
    }
    if (t == null) {
      t = tryToConvertToSavingsRelated();
    }
    if (t == null) {
      t = tryToConvertToAutoInvest();
    }
    return t;
  }

  private boolean consistsOfMultipleBuySellOperations() {
    return consistsOfMultiple(Operation.BUY, Operation.SELL, Operation.FEE)
        || consistsOfMultiple(Operation.BUY, Operation.SELL);
  }

  private Transaction tryToConvertToSavingsRelated() {
    Transaction t = null;
    if (consistsOf(EARN_SUBSCRIPTION, Operation.SAVINGS_DISTRIBUTION)
        || consistsOf(Operation.SAVINGS_DISTRIBUTION)
        || consistsOf(EARN_SUBSCRIPTION)
        || consistsOfMultiple(EARN_SUBSCRIPTION)) {
      t = new SavingsSubscriptionTransaction(this);
    } else if (consistsOf(Operation.EARN_REDEMPTION)
        || consistsOfMultiple(Operation.EARN_REDEMPTION)) {
      t = new SavingsRedemptionTransaction(this);
    } else if (consistsOf(Operation.EARN_INTEREST)) {
      t = new SavingsInterestTransaction(this);
    } else if (consistsOf(Operation.CASHBACK_VOUCHER)) {
      t = new CashbackTransaction(this);
    } else if (consistsOf(Operation.COMMISSION_REBATE)) {
      t = new CommissionTransaction(this);
    } else if (consistsOf(Operation.DISTRIBUTION)) {
      t = new DistributionTransaction(this);
    } else if (consistsOfMultiple(Operation.SMALL_ASSETS_EXCHANGE_BNB)) {
      t = new DustCollectionTransaction(this);
    }
    return t;
  }

  private Transaction tryToConvertToBuyOrSell() {
    RawAccountChange buy = getFirstBuyTypeChange();
    RawAccountChange sell = getFirstSellTypeChange();
    boolean hasFee = getFirstChangeOfType(Operation.FEE) != null;
    int expectedOpCount = hasFee ? 3 : 2;
    if (getTotalOperationCount() != expectedOpCount) {
      return null;
    }

    Transaction t = null;
    if (buy != null && sell != null) {
      if (isSell()) {
        t = new SellTransaction(this);
      } else if (isBuyWithUsd()) {
        t = new BuyTransaction(this);
      } else if (isCoinToCoinBuy()) {
        t = new CoinToCoinTransaction(this);
      } else {
        throw new IllegalArgumentException("Neither buy nor sell? " + this);
      }
    }
    return t;
  }

  private AutoInvestTransaction tryToConvertToAutoInvest() {
    return this instanceof AutoInvestTransaction invest ? invest : null;
  }

  /**
   * Get the total number of operations (raw changes).
   *
   * @return The total number of raw changes for this transaction.
   */
  public final int getTotalOperationCount() {
    int count = 0;
    for (List<RawAccountChange> changes : atomicAccountChanges.values()) {
      count += changes.size();
    }
    return count;
  }

  private Transaction tryToConvertToDepositOrWithdraw() {
    Transaction t = null;
    if (consistsOf(Operation.DEPOSIT) || consistsOf(Operation.FIAT_DEPOSIT)) {
      t = new DepositTransaction(this);
    } else if (consistsOf(Operation.WITHDRAW)) {
      t = new WithdrawTransaction(this);
    }
    return t;
  }

  private boolean isSell() {
    RawAccountChange bought = getFirstBuyTypeChange();
    return bought != null && bought.getAsset().equals("USDT");
  }

  private boolean isBuyWithUsd() {
    RawAccountChange sold = getFirstSellTypeChange();
    return sold != null && sold.getAsset().equals("USDT");
  }

  private boolean isCoinToCoinBuy() {
    RawAccountChange sold = getFirstSellTypeChange();
    RawAccountChange bought = getFirstBuyTypeChange();
    return sold != null && bought != null
        && !bought.getAsset().equals("USDT")
        && !sold.getAsset().equals("USDT");
  }

  /**
   * Returns true if this transaction consists ONLY of the given operations, one of each type.
   *
   * @param operations The expected operations
   * @return True if this transaction consists ONLY of the given change operations
   */
  private boolean consistsOf(Operation... operations) {
    return getOperationMultiSet().equals(new OperationMultiSet(operations));
  }

  /**
   * Returns true if this transaction consists ONLY of the given operations, N of each type.
   * N must be equal for all operation types, and N must be greater than 1.
   * N is detected automatically.
   *
   * @param operations The expected operations
   * @return True if this transaction consists ONLY of the given change operations
   */
  private boolean consistsOfMultiple(Operation... operations) {
    int n = getCountOfOperationsWithType(operations[0]);
    return n > 1 && getOperationMultiSet().equals(new OperationMultiSet(n, operations));
  }

  private int getCountOfOperationsWithType(Operation operation) {
    List<RawAccountChange> changes = atomicAccountChanges.get(operation);
    return changes != null ? changes.size() : 0;
  }


  /**
   * Get multiset containing the count of each operation type (not the operation itself).
   *
   * @return Multiset of all operations: count by type
   */
  public final OperationMultiSet getOperationMultiSet() {
    OperationMultiSet operationMultiSet = new OperationMultiSet();
    for (Map.Entry<Operation, List<RawAccountChange>> entry : atomicAccountChanges.entrySet()) {
      operationMultiSet.add(entry.getKey(), entry.getValue().size());
    }
    return operationMultiSet;
  }

  /**
   * Merge the raw account changes by their type. For example, if there are three BUY changes:
   * Buy 0.1 BTC, BUY 0.2 BTC, BUY 0.3 BTC, these will be merged into a single change: BUY 0.6 BTC.
   */
  private void mergeRawChangesByType() {
    Map<Operation, List<RawAccountChange>> mergedChanges = new EnumMap<>(Operation.class);

    for (Map.Entry<Operation, List<RawAccountChange>> e : atomicAccountChanges.entrySet()) {
      List<RawAccountChange> originalChanges = e.getValue();
      RawAccountChange mergedChange = RawAccountChange.merge(originalChanges);
      mergedChanges.put(e.getKey(), Collections.singletonList(mergedChange));
    }
    atomicAccountChanges = mergedChanges;
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
   * Get the first account change which is sell-like type (including SELL, etc).
   *
   * @return The first change or null if no change of this type is found.
   */
  protected final RawAccountChange getFirstSellTypeChange() {
    return getFirstChangeOfType(Operation.SELL);
  }

  /**
   * Get the first account change which is buy-like type (including BUY, etc).
   *
   * @return The first change or null if no change of this type is found.
   */
  protected final RawAccountChange getFirstBuyTypeChange() {
    return getFirstChangeOfType(Operation.BUY);
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
    return "";
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
   * Get the amount of fee, in USDT. Note: the value will be negative!
   *
   * @return The fee amount, converted to the USDT currency
   */
  public final Decimal getFeeInUsdt() {
    return feeInUsdt;
  }

  /**
   * Get obtain-price of the base currency, calculated in USDT.
   *
   * @return The obtain-price of the main asset, in USDT
   */
  public final Decimal getObtainPrice() {
    return baseObtainPriceInUsdt;
  }

  /**
   * Get the average transaction price (sell price, buy price, etc.), calculated in USDT.
   *
   * @return The average price of the transaction, in USDT
   */
  public final Decimal getAvgPriceInUsdt() {
    return avgPriceInUsdt;
  }

  /**
   * Get Profit & Loss (PNL) of this single transaction.
   *
   * @return The PNL, in USDT
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

  /**
   * Find out the fee in USDT, store it.
   *
   * @throws IllegalStateException When no fee raw-account-change is found
   */
  protected final void calculateFeeInUsdt(Wallet wallet) throws IllegalStateException {
    RawAccountChange feeOp = getFirstChangeOfType(Operation.FEE);
    if (feeOp != null) {
      if (feeOp.getAsset().equals("USDT")) {
        feeInUsdt = feeOp.getAmount();
      } else {
        feeInUsdt = feeOp.getAmount().multiply(wallet.getAvgObtainPrice(feeOp.getAsset()));
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Transaction that = (Transaction) o;
    return utcTime == that.utcTime
        && Objects.equals(atomicAccountChanges, that.atomicAccountChanges)
        && Objects.equals(baseCurrency, that.baseCurrency)
        && Objects.equals(baseCurrencyAmount, that.baseCurrencyAmount)
        && Objects.equals(baseObtainPriceInUsdt, that.baseObtainPriceInUsdt)
        && Objects.equals(avgPriceInUsdt, that.avgPriceInUsdt)
        && Objects.equals(quoteCurrency, that.quoteCurrency)
        && Objects.equals(fee, that.fee)
        && Objects.equals(feeCurrency, that.feeCurrency)
        && Objects.equals(feeInUsdt, that.feeInUsdt)
        && Objects.equals(pnl, that.pnl)
        && Objects.equals(quoteAmount, that.quoteAmount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(atomicAccountChanges, utcTime, baseCurrency, baseCurrencyAmount,
        baseObtainPriceInUsdt, avgPriceInUsdt, quoteCurrency, fee, feeCurrency, feeInUsdt,
        pnl, quoteAmount);
  }

  /**
   * Get all changes of given type.
   *
   * @param type The type of changes to consider
   * @return The list of all changes or an empty list if no changes of that type are stored
   */
  protected List<RawAccountChange> getChangesOfType(Operation type) {
    return atomicAccountChanges.getOrDefault(type, Collections.emptyList());
  }

  /**
   * Create the summary of changes that this transaction makes to the wallet, based on asset
   * changes in all the individual operations.
   *
   * @return The sum of all operation changes, as wallet difference
   */
  public final WalletDiff getOperationDiff() {
    WalletDiff diff = new WalletDiff();
    for (List<RawAccountChange> changes : atomicAccountChanges.values()) {
      for (RawAccountChange change : changes) {
        diff.add(change.getAsset(), change.getAmount());
      }
    }
    return diff;
  }

  /**
   * Get the number of atomic changes of specific type stores inside this transaction.
   *
   * @param type The type of operation which is of interest
   * @return The number of changes of this type, 0 if none
   */
  public int getCountForChangesOfType(Operation type) {
    return getChangesOfType(type).size();
  }

  /**
   * Check whether the provided asset is USD or one of it's coin-equivalents.
   *
   * @param asset The asset
   * @return True when it is USD or alike (USDT, BUSD)
   */
  public static boolean isUsdLike(String asset) {
    return "USD".equals(asset) || "BUSD".equals(asset) || "USDT".equals(asset)
        || "USDC".equals(asset);
  }
}
