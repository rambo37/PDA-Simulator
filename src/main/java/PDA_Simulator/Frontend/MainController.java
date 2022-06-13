package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.AcceptanceCriteria;
import PDA_Simulator.Backend.PDA;
import PDA_Simulator.Backend.PDAConfiguration;
import PDA_Simulator.Backend.PDATransition;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class is the main controller of the application and handles all the MenuBar functionality,
 * the creation and manipulation of the underlying PDA object for other controllers and the quick
 * run functionality.
 *
 * @author Savraj Bassi
 */

public class MainController {
    // Enum for the selected transition function
    enum TransitionFunction {
        SYMBOL_AND_SYMBOL,
        SYMBOL_AND_STRING,
        STRING_AND_STRING
    }

    // The application icon for use in all windows
    private static final Image APPLICATION_ICON = new Image("Images/icon.png");
    // The maximum number of steps a computation can run for by default
    private static final int DEFAULT_MAX_STEPS = 50;
    // The underlying PDA object
    private static PDA pda;
    // The current maximum number of steps a computation can run for
    private static int currentMaxSteps = 50;
    // An ArrayList to store all accepting computations that were found
    private static ArrayList<ArrayList<PDAConfiguration>> acceptingComputations = new ArrayList<>();
    // The dialog to specify the initial stack symbol
    private final TextInputDialog initialStackSymbolDialog = new TextInputDialog();
    // A list of all PDAStateNodes
    private final ArrayList<PDAStateNode> stateNodes = new ArrayList<>();
    // An alert for unsaved changes
    private final Alert warning = new Alert(Alert.AlertType.WARNING);
    // The FileChooser used for opening and saving PDAs
    private final FileChooser fileChooser = new FileChooser();
    // A dialog to show all accepting computations that were found during the quick run
    private final Dialog<String> acceptingComputationsDialog = new Dialog<>();
    // The VBox that is inside the acceptingComputationsDialog
    private final VBox acceptingComputationsDialogVBox = new VBox();
    // Boolean variables indicating which button in the unsaved changes warning Alert was clicked
    boolean savePressed = false;
    boolean doNotSavePressed = false;
    boolean cancelPressed = false;
    // A boolean to indicate whether a sample has been loaded or not
    boolean sampleClicked = false;
    // The main Stage of the application
    private Stage stage;
    private PDAStateNodeController pdaStateNodeController;
    // The currently selected transition function (determined by the selected MenuItem)
    private TransitionFunction selectedTransitionFunction = TransitionFunction.SYMBOL_AND_SYMBOL;
    // The file currently opened by the user
    private File file = null;
    // This index stores the current index of the accepting computations dialog
    private int index = 0;
    // The x coordinate of the mouse.
    private double x;
    // The y coordinate of the mouse.
    private double y;
    // The selection rectangle used to select multiple nodes.
    private final Rectangle selection = new Rectangle();
    // Whether a PDAStateNode is being dragged or not
    private boolean nodeDrag = false;
    // Whether the nondeterministic transitions are currently highlighted or not
    private boolean nondeterminismHighlighted = false;
    // Below are all the components with fx:ids in main-view.fxml.
    @FXML
    private ToggleGroup acceptanceMenu = new ToggleGroup();
    @FXML
    private ToggleGroup stackMenu = new ToggleGroup();
    @FXML
    private RadioMenuItem emptyStack = new RadioMenuItem();
    @FXML
    private ToggleGroup transitionMenu = new ToggleGroup();
    @FXML
    private Button determinismButton;
    @FXML
    private HBox hBox;
    @FXML
    private Pane canvas;
    @FXML
    private TableView<PDATransition> transitionTable;
    @FXML
    private TableColumn<PDATransition, String> currentState;
    @FXML
    private TableColumn<PDATransition, String> inputSymbol;
    @FXML
    private TableColumn<PDATransition, String> pop;
    @FXML
    private TableColumn<PDATransition, String> push;
    @FXML
    private TableColumn<PDATransition, String> newState;
    @FXML
    private TextField currentStateTextField;
    @FXML
    private TextField inputSymbolTextField;
    @FXML
    private TextField popTextField;
    @FXML
    private TextField pushTextField;
    @FXML
    private TextField newStateTextField;
    @FXML
    private Button addTransitionButton;
    @FXML
    private TextField inputString;
    @FXML
    private TextArea notes;

    /**
     * Initialises the PDA object, the PDAStateNodeController and several JavaFX controls such as
     * the transition table, the text fields and the canvas Pane.
     */
    @FXML
    public void initialize() {
        createInitialStackSymbolDialog();
        createWarningAlert();
        initialiseTextFields();
        createAcceptingComputationsDialog();
        // Use the reset method to create the PDA, initialise the PDAStateNodeController, create
        // the PDAStateNode for the initial state and set the content of the transition table
        reset(true);

        // Whenever the user clicks on a row in the transition table, automatically fill out the
        // TextFields underneath the transition table to make it easier to delete transitions using
        // the table format
        transitionTable.getSelectionModel().selectedItemProperty().addListener((observableValue,
                                                                                previous,
                                                                                current) -> {
            if (current != null) {
                currentStateTextField.setText(current.getCurrentState());
                inputSymbolTextField.setText(current.getInputSymbol());
                popTextField.setText(current.getPopString());
                pushTextField.setText(current.getPushString());
                newStateTextField.setText(current.getNewState());
            }
        });

        // Set the cell value factory to use the same name as the field of the PDATransition
        // class that it is for. The currentState TableColumn is for the "currentState" field of the
        // PDATransition class.
        currentState.setCellValueFactory(new PropertyValueFactory<>("currentState"));
        // Use the custom TransitionTableCell class for the cells of the transition table
        currentState.setCellFactory(param -> new TransitionTableCell());
        // Ensure the edit is valid before committing it
        currentState.setOnEditCommit(event -> {
            PDATransition oldTransition = event.getRowValue(); // event.getRowValue() is the row
            // before the edit
            String inputSymbol = event.getRowValue().getInputSymbol();
            String popString = event.getRowValue().getPopString();
            String pushString = event.getRowValue().getPushString();
            String newState = event.getRowValue().getNewState();
            PDATransition newTransition;
            newTransition = new PDATransition(event.getNewValue(), inputSymbol, popString,
                    pushString, newState);

            boolean successful = pda.editTransition(oldTransition, newTransition);
            if (!successful) {
                // If a transition was just changed into an existing transition, refresh the table
                // so the edit disappears
                transitionTable.refresh();
            }
        });

        inputSymbol.setCellValueFactory(new PropertyValueFactory<>("inputSymbol"));
        inputSymbol.setCellFactory(param -> new TransitionTableCell());
        inputSymbol.setOnEditCommit(event -> {
            PDATransition oldTransition = event.getRowValue();
            String currentState = event.getRowValue().getCurrentState();
            String popString = event.getRowValue().getPopString();
            String pushString = event.getRowValue().getPushString();
            String newState = event.getRowValue().getNewState();
            PDATransition newTransition;
            newTransition = new PDATransition(currentState, event.getNewValue(), popString,
                    pushString, newState);

            boolean successful = pda.editTransition(oldTransition, newTransition);
            if (!successful) {
                transitionTable.refresh();
            }
        });

        pop.setCellValueFactory(new PropertyValueFactory<>("popString"));
        pop.setCellFactory(param -> new TransitionTableCell());
        pop.setOnEditCommit(event -> {
            PDATransition oldTransition = event.getRowValue();
            String currentState = event.getRowValue().getCurrentState();
            String inputSymbol = event.getRowValue().getInputSymbol();
            String pushString = event.getRowValue().getPushString();
            String newState = event.getRowValue().getNewState();
            PDATransition newTransition;
            newTransition = new PDATransition(currentState, inputSymbol, event.getNewValue(),
                    pushString, newState);

            boolean successful = pda.editTransition(oldTransition, newTransition);
            if (!successful) {
                transitionTable.refresh();
            }
        });

        push.setCellValueFactory(new PropertyValueFactory<>("pushString"));
        push.setCellFactory(param -> new TransitionTableCell());
        push.setOnEditCommit(event -> {
            PDATransition oldTransition = event.getRowValue();
            String currentState = event.getRowValue().getCurrentState();
            String inputSymbol = event.getRowValue().getInputSymbol();
            String popString = event.getRowValue().getPopString();
            String newState = event.getRowValue().getNewState();
            PDATransition newTransition;
            newTransition = new PDATransition(currentState, inputSymbol, popString,
                    event.getNewValue(), newState);

            boolean successful = pda.editTransition(oldTransition, newTransition);
            if (!successful) {
                transitionTable.refresh();
            }
        });

        newState.setCellValueFactory(new PropertyValueFactory<>("newState"));
        newState.setCellFactory(param -> new TransitionTableCell());
        newState.setOnEditCommit(event -> {
            PDATransition oldTransition = event.getRowValue();
            String currentState = event.getRowValue().getCurrentState();
            String inputSymbol = event.getRowValue().getInputSymbol();
            String popString = event.getRowValue().getPopString();
            String pushString = event.getRowValue().getPushString();
            PDATransition newTransition;
            newTransition = new PDATransition(currentState, inputSymbol, popString, pushString,
                    event.getNewValue());

            boolean successful = pda.editTransition(oldTransition, newTransition);
            if (!successful) {
                transitionTable.refresh();
            }
        });


        // Set the preferred height of the canvas Pane to a large quantity so that it cannot be
        // reduced in size when the minimum height is lowered.
        canvas.setPrefHeight(5000);

        selection.setStyle("-fx-fill: rgba(0,0,255,0.1);" +
                "-fx-stroke: blue;" +
                "-fx-stroke-width: 1;" +
                "-fx-stroke-dash-array: 2;");

        // Initialise the selection rectangle when the user clicks in the canvas Pane. The rectangle
        // is positioned at the same position that the mouse click occurs, the size of the rectangle
        // is set to 0x0, and it is made visible.
        canvas.setOnMousePressed(event -> {
            // Store the coordinates of the mouse
            x = event.getX();
            y = event.getY();
            selection.setX(x);
            selection.setY(y);
            selection.setWidth(0);
            selection.setHeight(0);
            selection.setVisible(true);

            nodeDrag = false;

            // Set nodeDrag to true if the click is within any PDAStateNode since any drag that
            // occurs after clicking on a PDAStateNode is attempting to drag a node.
            for (PDAStateNode stateNode : stateNodes) {
                if (stateNode.containsCoordinates(x, y)) {
                    nodeDrag = true;
                }
            }

            // Deselect all PDAStateNodes once the user clicks on anything other than a node
            if (!nodeDrag) {
                for (PDAStateNode stateNode : stateNodes) {
                    stateNode.deselectNode();
                }
            }
        });

        // When a mouse drag is performed in the canvas, expand the selection rectangle unless the
        // drag was done on a node. In that case, nothing should happen to the selection rectangle.
        canvas.setOnMouseDragged(event -> {
            // Only create a selection rectangle if the drag did not originate from a PDAStateNode
            if (!nodeDrag) {
                // Set the coordinates of the rectangle to either the coordinates from which the
                // drag began from or the current mouse position, based on which is smaller.
                selection.setX(Math.min(event.getX(), x));
                selection.setY(Math.min(event.getY(), y));

                // The width and height of the rectangle are set to the absolute difference between
                // the current mouse coordinates and the initial coordinates the drag began from.
                selection.setWidth(Math.abs(event.getX() - x));
                selection.setHeight(Math.abs(event.getY() - y));

                // Select all PDAStateNodes that are within the selection rectangle and deselect
                // the nodes which are not
                for (PDAStateNode stateNode : stateNodes) {
                    if (inSelectionRectangle(stateNode)) {
                        stateNode.selectNode();
                    }
                    else {
                        stateNode.deselectNode();
                    }
                }
            }

        });

        // Make the selection rectangle disappear once the mouse click is released
        canvas.setOnMouseReleased(event -> selection.setVisible(false));
    }

