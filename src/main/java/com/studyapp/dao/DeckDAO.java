package com.studyapp.dao;

import java.sql.SQLException;
import java.util.List;

import com.studyapp.model.Deck;

public interface DeckDAO {
    void insert(Deck deck) throws SQLException;
    void update(Deck deck) throws SQLException;
    void delete(int deckID) throws SQLException;
    Deck findByID(int deckID);
    List<Deck> getAllDecks();
    int getLastID();

    /**
     * Inserts a new deck using auto-increment id and returns the generated id.
     * Throws SQLException (e.g. duplicate name) that callers can handle.
     */
    int insertAutoAndGetID(String name, String description) throws SQLException;
}