package no.strazdins.transaction;

import java.util.Objects;
import no.strazdins.data.Decimal;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.RawAccountChange;
import no.strazdins.tool.TimeConverter;

/**
 * A deposit or withdrawal transaction - transfer to or from an external account.
 */
public abstract class ExternalTransferTransaction extends Transaction {
  protected RawAccountChange change;

  protected ExternalTransferTransaction(Transaction t) {
    super(t);
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    ExtraInfoEntry ei = null;
    if (!isUsdLike(change.getAsset())) {
      String date = TimeConverter.utcTimeToDateString(utcTime);
      String hint = "<" + change.getAsset() + " price in USD on " + date + ">";
      ei = new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, change.getAsset(), hint);
    }
    return ei;
  }

  /**
   * Find out obtain or realization value of the withdrawn asset - either from the extra info or
   * use 1.0 if it is a USD-like asset.
   *
   * @param extraInfo The user-provided extra info
   * @return The obtain-price or realization-price of the asset
   * @throws IllegalArgumentException When price can neither be determined from ExtraInfo
   *                                  nor inferred
   */
  protected Decimal findExternalPrice(ExtraInfoEntry extraInfo) throws IllegalArgumentException {
    if (isUsdLike(change.getAsset())) {
      return Decimal.ONE;
    } else if (extraInfo != null) {
      return new Decimal(extraInfo.value());
    } else {
      throw new IllegalArgumentException("Obtain/realization price must be specified");
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
    if (!super.equals(o)) {
      return false;
    }
    WithdrawTransaction that = (WithdrawTransaction) o;
    return Objects.equals(change, that.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), change);
  }
}