    /**
     * Checks if the given PDAStateNode is within the selection rectangle.
     * @param stateNode The PDAStateNode being checked.
     * @return True if the node is within the rectangle and false otherwise.
     */
    private boolean inSelectionRectangle(PDAStateNode stateNode) {
        double minX = stateNode.getLayoutX();
        double minY = stateNode.getLayoutY();
        double maxX = minX + 150;
        double maxY = minY + 50;

        for (double i = minX; i < maxX; i++) {
            for (double j = minY; j < maxY; j++) {
                if (selection.contains(i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates the initial stack symbol dialog. This dialog is shown when the user clicks on the
     * "Initial stack symbol" menu item of the Stack menu in the menu bar and allows the user to
     * specify the initial stack symbol.
     */
    private void createInitialStackSymbolDialog() {
        initialStackSymbolDialog.setTitle("Initial stack symbol");
        initialStackSymbolDialog.setContentText("Please enter the initial stack symbol");

        Stage stage = (Stage) initialStackSymbolDialog.getDialogPane().getScene().getWindow();
        stage.getIcons().add(APPLICATION_ICON);

        // Error message to display if the user does not specify a symbol, initially hidden
        Label errorLabel = new Label("Please enter a valid symbol.");
        errorLabel.setTextFill(Color.RED);
        errorLabel.setVisible(false);

        GridPane content = (GridPane) initialStackSymbolDialog.getDialogPane().getContent();
        content.add(errorLabel, 0, 2);

        // Set up the TextField so that the user is only able to specify at most a single symbol
        TextField textField = initialStackSymbolDialog.getEditor();
        textField.setMaxWidth(25);
        textField.setMinWidth(25);
        textField.setTextFormatter(symbolTextFormatter());

        // If the user presses ok, then check that they entered a character in the TextField. If
        // not, display the error message and do not close the dialog.
        Button ok = (Button) initialStackSymbolDialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, (event -> {
            if (textField.getText().isEmpty()) {
                event.consume();
                errorLabel.setVisible(true);
            }
        }));

        // When the dialog is closed, if the TextField is empty, then select the emptyStack
        // MenuItem and invoke the function that would be invoked if the user selected the MenuItem.
        // If the TextField is nonempty, then update the initial stack symbol of the PDA do not
        // bother changing the selected MenuItem.
        initialStackSymbolDialog.setOnCloseRequest(dialogEvent -> {
            if (textField.getText().isEmpty()) {
                emptyStack.setSelected(true);
                onEmptyStackClick();
            } else {
                pda.setInitialStackSymbol(textField.getText());
            }
            // Hide the error label upon closure, so it is not visible when the dialog is reopened
            errorLabel.setVisible(false);
        });
    }

    /**
     * Creates and returns a TextFormatter object that only allows at most a single character to
     * be entered. Used for text fields that specify a symbol (such as the initial stack symbol,
     * current input symbol, pop symbol and push symbol).
     *
     * @return The symbol TextFormatter
     */
    public static TextFormatter<String> symbolTextFormatter() {
        return new TextFormatter<>((TextFormatter.Change change) -> {
            String text = change.getControlNewText();
            // If a text field that only allows a single symbol to be entered has multiple symbols
            // in it (i.e. because it was previously a string text field), then it is necessary to
            // permit change that does not change the content to allow moving the text cursor with
            // arrow keys and selecting. Deleting must also be allowed so the user can go from
            // multiple symbols down to either zero or one symbol.
            if (text.matches("([a-zA-Z]|[0-9]|#|~|<|>|\\.|:|;" +
                    "|@|'|\\?|-|_|\\+|\\*|\\||!|\"|£|\\$|%|\\^|&)?") ||
                    !change.isContentChange() || change.isDeleted()) {
                return change;
            } else {
                return null;
            }
        });
    }

    /**
     * Creates and returns a TextFormatter object that allows 0 or more symbols to be entered.
     * Used for text fields that specify a string (such as the current state, new state, pop string
     * and push string text fields).
     *
     * @return The string TextFormatter
     */
    public static TextFormatter<String> stringTextFormatter() {
        return new TextFormatter<>((TextFormatter.Change change) -> {
            String text = change.getControlNewText();
            // Also prevent the user from entering the string "null" as that can interfere with
            // loading correctly
            if (text.matches("([a-zA-Z]|[0-9]|#|~|<|>|\\.|:|;" +
                    "|@|'|\\?|-|_|\\+|\\*|\\||!|\"|£|\\$|%|\\^|&)*") &&
                    !text.matches("null")) {
                return change;
            } else {
                return null;
            }
        });
    }

    /**
     * Inner class to override the default ordering of the buttons of the warning dialog
     */
    private static class WarningDialogPane extends DialogPane {

        @Override
        protected Node createButtonBar() {
            ButtonBar node = (ButtonBar) super.createButtonBar();
            node.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
            return node;
        }
    }

    /**
     * Creates the alert that warns users about unsaved changes.
     */
    private void createWarningAlert() {
        warning.setTitle("Warning");
        WarningDialogPane warningDialogPane = new WarningDialogPane();
        warningDialogPane.setContentText("Would you like to save your changes? Unsaved changes " +
                "will be lost.");
        warningDialogPane.setHeaderText("Unsaved changes");
        warning.setDialogPane(warningDialogPane);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType doNotSave = new ButtonType("Do not save", ButtonBar.ButtonData.NO);
        warningDialogPane.getButtonTypes().remove(ButtonType.OK);
        warningDialogPane.getButtonTypes().addAll(save, doNotSave, ButtonType.CANCEL);

        warningDialogPane.lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            if (save()) {
                // If the user saves their changes, then set savePressed to true
                savePressed = true;
            } else {
                // If the save method returns false, set the cancelPressed boolean to true to
                // indicate the dialog was closed but not as a result of successfully saving or
                // pressing the "do not save" button.
                cancelPressed = true;
            }
        });

        warningDialogPane.lookupButton(doNotSave).addEventFilter(ActionEvent.ACTION,
                event -> doNotSavePressed = true);

        warningDialogPane.lookupButton(ButtonType.CANCEL).addEventFilter(ActionEvent.ACTION,
                event -> cancelPressed = true);

        warning.setOnCloseRequest(dialogEvent -> {
            // If the dialog is closed but none of the three buttons were pressed, then the 'X'
            // button was pressed.
            if (!savePressed && !doNotSavePressed && !cancelPressed) {
                cancelPressed = true;
            }
        });

        // Set the icon of the warning dialog
        Stage stage = (Stage) warning.getDialogPane().getScene().getWindow();
        stage.getIcons().add(APPLICATION_ICON);
    }

    /**
     * Initialises the TextFields underneath the transition table and the TextField for the input
     * string.
     */
    private void initialiseTextFields() {
        // Bind the width of each textField to the width of the respective table column
        currentStateTextField.prefWidthProperty().bind(currentState.widthProperty());
        inputSymbolTextField.prefWidthProperty().bind(inputSymbol.widthProperty());
        popTextField.prefWidthProperty().bind(pop.widthProperty());
        pushTextField.prefWidthProperty().bind(push.widthProperty());
        newStateTextField.prefWidthProperty().bind(newState.widthProperty());
        inputString.prefWidthProperty().bind(transitionTable.widthProperty());

        // Set the appropriate text formatter for each textField
        currentStateTextField.setTextFormatter(stringTextFormatter());
        inputSymbolTextField.setTextFormatter(symbolTextFormatter());
        popTextField.setTextFormatter(symbolTextFormatter());
        pushTextField.setTextFormatter(symbolTextFormatter());
        newStateTextField.setTextFormatter(stringTextFormatter());
        inputString.setTextFormatter(stringTextFormatter());

        ArrayList<TextField> transitionTextFields = new ArrayList<>();
        transitionTextFields.add(currentStateTextField);
        transitionTextFields.add(inputSymbolTextField);
        transitionTextFields.add(popTextField);
        transitionTextFields.add(pushTextField);
        transitionTextFields.add(newStateTextField);

        transitionTextFields.forEach(t -> t.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Move to the next TextField when the user presses enter if the TextField content is
            // valid
            if (event.getCode() == KeyCode.ENTER) {
                selectNext(transitionTextFields, t);
            }
        }));
    }

