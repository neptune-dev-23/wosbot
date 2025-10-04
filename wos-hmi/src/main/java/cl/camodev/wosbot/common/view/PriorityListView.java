package cl.camodev.wosbot.common.view;

import cl.camodev.wosbot.ot.DTOPriorityItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic custom control to manage priority lists.
 * Can be used for purchase priorities, tasks, events, etc.
 *
 * Features:
 * - Enable/Disable items
 * - Sort by priority (1 = highest priority)
 * - Move items up/down with buttons
 * - Drag and drop items to reorder
 */
public class PriorityListView extends HBox {

    private static final DataFormat PRIORITY_DATA_FORMAT = new DataFormat("application/x-java-priority-item");
    private final ListView<DTOPriorityItem> listView;
    private final ObservableList<DTOPriorityItem> items;
    private Runnable onChangeCallback;

    public PriorityListView() {
        this.items = FXCollections.observableArrayList();
        this.listView = new ListView<>(items);

        setupListView();
        setupLayout();
        applyDarkStyles();
    }

    private void setupListView() {
        listView.setCellFactory(lv -> new PriorityItemCell());
        listView.setPrefHeight(200);
        listView.setFixedCellSize(32); // Smaller cell height
    }

    private void setupLayout() {
        HBox.setHgrow(listView, Priority.ALWAYS);

        // Control buttons in a vertical box on the right
        VBox controls = new VBox(5);
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0, 0, 0, 5));

        Button moveUpBtn = new Button("↑");
        Button moveDownBtn = new Button("↓");

        // Make buttons smaller and square
        moveUpBtn.setPrefSize(35, 35);
        moveDownBtn.setPrefSize(35, 35);
        moveUpBtn.setMinSize(35, 35);
        moveDownBtn.setMinSize(35, 35);

        moveUpBtn.setOnAction(e -> moveSelectedItem(-1));
        moveDownBtn.setOnAction(e -> moveSelectedItem(1));

        controls.getChildren().addAll(moveUpBtn, moveDownBtn);

        this.getChildren().addAll(listView, controls);
    }

    /**
     * Apply dark theme styles to the list view
     */
    private void applyDarkStyles() {
        // Dark background for the list
        listView.setStyle(
                "-fx-background-color: #2b2b2b;" +
                        "-fx-border-color: #3c3f41;" +
                        "-fx-border-width: 1px;"
        );
    }

    /**
     * Sets the items in the list
     */
    public void setItems(List<DTOPriorityItem> priorities) {
        items.clear();
        if (priorities != null && !priorities.isEmpty()) {
            items.addAll(priorities);
            sortByPriority();
        }
    }

    /**
     * Gets the current items
     */
    public List<DTOPriorityItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Moves an item up or down
     */
    private void moveSelectedItem(int direction) {
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        int newIndex = selectedIndex + direction;
        if (newIndex < 0 || newIndex >= items.size()) {
            return;
        }

        DTOPriorityItem item = items.get(selectedIndex);
        items.remove(selectedIndex);
        items.add(newIndex, item);

        updatePriorities();
        listView.getSelectionModel().select(newIndex);

        if (onChangeCallback != null) {
            onChangeCallback.run();
        }
    }

    /**
     * Updates priorities based on list order
     */
    private void updatePriorities() {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPriority(i + 1);
        }
    }

    /**
     * Sorts items by priority
     */
    private void sortByPriority() {
        FXCollections.sort(items, (p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()));
    }

    /**
     * Sets a callback that will be executed when there are changes
     */
    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }

    /**
     * Converts the list to String to store in configuration
     * Format: "item1:priority1:enabled1|item2:priority2:enabled2|..."
     */
    public String toConfigString() {
        return items.stream()
                .map(DTOPriorityItem::toConfigString)
                .collect(Collectors.joining("|"));
    }

    /**
     * Loads the list from a configuration String
     */
    public void fromConfigString(String configString) {
        items.clear();

        if (configString == null || configString.trim().isEmpty()) {
            return;
        }

        String[] itemStrings = configString.split("\\|");
        for (String itemString : itemStrings) {
            DTOPriorityItem priority = DTOPriorityItem.fromConfigString(itemString);
            if (priority != null) {
                items.add(priority);
            }
        }

        sortByPriority();
    }

    /**
     * Custom cell to display each item with drag and drop support
     */
    private class PriorityItemCell extends ListCell<DTOPriorityItem> {

        private final HBox content;
        private final CheckBox enabledCheckBox;
        private final Label priorityLabel;
        private final Label nameLabel;
        private final Label dragIndicator;

        public PriorityItemCell() {
            content = new HBox(8);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(3, 8, 3, 8));

            // Visual drag indicator
            dragIndicator = new Label("☰");
            dragIndicator.setStyle("-fx-font-size: 14px; -fx-text-fill: #808080; -fx-cursor: move;");
            dragIndicator.setMinWidth(18);

            enabledCheckBox = new CheckBox();
            priorityLabel = new Label();
            priorityLabel.setMinWidth(25);
            priorityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #bbbbbb;");

            nameLabel = new Label();
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc;");
            HBox.setHgrow(nameLabel, Priority.ALWAYS);

            content.getChildren().addAll(dragIndicator, enabledCheckBox, priorityLabel, nameLabel);

            // Apply dark theme to the cell
            setStyle(
                    "-fx-background-color: #2b2b2b;" +
                            "-fx-text-fill: #cccccc;"
            );

            setupDragAndDrop();
        }

        private void setupDragAndDrop() {
            // Setup drag start
            setOnDragDetected(event -> {
                if (getItem() == null) {
                    return;
                }

                Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(PRIORITY_DATA_FORMAT, getIndex());
                dragboard.setContent(content);

                event.consume();
            });

            // Update style based on cursor position
            setOnDragOver(event -> {
                if (event.getGestureSource() != this &&
                        event.getDragboard().hasContent(PRIORITY_DATA_FORMAT)) {
                    event.acceptTransferModes(TransferMode.MOVE);

                    // Determine if cursor is in upper or lower half
                    double mouseY = event.getY();
                    double cellHeight = getHeight();

                    if (mouseY < cellHeight / 2) {
                        // Upper half - show border on top
                        setStyle(
                                "-fx-background-color: #2b2b2b;" +
                                        "-fx-border-color: #4A9EFF transparent transparent transparent;" +
                                        "-fx-border-width: 2 0 0 0;"
                        );
                    } else {
                        // Lower half - show border on bottom
                        setStyle(
                                "-fx-background-color: #2b2b2b;" +
                                        "-fx-border-color: transparent transparent #4A9EFF transparent;" +
                                        "-fx-border-width: 0 0 2 0;"
                        );
                    }
                }
                event.consume();
            });

            // Restore style when exiting cell
            setOnDragExited(event -> {
                setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #cccccc;");
                event.consume();
            });

            // Setup drop event
            setOnDragDropped(event -> {
                if (getItem() == null) {
                    return;
                }

                Dragboard db = event.getDragboard();
                boolean success = false;

                if (db.hasContent(PRIORITY_DATA_FORMAT)) {
                    int draggedIdx = (Integer) db.getContent(PRIORITY_DATA_FORMAT);
                    int thisIdx = getIndex();

                    // Determine if cursor is in upper or lower half
                    double mouseY = event.getY();
                    double cellHeight = getHeight();
                    boolean dropAbove = mouseY < cellHeight / 2;

                    DTOPriorityItem draggedItem = items.get(draggedIdx);
                    items.remove(draggedIdx);

                    // Calculate target index
                    int targetIdx = thisIdx;

                    if (draggedIdx < thisIdx) {
                        // Item dragged from top to bottom
                        if (dropAbove) {
                            targetIdx = thisIdx - 1;
                        } else {
                            targetIdx = thisIdx;
                        }
                    } else {
                        // Item dragged from bottom to top
                        if (dropAbove) {
                            targetIdx = thisIdx;
                        } else {
                            targetIdx = thisIdx + 1;
                        }
                    }

                    // Ensure index is within valid range
                    targetIdx = Math.max(0, Math.min(targetIdx, items.size()));

                    items.add(targetIdx, draggedItem);
                    updatePriorities();

                    listView.getSelectionModel().select(targetIdx);

                    if (onChangeCallback != null) {
                        onChangeCallback.run();
                    }

                    success = true;
                }

                event.setDropCompleted(success);
                event.consume();
            });

            // Finalize drag
            setOnDragDone(event -> {
                event.consume();
            });
        }

        @Override
        protected void updateItem(DTOPriorityItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setStyle("-fx-background-color: #2b2b2b;");
            } else {
                enabledCheckBox.setSelected(item.isEnabled());
                priorityLabel.setText(item.getPriority() + ".");
                nameLabel.setText(item.getName());

                // Listener for checkbox changes
                enabledCheckBox.setOnAction(e -> {
                    item.setEnabled(enabledCheckBox.isSelected());
                    if (onChangeCallback != null) {
                        onChangeCallback.run();
                    }
                });

                // Visual style based on enabled state
                if (item.isEnabled()) {
                    nameLabel.setStyle("-fx-opacity: 1.0; -fx-font-size: 12px; -fx-text-fill: #cccccc;");
                    dragIndicator.setStyle("-fx-font-size: 14px; -fx-text-fill: #808080; -fx-cursor: move;");
                    priorityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #4A9EFF;");
                } else {
                    nameLabel.setStyle("-fx-opacity: 0.5; -fx-font-size: 12px; -fx-text-fill: #888888;");
                    dragIndicator.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555; -fx-cursor: move;");
                    priorityLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #666666;");
                }

                // Dark theme background
                setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #cccccc;");

                // Hover effect
                setOnMouseEntered(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: #313335; -fx-text-fill: #cccccc;");
                    }
                });

                setOnMouseExited(e -> {
                    if (!isEmpty()) {
                        setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: #cccccc;");
                    }
                });

                setGraphic(content);
            }
        }
    }
}
