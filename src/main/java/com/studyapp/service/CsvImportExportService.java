package com.studyapp.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.studyapp.controller.CustomException;
import com.studyapp.controller.MainController;
import com.studyapp.model.Deck;
import com.studyapp.model.Flashcard;

public class CsvImportExportService {

    // The expected header row written at the top of every exported CSV
    private static final String HEADER = "deck_name,description,question,answer,difficulty";

    // Column names used to validate an imported file's header row (order matters)
    private static final List<String> EXPECTED_HEADERS = List.of(
            "deck_name",
            "description",
            "question",
            "answer",
            "difficulty"
    );

    /**
     * Imports decks and flashcards from a CSV file into the application.
     *
     * Flow:
     *  1. Parse the raw file text into a list of rows via parseCsv().
     *  2. Validate the header row and strip blanks via extractDataRows().
     *  3. Group data rows by deck name (case-insensitive) into CsvDeckData buckets:
     *     - First occurrence of a deck name registers the deck + description.
     *     - Subsequent rows for the same deck append cards.
     *     - Conflicting descriptions for the same deck name throw an exception.
     *     - Rows with a blank question or answer are skipped.
     *  4. For each collected deck:
     *     - Skip if a deck with the same name already exists in memory.
     *     - Create the deck via MainController.
     *     - Create each card via MainController.
     *  5. Return the number of decks successfully imported.
     *
     * Note: All created objects are held in memory only until saveChanges() is called.
     */
    public int importFromFile(File file, MainController mc) throws CustomException {
        // Step 1-2: parse file and validate/strip header
        List<List<String>> rows = parseCsv(file);
        List<List<String>> dataRows = extractDataRows(rows);

        // Step 3: group rows by deck name, collecting cards per deck
        Map<String, CsvDeckData> deckRows = new LinkedHashMap<>();
        for (List<String> row : dataRows) {
            String rawName = row.get(0);
            if (rawName == null || rawName.isBlank()) {
                continue;
            }

            // Trim and normalise each field
            String deckName = rawName.trim();
            String description = safeTrim(row.get(1));
            String question = safeTrim(row.get(2));
            String answer = safeTrim(row.get(3));
            String difficulty = normaliseDifficulty(row.get(4));

            // Use a lowercase key so grouping is case-insensitive
            String deckKey = deckName.toLowerCase(Locale.ROOT);
            CsvDeckData deckData = deckRows.get(deckKey);
            if (deckData == null) {
                // First time seeing this deck — register it
                deckData = new CsvDeckData(deckName, description);
                deckRows.put(deckKey, deckData);
            } else if (!description.isBlank()
                    && !deckData.description().isBlank()
                    && !deckData.description().equals(description)) {
                // Two rows claim different descriptions for the same deck — ambiguous
                throw new CustomException("CSV contains conflicting descriptions for deck '" + deckName + "'.");
            } else if (deckData.description().isBlank() && !description.isBlank()) {
                // Fill in a description that was missing on earlier rows
                deckData = new CsvDeckData(deckData.deckName(), description, deckData.cards());
                deckRows.put(deckKey, deckData);
            }

            // Skip rows that have no card data
            if (question.isBlank() || answer.isBlank()) {
                continue;
            }

            deckRows.get(deckKey).cards().add(new CardJson(question, answer, difficulty));
        }

        // Step 4: persist each collected deck and its cards through MainController
        int imported = 0;

        for (CsvDeckData deckData : deckRows.values()) {
            // Skip decks whose names already exist in memory (prevents duplicates)
            boolean exists = mc.allDecks().stream()
                    .anyMatch(d -> d.getName().equalsIgnoreCase(deckData.deckName()));
            if (exists) {
                continue;
            }

            Deck newDeck = mc.createDeck(deckData.deckName(), deckData.description());

            for (CardJson card : deckData.cards()) {
                mc.createFlashcard(
                        newDeck.getDeckID(),
                        card.getQuestion(),
                        card.getAnswer(),
                        card.getDifficulty()
                );
            }

            imported++;
        }

        return imported;
    }

