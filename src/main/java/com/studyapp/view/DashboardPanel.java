package com.studyapp.view;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import com.studyapp.db.DatabaseConnection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

public class DashboardPanel {

    private static final String PRIMARY_BLUE    = "#2a548f";
    private static final String HEADER_BLUE     = "#41729f";
    private static final String BORDER_STYLE    = "-fx-border-color: " + PRIMARY_BLUE
            + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;";
    private static final String EASY_PIE_COLOR   = "#16a34a";
    private static final String MEDIUM_PIE_COLOR = "#d97706";
    private static final String HARD_PIE_COLOR   = "#dc2626";

    // ── Stats container ───────────────────────────────────────────────────────

    private static class DashStats {
        int    totalDecks  = 0;
        int    totalCards  = 0;
        int    totalReviews = 0;
        String accuracyStr  = "--";
        String studyTimeStr = "--";
        int    easyCount    = 0;
        int    mediumCount  = 0;
        int    hardCount    = 0;
    }

    private static DashStats loadStats() {
        DashStats s = new DashStats();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement  stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Deck")) {
                if (rs.next()) s.totalDecks = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Card")) {
                if (rs.next()) s.totalCards = rs.getInt(1);
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*), "
                    + "COALESCE(ROUND(SUM(is_correct) * 100.0 / NULLIF(COUNT(*), 0), 1), 0) "
                    + "FROM Card_Review")) {
                if (rs.next()) {
                    s.totalReviews = rs.getInt(1);
                    s.accuracyStr  = s.totalReviews > 0 ? rs.getString(2) + "%" : "--";
                }
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, started_at, ended_at)), 0) "
                    + "FROM Study_Session WHERE ended_at IS NOT NULL")) {
                if (rs.next()) {
                    long mins = rs.getLong(1);
                    s.studyTimeStr = mins >= 60
                            ? String.format("%.1f hrs", mins / 60.0)
                            : (mins > 0 ? mins + " min" : "--");
                }
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COALESCE(difficulty, 'Unset'), COUNT(*) "
                    + "FROM Card GROUP BY difficulty")) {
                while (rs.next()) {
                    String diff = rs.getString(1);
                    int    cnt  = rs.getInt(2);
                    switch (diff) {
                        case "Easy"   -> s.easyCount   = cnt;
                        case "Medium" -> s.mediumCount = cnt;
                        case "Hard"   -> s.hardCount   = cnt;
                    }
                }
            }
        } catch (Exception ignored) {
            // DB not yet connected — stats stay at defaults
        }
        return s;
    }

    // ── Panel builder ─────────────────────────────────────────────────────────

    public static VBox create(BorderPane mainLayout) {
        DashStats stats = loadStats();

        VBox wrapper = new VBox();
        wrapper.setPadding(new Insets(20));
        wrapper.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);

        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle(BORDER_STYLE);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        Label dashHeader = createHeaderLabel("Dashboard");

        HBox statsRow = new HBox(20);
        statsRow.getChildren().addAll(
                createStatCard("Accuracy",       stats.accuracyStr),
                createStatCard("Cards Reviewed", String.valueOf(stats.totalReviews)),
                createStatCard("Study Time",     stats.studyTimeStr));
        for (Node node : statsRow.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }

        HBox bottomContent = new HBox(20);
        VBox.setVgrow(bottomContent, Priority.ALWAYS);

        // Left: summary panel
        VBox flowPanel = new VBox(15);
        flowPanel.setStyle(BORDER_STYLE);
        flowPanel.setPadding(new Insets(18));
        flowPanel.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(flowPanel, Priority.ALWAYS);

        Label flowHeader = new Label("Summary");
        flowHeader.setFont(Font.font("Serif", 22));
        flowHeader.setTextFill(Color.web(PRIMARY_BLUE));

        Label summaryText = new Label(
                "Decks: " + stats.totalDecks + "     Cards: " + stats.totalCards);
        summaryText.setFont(Font.font("Serif", 16));
        summaryText.setWrapText(true);
        summaryText.setTextFill(Color.web("#475569"));

        VBox flowSteps = new VBox(10);
        flowSteps.getChildren().addAll(
                createFlowStep("1", "Setup Panel"),
                createFlowStep("2", "Dashboard"),
                createFlowStep("3", "My Decks → Deck Detail → Cards"),
                createFlowStep("4", "Card Detail"));

        Button nextButton = new Button("Open My Decks");
        String buttonStyle = "-fx-background-color: #e6eaf5; -fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: black;"
                + " -fx-padding: 10 20; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #d0dcf5; -fx-border-color: " + PRIMARY_BLUE
                + "; -fx-border-radius: 8; -fx-background-radius: 8; -fx-text-fill: black;"
                + " -fx-padding: 10 20; -fx-cursor: hand;";
        nextButton.setStyle(buttonStyle);
        nextButton.setOnMouseEntered(e -> nextButton.setStyle(hoverStyle));
        nextButton.setOnMouseExited(e -> nextButton.setStyle(buttonStyle));
        nextButton.setOnAction(e -> {
            MainFrame.activateMyDecks();
            mainLayout.setCenter(MyDeckPanel.create(mainLayout));
        });

        flowPanel.getChildren().addAll(flowHeader, summaryText, flowSteps, nextButton);

        // Right: difficulty pie chart
        VBox chartBox = new VBox(15);
        chartBox.setStyle(BORDER_STYLE);
        chartBox.setPadding(new Insets(15));
        chartBox.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(chartBox, Priority.SOMETIMES);
        chartBox.setMinWidth(350);

        Label chartTitle = new Label("Card Difficulty Mix");
        chartTitle.setFont(Font.font("SansSerif", 16));
        chartTitle.setTextFill(Color.BLACK);

        // Use a tiny nonzero fallback so the PieChart renders when counts are 0
        PieChart.Data easyData   = new PieChart.Data("Easy",   stats.easyCount   > 0 ? stats.easyCount   : 0.001);
        PieChart.Data mediumData = new PieChart.Data("Medium", stats.mediumCount > 0 ? stats.mediumCount : 0.001);
        PieChart.Data hardData   = new PieChart.Data("Hard",   stats.hardCount   > 0 ? stats.hardCount   : 0.001);

        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(easyData, mediumData, hardData);
        bindPieSliceColor(easyData,   EASY_PIE_COLOR);
        bindPieSliceColor(mediumData, MEDIUM_PIE_COLOR);
        bindPieSliceColor(hardData,   HARD_PIE_COLOR);

        HBox customLegend = new HBox(15);
        customLegend.setAlignment(Pos.CENTER);
        customLegend.getChildren().addAll(
                createLegendItem("Easy",   EASY_PIE_COLOR),
                createLegendItem("Medium", MEDIUM_PIE_COLOR),
                createLegendItem("Hard",   HARD_PIE_COLOR));

        PieChart chart = new PieChart(pieChartData);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(false);
        chart.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(chart, Priority.ALWAYS);

        chartBox.getChildren().addAll(chartTitle, customLegend, chart);

        bottomContent.getChildren().addAll(flowPanel, chartBox);
        mainContent.getChildren().addAll(dashHeader, statsRow, bottomContent);
        wrapper.getChildren().add(mainContent);
        return wrapper;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static HBox createFlowStep(String stepNumber, String label) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox markerWrap = new VBox(new Label(stepNumber));
        markerWrap.setAlignment(Pos.CENTER);
        markerWrap.setPrefSize(24, 24);
        markerWrap.setMaxSize(24, 24);
        markerWrap.setStyle("-fx-background-color: " + PRIMARY_BLUE + "; -fx-background-radius: 999;");
        ((Label) markerWrap.getChildren().get(0)).setTextFill(Color.WHITE);
        ((Label) markerWrap.getChildren().get(0)).setFont(Font.font("SansSerif", 12));

        Label text = new Label(label);
        text.setFont(Font.font("Serif", 16));
        text.setTextFill(Color.BLACK);

        row.getChildren().addAll(markerWrap, text);
        return row;
    }

    private static void bindPieSliceColor(PieChart.Data data, String color) {
        data.nodeProperty().addListener((obs, oldNode, newNode) -> applyPieSliceColor(newNode, color));
        applyPieSliceColor(data.getNode(), color);
    }

    private static void applyPieSliceColor(Node node, String color) {
        if (node != null) node.setStyle("-fx-pie-color: " + color + ";");
    }

    private static HBox createLegendItem(String name, String colorHex) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER);
        Circle dot = new Circle(6, Color.web(colorHex));
        Label lbl = new Label(name);
        lbl.setFont(Font.font("SansSerif", 13));
        lbl.setTextFill(Color.web("#333333"));
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    private static Label createHeaderLabel(String text) {
        Label header = new Label(text);
        header.setFont(Font.font("Serif", 32));
        header.setTextFill(Color.WHITE);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);
        header.setStyle("-fx-background-color: " + HEADER_BLUE
                + "; -fx-background-radius: 8; -fx-padding: 10;");
        return header;
    }

    private static VBox createStatCard(String title, String value) {
        VBox box = new VBox(5);
        box.setStyle(BORDER_STYLE);
        box.setPadding(new Insets(15, 20, 15, 20));
        box.setMaxWidth(Double.MAX_VALUE);

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Serif", 18));
        titleLbl.setTextFill(Color.BLACK);

        Label valLbl = new Label(value);
        valLbl.setFont(Font.font("Serif", 22));
        valLbl.setTextFill(Color.web(PRIMARY_BLUE));

        box.getChildren().addAll(titleLbl, valLbl);
        return box;
    }
}