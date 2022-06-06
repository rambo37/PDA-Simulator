package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDATransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;

import java.util.ArrayList;

/**
 * A custom implementation for TableCells that allows for editing the next/previous cell with the
 * use of tab/shift+tab, displays the epsilon character for blank cells, highlighting of invalid
 * cells in the transition table and for highlighting entire rows of the transition table.
 *
 * @author Savraj Bassi
 */

public class TransitionTableCell extends TableCell<PDATransition, String> {
    private final String EPSILON = "ε";
    // The textField that is shown when the user attempts to edit this cell.
    private TextField textField;
    // The list of applicable transitions that require highlighting
    private ArrayList<PDATransition> applicableTransitions = null;

    /**
     * Default constructor takes no arguments.
     */
    public TransitionTableCell() {
    }

    /**
     * This constructor is used for the step by step and animation modes to allow certain rows of
     * the table to be highlighted.
     *
     * @param applicableTransitions The transitions of the table that need to be highlighted.
     */
    public TransitionTableCell(ArrayList<PDATransition> applicableTransitions) {
        this.applicableTransitions = applicableTransitions;
    }

    /**
     * Cancels the edit for this TableCell.
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();

        // Upon cancelling the edit, set the text to whatever it was unless it was empty. In that
        // case, set it to epsilon.
        if (getItem() == null) {
            setText(null);
        } else {
            if (getItem().isEmpty()) {
                setText(EPSILON);
            } else {
                setText(getItem());
            }
        }
        // Hide the textField since we are no longer editing
        setGraphic(null);
    }

    /**
     * This method is used to highlight the cell if it is invalid or to highlight a row in the
     * transition table. It is called when the cell factories are set again. See subsections 4.5.2
     * and 4.11.6 of the report for further details.
     *
     * @param item  The new item (string) for this cell
     * @param empty Whether this cell is part of a PDATransition
     */
    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        // Set everything to null if this is an empty cell (not part of a PDATransition). This
        // was used to fix the issue of empty cells ending up with epsilon symbols in them or
        // having a background colour
        if (empty) {
            setText(null);
            setGraphic(null);
            setBackground(null);
        } else {
            // If applicableTransitions is not null, then the table this cell belongs to is being
            // used in the step by step mode. Therefore, highlight this row if the transition in
            // this row is in applicableTransitions.
            if (applicableTransitions != null) {
                PDATransition transition = getTableRow().getItem();
                // Highlight the table row this cell belongs to if this row is an applicable
                // transition
                if (transition != null && applicableTransitions.contains(transition)) {
                    getTableRow().setBackground(new Background(new BackgroundFill(Color.rgb(5,
                            208, 16), null, null)));
                }
            }

            // If this cel is invalid, highlight it
            highlightCellIfInvalid();

            // Display the textField when editing and hide the text
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(getItem());
                }
                setText(null);
                setGraphic(textField);
            } else {
                // Display the text when not editing and hide the textField
                if (item.equals("")) {
                    setText(EPSILON);
                } else {
                    setText(getItem());
                }
                setGraphic(null);
            }
        }
    }

    /**
     * Checks if this cell is invalid and if so, it highlights it. If the cell is valid however,
     * it unhighlights the cell in case it was previously highlighted.
     */
    private void highlightCellIfInvalid() {
        if (getTableColumn() != null) {
            if (getTableColumn().getText().contains("symbol")) {
                if (getItem() != null && getItem().length() > 1) {
                    highlightCell();
                } else {
                    unhighlightCell();
                }
            }
        }
    }

    /**
     * Highlights a cell to make it clear that it is invalid. Changes the colour to a red shade
     * and changes the colour of the text to white to make it easier to see.
     */
    private void highlightCell() {
        setBackground(new Background(new BackgroundFill(Color.rgb(255, 77, 77), null, null)));
        setTextFill(Color.WHITE);
    }

    /**
     * Unhighlights a cell by setting the background and the colour of the text back to normal.
     */
    private void unhighlightCell() {
        setBackground(new Background(new BackgroundFill(null, null, null)));
        setTextFill(Color.BLACK);
    }

    /**
     * Begins the edit for this TableCell.
     */
    @Override
    public void startEdit() {
        super.startEdit();
        createTextField();
        // Hide the text of the cell
        setText(null);
        // Setting the graphic to the textField shows the textField
        setGraphic(textField);
        // Make the textField focused and make it highlight the text it contains
        Platform.runLater(() -> {
            textField.requestFocus();
            textField.selectAll();
        });
        getTableView().getSelectionModel().clearSelection();
    }

    /**
     * Creates the textField used for editing transition table cells. See subsection 4.5.3 of the
     * report for further details.
     */
    private void createTextField() {
        // textField initially contains the text of the cell
        textField = new TextField(getItem());

        // Consume the event to override the default behaviour
        textField.setOnKeyPressed(Event::consume);

        // When the textField loses focus, invoke cancelEdit to make the textField disappear and
        // the cell text appear.
        textField.focusedProperty().addListener((observableValue, oldPropertyValue,
                                                 newPropertyValue) -> {
            if (!newPropertyValue) {
                cancelEdit();
            }
        });

        // When the user presses enter, attempt to commit the edit if the textField's text is valid
        textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (isValid(textField.getText())) {
                    // Since the text is valid, invoke commitEdit, move to the next cell and
                    // remove the highlight in case the cell was highlighted
                    commitEdit(textField.getText());
                    moveToNextCell();
                    unhighlightCell();
                } else {
                    // Highlight the cell, and do not move to the next cell when they press enter if
                    // it is invalid
                    highlightCell();
                    getTableView().edit(getTableRow().getIndex(), getTableColumn());
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                // Allow for cancelling the edit with the escape key
                cancelEdit();
            } // If the user presses tab, move to the next cell and highlight/unhighlight as
            // required
            else if (!event.isShiftDown() && event.getCode() == KeyCode.TAB) {
                // If the content of the cell (which can be different from the textField's
                // content) is valid, the cell does not need to be highlighted
                if (isValid(getItem())) {
                    unhighlightCell();
                } else {
                    highlightCell();
                }
                moveToNextCell();
            }
            // If shift and tab are pressed together, move backwards instead but do the same
            // checking
            else if (event.isShiftDown() && event.getCode() == KeyCode.TAB) {
                if (isValid(getItem())) {
                    unhighlightCell();
                } else {
                    highlightCell();
                }
                moveToPreviousCell();
            }
        });

        // Set the TextFormatter of the textField based on whether the column name contains
        // "symbol". If it does, then use the symbolTextFormatter, else use the stringTextFormatter.
        if (getTableColumn().getText().contains("symbol")) {
            textField.setTextFormatter(MainController.symbolTextFormatter());
        } else {
            textField.setTextFormatter(MainController.stringTextFormatter());
        }
    }

    /**
     * Checks if the provided text for this cell is valid. The text is invalid if the table column
     * contains the word "state" and the length is zero (since state names cannot be blank) or if
     * the table column contains the word "symbol" but the string has more than 1 character (symbol
     * means at most one character).
     *
     * @param text The text being checked.
     * @return True if the text is valid and false otherwise.
     */
    private boolean isValid(String text) {
        if (getTableColumn().getText().contains("state")) {
            return text.length() != 0;
        }
        if (getTableColumn().getText().contains("symbol")) {
            return text.length() <= 1;
        }
        return true;
    }

    /**
     * Moves to the previous cell in the table by invoking the edit method of the tableView with
     * the index and table column of the previous cell.
     */
    private void moveToPreviousCell() {
        int currentRow = getTableRow().getIndex();
        int columnIndex = getTableView().getColumns().indexOf(getTableColumn());

        // Determine which column is the previous column. If this is the first column in the
        // list, then there is no previous column so leave prevCol as null.
        TableColumn<PDATransition, ?> prevCol = null;
        if (columnIndex > 0) {
            prevCol = getTableView().getColumns().get(columnIndex - 1);
        }

        if (prevCol != null) {
            getTableView().edit(currentRow, prevCol);
        } else {
            // As there is no previous column, we need to go to the last column on the previous row
            TableColumn<PDATransition, ?> lastColumn = getTableView().getColumns().get(4);
            getTableView().edit(currentRow - 1, lastColumn);
        }
        getTableView().getSelectionModel().clearSelection();
    }

    /**
     * Moves to the next cell in the table by invoking the edit method of the tableView with the
     * index and table column of the next cell.
     */
    private void moveToNextCell() {
        int currentRow = getTableRow().getIndex();
        int columnIndex = getTableView().getColumns().indexOf(getTableColumn());

        // Determine which column is the next column. If this is the last column in the list,
        // then there is no next column so leave nextCol as null.
        TableColumn<PDATransition, ?> nextCol = null;
        if (columnIndex < getTableView().getColumns().size() - 1) {
            nextCol = getTableView().getColumns().get(columnIndex + 1);
        }

        if (nextCol != null) {
            getTableView().edit(currentRow, nextCol);
        } else {
            int lastRow = getTableView().getItems().size() - 1;
            // If there is another row after this row, edit the cell in the first column of the
            // next row
            if (currentRow < lastRow) {
                getTableView().edit(currentRow + 1, getTableView().getColumns().get(0));
            }
        }
        getTableView().getSelectionModel().clearSelection();
    }
}