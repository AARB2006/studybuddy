package com.studyapp.view;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.studyapp.dao.impl.DeckDAOImpl;
import com.studyapp.dao.impl.FlashcardDAOImpl;
import com.studyapp.model.Deck;
import com.studyapp.model.Flashcard;
import com.studyapp.service.JsonImportExportService;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MyDeckPanel {

    private static final String PRIMARY_BLUE = "#2a548f";
    private static final String HEADER_BLUE  = "#41729f";
    private static final String BORDER_STYLE = "-fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;";

    // ── Data record ───────────────────────────────────────────────────────────

    public record DeckData(Deck deck, int cardCount, int progressPercent, List<Flashcard> cards) {}

    // ── Load from DB ──────────────────────────────────────────────────────────

    public static List<DeckData> loadFromDatabase() {
        DeckDAOImpl deckDao = new DeckDAOImpl();
        FlashcardDAOImpl cardDao = new FlashcardDAOImpl();
        List<Deck> decks = deckDao.getAllDecks();
        List<DeckData> result = new ArrayList<>();
        for (Deck deck : decks) {
            List<Flashcard> cards = cardDao.getByDeck(deck.getDeckID());
            result.add(new DeckData(deck, cards.size(), 0, cards));
        }
        return result;
    }

    // ── Panel builder ─────────────────────────────────────────────────────────

    public static VBox create(BorderPane mainLayout) {
        List<DeckData> decks = loadFromDatabase();

        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle(BORDER_STYLE);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        Label header = new Label("My Decks");
        header.setFont(Font.font("Serif", 32));
        header.setTextFill(Color.WHITE);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + HEADER_BLUE
                + "; -fx-background-radius: 8; -fx-padding: 10;");

        // ── Action row ────────────────────────────────────────────────────────
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        // "Import Decks" button — stacked ABOVE the "new" button
        Button importBtn = new Button("Import Decks");
        importBtn.setMaxWidth(Double.MAX_VALUE);
        importBtn.setFont(Font.font("Serif", 13));
        importBtn.setStyle("-fx-background-color: white; -fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: "
                + PRIMARY_BLUE + "; -fx-padding: 5 14; -fx-cursor: hand;");
        importBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select JSON File");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = chooser.showOpenDialog(mainLayout.getScene().getWindow());
            if (file != null) {
                try (FileReader reader = new FileReader(file)) {
                    new JsonImportExportService().importDecks(reader);
                    mainLayout.setCenter(MyDeckPanel.create(mainLayout));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // "new" button — opens Create Deck popup (storyboard 4)
        Button newBtn = new Button("new");
        newBtn.setMaxWidth(Double.MAX_VALUE);
        newBtn.setFont(Font.font("Serif", 13));
        newBtn.setStyle("-fx-background-color: white; -fx-border-color: #22c55e; -fx-border-radius: 20;"
                + " -fx-background-radius: 20; -fx-text-fill: #22c55e; -fx-padding: 5 14; -fx-cursor: hand;");
        newBtn.setOnAction(e -> showCreateDeckPopup(mainLayout));

        // Stack Import Decks above new in a VBox
        VBox leftBtns = new VBox(6);
        leftBtns.setAlignment(Pos.CENTER_LEFT);
        leftBtns.getChildren().addAll(importBtn, newBtn);

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setDisable(true);
        searchField.setPrefWidth(220);
        searchField.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 5;"
                + " -fx-background-radius: 5; -fx-padding: 5 10;");

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setFont(Font.font(16));

        Label sortLabel = new Label("sort by:");
        sortLabel.setFont(Font.font("Serif", 14));

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.setDisable(true);
        sortBox.setPrefWidth(130);

        actionRow.getChildren().addAll(leftBtns, leftSpacer,
                searchField, searchIcon, sortLabel, sortBox);

        // ── Deck list ─────────────────────────────────────────────────────────
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;"
                + " -fx-padding: 0; -fx-control-inner-background: white;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox deckList = new VBox(15);
        deckList.setPadding(new Insets(5, 15, 5, 5));
        deckList.setStyle("-fx-background-color: white;");

        if (decks.isEmpty()) {
            Label empty = new Label("No decks yet. Create one or import a JSON file.");
            empty.setFont(Font.font("Serif", 16));
            empty.setTextFill(Color.web("#6b7280"));
            deckList.getChildren().add(empty);
        } else {
            for (DeckData dd : decks) {
                deckList.getChildren().add(createDeckRow(dd, mainLayout));
            }
        }

        scrollPane.setContent(deckList);
        mainContent.getChildren().addAll(header, actionRow, scrollPane);
        wrapper.getChildren().add(mainContent);
        return wrapper;
    }

    // ── Create Deck popup (storyboard 4) ──────────────────────────────────────

    private static void showCreateDeckPopup(BorderPane mainLayout) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle("Create Deck");
        popup.setResizable(false);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #f0f4fc;");
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(340);

        Label title = new Label("Create Deck");
        title.setFont(Font.font("Serif", 26));
        title.setTextFill(Color.web(PRIMARY_BLUE));

        Label nameLabel = new Label("Enter Deck Name");
        nameLabel.setFont(Font.font("Serif", 15));
        nameLabel.setTextFill(Color.web(PRIMARY_BLUE));

        TextField nameField = new TextField();
        nameField.setPrefWidth(280);
        nameField.setStyle("-fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        Label descLabel = new Label("Enter Description");
        descLabel.setFont(Font.font("Serif", 15));
        descLabel.setTextFill(Color.web(PRIMARY_BLUE));

        TextArea descArea = new TextArea();
        descArea.setPrefWidth(280);
        descArea.setPrefHeight(120);
        descArea.setStyle("-fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);
        errorLabel.setFont(Font.font("Serif", 13));

        Button createBtn = new Button("CREATE");
        createBtn.setFont(Font.font("Serif", 15));
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setStyle("-fx-background-color: #d0daf5; -fx-border-radius: 20;"
                + " -fx-background-radius: 20; -fx-padding: 12; -fx-cursor: hand;");

        createBtn.setOnAction(ev -> {
            String name = nameField.getText().trim();
            if (name.isBlank()) {
                errorLabel.setText("Deck name cannot be empty.");
                return;
            }
            Deck newDeck = new Deck();
            newDeck.setName(name);
            newDeck.setDescription(descArea.getText().trim());
            newDeck.setCreatedAt(java.time.LocalDateTime.now());
            try {
                new DeckDAOImpl().insert(newDeck);
                popup.close();
                mainLayout.setCenter(MyDeckPanel.create(mainLayout));
            } catch (Exception ex) {
                errorLabel.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        root.getChildren().addAll(title, nameLabel, nameField, descLabel, descArea, errorLabel, createBtn);

        popup.setScene(new Scene(root));
        popup.showAndWait();
    }

    // ── Deck row ──────────────────────────────────────────────────────────────

    private static HBox createDeckRow(DeckData dd, BorderPane mainLayout) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(15));
        row.setStyle("-fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: white;");

        VBox nameInfo = new VBox(3);
        HBox.setHgrow(nameInfo, Priority.ALWAYS);

        Label idLbl = new Label("ID: " + dd.deck().getDeckID());
        idLbl.setFont(Font.font("Serif", 12));
        idLbl.setTextFill(Color.web("#6b7280"));

        Label nameLbl = new Label(dd.deck().getName());
        nameLbl.setFont(Font.font("Serif", 20));
        nameLbl.setTextFill(Color.BLACK);

        nameInfo.getChildren().addAll(idLbl, nameLbl);

        VBox stats = new VBox(4);
        stats.setAlignment(Pos.CENTER_LEFT);

        Label cardsLbl = new Label("Cards: " + dd.cardCount());
        cardsLbl.setFont(Font.font("Serif", 13));
        cardsLbl.setTextFill(Color.web("#475569"));

        Label progressLbl = new Label("Progress: " + dd.progressPercent() + "%");
        progressLbl.setFont(Font.font("Serif", 13));
        progressLbl.setTextFill(Color.web("#475569"));

        stats.getChildren().addAll(cardsLbl, progressLbl);

        Button selectBtn = new Button("SELECT");
        selectBtn.setFont(Font.font("Serif", 14));
        String selDefault = "-fx-background-color: #e6eaf5; -fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-text-fill: black; -fx-padding: 10 20; -fx-cursor: hand;";
        String selHover = "-fx-background-color: " + PRIMARY_BLUE + "; -fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-text-fill: white; -fx-padding: 10 20; -fx-cursor: hand;";
        selectBtn.setStyle(selDefault);
        selectBtn.setOnMouseEntered(e -> selectBtn.setStyle(selHover));
        selectBtn.setOnMouseExited(e -> selectBtn.setStyle(selDefault));
        selectBtn.setOnAction(e -> DeckDetailPanel.show(mainLayout, dd));

        row.getChildren().addAll(nameInfo, stats, selectBtn);
        return row;
    }
}