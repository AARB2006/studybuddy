package com.studyapp.view;

import java.util.List;

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

public class AllCardsPanel {

    private static final String PRIMARY_BLUE = "#2a548f";
    private static final String HEADER_BLUE  = "#41729f";
    private static final String BORDER_STYLE = "-fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;";

    /** Called from the global sidebar "All Cards" nav button (no deck context). */
    public static VBox create(BorderPane mainLayout) {
        return create(mainLayout, null);
    }

    /** Legacy overload; kept so existing callers compile. */
    public static VBox create(BorderPane mainLayout, com.studyapp.model.Deck deck) {
        return create(mainLayout, null, null);
    }

    /**
     * Called from DeckDetailPanel's "Cards" button.
     * @param dd          the deck whose cards to show
     * @param savedSidebar the global sidebar to restore when going back
     */
    public static void show(BorderPane mainLayout, DeckData dd, Node savedSidebar) {
        mainLayout.setLeft(buildSidebar(mainLayout, dd, savedSidebar));
        mainLayout.setCenter(buildContent(mainLayout, dd, savedSidebar));
    }

    // ── Internal: full show with sidebar+content ──────────────────────────────

    private static VBox create(BorderPane mainLayout, DeckData dd, Node savedSidebar) {
        if (dd != null) {
            show(mainLayout, dd, savedSidebar);
            return new VBox(); // content set directly on mainLayout
        }
        // Global "All Cards" view — show all cards from DB without deck filter
        mainLayout.setLeft(mainLayout.getLeft()); // keep existing sidebar
        return buildContent(mainLayout, null, null);
    }

    // ── Sidebar (storyboard 3 left panel) ────────────────────────────────────

    private static VBox buildSidebar(BorderPane mainLayout, DeckData dd, Node savedSidebar) {
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

        // Edit — disabled
        Button editBtn = createDisabledBtn("Edit");

        // Cards — ACTIVE (current screen)
        Button cardsBtn = new Button("Cards");
        cardsBtn.setMaxWidth(Double.MAX_VALUE);
        cardsBtn.setFont(Font.font("Serif", 16));
        cardsBtn.setStyle("-fx-background-color: #e6eaf5; -fx-text-fill: black; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-padding: 10 15;");
        cardsBtn.setDisable(true); // already on this screen

        // Study — disabled
        Button studyBtn = createDisabledBtn("Study");

        // Import — disabled
        Button importBtn = createDisabledBtn("Import");

        // Export — disabled
        Button exportBtn = createDisabledBtn("Export");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Back → returns to DeckDetailPanel
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
        backBtn.setOnAction(e -> DeckDetailPanel.show(mainLayout, dd));

        buttonBox.getChildren().addAll(editBtn, cardsBtn, studyBtn, importBtn, exportBtn, spacer, backBtn);
        sidebar.getChildren().addAll(title, buttonBox);
        return sidebar;
    }

    private static Button createDisabledBtn(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Serif", 16));
        btn.setDisable(true);
        btn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-border-color: "
                + PRIMARY_BLUE + "; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10 15;");
        return btn;
    }

    // ── Content (card list) ───────────────────────────────────────────────────

    private static VBox buildContent(BorderPane mainLayout, DeckData dd, Node savedSidebar) {
        List<Flashcard> cards;
        String headerText;
        if (dd != null) {
            cards = dd.cards();
            headerText = dd.deck().getName();
        } else {
            cards = new com.studyapp.dao.impl.FlashcardDAOImpl().getAllFlashcards();
            headerText = "All Cards";
        }

        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle(BORDER_STYLE);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        Label header = new Label(headerText);
        header.setFont(Font.font("Serif", 32));
        header.setTextFill(Color.WHITE);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + HEADER_BLUE
                + "; -fx-background-radius: 8; -fx-padding: 10;");

        // Action row: new | search | search by
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button newCardBtn = new Button("new");
        newCardBtn.setFont(Font.font("Serif", 14));
        newCardBtn.setDisable(true);
        newCardBtn.setStyle("-fx-background-color: white; -fx-border-color: #22c55e; -fx-border-radius: 20;"
                + " -fx-background-radius: 20; -fx-text-fill: #22c55e; -fx-padding: 5 18;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setDisable(true);
        searchField.setPrefWidth(200);
        searchField.setStyle("-fx-border-color: #cbd5e1; -fx-border-radius: 5;"
                + " -fx-background-radius: 5; -fx-padding: 5 10;");

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setFont(Font.font(16));

        Label searchByLabel = new Label("search by:");
        searchByLabel.setFont(Font.font("Serif", 14));

        ComboBox<String> searchByBox = new ComboBox<>();
        searchByBox.setDisable(true);
        searchByBox.setPrefWidth(130);

        actionRow.getChildren().addAll(newCardBtn, spacer, searchField, searchIcon, searchByLabel, searchByBox);

        // Card list
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;"
                + " -fx-padding: 0; -fx-control-inner-background: white;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox cardsBox = new VBox(15);
        cardsBox.setPadding(new Insets(5, 15, 5, 5));
        cardsBox.setStyle("-fx-background-color: white;");

        if (cards.isEmpty()) {
            Label empty = new Label("No cards in this deck yet.");
            empty.setFont(Font.font("Serif", 16));
            empty.setTextFill(Color.web("#6b7280"));
            cardsBox.getChildren().add(empty);
        } else {
            for (Flashcard card : cards) {
                cardsBox.getChildren().add(createCardRow(card, mainLayout, dd, savedSidebar));
            }
        }

        scrollPane.setContent(cardsBox);
        mainContent.getChildren().addAll(header, actionRow, scrollPane);
        wrapper.getChildren().add(mainContent);
        return wrapper;
    }

    // ── Card row with SELECT button → storyboard 5 ───────────────────────────

    private static HBox createCardRow(Flashcard card, BorderPane mainLayout, DeckData dd, Node savedSidebar) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.setStyle("-fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: white;");

        Label qLbl = new Label("Q. " + card.getQuestion());
        qLbl.setFont(Font.font("Serif", 15));
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
        selectBtn.setOnMouseExited(e -> selectBtn.setStyle(selDefault));
        // SELECT → storyboard 5 (CardDetailPanel)
        selectBtn.setOnAction(e -> CardDetailPanel.show(mainLayout, card, dd, savedSidebar));

        row.getChildren().addAll(qLbl, selectBtn);
        return row;
    }
}