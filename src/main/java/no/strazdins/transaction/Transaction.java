package no.strazdins.transaction;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
  public void append(RawAccountChange change) {
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
  public Transaction clarifyTransactionType() {
    if (consistsOf(Operation.DEPOSIT)) {
      return new DepositTransaction(this);
    }
    // TODO - implement other transaction types
    return null;
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
  protected RawAccountChange getFirstChangeOfType(Operation operation) {
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
  public long getUtcTime() {
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
}
