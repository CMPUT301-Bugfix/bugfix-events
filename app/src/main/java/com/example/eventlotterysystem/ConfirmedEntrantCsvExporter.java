package com.example.eventlotterysystem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This is a helper class for exporting confirmed Entrants into a CSV file
 * and for checking if the export views should be shown
 */
final class ConfirmedEntrantCsvExporter {
    /**
     * helper class for CSV export only, so it should not be instantiated
     */
    private ConfirmedEntrantCsvExporter() {
    }

    /**
     * checks if the export area should be shown based on the Entrant status filter
     * @param statusFilter
     * the current String status filter for the Entrants list
     * @return
     * true if the status is CONFIRMED, false otherwise
     */
    static boolean shouldShowExportArea(@Nullable String statusFilter) {
        return EventRepository.WAITLIST_STATUS_CONFIRMED.equals(statusFilter);
    }

    /**
     * builds the suggested file name for the confirmed Entrants CSV export
     * @param eventId
     * the String id of the Event being exported
     * @param now
     * the current Date used for the timestamp in the file name
     * @return
     * a String file name for the CSV export
     */
    @NonNull
    static String buildSuggestedFileName(@Nullable String eventId, @NonNull Date now) {
        String safeEventId = eventId == null || eventId.trim().isEmpty()
                ? "event"
                : eventId.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now);
        return "confirmed-entrants-" + safeEventId + "-" + timestamp + ".csv";
    }

    /**
     * builds the CSV text for the confirmed Entrants list including the header row
     * @param entrants
     * the list of confirmed UserProfiles that will be exported
     * @return
     * a String containing the CSV text for the confirmed Entrants
     * @throws IOException
     * if the CSV cannot be written into the StringWriter
     */
    @NonNull
    static String buildCsv(@NonNull List<UserProfile> entrants) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[] {
                    "full_name",
                    "username",
                    "email",
                    "phone_number",
                    "account_type"
            }, false);

            for (UserProfile entrant : entrants) {
                csvWriter.writeNext(new String[] {
                        safeValue(entrant.getName()),
                        safeValue(entrant.getUsername()),
                        safeValue(entrant.getEmail()),
                        safeValue(entrant.getPhoneNumber()),
                        safeValue(entrant.getAccountType())
                }, false);
            }
        }
        return stringWriter.toString();
    }

    /**
     * cleans a profile value before it is written into the CSV file
     * @param value
     * the String value from the UserProfile
     * @return
     * the trimmed value, or an empty String if the value is null or blank
     */
    @NonNull
    private static String safeValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.trim();
    }
}
