package com.studyapp.view;

import java.util.List;

import com.studyapp.dao.impl.FlashcardDAOImpl;
import com.studyapp.model.Flashcard;
import com.studyapp.view.MyDeckPanel.DeckData;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Storyboard 3 — per-deck card list with SELECT buttons.
 */
public class DeckCardsPanel {

    private static final String PRIMARY_BLUE  = "#2a548f";
    private static final String HEADER_BLUE   = "#41729f";
    private static final String BORDER_STYLE  =
            "-fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;";
    private static final String INACTIVE_STYLE =
            "-fx-background-color: white; -fx-text-fill: black;"
            + " -fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 15;";
    private static final String ACTIVE_STYLE =
            "-fx-background-color: #e6eaf5; -fx-text-fill: black;"
            + " -fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 15;";

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void show(BorderPane mainLayout, DeckData dd, Node outerSidebar) {
        // Re-query from DB for real-time data
        List<Flashcard> liveCards;
        try {
            liveCards = new FlashcardDAOImpl().getByDeck(dd.deck().getDeckID()); // fixed: was getCardsByDeckID
        } catch (Exception e) {
            liveCards = dd.cards();
        }
        DeckData liveDd = new DeckData(dd.deck(), liveCards.size(), dd.progressPercent(), liveCards);

        mainLayout.setLeft(buildSidebar(mainLayout, liveDd, outerSidebar));
        mainLayout.setCenter(buildContent(mainLayout, liveDd, outerSidebar));
    }

    // ── Inner sidebar ─────────────────────────────────────────────────────────

    private static VBox buildSidebar(BorderPane mainLayout, DeckData dd, Node outerSidebar) {
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

        Button editBtn   = createDisabledBtn("Edit");
        Button cardsBtn  = createActiveBtn("Cards");   // currently active view
        Button studyBtn  = createDisabledBtn("Study");
        Button importBtn = createDisabledBtn("Import");
        Button exportBtn = createDisabledBtn("Export");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("BACK");
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setFont(Font.font("Serif", 16));
        String backDefault =
                "-fx-background-color: #ff9999; -fx-text-fill: black; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15; -fx-cursor: hand;";
        String backHover =
                "-fx-background-color: #ff6666; -fx-text-fill: white; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15; -fx-cursor: hand;";
        backBtn.setStyle(backDefault);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backHover));
        backBtn.setOnMouseExited(e  -> backBtn.setStyle(backDefault));
        backBtn.setOnAction(e -> DeckDetailPanel.show(mainLayout, dd)); // fixed: removed extra outerSidebar param

        buttonBox.getChildren().addAll(editBtn, cardsBtn, studyBtn, importBtn, exportBtn, spacer, backBtn);
        sidebar.getChildren().addAll(title, buttonBox);
        return sidebar;
    }

    // ── Main content ──────────────────────────────────────────────────────────

    private static VBox buildContent(BorderPane mainLayout, DeckData dd, Node outerSidebar) {
        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle(BORDER_STYLE);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // Header
        Label header = new Label(dd.deck().getName());
        header.setFont(Font.font("Serif", 32));
        header.setTextFill(Color.WHITE);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + HEADER_BLUE
                + "; -fx-background-radius: 8; -fx-padding: 10;");

        // Action row
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button newCardBtn = new Button("new");
        newCardBtn.setDisable(true);
        newCardBtn.setFont(Font.font("Serif", 14));
        newCardBtn.setStyle(
                "-fx-background-color: white; -fx-border-color: #22c55e; -fx-border-radius: 20;"
                + " -fx-background-radius: 20; -fx-text-fill: #22c55e; -fx-padding: 5 18;");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setDisable(true);
        searchField.setPrefWidth(220);
        searchField.setStyle(
                "-fx-border-color: #cbd5e1; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 5 10;");

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setFont(Font.font(16));

        Label searchByLbl = new Label("search by:");
        searchByLbl.setFont(Font.font("Serif", 14));

        ComboBox<String> sortBox = new ComboBox<>();
        sortBox.setDisable(true);
        sortBox.setPrefWidth(130);

        actionRow.getChildren().addAll(newCardBtn, leftSpacer, searchField, searchIcon, searchByLbl, sortBox);

        // Cards list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
                "-fx-background-color: transparent; -fx-background-insets: 0;"
                + " -fx-padding: 0; -fx-control-inner-background: white;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox cardList = new VBox(12);
        cardList.setPadding(new Insets(5, 15, 5, 5));
        cardList.setStyle("-fx-background-color: white;");

        if (dd.cards().isEmpty()) {
            Label empty = new Label("No cards in this deck yet.");
            empty.setFont(Font.font("Serif", 15));
            empty.setTextFill(Color.web("#6b7280"));
            cardList.getChildren().add(empty);
        } else {
            for (Flashcard card : dd.cards()) {
                cardList.getChildren().add(createCardRow(mainLayout, card, dd, outerSidebar));
            }
        }

        scrollPane.setContent(cardList);
        mainContent.getChildren().addAll(header, actionRow, scrollPane);
        wrapper.getChildren().add(mainContent);
        return wrapper;
    }

    // ── Card row ──────────────────────────────────────────────────────────────

    private static HBox createCardRow(BorderPane mainLayout, Flashcard card,
                                       DeckData dd, Node outerSidebar) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setStyle("-fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: white;");

        Label qLbl = new Label("Q. " + card.getQuestion());
        qLbl.setFont(Font.font("Serif", 15));
        qLbl.setTextFill(Color.BLACK);
        qLbl.setWrapText(true);
        HBox.setHgrow(qLbl, Priority.ALWAYS);

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
        selectBtn.setOnMouseExited(e  -> selectBtn.setStyle(selDefault));
        selectBtn.setOnAction(e -> CardDetailPanel.show(mainLayout, card, dd, outerSidebar));

        row.getChildren().addAll(qLbl, selectBtn);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Button createDisabledBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Serif", 16));
        btn.setDisable(true);
        btn.setStyle(INACTIVE_STYLE);
        return btn;
    }

    private static Button createActiveBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Serif", 16));
        btn.setStyle(ACTIVE_STYLE);
        return btn;
    }
}