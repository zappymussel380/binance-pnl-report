package no.strazdins.transaction;

import java.util.Iterator;
import no.strazdins.data.ExtraInfoEntry;
import no.strazdins.data.ExtraInfoType;
import no.strazdins.data.Operation;
import no.strazdins.data.RawAccountChange;

/**
 * Purchase crypto with card.
 */
public class CardPurchaseTransaction extends Transaction {

  /**
   * Create a Card-purchase transaction.
   *
   * @param t The base transaction to copy data from
   * @throws IllegalStateException When some data is missing in the raw account changes
   */
  public CardPurchaseTransaction(Transaction t) throws IllegalStateException {
    super(t);
    initCoin();
    initFiat();
  }

  private void initFiat() throws IllegalStateException {
    RawAccountChange fiatSpent = getFiatChange();
    quoteCurrency = fiatSpent.getAsset();
    quoteAmount = fiatSpent.getAmount();
  }

  private void initCoin() throws IllegalStateException {
    RawAccountChange coinPurchase = getCoinPurchase();
    if (coinPurchase == null) {
      throw new IllegalStateException("Coin purchase changes missing");
    }
    baseCurrency = coinPurchase.getAsset();
    baseCurrencyAmount = coinPurchase.getAmount();
  }

  private RawAccountChange getCoinPurchase() {
    RawAccountChange purchase = null;
    Iterator<RawAccountChange> it = getChangesOfType(Operation.BUY_CRYPTO).iterator();
    while (purchase == null && it.hasNext()) {
      RawAccountChange change = it.next();
      if (!isFiat(change.getAsset())) {
        purchase = change;
      }
    }
    return purchase;
  }

  private RawAccountChange getFiatChange() throws IllegalStateException {
    RawAccountChange positiveFiat = findPositiveFiatChange();
    RawAccountChange negativeFiat = findNegativeFiatChange();
    if (positiveFiat == null || negativeFiat == null) {
      throw new IllegalStateException(
          "Card purchase must contain positive and negative fiat changes");
    }
    if (positiveFiat.getAmount() == null
        || !positiveFiat.getAmount().equals(negativeFiat.getAmount().negate())) {
      throw new IllegalStateException("Positive fiat change amount ("
          + positiveFiat.getAmount().getNiceString() + ") does not match negative amount ("
          + negativeFiat.getAmount().getNiceString() + ")");
    }

    return negativeFiat;
  }

  private RawAccountChange findPositiveFiatChange() {
    return findFiatChange(true);
  }

  private RawAccountChange findNegativeFiatChange() {
    return findFiatChange(false);
  }

  private RawAccountChange findFiatChange(boolean mustBePositive) {
    RawAccountChange fiatChange = null;
    Iterator<RawAccountChange> it = getChangesOfType(Operation.BUY_CRYPTO).iterator();
    while (fiatChange == null && it.hasNext()) {
      RawAccountChange change = it.next();
      if (isFiat(change.getAsset()) && mustBePositive == change.getAmount().isPositive()) {
        fiatChange = change;
      }
    }
    return fiatChange;
  }

  @Override
  public String getType() {
    return "Card purchase";
  }

  @Override
  public ExtraInfoEntry getNecessaryExtraInfo() {
    ExtraInfoEntry ei = null;
    if (!isUsdLike(quoteCurrency)) {
      ei = new ExtraInfoEntry(utcTime, ExtraInfoType.ASSET_PRICE, quoteCurrency,
          quoteCurrency + " price in USD");
    }
    return ei;
  }
}
