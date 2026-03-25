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

final class ConfirmedEntrantCsvExporter {
    private ConfirmedEntrantCsvExporter() {
    }

    static boolean shouldShowExportArea(@Nullable String statusFilter) {
        return EventRepository.WAITLIST_STATUS_CONFIRMED.equals(statusFilter);
    }

    @NonNull
    static String buildSuggestedFileName(@Nullable String eventId, @NonNull Date now) {
        String safeEventId = eventId == null || eventId.trim().isEmpty()
                ? "event"
                : eventId.trim().replaceAll("[^A-Za-z0-9._-]", "_");
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now);
        return "confirmed-entrants-" + safeEventId + "-" + timestamp + ".csv";
    }

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

    @NonNull
    private static String safeValue(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return value.trim();
    }
}
