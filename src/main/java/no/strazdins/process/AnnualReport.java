package no.strazdins.process;

import no.strazdins.data.Decimal;

/**
 * Contains the information necessary for presenting one annual Profit-and-loss (PNL) report.
 *
 * @param timestamp      Timestamp of the year-end, December 31st 23:59:59.000
 * @param pnlUsd         PNL in USD
 * @param exchangeRate   Exchange rate HomeCurrency/USD
 * @param pnlHc          PNL in Home currency
 * @param walletValueUsd Wallet value in USD
 * @param walletValueHc  Wallet value in Home currency
 */
public record AnnualReport(long timestamp, Decimal pnlUsd, Decimal exchangeRate, Decimal pnlHc,
                           Decimal walletValueUsd, Decimal walletValueHc) {
}