    /**
     * Selects the next TextField in the set of transition TextFields if there is one by making
     * it request focus. If there is no next TextField (because the TextField is the last one),
     * then it instead fires the button to add the transition.
     *
     * @param transitionTextFields The list of transition TextFields.
     * @param current              The currently focused TextField in the list of transition
     *                             TextFields.
     */
    private void selectNext(ArrayList<TextField> transitionTextFields, TextField current) {
        int index = transitionTextFields.indexOf(current);
        if (index != (transitionTextFields.size() - 1)) {
            transitionTextFields.get(index + 1).requestFocus();
        } else {
            addTransitionButton.fire();
        }
    }

    /**
     * The acceptingComputationsDialog shows the user all the generated accepting computations.
     * It allows the user to flick through them one at a time if there are several of them using
     * two buttons: Previous and Next.
     */
    private void createAcceptingComputationsDialog() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToHeight(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setPrefWidth(500);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        acceptingComputationsDialogVBox.getStylesheets().add("CSS/configuration.css");
        acceptingComputationsDialogVBox.setSpacing(15);
        scrollPane.setContent(acceptingComputationsDialogVBox);

        DialogPane dialogPane = acceptingComputationsDialog.getDialogPane();
        ButtonType animation = new ButtonType("Play animation", ButtonBar.ButtonData.YES);
        dialogPane.getButtonTypes().addAll(animation, ButtonType.PREVIOUS, ButtonType.NEXT,
                ButtonType.CLOSE);

        Button animationButton = (Button) dialogPane.lookupButton(animation);
        animationButton.addEventFilter(ActionEvent.ACTION, event -> {
            // Consume event to prevent dialog closure
            event.consume();
            // Display the animation window and give the computation at the current index of
            // acceptingComputations so that it can be animated.
            //See section 4.12 of the report for further details.
            showAnimationWindow(acceptingComputations.get(index));
        });

        Button prev = (Button) dialogPane.lookupButton(ButtonType.PREVIOUS);
        Button next = (Button) dialogPane.lookupButton(ButtonType.NEXT);

        next.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            // Set the position of the vertical scroll bar of the ScrollPane to the top
            scrollPane.setVvalue(0);
            int size = acceptingComputations.size();
            // Only increment index and display the next computation if it will be within the
            // bounds of the ArrayList
            if (index + 1 < size) {
                index++;
                // The previous button can be enabled if we move to the next computation as there
                // will be a previous one
                prev.setDisable(false);
                // Update the title of the dialog
                acceptingComputationsDialog.setTitle("Computation " + (index + 1) + " of " + size);
                // Display the accepting computation at the specified index of acceptingComputations
                displayComputation(acceptingComputations.get(index));
                // Disable the next button if there are no more computations left after this one
                if (index + 1 >= size) {
                    next.setDisable(true);
                }
            }
        });

        // Same idea for the Previous button
        prev.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            scrollPane.setVvalue(0);
            int size = acceptingComputations.size();
            if (index - 1 >= 0) {
                index--;
                next.setDisable(false);
                acceptingComputationsDialog.setTitle("Computation " + (index + 1) + " of " + size);
                displayComputation(acceptingComputations.get(index));

                if (index - 1 < 0) {
                    prev.setDisable(true);
                }
            }
        });

        dialogPane.setContent(scrollPane);
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(APPLICATION_ICON);
    }

    /**
     * Resets the PDA by removing all traces of the current one and then by creating a new one,
     * either with or without an initial state depending on the createInitialState argument. Used to
     * create a new PDA or to load an existing one.
     *
     * @param createInitialState Whether the newly created PDA should have an initial state or not.
     */
    private void reset(boolean createInitialState) {
        canvas.getChildren().clear();
        canvas.getChildren().add(selection);
        stateNodes.clear();
        inputString.clear();
        notes.clear();
        // Clear the text fields beneath the transition table
        currentStateTextField.clear();
        inputSymbolTextField.clear();
        popTextField.clear();
        pushTextField.clear();
        newStateTextField.clear();

        // Determinism button requires manual resetting since the deterministic property starts
        // off as true but the determinism button would be in its previous state
        determinismButton.setId("deterministic");
        determinismButton.setText("Deterministic");

        // Reset the various frontend menus
        transitionMenu.getToggles().get(0).setSelected(true);
        selectedTransitionFunction = TransitionFunction.SYMBOL_AND_SYMBOL;
        acceptanceMenu.getToggles().get(0).setSelected(true);
        TextField textField = initialStackSymbolDialog.getEditor();
        textField.setText("");
        stackMenu.getToggles().get(0).setSelected(true);

        pda = new PDA();
        // Set the content of the transition table to the observable list of PDATransitions.
        // Additions and removals to this list are automatically made to the transition table.
        transitionTable.setItems(pda.getTransitions());
        initialisePDAStateNodeController();
        // Add the listeners to the PDA before creating the initial state (if required) so that
        // the listener for the states ObservableList will fire
        addListenersToPDA();

        // Create an initial state if necessary
        if (createInitialState) {
            pda.addState();
        }
    }

    /**
     * Initialise the controller for PDAStateNodes. The fxml file used by this controller just
     * contains the content common to the dialogs to add and edit transitions. This content is a
     * VBox and is passed to the controller, so it can reuse it for both dialogs. The
     * PDAStateNodeController also requires text formatters for the text fields it contains to
     * prevent the user from entering invalid input in the text fields.
     */
    private void initialisePDAStateNodeController() {
        FXMLLoader fxmlLoader = new FXMLLoader(PDASimulator.class.getResource("/FXML" +
                "/transition-dialog-content.fxml"));
        try {
            VBox vBox = fxmlLoader.load();
            pdaStateNodeController = fxmlLoader.getController();
            pdaStateNodeController.setVBox(vBox);
            pdaStateNodeController.setPopTextFormatter(symbolTextFormatter());
            pdaStateNodeController.setPushTextFormatter(symbolTextFormatter());
            pdaStateNodeController.setStateNodes(stateNodes);
            pdaStateNodeController.setApplicationIcon(APPLICATION_ICON);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds four different listeners to the PDA object so that changes to the PDA object result
     * in automatic changes in the frontend.
     */
    private void addListenersToPDA() {
        // This listener is responsible for updating the visual appearance of the determinismButton
        // whenever the determinism of the PDA changes by changing the text and the CSS ID.
        pda.addDeterministicPropertyListener((observable, oldValue, newValue) -> {
            if (newValue) {
                determinismButton.setId("deterministic");
                determinismButton.setText("Deterministic");
            } else {
                determinismButton.setId("nondeterministic");
                determinismButton.setText("Nondeterministic");
            }
        });

        // Decide which node should be displayed as the initial state whenever the initialState
        // Property changes.
        pda.addInitialStatePropertyListener((observable, oldValue, newValue) -> {
            for (PDAStateNode pdaStateNode : stateNodes) {
                if (newValue == null) {
                    pdaStateNode.makeNotInitialState();
                } else if (newValue.equals(pdaStateNode.getStateName())) {
                    pdaStateNode.makeInitialState();
                } else {
                    pdaStateNode.makeNotInitialState();
                }
            }
        });

        // A listener for changes to the list of states. There is no need to do anything if a
        // state is renamed since the PDAStatNode class already handles that.
        pda.addStatesListener(change -> {
            while (change.next()) {
                // If a state is created, create a new PDAStateNode and add it to the canvas and
                // the stateNodes list
                if (!change.wasReplaced() && change.wasAdded()) {
                    for (String newState : change.getAddedSubList()) {
                        PDAStateNode newStateNode = new PDAStateNode(newState,
                                pdaStateNodeController, true);
                        canvas.getChildren().add(newStateNode);
                        stateNodes.add(newStateNode);
                    }
                }
                // If a state is removed, remove it from the list of stateNodes. The PDAStateNode
                // class removes the PDAStateNode from the canvas directly since deletion is only
                // possible by right-clicking the PDAStateNode.
                if (change.wasRemoved()) {
                    stateNodes.removeIf(node ->
                            node.getStateName().equals(change.getRemoved().get(0)));
                }
            }
        });

        // A listener for changes to the list of transitions.
        pda.addTransitionsListener(change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    // Make the PDAStateNodeController create the newly created transition
                    for (PDATransition newTransition : change.getAddedSubList()) {
                        //See subsection 4.3.3 of the report for further details.
                        pdaStateNodeController.createTransition(newTransition);
                    }
                } else if (change.wasRemoved()) {
                    // Make the PDAStateNodeController delete the deleted transitions
                    for (PDATransition deletedTransition : change.getRemoved()) {
                        //See subsection 4.3.5 of the report for further details.
                        pdaStateNodeController.deleteTransition(deletedTransition);
                    }
                }
            }
        });
    }

    /**
     * Sets the stage field with the given stage, adds listeners to the stage and sets the icon
     * of the stage.
     *
     * @param stage The main stage of the application
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.getIcons().add(APPLICATION_ICON);
        // Add a listener that fires whenever a window close attempt occurs to check for unsaved
        // changes
        stage.setOnCloseRequest(windowEvent -> {
            // If checkForUnsavedChanges returns true, then the user closed the dialog so consume
            // the windowEvent to ensure the window does not close
            if (checkForUnsavedChanges()) {
                windowEvent.consume();
            }
        });
        // Add listeners to the stage to support the usual keyboard shortcuts for new, open and
        // save/save as.
        stage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.N) {
                newPDA();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.O) {
                open();
            }
            if (event.isControlDown() && event.getCode() == KeyCode.S) {
                save();
            }
            // Allow deletion of selected states with the backspace/delete keys.
            if (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.DELETE) {
                ArrayList<PDAStateNode> selectedNodes = new ArrayList<>();
                for (PDAStateNode stateNode : stateNodes) {
                    if (stateNode.isSelected()) {
                        selectedNodes.add(stateNode);
                    }
                }

                for (PDAStateNode stateNode : selectedNodes) {
                    pda.deleteState(stateNode.getStateName());
                    canvas.getChildren().remove(stateNode);
                }
            }
            // Select all nodes if the user presses ctrl+A
            if (event.isControlDown() && event.getCode() == KeyCode.A) {
                for (PDAStateNode stateNode : stateNodes) {
                    stateNode.selectNode();
                }
            }
        });

        // Prevent the left-hand side of the SplitPane (which has hBox as the root element) from
        // being reduced in size to less than 60% of the stage width.
        hBox.minWidthProperty().bind(stage.widthProperty().multiply(0.6));
    }

    /**
     * Checks if there are any unsaved changes and if so, it displays the warning dialog. If the
     * user then presses the cancel button of the dialog or the 'X' button to close it, it returns
     * true so the calling method knows whether to proceed or not. See subsection 4.7.1 of the
     * report for further details.
     *
     * @return True if the dialog cancel or 'X' button are clicked and false otherwise.
     */
    private boolean checkForUnsavedChanges() {
        if (unsavedChanges()) {
            // Use showAndWait() rather than show() so the code below does not execute until after
            // the Alert has been responded to by the user
            warning.showAndWait();

            // Reset the boolean variables
            savePressed = false;
            doNotSavePressed = false;

            if (cancelPressed) {
                cancelPressed = false;
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if there are any unsaved changes. If file is null, then that means the user has not
     * saved or opened an existing PDA. Therefore, check if they have made any changes to the file
     * by comparing the String for a newly created PDA with the String returned from the
     * generateString method. If file is not null, then compare the String returned from the
     * generateString method to the string in the file. If they are different, then there are
     * unsaved changes. See subsection 4.7.1 of the report for further details.
     *
     * @return True if there are any unsaved changes and false otherwise.
     */
    private boolean unsavedChanges() {
        String currentPDAString = generateString();
        if (file == null) {
            // If this check for unsaved changes was a result of doing something after loading a
            // sample, then return false so the warning alert does not open.
            if (sampleClicked) {
                return false;
            }
            String initialPDAString = "{states=[q0], transitions=[], initialState=q0, " +
                    "acceptingStates=[], initialStackSymbol=null, " +
                    "acceptanceCriteria=ACCEPTING_STATE}{(q0,50.0,50.0) [transition function = " +
                    "SYMBOL_AND_SYMBOL]}\n";
            return !initialPDAString.equals(currentPDAString);
        } else {
            try {
                String currentlySavedString =
                        new String(Files.readAllBytes(Paths.get(file.toString())));
                if (currentlySavedString.equals(currentPDAString)) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    /**
     * Creates a new PDA. This method is invoked when the user presses the "New" MenuItem in the
     * File menu. Before creating a new PDA, a check is done to ensure that the warning dialog is
     * displayed if there are any unsaved changes.
     */
    @FXML
    private void newPDA() {
        // If checkForUnsavedChanges returns true, then the user closed the dialog so return from
        // this method to prevent the new operation from happening
        if (checkForUnsavedChanges()) {
            return;
        }
        // Set the file to null since the user is now working on a new PDA which is not yet saved
        file = null;
        // Set sampleClicked to false since file is null, but not as a result of loading a sample.
        sampleClicked = false;
        // Reset the PDA
        reset(true);
    }

    /**
     * Method to load previously created pushdown automata. First checks for unsaved changes and
     * displays the warning if necessary. If there are no unsaved changes, the user saves, or the
     * user presses the "do not save" button, then a fileChooser which only allows text files to
     * be selected is opened. If the text file the user selects contains a valid PDA, the old PDA
     * is cleared and replaced with the loaded one. Else, an error Alert is shown to the user.
     * See subsection 4.7.3 of the report for further details.
     */
    @FXML
    private void open() {
        // If checkForUnsavedChanges returns true, then the user closed the dialog so return from
        // this method to prevent the open operation from happening
        if (checkForUnsavedChanges()) {
            return;
        }

        fileChooser.setTitle("Open");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Documents " +
                "(*.txt)", "*.txt"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                // Set the initial directory the fileChooser opens into to the directory in which
                // the specified text file is within so future Open/Save as operations go to that
                // directory.
                fileChooser.setInitialDirectory(file.getParentFile());
                String fullString = new String(Files.readAllBytes(Paths.get(file.toString())));
                loadPDAFromString(fullString, file);
            } catch (IOException e) {
                displayInvalidFileAlert();
            }
        }
    }

    /**
     * Creates and displays the Alert that is shown to the user when they attempt to load a file
     * that does not contain a valid PDA.
     */
    private void displayInvalidFileAlert() {
        String title = "Invalid file specified";
        String contentText = "The file you have specified does not contain a valid PDA.";
        createAndDisplayErrorAlert(title, contentText);
    }

    /**
     * Creates and displays error Alerts.
     *
     * @param title       The desired title of the Alert
     * @param contentText The desired content text of the Alert
     */
    private void createAndDisplayErrorAlert(String title, String contentText) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle(title);
        error.setContentText(contentText);
        Stage stage = (Stage) error.getDialogPane().getScene().getWindow();
        stage.getIcons().add(APPLICATION_ICON);
        error.show();
    }

    /**
     * Attempts to load a PDA from a string. If this fails (because the file does not contain a
     * valid PDA), an error dialog is displayed to the user. A proper PDA string consists of two
     * parts. The first part contains all the information about the PDA that the PDA class uses /
     * needs to know. The second part contains everything relevant to the frontend that is not
     * stored in the PDA object. See subsection 4.7.3 of the report for further details.
     *
     * @param fullString The complete string containing the PDA object information and the
     *                   frontend information
     * @param file       The file the string was read from (null if the string being loaded is a
     *                   sample)
     */
    private void loadPDAFromString(String fullString, File file) {
        try {
            int middle = fullString.indexOf("}{");
            String pdaString = fullString.substring(0, middle + 1);
            String frontendString = fullString.substring(middle + 1);

            // Use a dummy PDA object to see if the file contains a real PDA. This is necessary
            // since the reset method should not be called unless the user specifies a valid PDA.
            PDA pda2 = new PDA();
            pda2.loadPDAFromString(pdaString);

            // If this is reached, the file at least contains a valid PDA string so attempt to load
            // the PDA.

            // Pass false to the reset method so that the PDA starts off empty
            reset(false);
            // Doing this again on the pda field in this class rather than on another PDA object
            // means all the listeners added to the pda object will fire, thus creating
            // PDAStateNodes and transition arrows, updating the determinism of the PDA, etc.
            pda.loadPDAFromString(pdaString);
            ArrayList<String> acceptingStates = pda.getAcceptingStates();

            int openSquareBracketIndex = frontendString.indexOf("[");
            String stateNodesString = frontendString.substring(1, openSquareBracketIndex);

            String[] stateNodesWithPositions = stateNodesString.split(", ");
            // Set the layoutX and layoutY of each PDAStateNode using the saved values
            for (String stateNodeWithPosition : stateNodesWithPositions) {
                for (PDAStateNode stateNode : stateNodes) {
                    int firstCommaIndex = stateNodeWithPosition.indexOf(",");
                    String stateName = stateNodeWithPosition.substring(1, firstCommaIndex);

                    // Set the layoutX and layoutY values of each PDAStateNode
                    if (stateNode.getStateName().equals(stateName)) {
                        int secondCommaIndex = stateNodeWithPosition.lastIndexOf(",");
                        String layoutX = stateNodeWithPosition.substring(firstCommaIndex + 1,
                                secondCommaIndex);
                        String layoutY =
                                stateNodeWithPosition.substring(secondCommaIndex + 1).replace(")",
                                        "");
                        stateNode.setLayoutX(Double.parseDouble(layoutX));
                        stateNode.setLayoutY(Double.parseDouble(layoutY));
                    }

                    // Tick the checkboxes in the frontend of all the accepting states
                    if (acceptingStates.contains(stateNode.getStateName())) {
                        stateNode.makeAcceptingState();
                    }
                }
            }

            // Increase the size of the Pane to be able to fit all children. The Pane can only
            // grow vertically.
            double maxX = 0;
            double maxY = 0;
            for (PDAStateNode stateNode : stateNodes) {
                if (stateNode.getLayoutX() > maxX) {
                    maxX = stateNode.getLayoutX();
                }
                if (stateNode.getLayoutY() > maxY) {
                    maxY = stateNode.getLayoutY();
                }
            }
            // Set the size of the pane to the minimum size required to fit all children
            canvas.setMinWidth(maxX + 150);
            canvas.setMinHeight(maxY + 50);


            // Select the correct acceptance criteria from the menu
            switch (pda.getAcceptanceCriteria()) {
                case ACCEPTING_STATE:
                    acceptanceMenu.getToggles().get(0).setSelected(true);
                    break;
                case EMPTY_STACK:
                    acceptanceMenu.getToggles().get(1).setSelected(true);
                    break;
                case BOTH:
                    acceptanceMenu.getToggles().get(2).setSelected(true);
            }

            int equalsIndex = frontendString.indexOf("=");
            int notesIndex = frontendString.indexOf("\n");
            String transitionFunction =
                    frontendString.substring(equalsIndex + 2, notesIndex).replace("]}", "");
            transitionFunction = transitionFunction.trim();

            // Select the appropriate transition function from the menu and invoke its respective
            // method
            switch (TransitionFunction.valueOf(transitionFunction)) {
                case SYMBOL_AND_SYMBOL: {
                    onSymbolAndSymbolTransitionClick();
                    transitionMenu.getToggles().get(0).setSelected(true);
                    break;
                }
                case SYMBOL_AND_STRING: {
                    onSymbolAndStringTransitionClick();
                    transitionMenu.getToggles().get(1).setSelected(true);
                    break;
                }
                case STRING_AND_STRING: {
                    onStringAndStringTransitionClick();
                    transitionMenu.getToggles().get(2).setSelected(true);
                }
            }

            // Set the text of the initialStackSymbolDialog to the initial stack symbol if it is
            // not null and select the Initial Stack Symbol menuItem.
            TextField textField = initialStackSymbolDialog.getEditor();
            if (pda.getInitialStackSymbol() != null) {
                textField.setText(pda.getInitialStackSymbol());
                stackMenu.getToggles().get(1).setSelected(true);
            } else {
                textField.setText("");
                emptyStack.setSelected(true);
            }

            notes.setText(frontendString.substring(notesIndex + 1));

            // Update the file field with the value of the file parameter. If the file parameter
            // was null, then the file field should also be null since the PDA was not loaded from a
            // file. Otherwise, the file field should point to the same file as the one the PDA was
            // successfully loaded from.
            this.file = file;
        } catch (StringIndexOutOfBoundsException | IllegalArgumentException e) {
            displayInvalidFileAlert();
        }
    }

    /**
     * The save function of the simulator. If the file field is null (the user has not already
     * opened or saved a file), then just call the saveAs() method. Otherwise, write the string
     * representation of the PDA to the file. See subsection 4.7.4 of the report for further
     * details.
     *
     * @return True if the save operation was successfully completed and false otherwise
     */
    @FXML
    private boolean save() {
        if (file == null) {
            return saveAs();
        } else {
            String completePDA = generateString();
            writeStringToFile(completePDA);
            return true;
        }
    }

    /**
     * saveAs allows the user to specify an existing or a new file to which the PDA will be saved.
     * Also, update the file field to point to the file the user specified (if they did specify a
     * file) so that the save method will save directly to that file rather than opening the saveAs
     * dialog. See subsection 4.7.4 of the report for further details.
     *
     * @return True if the automaton was successfully saved and false otherwise
     */
    @FXML
    private boolean saveAs() {
        fileChooser.setTitle("Save as");
        // Only allow the user to see/select text files
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Documents " +
                "(*.txt)", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            String completePDA = generateString();
            this.file = file;
            fileChooser.setInitialDirectory(file.getParentFile());
            writeStringToFile(completePDA);
            return true;
        }
        return false;
    }

    /**
     * Generates a string that contains all the information needed to reconstruct the PDA the
     * user has built. This is achieved by first calling the toString method of the pda object
     * and then appending information that is only known in the frontend. This includes the
     * positions of all the PDAStateNodes and the selected transition function.
     *
     * @return A complete string that can be used to rebuild the PDA and set up the frontend of
     * the application
     */
    private String generateString() {
        String originalPDA = pda.toString();
        StringBuilder frontendString = new StringBuilder("{");

        if (stateNodes.size() >= 1) {
            for (PDAStateNode stateNode : stateNodes) {
                frontendString.append("(").append(stateNode.getStateName()).append(",");
                frontendString.append(stateNode.getLayoutX()).append(",");
                frontendString.append(stateNode.getLayoutY()).append(")").append(", ");
            }

            int lastCommaIndex = frontendString.lastIndexOf(", ");
            frontendString.replace(lastCommaIndex, frontendString.length() - 1, "");
        }

        frontendString.append("[transition function = ");
        frontendString.append(selectedTransitionFunction.toString()).append("]");

        frontendString.append("}");
        frontendString.append("\n");
        frontendString.append(notes.getText());

        return originalPDA + frontendString;
    }

    /**
     * This method writes the given string to the file field. It is used to write the string that
     * contains all the information needed to reconstruct the PDA to the file specified by the file
     * field.
     *
     * @param completePDA The complete string representation of the PDA (PDA object and frontend
     *                    information).
     */
    private void writeStringToFile(String completePDA) {
        try {
            PrintWriter writer = new PrintWriter(file);
            writer.print(completePDA);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the acceptance criteria of the PDA to accepting state.
     */
    @FXML
    private void onAcceptingStatesAcceptanceClick() {
        pda.changeAcceptanceCriteria(AcceptanceCriteria.ACCEPTING_STATE);
    }

    /**
     * Changes the acceptance criteria of the PDA to empty stack.
     */
    @FXML
    private void onEmptyStackAcceptanceClick() {
        pda.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
    }

    /**
     * Changes the acceptance criteria of the PDA to both.
     */
    @FXML
    private void onBothAcceptanceClick() {
        pda.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
    }

    /**
     * Changes the initial stack symbol of the PDA to null.
     */
    @FXML
    private void onEmptyStackClick() {
        pda.setInitialStackSymbol(null);
    }

    /**
     * Displays the initial stack symbol dialog so the user can specify the initial stack symbol.
     */
    @FXML
    private void onInitialStackSymbolClick() {
        initialStackSymbolDialog.show();
    }

    // The three functions below need not change the PDA at all - the PDA always works with strings
    // of symbols since these include single symbols. Need to make sure that when the user is
    // entering the transitions though that the correct text appears and that the input is
    // restricted accordingly. See section 4.6 of the report for further details.

    /**
     * This function is invoked when the user selects the Pop symbol and push symbol transition
     * function from the transition function menu. The method updates the
     * selectedTransitionFunction, updates the text in various parts of the application, sets the
     * currentState CellFactory again and sets the text formatters used in the application to
     * specify the pop/push symbol/string. It is necessary to set the CellFactory again to force
     * the updateItem method of TransitionTableCell to be invoked and thus to update the table cells
     * with red backgrounds if they contain a string when they should only contain at most a single
     * symbol.
     */
    @FXML
    private void onSymbolAndSymbolTransitionClick() {
        selectedTransitionFunction = TransitionFunction.SYMBOL_AND_SYMBOL;
        updateText(pop, popTextField, "Pop symbol");
        popTextField.setTextFormatter(symbolTextFormatter());
        pdaStateNodeController.setPopTextFormatter(symbolTextFormatter());
        updateText(push, pushTextField, "Push symbol");
        pushTextField.setTextFormatter(symbolTextFormatter());
        pdaStateNodeController.setPushTextFormatter(symbolTextFormatter());
        currentState.setCellFactory(param -> new TransitionTableCell());
        pdaStateNodeController.transitionFunctionChanged();
    }

    /**
     * This function is invoked when the user selects the Pop symbol and push string transition
     * function from the transition function menu.
     */
    @FXML
    private void onSymbolAndStringTransitionClick() {
        selectedTransitionFunction = TransitionFunction.SYMBOL_AND_STRING;
        updateText(pop, popTextField, "Pop symbol");
        popTextField.setTextFormatter(symbolTextFormatter());
        pdaStateNodeController.setPopTextFormatter(symbolTextFormatter());
        updateText(push, pushTextField, "Push string");
        pushTextField.setTextFormatter(stringTextFormatter());
        pdaStateNodeController.setPushTextFormatter(stringTextFormatter());
        currentState.setCellFactory(param -> new TransitionTableCell());
        pdaStateNodeController.transitionFunctionChanged();
    }

    /**
     * This function is invoked when the user selects the Pop string and push string transition
     * function from the transition function menu.
     */
    @FXML
    private void onStringAndStringTransitionClick() {
        selectedTransitionFunction = TransitionFunction.STRING_AND_STRING;
        updateText(pop, popTextField, "Pop string");
        popTextField.setTextFormatter(stringTextFormatter());
        pdaStateNodeController.setPopTextFormatter(stringTextFormatter());
        updateText(push, pushTextField, "Push string");
        pushTextField.setTextFormatter(stringTextFormatter());
        pdaStateNodeController.setPushTextFormatter(stringTextFormatter());
        currentState.setCellFactory(param -> new TransitionTableCell());
        pdaStateNodeController.transitionFunctionChanged();
    }

    /**
     * Updates the text of the given TableColumn and the given TextField.
     *
     * @param column    The TableColumn that needs its text changed.
     * @param textField The TextField that needs its prompt text changed.
     * @param newText   The new text that the TableColumn and TextField will use.
     */
    private void updateText(TableColumn<PDATransition, String> column, TextField textField,
                            String newText) {
        column.setText(newText);
        textField.setPromptText(newText);
        textField.setText("");
        pdaStateNodeController.updateLabelText(newText);
    }

    /**
     * Loads sample 1. Before loading the sample, check for unsaved changes and cancel the
     * loading if the user presses cancel. Set sampleClicked to true to indicate that the file field
     * is null because a sample has been loaded.
     */
    @FXML
    private void loadSample1() {
        // If checkForUnsavedChanges returns true, then the user closed the dialog so return from
        // this method to prevent the loading of the sample from happening
        if (checkForUnsavedChanges()) {
            return;
        }
        String sample1 = "{states=[q0, q1, q2], transitions=[{(q0,a,) -> (A,q0)}, {(q0,b,A) -> (," +
                "q1)}, {(q1,,#) -> (,q2)}, {(q1,b,A) -> (,q1)}], initialState=q0, " +
                "acceptingStates=[], initialStackSymbol=#, acceptanceCriteria=EMPTY_STACK}{(q0," +
                "118.0,175.0), (q1,448.0,174.0), (q2,785.0,174.0) [transition function = " +
                "STRING_AND_STRING]}\n" +
                "This pushdown automaton over the alphabet {a, b} accepts input strings of the " +
                "form (a^n)(b^n) where n >= 0.";

        // Pass null as the file since this string is not from a user file
        loadPDAFromString(sample1, null);
        // Set sampleClicked to true since the user loaded a sample
        sampleClicked = true;
    }

    /**
     * Loads sample 2.
     */
    @FXML
    private void loadSample2() {
        if (checkForUnsavedChanges()) {
            return;
        }
        String sample2 = "{states=[q0, q1], transitions=[{(q0,a,) -> (1,q0)}, {(q0,,1) -> (1,q1)" +
                "}, {(q1,b,1) -> (,q1)}], initialState=q0, acceptingStates=[q1], " +
                "initialStackSymbol=null, acceptanceCriteria=BOTH}{(q0,334.0,293.0), (q1,676.0," +
                "294.0) [transition function = STRING_AND_STRING]}\n" +
                "Like sample 1, this pushdown automaton over the alphabet {a, b} accepts input " +
                "strings of the form (a^n)(b^n) where n >= 0.\n" +
                "\n" +
                "Note that the transition from q0 to q1 is only applicable once at least one 'a' " +
                "has been consumed from the input tape. This is because the stack starts off " +
                "empty and the transition from q0 to q1 is only applicable if there is a 1 on the" +
                " top of the stack.\n" +
                "\n" +
                "As the acceptance criteria requires the stack to be empty in addition for the " +
                "automaton to end in an accepting state, strings consisting of just the symbol " +
                "'a' are not accepted.";
        loadPDAFromString(sample2, null);
        sampleClicked = true;
    }

    /**
     * Loads sample 3.
     */
    @FXML
    private void loadSample3() {
        if (checkForUnsavedChanges()) {
            return;
        }
        String sample3 = "{states=[q0, q1], transitions=[{(q0,a,) -> (a,q0)}, {(q0,b,) -> (b,q0)" +
                "}, {(q0,,) -> (,q1)}, {(q1,a,a) -> (,q1)}, {(q1,b,b) -> (,q1)}], " +
                "initialState=q0, acceptingStates=[q1], initialStackSymbol=null, " +
                "acceptanceCriteria=BOTH}{(q0,390.0,304.0), (q1,804.0,304.0) [transition function" +
                " = SYMBOL_AND_SYMBOL]}\n" +
                "This PDA is defined over the alphabet {a,b} and accepts strings of the form w" +
                "(w^r) where w^r denotes the reverse of the string w. Some examples of such " +
                "strings are:\n" +
                "aa\n" +
                "abba\n" +
                "aaaaaabbbbbbaaaaaa";
        loadPDAFromString(sample3, null);
        sampleClicked = true;
    }

    /**
     * Loads sample 4.
     */
    @FXML
    private void loadSample4() {
        if (checkForUnsavedChanges()) {
            return;
        }
        String sample4 = "{states=[q0, q1, q2, q3, q4, q5], transitions=[{(q0,,) -> (#,q1)}, {" +
                "(q1,A,) -> (A,q2)}, {(q2,A,) -> (A,q2)}, {(q2,B,A) -> (,q3)}, {(q3,B,A) -> (,q3)" +
                "}, {(q3,A,) -> (A,q2)}, {(q3,,#) -> (#,q1)}, {(q1,B,) -> (B,q4)}, {(q4,B,) -> " +
                "(B,q4)}, {(q4,A,B) -> (,q5)}, {(q5,B,) -> (B,q4)}, {(q5,A,B) -> (,q5)}, {(q5,,#)" +
                " -> (#,q1)}], initialState=q0, acceptingStates=[q1], initialStackSymbol=null, " +
                "acceptanceCriteria=ACCEPTING_STATE}{(q0,44.0,383.0), (q1,333.0,384.0), (q2,537" +
                ".0,174.0), (q3,940.0,174.0), (q4,537.0,670.0), (q5,940.0,670.0) [transition " +
                "function = STRING_AND_STRING]}\n" +
                "This pushdown automaton accepts all strings over the alphabet {A, B} which " +
                "consist of an equal number of As and Bs, regardless of the order. This also " +
                "includes the empty string (0 As and Bs).\n" +
                "\n" +
                "Although samples 2 and 3 are nondeterministic, only this PDA and the one in " +
                "sample 5 have multiple accepting computations for some input strings. This is " +
                "only for strings with an interleaving of As and Bs such as ABAB. For strings " +
                "with no interleaving (like AABB), the automaton has only a single accepting " +
                "computation.";
        loadPDAFromString(sample4, null);
        sampleClicked = true;
    }

    /**
     * Loads sample 5.
     */
    @FXML
    private void loadSample5() {
        if (checkForUnsavedChanges()) {
            return;
        }
        String sample5 = "{states=[q0, q1], transitions=[{(q0,a,) -> (1,q0)}, {(q0,a,) -> (11,q0)" +
                "}, {(q0,,) -> (,q1)}, {(q1,b,1) -> (,q1)}], initialState=q0, " +
                "acceptingStates=[q1], initialStackSymbol=null, acceptanceCriteria=BOTH}{(q0,271" +
                ".0,292.0), (q1,688.0,293.0) [transition function = SYMBOL_AND_STRING]}\n" +
                "The final sample accepts the language (a^n)(b^m) where n <= m and m <= 2*n.\n" +
                "\n" +
                "Some examples of input strings accepted by this PDA include:\n" +
                "\n" +
                "ab\n" +
                "aabb\n" +
                "aabbb\n" +
                "\n" +
                "Like sample 4, this PDA can have multiple accepting computations for a given " +
                "input word.";
        loadPDAFromString(sample5, null);
        sampleClicked = true;
    }

    /**
     * Creates a PDA state in the pda object. The listeners will automatically create a PDAStateNode
     * so nothing else needs to be done.
     */
    @FXML
    private void onAddStateButtonClick() {
        pda.addState();
    }

    /**
     * Highlights the transitions that cause nondeterminism (if there are any) or unhighlights the
     * nondeterministic transitions, depending on whether they are currently highlighted or not.
     */
    @FXML
    private void onDeterminismButtonClick() {
        // Whenever the button is pressed, flip the value of nondeterminismHighlighted
        nondeterminismHighlighted = !nondeterminismHighlighted;

        // If nondeterminismHighlighted is true, then highlight the nondeterministic transitions
        // and otherwise unhighlight them.
        if (nondeterminismHighlighted) {
            HashSet<PDATransition> transitions = pda.getNondeterministicTransitions();
            for (PDATransition transition : transitions) {
                pdaStateNodeController.highlightTransition(transition);
            }
        }
        else {
            for (PDATransition transition : getTransitions()) {
                pdaStateNodeController.unhighlightTransition(transition);
            }
        }
    }

    /**
     * This function is called when the user clicks the add transition button underneath the
     * transition table. It first checks that the TextFields for the currentState and the newState
     * are not empty since transitions must specify the states involved. The input symbol, pop
     * symbol/string and push symbol/string can however be empty. If a TextField is invalid, it is
     * highlighted in red and the method.
     */
    @FXML
    private void onAddTransitionButtonClick() {
        String contentText = "The \"current state\" and the \"new state\" of a transition cannot " +
                "be blank.";

        if (textFieldIsInvalid(currentStateTextField)) {
            String title = "Missing current state";
            createAndDisplayErrorAlert(title, contentText);
            return;
        }

        if (textFieldIsInvalid(newStateTextField)) {
            String title = "Missing new state";
            createAndDisplayErrorAlert(title, contentText);
            return;
        }

        PDATransition transition = getTransitionFromTextFields();
        boolean successful = pda.addTransition(transition);
        if (!successful) {
            String title = "Transition already exists";
            contentText = "The transition you attempted to add already exists.";
            createAndDisplayErrorAlert(title, contentText);
        }
    }

    /**
     * Checks if the given TextField is invalid. The only TextFields that can possibly be invalid
     * are the ones used to specify the current state and the next state of the transition since
     * these cannot be empty.
     *
     * @param textField The TextField being checked
     * @return True if the TextField is invalid and false if it is valid.
     */
    private boolean textFieldIsInvalid(TextField textField) {
        if (textField == currentStateTextField || textField == newStateTextField) {
            return textField.getText().length() == 0;
        }
        return false;
    }

    /**
     * Use the text fields underneath the transition table to create a PDATransition and return it.
     *
     * @return The PDATransition specified by the text fields
     */
    private PDATransition getTransitionFromTextFields() {
        String currentState = currentStateTextField.getText();
        String inputSymbol = inputSymbolTextField.getText();
        String pop = popTextField.getText();
        String push = pushTextField.getText();
        String newState = newStateTextField.getText();
        return new PDATransition(currentState, inputSymbol, pop, push, newState);
    }

    /**
     * Deletes the transition specified by the TextFields underneath the transition table.
     */
    @FXML
    private void onDeleteTransitionButtonClick() {
        PDATransition transition = getTransitionFromTextFields();
        pda.deleteTransition(transition);
    }

    /**
     * Displays the help menu. Called when the user clicks on the "Show help" MenuItem.
     */
    @FXML
    private void showHelpMenu() {
        Dialog<String> helpMenu = new Dialog<>();
        helpMenu.setTitle("Help");
        DialogPane dialogPane = helpMenu.getDialogPane();

        FXMLLoader fxmlLoader = new FXMLLoader(PDASimulator.class.getResource("/FXML/" +
                "help-menu.fxml"));
        try {
            ScrollPane scrollPane = fxmlLoader.load();
            dialogPane.setContent(scrollPane);

        } catch (IOException e) {
            e.printStackTrace();
        }

        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(APPLICATION_ICON);
        helpMenu.show();
    }

    /**
     * This method checks if all the transitions abide by the selected transition function. If
     * the selected transition function allows popping and pushing of strings (i.e. it is
     * TransitionFunction.STRING_AND_STRING), then all transitions are automatically valid. If there
     * are any invalid transitions, create and display an error Alert.
     *
     * @return True if all transitions are valid and false otherwise.
     */
    private boolean transitionsAreValid() {
        String title = "Transition function error";
        if (selectedTransitionFunction == TransitionFunction.SYMBOL_AND_SYMBOL) {
            for (PDATransition transition : pda.getTransitions()) {
                String popString = transition.getPopString();
                // If either the pop string or push string has length > 1, the transition is invalid
                if (popString.length() > 1 || transition.getPushString().length() > 1) {
                    String contentText = "The selected transition function does not allow for " +
                            "popping or pushing of strings of stack symbols. Please select " +
                            "another transition function or edit any invalid transitions.";
                    createAndDisplayErrorAlert(title, contentText);
                    return false;
                }
            }
        }
        if (selectedTransitionFunction == TransitionFunction.SYMBOL_AND_STRING) {
            for (PDATransition transition : pda.getTransitions()) {
                // Only if the pop string has length > 1, the transition is invalid. Push string
                // can be of any length.
                if (transition.getPopString().length() > 1) {
                    String contentText = "The selected transition function does not allow for " +
                            "popping of strings of stack symbols. Please select another " +
                            "transition function or edit any invalid transitions.";
                    createAndDisplayErrorAlert(title, contentText);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if the PDA is unable to run. It cannot run if it is missing an initial state, if the
     * acceptance criteria requires there to be at least one accepting state but has none or if any
     * transitions are invalid.
     *
     * @return True if the PDA is invalid and therefore cannot be run and false otherwise.
     */
    private boolean pdaCannotRun() {
        if (pda.getInitialState() == null) {
            String title = "Missing initial state";
            String contentText = "The PDA needs an initial state in order to be ran. Please " +
                    "create an initial state by right-clicking on a state and selecting the" +
                    " checkbox.";
            createAndDisplayErrorAlert(title, contentText);
            return true;
        }
        if (pda.getAcceptanceCriteria() != AcceptanceCriteria.EMPTY_STACK) {
            if (pda.getAcceptingStates().isEmpty()) {
                String title = "Missing accepting state";
                String contentText = "The specified acceptance criteria requires there to be at " +
                        "least one accepting state for the PDA to be able to accept input words. " +
                        "Please create at least one accepting state by ticking the checkbox " +
                        "within a state or use the Empty stack acceptance criteria instead, if " +
                        "appropriate.";
                createAndDisplayErrorAlert(title, contentText);
                return true;
            }
        }

        return !transitionsAreValid();
    }

    /**
     * Tries to display all accepting computations for the given input. It first checks if the PDA
     * is able to run and if so, it attempts to find the accepting computations for the provided
     * input string. If getAcceptingComputations in the PDA class returns null, then there are no
     * accepting computations for the input string of any length, so inform the user with an Alert.
     * If the return value is instead not null but empty, then that means no accepting computations
     * were found in the length limit. In this case, the user is asked if they want to try again
     * with double the number of allowed steps. This process can happen repeatedly. If the
     * acceptingComputations field is non-empty, a dialog is shown to the user with all discovered
     * accepting computations within the length limit. See section 4.10 of the report for further
     * details.
     */
    @FXML
    private void onQuickRunButtonClicked() {
        if (pdaCannotRun()) {
            return;
        }

        // Since the PDA can run, attempt to find the accepting computations for the input string.
        acceptingComputations = pda.getAcceptingComputations(inputString.getText(),
                currentMaxSteps);

        // If acceptingComputations is null, then there are no accepting computations irrespective
        // of the length limit
        if (acceptingComputations == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Input string rejected");
            alert.setHeaderText(null);
            Label label = new Label("Input string has no accepting computations.");

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setContent(label);
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.getIcons().add(APPLICATION_ICON);
            alert.showAndWait();
            return;
        }

        // If acceptingComputations is empty, the PDA could not find an accepting computation
        // within the length limit
        if (acceptingComputations.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No accepting computations found");
            alert.setHeaderText(null);
            // Ask the user if they want to run for double the number of steps.
            Label label = new Label("There are no accepting computations for the input " +
                    "string of length " +
                    currentMaxSteps + " or less.\nWould you like to try again with a computation " +
                    "length limit of " + (2 * currentMaxSteps) + "?");

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setContent(label);
            Stage stage = (Stage) dialogPane.getScene().getWindow();
            stage.getIcons().add(APPLICATION_ICON);

            dialogPane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
            // Double the maximum number of steps and run the PDA again using this method so the
            // process can repeat
            dialogPane.lookupButton(ButtonType.YES).addEventFilter(ActionEvent.ACTION, event -> {
                currentMaxSteps *= 2;
                alert.close();
                onQuickRunButtonClicked();
            });
            // Reset the currentMaxSteps if the user selects the No button
            dialogPane.lookupButton(ButtonType.NO).addEventFilter(ActionEvent.ACTION, event ->
                    currentMaxSteps = DEFAULT_MAX_STEPS);

            // Remove the automatically generated ButtonType.OK
            dialogPane.getButtonTypes().remove(ButtonType.OK);
            alert.show();
            return;
        }

        // If this is reached, there is at least one accepting computation, so display the
        // accepting computation(s)

        // Reset the index
        index = 0;
        // Display the first computation in the list and update the title of the dialog accordingly
        displayComputation(acceptingComputations.get(index));
        acceptingComputationsDialog.setTitle("Computation " + (index + 1) + " of " +
                acceptingComputations.size());

        DialogPane dialogPane = acceptingComputationsDialog.getDialogPane();

        // Previous button should initially be disabled since there is no computation before the
        // first one in the list
        Button prev = (Button) dialogPane.lookupButton(ButtonType.PREVIOUS);
        prev.setDisable(true);

        // Next button should be disabled if there is only one computation in the list
        Button next = (Button) dialogPane.lookupButton(ButtonType.NEXT);
        next.setDisable(acceptingComputations.size() == 1);

        acceptingComputationsDialog.show();

        // Reset the max steps for future runs
        currentMaxSteps = DEFAULT_MAX_STEPS;
    }

    /**
     * This method displays the given PDA computation in the VBox of the acceptingComputationsDialog
     * by adding a VBox for each configuration in the computation to the outer
     * acceptingComputationsDialogVBox.
     *
     * @param computation The accepting PDA computation that needs to be displayed.
     */
    private void displayComputation(ArrayList<PDAConfiguration> computation) {
        // Start by clearing the previous computation
        acceptingComputationsDialogVBox.getChildren().clear();

        // Iterate over each configuration and generate a VBox that contains all the configuration
        // information
        for (PDAConfiguration configuration : computation) {
            VBox configurationBox = getConfigurationVBox(configuration);
            configurationBox.setMinWidth(467);

            // Add this configuration VBox to the dialog's VBox
            acceptingComputationsDialogVBox.getChildren().add(configurationBox);
        }
    }

    /**
     * Create a configurationVBox. This configuration VBox shows the current state, the current
     * stack and the current state of the input word
     *
     * @param configuration The configuration for which we want to generate a configuration VBox.
     * @return The VBox for the given configuration.
     */
    public static VBox getConfigurationVBox(PDAConfiguration configuration) {
        VBox configurationBox = new VBox();
        Label state = new Label("State: " + configuration.getState());
        state.setFont(new Font(15));
        Label stack = new Label("Stack: " + configuration.getStack());
        stack.setFont(new Font(15));
        HBox input = new HBox();

        String inputString = PDAConfiguration.getInputString();
        int inputStringIndex = configuration.getIndex();

        Label consumedInput = new Label();
        consumedInput.setFont(new Font(15));
        Label currentInputSymbol = new Label();
        currentInputSymbol.setFont(new Font(15));
        Label remainingInput = new Label();
        remainingInput.setFont(new Font(15));

        // Check how much of the input string has been consumed by comparing the configuration
        // index to the length of the input string
        if (inputStringIndex < inputString.length()) {
            // If the index is not 0, set the consumedInput Label's text to the portion of the
            // string that has already been consumed.
            if (inputStringIndex != 0) {
                consumedInput.setText(inputString.substring(0, inputStringIndex));
            }
            currentInputSymbol.setText(configuration.getInputSymbol());
            // The remainingInput Label should contain everything after the inputStringIndex
            remainingInput.setText(inputString.substring(inputStringIndex + 1));
        } else if (inputString.isEmpty()) {
            // Set the current input symbol to epsilon to denote the input string is the empty
            // string
            currentInputSymbol.setText("ε");
        } else {
            // If we reach here, the entire input string has been consumed, so we only need to
            // set the text of the consumedInput Label (and we set it to the entire input string).
            consumedInput.setText(inputString);
        }

        // Nest the consumedInput Label is a container with the style class "consumed". Cannot
        // set the style class of the configurationVBox or the HBox to "consumed" since otherwise
        // all Labels would have the text strikethrough CSS applied to them.
        Pane pane = new Pane();
        pane.getStyleClass().add("consumed");
        pane.getChildren().add(consumedInput);

        currentInputSymbol.setTextFill(Color.RED);

        input.getChildren().addAll(pane, currentInputSymbol, remainingInput);
        input.setSpacing(10);

        configurationBox.getChildren().addAll(state, stack, input);
        // The acceptingComputationsDialogVBox which this configurationVBox will be nested in has
        // configuration.css in its stylesheets already
        configurationBox.setId("pdaConfiguration");

        return configurationBox;
    }

    /**
     * If the user presses the step by step run, the PDA is checked to see if it can be run. If
     * so, a new window is launched which becomes the only interactive window of the application
     * while it is open. The StepByStepController then takes over from there. See subsection 4.11.1
     * of the report for further details.
     */
    @FXML
    private void onStepByStepButtonClicked() {
        if (pdaCannotRun()) {
            return;
        }
        // Pass the text in the inputString TextField to the initialise method so the step-by-mode
        // controller knows which input string to run the animation for
        SplitPane root = initialiseStepByStepModeController(inputString.getText());
        Stage stepByStepStage = new Stage();
        // Set the initOwner to the main stage and the initModality to Modality.WINDOW_MODAL so
        // that it is impossible for the user to interact with the main window while this new window
        // is open. This is to prevent changes from being made to the PDA object while the step-by-
        // step mode is running.
        stepByStepStage.initOwner(stage);
        stepByStepStage.initModality(Modality.WINDOW_MODAL);
        stepByStepStage.getIcons().add(APPLICATION_ICON);
        assert root != null;
        Scene scene = new Scene(root);
        stepByStepStage.setTitle("Step by step computation");
        stepByStepStage.setScene(scene);
        stepByStepStage.setMaximized(true);
        stepByStepStage.show();
    }

    /**
     * Initialises the StepByStepModeController with everything it needs and returns the root
     * element of the FXML document step-by-step-view.fxml so that it can be passed to the Scene of
     * the new window.
     *
     * @param inputString The input string which the step by step mode is to run the PDA on
     * @return The root element of the fxml document
     */
    private SplitPane initialiseStepByStepModeController(String inputString) {
        FXMLLoader fxmlLoader = new FXMLLoader(PDASimulator.class.getResource("/FXML/" +
                "step-by-step-view.fxml"));

        try {
            SplitPane root = fxmlLoader.load();
            StepByStepController stepByStepController = fxmlLoader.getController();
            // Get a non-editable version of the canvas Pane for use in the step by step mode window
            Pane canvas = pdaStateNodeController.getNonEditablePDAPane();
            stepByStepController.setPDAScrollPaneContent(canvas);
            // Set up the transition table of the step by step mode window
            stepByStepController.setTransitionTableContent(pda.getTransitions());
            stepByStepController.setColumnNames(pop.getText(), push.getText());
            // Set the initial computation that the step by step mode begins with
            PDAConfiguration initialConfiguration = pda.getInitialConfiguration(inputString);
            ArrayList<PDAConfiguration> computation = new ArrayList<>();
            computation.add(initialConfiguration);
            stepByStepController.createInitialComputation(computation);

            return root;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * When the user presses the animation button, the PDA is first checked to see if it can be
     * run. If so, a new window is launched which becomes the only window of the application while
     * it is open. The computation given to the AnimationController is a randomly generated
     * computation. See section 4.12 of the report for further details.
     */
    @FXML
    private void onAnimationButtonClicked() {
        if (pdaCannotRun()) {
            return;
        }
        String inputString = this.inputString.getText();
        showAnimationWindow(pda.getRandomComputation(inputString));
    }

    /**
     * Shows the animation window after initialising the AnimationController.
     *
     * @param computation The computation the animation is for.
     */
    private void showAnimationWindow(ArrayList<PDAConfiguration> computation) {
        SplitPane root = initialiseAnimationController(inputString.getText(), computation);
        Stage animationStage = new Stage();
        // Set the initOwner to the main stage and the initModality to Modality.WINDOW_MODAL so
        // that it is impossible for the user to interact with the main window while this new
        // window is open. This is to prevent changes from being made to the PDA object while the
        // animation mode is running.
        animationStage.initOwner(stage);
        animationStage.initModality(Modality.WINDOW_MODAL);
        animationStage.getIcons().add(APPLICATION_ICON);
        assert root != null;
        Scene scene = new Scene(root);
        animationStage.setTitle("Animation");
        animationStage.setScene(scene);
        animationStage.setMaximized(true);
        animationStage.show();
    }

    /**
     * Initialises the AnimationController with everything it needs and returns the root element
     * of the FXML document animation-view.fxml so that it can be passed to the Scene of the new
     * window. See section 4.12 of the report for further details.
     *
     * @param inputString The inputString the animation is going to run the PDA on
     * @param computation The computation the AnimationController will animate
     * @return The root of the fxml document
     */
    private SplitPane initialiseAnimationController(String inputString,
                                                    ArrayList<PDAConfiguration> computation) {
        FXMLLoader fxmlLoader = new FXMLLoader(PDASimulator.class.getResource("/FXML/" +
                "animation-view.fxml"));
        try {
            SplitPane root = fxmlLoader.load();
            AnimationController animationController = fxmlLoader.getController();
            // Get a non-editable version of the canvas Pane for use in the step by step mode window
            Pane canvas = pdaStateNodeController.getNonEditablePDAPane();
            animationController.setPDAScrollPaneContent(canvas);
            // Set up the transition table of the step by step mode window
            animationController.setTransitionTableContent(pda.getTransitions());
            animationController.setColumnNames(pop.getText(), push.getText());
            // Set the input string of the animation controller to the input string that was in
            // the TextField when the
            // animation button was pressed.
            animationController.setInputString(inputString);
            // Now that the animation controller has the input string, it can create the input tape
            animationController.createInputTape();
            // Set the computation that the animationController will play.
            animationController.setComputation(computation);

            return root;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Public static methods to allow the other controllers to interact with the PDA object

    /**
     * Gets the complete list of PDATransitions from the PDA object.
     *
     * @return The PDATransitions.
     */
    public static ObservableList<PDATransition> getTransitions() {
        return pda.getTransitions();
    }

    /**
     * Attempts to edit a transition of the PDA.
     *
     * @param oldTransition The transition that is to be edited.
     * @param newTransition The desired new transition the old transition should be edited into.
     * @return True if the edit was successful and false otherwise.
     */
    public static boolean editTransition(PDATransition oldTransition, PDATransition newTransition) {
        return pda.editTransition(oldTransition, newTransition);
    }

    /**
     * Deletes the provided transition from the PDA.
     *
     * @param transition The transition that needs to be deleted.
     */
    public static void deleteTransition(PDATransition transition) {
        pda.deleteTransition(transition);
    }

    /**
     * Attempts to add a new transition to the PDA.
     *
     * @return True if the transition was successfully added to the PDA and false otherwise.
     */
    public static boolean addTransition(PDATransition transition) {
        return pda.addTransition(transition);
    }

    /**
     * Gets the list of all accepting states from the PDA object.
     *
     * @return An ArrayList of all accepting states.
     */
    public static ArrayList<String> getAcceptingStates() {
        return pda.getAcceptingStates();
    }

    /**
     * Attempts to rename the provided state to the provided new name.
     *
     * @param state   The state that is to be renamed.
     * @param newName The new name for the state that is to be renamed.
     * @return True if the renaming was successful and false otherwise.
     */
    public static boolean renameState(String state, String newName) {
        return pda.renameState(state, newName);
    }

    /**
     * Changes whether the given state is accepting or not based on whether it is currently
     * accepting or not.
     *
     * @param state The state being modified.
     */
    public static void changeAcceptingState(String state) {
        pda.changeAcceptingState(state);
    }

    /**
     * Changes the initial state of the PDA to the given state.
     *
     * @param newInitialState The new initial state of the PDA.
     */
    public static void changeInitialState(String newInitialState) {
        pda.changeInitialState(newInitialState);
    }

    /**
     * Deletes the given state from the PDA.
     *
     * @param state The state to be deleted.
     */
    public static void deleteState(String state) {
        pda.deleteState(state);
    }

    /**
     * Gets the applicable transitions of the PDA for the given PDAConfiguration
     *
     * @param configuration The PDAConfiguration we want the applicable transitions for
     * @return The applicable transitions for the given PDAConfiguration
     */
    public static ArrayList<PDATransition> getApplicableTransitions(PDAConfiguration configuration) {
        return pda.getApplicableTransitions(configuration);
    }

    /**
     * Gets the PDAConfiguration obtained by applying a PDATransition in a given PDAConfiguration
     *
     * @param configuration The given PDAConfiguration
     * @param transition    The PDATransition to be applied in the given configuration
     * @return The new PDAConfiguration
     */
    public static PDAConfiguration applyTransition(PDAConfiguration configuration,
                                                   PDATransition transition) {
        return pda.applyTransition(configuration, transition);
    }

    /**
     * Gets the PDATransition that was applied to go from configuration to nextConfiguration.
     * @param configuration     The original configuration.
     * @param nextConfiguration The configuration obtained after applying a PDATransition.
     * @return                  The PDATransition that was applied to obtain nextConfiguration.
     */
    public static PDATransition getAppliedTransition(PDAConfiguration configuration,
                                                     PDAConfiguration nextConfiguration) {
        // Get the applicable transitions for configuration. It was one of these that was applied to
        // generate the nextConfiguration.
        ArrayList<PDATransition> applicableTransitions;
        applicableTransitions = pda.getApplicableTransitions(configuration);

        for (PDATransition transition : applicableTransitions) {
            PDAConfiguration newConfiguration = pda.applyTransition(configuration, transition);
            // If the PDAConfiguration generated by applying the transition is equal to
            // nextConfiguration, then return that transition
            if (newConfiguration.equals(nextConfiguration)) {
                return transition;
            }
        }
        return null;
    }

    /**
     * Check whether the PDAConfiguration is an accepting configuration or not
     *
     * @param configuration The PDAConfiguration that needs to be checked
     * @return True if it is an accepting configuration and false otherwise
     */
    public static boolean isAcceptingConfiguration(PDAConfiguration configuration) {
        return pda.isAcceptingConfiguration(configuration);
    }

    /**
     * Set the CSS ID of the given TextField to "error" so that it gets highlighted.
     *
     * @param textField The TextField with an error.
     */
    public static void highlightTextField(TextField textField) {
        textField.setId("error");
    }

    /**
     * Set the CSS ID of the given TextField to "" so that the CSS gets removed.
     *
     * @param textField The TextField with no errors.
     */
    public static void unhighlightTextField(TextField textField) {
        textField.setId("");
    }
}