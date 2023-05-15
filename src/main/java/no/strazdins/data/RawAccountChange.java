package no.strazdins.data;

import java.util.List;
import java.util.Objects;
import no.strazdins.tool.TimeConverter;

/**
 * One single, atomic account change (part of a larger transaction).
 */
public class RawAccountChange {
  private final long utcTime;
  private final AccountType account;
  private final Operation operation;
  private final String asset;
  private Decimal changeAmount;
  private final String remark;

  /**
   * Create a new account-change record.
   *
   * @param utcTime      UTC timestamp, with milliseconds
   * @param account      The account that was used (Spot, Earn, etc)
   * @param operation    Performed operation
   * @param asset        The involved asset (coin or fiat)
   * @param changeAmount The amount of the change - how much coin was added/removed
   *                     to/from the accounts
   * @param remark       A comment
   */
  public RawAccountChange(long utcTime, AccountType account, Operation operation,
                          String asset, Decimal changeAmount, String remark) {
    this.utcTime = utcTime;
    this.account = account;
    this.operation = operation;
    this.asset = asset;
    this.changeAmount = changeAmount;
    this.remark = remark;
    runAssertions();
  }

  private void runAssertions() {
    if (operation == Operation.BUY && !changeAmount.isPositive()) {
      throw new IllegalArgumentException("Amount must be positive for all buy-type changes @ "
       + TimeConverter.utcTimeToString(utcTime));
    }
    if (operation == Operation.SELL && !changeAmount.isNegative()) {
      throw new IllegalArgumentException("Amount must be negative for all sell-type changes @ "
          + TimeConverter.utcTimeToString(utcTime) + "[" + this + "]");
    }
    if (operation == Operation.WITHDRAW && !changeAmount.isNegative()) {
      throw new IllegalArgumentException("Amount must be negative for all withdraw-type changes @ "
          + TimeConverter.utcTimeToString(utcTime) + "[" + this + "]");
    }
  }

  /**
   * Merge the given list of original changes into a single change.
   *
   * @param originalChanges The list of original changes to merge
   * @return A single change, where all original changes are merged
   * @throws IllegalArgumentException When the changes can't be merged: different types or assets
   */
  public static RawAccountChange merge(List<RawAccountChange> originalChanges)
      throws IllegalArgumentException {
    if (originalChanges == null || originalChanges.isEmpty()) {
      throw new IllegalArgumentException("Can't merge an empty list");
    }

    RawAccountChange merged = new RawAccountChange(originalChanges.get(0));

    for (int i = 1; i < originalChanges.size(); ++i) {
      RawAccountChange c = originalChanges.get(i);
      if (c.utcTime != merged.utcTime || c.account != merged.account
          || c.operation != merged.operation || !c.asset.equals(merged.asset)) {
        throw new IllegalArgumentException(
            "Merged changes must have the same time, account, operation and asset, time="
                + TimeConverter.utcTimeToString(merged.utcTime)
        );
      }
      merged.changeAmount = merged.changeAmount.add(c.changeAmount);
    }

    return merged;
  }

  /**
   * Copy an object.
   *
   * @param original The object to copy
   */
  public RawAccountChange(RawAccountChange original) {
    this.account = original.account;
    this.changeAmount = original.changeAmount;
    this.asset = original.asset;
    this.remark = original.remark;
    this.operation = original.operation;
    this.utcTime = original.utcTime;
  }

  /**
   * Get UTC timestamp of the change action, with milliseconds.
   *
   * @return The UTC time when this change was made to the account
   */
  public long getUtcTime() {
    return utcTime;
  }

  /**
   * Get the type of the operation.
   *
   * @return The type of the operation performed in this change
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * Get the asset.
   *
   * @return The asset involved in the account change action
   */
  public String getAsset() {
    return asset;
  }

  @Override
  public String toString() {
    return "RawAccountChange{"
        + "utcTime=" + TimeConverter.utcTimeToString(utcTime)
        + ", account=" + account
        + ", operation=" + operation
        + ", asset='" + asset + '\''
        + ", changeAmount='" + changeAmount + '\''
        + ", remark='" + remark + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RawAccountChange that = (RawAccountChange) o;
    return utcTime == that.utcTime && account == that.account && operation == that.operation
        && Objects.equals(asset, that.asset) && Objects.equals(changeAmount, that.changeAmount)
        && Objects.equals(remark, that.remark);
  }

  @Override
  public int hashCode() {
    return Objects.hash(utcTime, account, operation, asset, changeAmount, remark);
  }

  /**
   * Get amount of asset change.
   *
   * @return The amount of the asset. A decimal formatted as a string. Can be negative.
   */
  public Decimal getAmount() {
    return changeAmount;
  }

  /**
   * Get the account to which the change was performed.
   *
   * @return The account
   */
  public AccountType getAccount() {
    return account;
  }
}
