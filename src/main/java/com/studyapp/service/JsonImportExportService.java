package com.studyapp.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.studyapp.controller.CustomException;
import com.studyapp.controller.MainController;
import com.studyapp.model.Deck;
import com.studyapp.model.Flashcard;

public class JsonImportExportService {

    // Shared Gson instance configured for pretty-printed output
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Imports decks and flashcards from a JSON file into the application.
     *
     * Flow:
     *  1. Parse the file into a list of DeckJson objects via parseFile().
     *  2. For each deck in the list:
     *     a. Skip if the deck name is blank.
     *     b. Skip if a deck with the same name already exists in memory (case-insensitive).
     *     c. Create the deck via MainController and collect it.
     *     d. For each card in the deck:
     *        - Skip if question or answer is blank.
     *        - Normalise the difficulty value (defaults to "Medium" if missing/invalid).
     *        - Create the flashcard via MainController.
     *  3. Return the total number of decks successfully imported.
     *
     * Note: All created objects are held in memory only until saveChanges() is called.
     */
    public int importFromFile(File file, MainController mc) throws CustomException {
        List<DeckJson> deckJsonList = parseFile(file);
        int imported = 0;
        List<Deck> importedDecks = new ArrayList<>();
        List<Flashcard> importedFlashcards = new ArrayList<>();

        for (DeckJson deckJson : deckJsonList) {
            String rawName = deckJson.getDeckName();
            // Skip decks with no name
            if (rawName == null || rawName.isBlank()) continue;
            String deckName = rawName.trim();

            // Skip if a deck with this name already exists (prevents duplicates)
            boolean exists = mc.allDecks().stream()
                    .anyMatch(d -> d.getName().equalsIgnoreCase(deckName));
            if (exists) continue;

            // Create the deck; null description is treated as empty string
            String desc = deckJson.getDescription() == null ? "" : deckJson.getDescription().trim();
            Deck newDeck = mc.createDeck(deckName, desc);
            importedDecks.add(newDeck);

            // Import each card belonging to this deck
            List<CardJson> cards = deckJson.getCards();
            if (cards != null) {
                for (CardJson c : cards) {
                    // Skip cards that are missing a question or answer
                    if (c.getQuestion() == null || c.getQuestion().isBlank()) continue;
                    if (c.getAnswer()   == null || c.getAnswer().isBlank())   continue;
                    String difficulty = normaliseDifficulty(c.getDifficulty());
                    Flashcard flashcard = mc.createFlashcard(newDeck.getDeckID(),
                            c.getQuestion().trim(),
                            c.getAnswer().trim(),
                            difficulty);
                    importedFlashcards.add(flashcard);
                }
            }
            imported++;
        }

        return imported;
    }

    /**
     * Exports a single deck and its flashcards to a JSON file.
     *
     * Flow:
     *  1. Convert each Flashcard into a CardJson (question, answer, difficulty).
     *  2. Wrap everything into a DeckJson with the current timestamp as exported_at.
     *  3. Serialise the DeckJson to the target file using Gson pretty-printing.
     *
     * Throws CustomException if the file cannot be written.
     */
    public void exportDeckToFile(Deck deck, List<Flashcard> cards, File file) throws CustomException {
        // Build the list of card data objects
        List<CardJson> cardJsonList = new ArrayList<>();
        for (Flashcard card : cards) {
            cardJsonList.add(new CardJson(
                    card.getQuestion(),
                    card.getAnswer(),
                    card.getDifficulty()));
        }

        // Assemble the top-level deck object with an export timestamp
        DeckJson deckJson = new DeckJson(
                deck.getName(),
                deck.getDescription(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                cardJsonList);

        // Write the JSON to the chosen file; rethrow IO errors as CustomException
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(deckJson, writer);
        } catch (IOException e) {
            throw new CustomException("Failed to write export file: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Reads and parses a JSON file into a list of DeckJson objects.
     *
     * Supports two formats:
     *  - Multi-deck:  { "decks": [ {...}, {...} ] }
     *  - Single-deck: { "deck_name": "...", "cards": [...] }
     *
     * Throws CustomException for malformed JSON, unreadable files, or a
     * root element that is not a JSON object.
     */
    private List<DeckJson> parseFile(File file) throws CustomException {
        try (FileReader reader = new FileReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);

            // Root must be a JSON object, not an array or primitive
            if (!root.isJsonObject()) {
                throw new CustomException("Invalid JSON: root must be an object.");
            }
            JsonObject obj = root.getAsJsonObject();

            // Multi-deck format: { "decks": [...] }
            if (obj.has("decks")) {
                DeckJson[] arr = GSON.fromJson(obj.get("decks"), DeckJson[].class);
                return Arrays.asList(arr);
            }

            // Single-deck format: { "deck_name": ..., "cards": [...] }
            return List.of(GSON.fromJson(obj, DeckJson.class));

        } catch (JsonParseException e) {
            throw new CustomException("Malformed JSON file: " + e.getMessage());
        } catch (IOException e) {
            throw new CustomException("Could not read file: " + e.getMessage());
        }
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
}
