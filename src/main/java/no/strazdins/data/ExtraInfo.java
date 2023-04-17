package no.strazdins.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import no.strazdins.tool.TimeConverter;

/**
 * Extra user-provided information.
 */
public class ExtraInfo implements Iterable<ExtraInfoEntry> {
  // Mapping timestamp to a list of extra info entries
  private final Map<Long, List<ExtraInfoEntry>> entries = new TreeMap<>();
  // Copy of all the entries
  private final List<ExtraInfoEntry> allEntries = new LinkedList<>();

  /**
   * Add an entry to the info storage.
   *
   * @param infoEntry The info entry to add
   */
  public void add(ExtraInfoEntry infoEntry) {
    List<ExtraInfoEntry> entryList = entries.get(infoEntry.utcTimestamp());
    if (entryList == null) {
      entryList = new LinkedList<>();
      entries.put(infoEntry.utcTimestamp(), entryList);
    }
    entryList.add(infoEntry);
    allEntries.add(infoEntry);
  }

  /**
   * Check if the extra info set is empty.
   *
   * @return True if no info is stored here, false otherwise
   */
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  /**
   * Get all entries stored here.
   *
   * @return All the extra info entries, ordered by timestamp.
   */
  public List<ExtraInfoEntry> getAllEntries() {
    return allEntries;
  }

  /**
   * Check if this information storage contains the provided entry.
   *
   * @param e The entry to check
   * @return True if this info storage contains the requested info entry, false otherwise
   */
  public boolean contains(ExtraInfoEntry e) {
    boolean found = false;
    Iterator<ExtraInfoEntry> it = getAllEntries().iterator();
    while (!found && it.hasNext()) {
      ExtraInfoEntry existingEntry = it.next();
      found = existingEntry.utcTimestamp() == e.utcTimestamp()
          && existingEntry.type().equals(e.type());
    }
    return found;
  }

  /**
   * Get stored extra info for a given time moment.
   *
   * @param utcTime UTC timestamp of the time moment in question, including milliseconds.
   * @return The stored ExtraInfo record, or null if none found.
   */
  public ExtraInfoEntry getAtTime(long utcTime) {
    List<ExtraInfoEntry> entryList = entries.get(utcTime);
    return entryList != null ? entryList.get(0) : null;
  }

  /**
   * Find price for a given asset at a given time moment.
   *
   * @param timestamp The time moment to consider
   * @param asset     The asset in question
   * @return The price of the asset at that moment or null if none found
   * @throws IllegalStateException When there is more than one price of the asset at that moment
   */
  public Decimal getAssetPriceAtTime(long timestamp, String asset) throws IllegalStateException {
    List<ExtraInfoEntry> entriesAtTime = entries.get(timestamp);
    List<ExtraInfoEntry> assetPrices = entriesAtTime.stream().filter(
        entry -> entry.asset().equals(asset) && entry.type().equals(ExtraInfoType.ASSET_PRICE)
    ).toList();
    if (assetPrices.size() > 1) {
      throw new IllegalStateException("Multiple " + asset + " prices at " + timestamp + "("
          + TimeConverter.utcTimeToString(timestamp) + ")");
    }
    return assetPrices.size() == 1 ? new Decimal(assetPrices.get(0).value()) : null;
  }

  @Override
  public Iterator<ExtraInfoEntry> iterator() {
    return allEntries.iterator();
  }
}