    /**
     * Exports a single deck and its flashcards to a CSV file.
     *
     * Flow:
     *  1. Write the fixed header row.
     *  2. If the deck has no cards, write a single row with only the deck metadata
     *     and empty question/answer/difficulty columns.
     *  3. Otherwise write one row per flashcard, repeating deck_name and description
     *     on every row (flat denormalised format).
     *
     * All field values are quoted and sanitised against CSV formula injection.
     * Throws CustomException if the file cannot be written.
     */
    public void exportDeckToFile(Deck deck, List<Flashcard> cards, File file) throws CustomException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            // Always write the header first
            writer.write(HEADER + "\n");

            if (cards == null || cards.isEmpty()) {
                // Deck exists but has no cards — write a metadata-only row
                writer.write(buildRow(
                        deck.getName(),
                        deck.getDescription(),
                        "",
                        "",
                        ""
                ) + "\n");
                return;
            }

            // One row per card, carrying deck_name and description on every line
            for (Flashcard card : cards) {
                writer.write(buildRow(
                        deck.getName(),
                        deck.getDescription(),
                        card.getQuestion(),
                        card.getAnswer(),
                        card.getDifficulty()
                ) + "\n");
            }
        } catch (IOException e) {
            throw new CustomException("Failed to write CSV export file.");
        }
    }

    /**
     * Validates the header row and returns only the non-blank data rows.
     *
     * Flow:
     *  1. Reject an entirely empty file.
     *  2. Skip any leading blank rows to find the header.
     *  3. Verify the header has exactly the required columns in the correct order.
     *  4. Collect every subsequent non-blank row, rejecting any with the wrong
     *     column count.
     */
    private List<List<String>> extractDataRows(List<List<String>> rows) throws CustomException {
        if (rows.isEmpty()) {
            throw new CustomException("CSV file is empty.");
        }

        // Skip leading blank lines to find the actual header row
        int headerIndex = 0;
        while (headerIndex < rows.size() && isBlankRow(rows.get(headerIndex))) {
            headerIndex++;
        }
        if (headerIndex >= rows.size()) {
            throw new CustomException("CSV file is empty.");
        }

        // Validate column count and each column name
        List<String> header = rows.get(headerIndex);
        if (header.size() != EXPECTED_HEADERS.size()) {
            throw new CustomException("Invalid CSV header. Expected: " + HEADER);
        }

        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!EXPECTED_HEADERS.get(i).equals(normalizeHeader(header.get(i)))) {
                throw new CustomException("Invalid CSV header. Expected: " + HEADER);
            }
        }

        // Collect data rows, skipping blanks and rejecting malformed ones
        List<List<String>> dataRows = new ArrayList<>();
        for (int i = headerIndex + 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            if (row.size() != EXPECTED_HEADERS.size()) {
                throw new CustomException("Invalid CSV row format near line " + (i + 1) + ".");
            }
            dataRows.add(row);
        }
        return dataRows;
    }

    /**
     * Reads a CSV file and splits it into a list of rows, each row being a list
     * of field strings. Handles RFC 4180 quoting rules:
     *  - Fields wrapped in double-quotes may contain commas and newlines.
     *  - A pair of double-quotes ("") inside a quoted field represents a literal quote.
     *
     * Additional handling:
     *  - Strips a leading UTF-8 BOM character if present.
     *  - Normalises CRLF (\r\n) to a single line break.
     *  - Throws CustomException for unmatched opening quotes or unreadable files.
     */
    private List<List<String>> parseCsv(File file) throws CustomException {
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            // Strip UTF-8 BOM that Excel and some editors prepend
            if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
                content = content.substring(1);
            }

            List<List<String>> rows = new ArrayList<>();
            List<String> currentRow = new ArrayList<>();
            StringBuilder currentField = new StringBuilder();
            boolean inQuotes = false;

            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);

                if (inQuotes) {
                    if (ch == '"') {
                        if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                            // Escaped quote ("") — append a single literal quote
                            currentField.append('"');
                            i++;
                        } else {
                            // Closing quote — exit quoted mode
                            inQuotes = false;
                        }
                    } else {
                        currentField.append(ch);
                    }
                    continue;
                }

                if (ch == '"') {
                    // Opening quote — enter quoted mode
                    inQuotes = true;
                } else if (ch == ',') {
                    // Comma outside quotes — field boundary
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                } else if (ch == '\r' || ch == '\n') {
                    // Line ending outside quotes — row boundary
                    currentRow.add(currentField.toString());
                    currentField.setLength(0);
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();

                    // Consume the \n of a CRLF pair so it isn't treated as a second row
                    if (ch == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                        i++;
                    }
                } else {
                    currentField.append(ch);
                }
            }

            // A file that ends without a trailing newline still has a quote open — error
            if (inQuotes) {
                throw new CustomException("Malformed CSV file: unmatched quotes.");
            }

            // Flush any remaining content as the final row
            if (currentField.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(currentField.toString());
                rows.add(currentRow);
            }

            return rows;
        } catch (IOException e) {
            throw new CustomException("Could not read file: " + e.getMessage());
        }
    }

    /** Returns true if every cell in the row is null or whitespace-only. */
    private boolean isBlankRow(List<String> row) {
        return row.stream().allMatch(value -> value == null || value.isBlank());
    }

    /** Trims and lowercases a header cell for case-insensitive comparison. */
    private String normalizeHeader(String value) {
        return safeTrim(value).toLowerCase(Locale.ROOT);
    }

    /** Null-safe trim — returns an empty string instead of throwing on null. */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Normalises a raw difficulty string to one of "Easy", "Medium", or "Hard".
     * Any null, blank, or unrecognised value defaults to "Medium".
     */
    private String normaliseDifficulty(String raw) {
        if (raw == null || raw.isBlank()) return "Medium";
        String t = raw.trim();
        if (t.equalsIgnoreCase("Easy"))   return "Easy";
        if (t.equalsIgnoreCase("Medium")) return "Medium";
        if (t.equalsIgnoreCase("Hard"))   return "Hard";
        return "Medium";
    }

    /**
     * Guards against CSV formula injection (a spreadsheet security issue where
     * values starting with =, +, -, @ are interpreted as formulas by Excel/Sheets).
     *
     * Flow:
     *  1. Remove null bytes.
     *  2. Find the first non-whitespace character (ignoring various Unicode spaces).
     *  3. If that character is a formula trigger (=, +, -, @, tab, CR), prepend a
     *     single quote to neutralise it.
     *  4. Apply the same neutralisation to any trigger character that appears after
     *     an embedded newline.
     */
    private String sanitizeFormulaInjection(String value) {
        if (value == null) return "";

        // Remove null bytes that could confuse parsers
        value = value.replace("\0", "");

        // Skip leading whitespace variants to find the effective first character
        int start = 0;
        while (start < value.length() && (Character.isWhitespace(value.charAt(start))
                || value.charAt(start) == '\u00A0'
                || value.charAt(start) == '\u200B'
                || value.charAt(start) == '\uFEFF')) {
            start++;
        }
        String trimmed = value.substring(start);

        if (trimmed.isEmpty()) return value;
        char first = trimmed.charAt(0);

        // Prepend a single quote if the value would be interpreted as a formula
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            value = "'" + value;
        }

        // Also neutralise formula triggers that appear at the start of embedded lines
        value = value.replaceAll("(\r\n|\r|\n)([=+\\-@\t\r])", "$1'$2");

        return value;
    }

    /**
     * Wraps a single field value in double-quotes for CSV output.
     * Any internal double-quotes are escaped by doubling them ("").
     * Formula injection is sanitised before quoting.
     */
    private String escapeCsv(String value) {
        value = sanitizeFormulaInjection(value == null ? "" : value);
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Builds a complete CSV row string from an arbitrary number of field values.
     * Each field is individually escaped and joined with commas.
     */
    private String buildRow(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(escapeCsv(values[i]));
        }
        return sb.toString();
    }

    /**
     * Immutable value object that accumulates a deck's name, description, and
     * cards while grouping CSV rows during import.
     * The compact constructor (deckName, description) initialises cards as an
     * empty mutable list so cards can be appended row by row.
     */
    private record CsvDeckData(String deckName, String description, List<CardJson> cards) {
        private CsvDeckData(String deckName, String description) {
            this(deckName, description, new ArrayList<>());
        }
    }
}
