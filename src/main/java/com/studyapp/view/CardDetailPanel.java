package com.studyapp.view;

import java.time.format.DateTimeFormatter;

import com.studyapp.model.Flashcard;
import com.studyapp.view.MyDeckPanel.DeckData;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CardDetailPanel {

    private static final String PRIMARY_BLUE  = "#2a548f";
    private static final String HEADER_BLUE   = "#41729f";
    private static final String BORDER_STYLE  = "-fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;";
    private static final String INACTIVE_STYLE = "-fx-background-color: white; -fx-text-fill: black;"
            + " -fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 15;";

    /**
     * Shows storyboard (5): card detail view.
     *
     * @param mainLayout   the root BorderPane
     * @param card         the card to display
     * @param dd           the parent DeckData (used for going back)
     * @param savedSidebar the global sidebar saved before entering the deck flow
     */
    public static void show(BorderPane mainLayout, Flashcard card, DeckData dd, Node savedSidebar) {
        mainLayout.setLeft(buildSidebar(mainLayout, card, dd, savedSidebar));
        mainLayout.setCenter(buildContent(card));
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private static VBox buildSidebar(BorderPane mainLayout, Flashcard card,
                                     DeckData dd, Node savedSidebar) {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(250);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(250);
        sidebar.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Study Assistant\nApplication");
        title.setFont(Font.font("Serif", 18));
        title.setTextFill(Color.web(PRIMARY_BLUE));
        VBox.setMargin(title, new Insets(0, 0, 10, 0));

        VBox buttonBox = new VBox(15);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setStyle(BORDER_STYLE);
        VBox.setVgrow(buttonBox, Priority.ALWAYS);

        // Edit — disabled for now
        Button editBtn = createDisabledBtn("Edit");

        // Delete — disabled for now
        Button deleteBtn = new Button("DELETE");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setFont(Font.font("Serif", 16));
        deleteBtn.setDisable(true);
        deleteBtn.setStyle("-fx-background-color: white; -fx-text-fill: #cc0000;"
                + " -fx-border-color: #cc0000; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Back → returns to AllCardsPanel for this deck
        Button backBtn = new Button("BACK");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setFont(Font.font("Serif", 16));
        String backDefault = "-fx-background-color: #ff9999; -fx-text-fill: black; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15; -fx-cursor: hand;";
        String backHover = "-fx-background-color: #ff6666; -fx-text-fill: white; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15; -fx-cursor: hand;";
        backBtn.setStyle(backDefault);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backHover));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(backDefault));
        backBtn.setOnAction(e -> AllCardsPanel.show(mainLayout, dd, savedSidebar));

        buttonBox.getChildren().addAll(editBtn, deleteBtn, spacer, backBtn);
        sidebar.getChildren().addAll(title, buttonBox);
        return sidebar;
    }

    private static Button createDisabledBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Serif", 16));
        btn.setDisable(true);
        btn.setStyle(INACTIVE_STYLE);
        return btn;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private static VBox buildContent(Flashcard card) {
        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle(BORDER_STYLE);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Front (question) — large teal card
        Label frontLbl = new Label("Front: " + card.getQuestion());
        frontLbl.setFont(Font.font("Serif", 20));
        frontLbl.setTextFill(Color.WHITE);
        frontLbl.setWrapText(true);
        frontLbl.setMaxWidth(Double.MAX_VALUE);
        frontLbl.setAlignment(Pos.TOP_LEFT);
        frontLbl.setPadding(new Insets(20));
        frontLbl.setStyle("-fx-background-color: " + HEADER_BLUE
                + "; -fx-background-radius: 10; -fx-min-height: 100;");

        // Back (answer)
        Label backLbl = new Label("Back: " + card.getAnswer());
        backLbl.setFont(Font.font("Serif", 18));
        backLbl.setWrapText(true);
        backLbl.setMaxWidth(Double.MAX_VALUE);
        backLbl.setPadding(new Insets(15));
        backLbl.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 5;"
                + " -fx-background-color: #f8fafc; -fx-background-radius: 5;");

        // Info box
        VBox infoBox = new VBox(8);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle(BORDER_STYLE);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String deckName = card.getDeck() != null ? card.getDeck().getName() : "—";
        String difficulty = card.getDifficulty() != null ? card.getDifficulty() : "—";
        String createdAt = card.getCreatedAt() != null ? card.getCreatedAt().format(fmt) : "—";

        infoBox.getChildren().addAll(
                infoLabel("ID: " + card.getCardID()),
                infoLabel("Deck: " + deckName),
                infoLabel("Difficulty: " + difficulty),
                infoLabel("Created at: " + createdAt));

        mainContent.getChildren().addAll(frontLbl, backLbl, infoBox);
        wrapper.getChildren().add(mainContent);
        return wrapper;
    }

    private static Label infoLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Serif", 14));
        return lbl;
    }
}