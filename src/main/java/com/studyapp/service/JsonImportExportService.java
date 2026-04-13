package com.studyapp.service;

import java.io.Reader;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.studyapp.dao.impl.DeckDAOImpl;
import com.studyapp.dao.impl.FlashcardDAOImpl;
import com.studyapp.model.Deck;
import com.studyapp.model.Flashcard;

public class JsonImportExportService {

    /**
     * Parses a JSON reader containing one or many DeckJson objects,
     * persists each deck + its cards to the database (skipping decks whose
     * name already exists), and returns the list of newly inserted Deck objects.
     */
    public List<Deck> importDecks(Reader reader) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<DeckJson>>() {}.getType();
        List<DeckJson> deckJsons = gson.fromJson(reader, listType);

        DeckDAOImpl deckDao = new DeckDAOImpl();
        FlashcardDAOImpl cardDao = new FlashcardDAOImpl();
        List<Deck> imported = new ArrayList<>();

        if (deckJsons == null) return imported;

        for (DeckJson dj : deckJsons) {
            if (dj.deck_name == null || dj.deck_name.isBlank()) continue;

            // Check if a deck with this name already exists
            boolean exists = deckDao.getAllDecks().stream()
                    .anyMatch(d -> d.getName().equalsIgnoreCase(dj.deck_name));
            if (exists) continue;

            Deck deck = new Deck();
            deck.setName(dj.deck_name);
            deck.setDescription(dj.description != null ? dj.description : "");
            deck.setCreatedAt(LocalDateTime.now());

            try {
                deckDao.insert(deck);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }

            // Retrieve the auto-assigned ID
            Deck saved = deckDao.getAllDecks().stream()
                    .filter(d -> d.getName().equalsIgnoreCase(dj.deck_name))
                    .findFirst().orElse(null);
            if (saved == null) continue;

            if (dj.cards != null) {
                for (CardJson cj : dj.cards) {
                    Flashcard card = new Flashcard();
                    card.setDeck(saved);
                    card.setQuestion(cj.question);
                    card.setAnswer(cj.answer);
                    card.setDifficulty(null);
                    card.setCreatedAt(LocalDateTime.now());
                    try {
                        cardDao.insert(card);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            imported.add(saved);
        }
        return imported;
    }
}