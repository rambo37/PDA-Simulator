package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDATransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * This class is the controller for the PDAStateNode object. It provides functions to manipulate
 * the PDA and also to get Dialogs for adding/editing transitions for use by PDAStateNodes. The
 * MainController lets this controller know about certain changes (such as the transition function
 * being changed) so that this controller can change the TextFormatters used by certain TextFields,
 * highlight/unhighlight certain TextFields and tell the PDAStateNodes to highlight/unhighlight
 * transitions, if necessary.
 *
 * @author Savraj Bassi
 */

public class PDAStateNodeController {
    // vBox contains the main content of the dialogs to create and edit transitions
    private VBox vBox;
    // An ArrayList of all the PDAStateNode objects
    private ArrayList<PDAStateNode> stateNodes;
    // The text used to describe the pop TextField
    private String pop = "Pop symbol";
    // The text used to describe the push TextField
    private String push = "Push symbol";
    // TextFormatter for the pop TextField
    private TextFormatter<String> popTextFormatter;
    // TextFormatter for the push TextField
    private TextFormatter<String> pushTextFormatter;
    // The application icon for use in the dialogs
    private Image applicationIcon;

    // Below are all the components with fx:ids in transition-dialog-content.fxml.
    @FXML
    protected Label currentStateLabel;
    @FXML
    protected Label newStateLabel;
    @FXML
    protected Label popLabel;
    @FXML
    protected Label pushLabel;
    @FXML
    protected Label errorLabel;
    @FXML
    protected TextField inputSymbolTextField;
    @FXML
    protected TextField popTextField;
    @FXML
    protected TextField pushTextField;

    /**
     * Initialises the inputSymbolTextField. The inputSymbolTextField will never be able to specify
     * more than a single character. Therefore, it has its TextFormatter set once and only once.
     */
    @FXML
    public void initialize() {
        inputSymbolTextField.setTextFormatter(MainController.symbolTextFormatter());
    }

    /**
     * Sets the VBox used in both dialogs. This is called by the MainController when it
     * initialises the PDAStateNodeController.
     *
     * @param vBox The VBox that is used by both the create and edit transition dialogs.
     */
    public void setVBox(VBox vBox) {
        this.vBox = vBox;
    }

    /**
     * Sets the ArrayList of PDAStateNodes.
     *
     * @param stateNodes The list of all PDAStateNodes.
     */
    public void setStateNodes(ArrayList<PDAStateNode> stateNodes) {
        this.stateNodes = stateNodes;
    }

    /**
     * Sets the applicationIcon variable to the icon of the application for use in the dialogs.
     *
     * @param applicationIcon The application icon.
     */
    public void setApplicationIcon(Image applicationIcon) {
        this.applicationIcon = applicationIcon;
    }

    /**
     * Highlights all transitions that are considered to be invalid. A transition can only be
     * invalid if the selected transition function does not permit for popping/pushing of strings
     * but the transition does specify strings for popping/pushing (as opposed to a single symbol).
     * The highlighting is achieved by invoking the method to highlight an invalid transition in the
     * PDAStateNode class if that PDAStateNode object contains the transition.
     */
    private void highlightInvalidTransitions() {
        // A list of all transitions that are considered to be invalid.
        ArrayList<PDATransition> invalidTransitions = new ArrayList<>();
        for (PDATransition transition : MainController.getTransitions()) {
            String popString = transition.getPopString();
            String pushString = transition.getPushString();
            // If pop contains the text "symbol", then that means the transition function does not
            // permit popping of strings of stack symbols. Therefore, if the popString has a length
            // > 1, then it is invalid.
            if (pop.contains("symbol") && popString.length() > 1) {
                invalidTransitions.add(transition);
            }
            if (push.contains("symbol") && pushString.length() > 1) {
                invalidTransitions.add(transition);
            }
        }

        // Go through the invalid transitions and the PDAStateNodes and if the PDAStateNode
        // contains an invalid transition, then highlight the transition red.
        for (PDATransition transition : invalidTransitions) {
            for (PDAStateNode node : stateNodes) {
                if (node.hasTransition(transition)) {
                    node.highlightTransitionRed(transition);
                }
            }
        }
    }

    /**
     * Unhighlights transitions that are considered to be valid. A transition is valid if it does
     * not disobey the transition function. This means that if the transition function only permits
     * popping/pushing a single stack symbol in a transition, then a valid transition does not pop
     * / push more than a single symbol. If the transition function allows popping and pushing of
     * strings, then all transitions are considered valid. All valid transitions re unhighlighted by
     * this method which means that the PDAStateNode changes the colour back to normal.
     */
    private void unhighlightValidTransitions() {
        ArrayList<PDATransition> validTransitions = new ArrayList<>();
        for (PDATransition transition : MainController.getTransitions()) {
            String popString = transition.getPopString();
            String pushString = transition.getPushString();

            // All transitions are valid as long as neither column contains the text "symbol" and
            // the respective transition string has a length that is > 1.
            if (!(pop.contains("symbol") && popString.length() > 1) &&
                    !(push.contains("symbol") && pushString.length() > 1)) {
                validTransitions.add(transition);
            }
        }

        // Go through the valid transitions and the PDAStateNodes and if the PDAStateNode contains
        // a valid transition, then reset the transition colour.
        for (PDATransition transition : validTransitions) {
            for (PDAStateNode node : stateNodes) {
                if (node.hasTransition(transition)) {
                    node.resetTransitionColour(transition);
                }
            }
        }
    }

    /**
     * Updates the text of the appropriate label as well as the respective string to the new text.
     *
     * @param newLabelText The new text to be used by the label.
     */
    public void updateLabelText(String newLabelText) {
        // If the new text contains the word "Pop", then it is intended for the popLabel.
        if (newLabelText.contains("Pop")) {
            pop = newLabelText;
            popLabel.setText(newLabelText);
        } else {
            push = newLabelText;
            pushLabel.setText(newLabelText);
        }
    }

    /**
     * Highlights all invalid transitions and unhighlights all valid transitions when the transition
     * function changes.
     */
    public void transitionFunctionChanged() {
        highlightInvalidTransitions();
        unhighlightValidTransitions();
    }

    /**
     * Sets the text formatter for the PopTextField.
     *
     * @param textFormatter The new TextFormatter.
     */
    public void setPopTextFormatter(TextFormatter<String> textFormatter) {
        popTextFormatter = textFormatter;
        popTextField.setTextFormatter(textFormatter);
    }

    /**
     * Sets the text formatter for the PushTextField.
     *
     * @param textFormatter The new TextFormatter.
     */
    public void setPushTextFormatter(TextFormatter<String> textFormatter) {
        pushTextFormatter = textFormatter;
        pushTextField.setTextFormatter(textFormatter);
    }

    /**
     * Checks if the popTextField is valid, using the same definition of valid as
     * unhighlightValidTransitions().
     *
     * @return True if the popTextField is valid and false otherwise.
     */
    private boolean popTextFieldIsValid() {
        return !pop.contains("symbol") || popTextField.getText().length() <= 1;
    }

    /**
     * Checks if the popTextField is valid, using the same definition of valid as
     * unhighlightValidTransitions().
     *
     * @return True if the popTextField is valid and false otherwise.
     */
    private boolean pushTextFieldIsValid() {
        return !push.contains("symbol") || pushTextField.getText().length() <= 1;
    }

    /**
     * Checks if both the pop and push TextFields are valid.
     *
     * @return True if the popTextField is valid and false otherwise.
     */
    private boolean textFieldsAreValid() {
        return popTextFieldIsValid() && pushTextFieldIsValid();
    }

    /**
     * Checks if the pop and push TextFields are valid and highlights/unhighlights them
     * accordingly using the static methods provided in the MainController class.
     */
    private void checkTextFields() {
        if (popTextFieldIsValid()) {
            MainController.unhighlightTextField(popTextField);
        } else {
            MainController.highlightTextField(popTextField);
        }

        if (pushTextFieldIsValid()) {
            MainController.unhighlightTextField(pushTextField);
        } else {
            MainController.highlightTextField(pushTextField);
        }
    }

    /**
     * This method is called by the MainController whenever a transition is added to the list of
     * transitions in the PDA object. When this happens, a transition needs to be created between
     * the two PDAStateNodes. See subsection 4.3.3 of the report for further details.
     *
     * @param newTransition The newly created transition.
     */
    public void createTransition(PDATransition newTransition) {
        String currentState = newTransition.getCurrentState();
        String newState = newTransition.getNewState();

        for (PDAStateNode stateNode : stateNodes) {
            // Find the PDAStateNode this new transition goes to
            if (stateNode.getStateName().equals(newState)) {
                // Create a transition from the currentState PDAStateNode of the transition to
                // the newState PDAStateNode
                for (PDAStateNode pdaStateNode : stateNodes) {
                    if (pdaStateNode.getStateName().equals(currentState)) {
                        pdaStateNode.createExistingTransition(stateNode, newTransition);
                    }
                }
            }
        }
    }

    /**
     * Deletes the specified transition from whichever PDAStateNode had it. See subsection 4.3.5 of
     * the report for further details.
     *
     * @param deletedTransition The transition that was deleted/
     */
    public void deleteTransition(PDATransition deletedTransition) {
        for (PDAStateNode stateNode : stateNodes) {
            if (stateNode.hasTransition(deletedTransition)) {
                stateNode.deleteTransition(deletedTransition);
            }
        }
    }

    /**
     * Inner class to override the default ordering of the buttons of the edit transition dialog
     */
    private static class EditTransitionDialogPane extends DialogPane {
        @Override
        protected Node createButtonBar() {
            ButtonBar node = (ButtonBar) super.createButtonBar();
            node.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
            return node;
        }
    }

    /**
     * Returns a dialog for editing an existing PDATransition object filled with all relevant
     * information. See subsection 4.3.5 of the report for further details.
     *
     * @param transition the PDATransition to create the dialog for.
     */
    public Dialog<PDATransition> getEditTransitionDialog(PDATransition transition) {
        Dialog<PDATransition> dialog = new Dialog<>();
        dialog.setTitle("Edit transition");
        // Use the custom EditTransitionDialogPane
        dialog.setDialogPane(new EditTransitionDialogPane());
        DialogPane dialogPane = dialog.getDialogPane();
        ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType delete = new ButtonType("Delete");
        // Buttons will be added in this order and with equal spacing due to the use of the
        // custom DialogPane
        dialogPane.getButtonTypes().addAll(confirm, ButtonType.CANCEL, delete);

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(applicationIcon);

        // Remove the textFormatters before filling the textFields in case the current version of
        // the transition is inconsistent with the specified transition function.
        popTextField.setTextFormatter(null);
        pushTextField.setTextFormatter(null);

        // Set the text of the state labels to the states specified by the transition and set the
        // content of the TextFields to the input symbol, pop string and push string specified by
        // the transition.
        String currentState = transition.getCurrentState();
        String inputSymbol = transition.getInputSymbol();
        String popString = transition.getPopString();
        String pushString = transition.getPushString();
        String newState = transition.getNewState();

        currentStateLabel.setText(currentState);
        inputSymbolTextField.setText(inputSymbol);
        popTextField.setText(popString);
        pushTextField.setText(pushString);
        newStateLabel.setText(newState);

        // Bring back the textFormatters after the textFields have been filled
        popTextField.setTextFormatter(popTextFormatter);
        pushTextField.setTextFormatter(pushTextFormatter);

        // Initialize the text fields so that pressing the enter key moves to the next one and so
        // the first one (the one for the inputSymbol) is focused when the dialog is opened.
        initializeTextFields();
        // Check if any of the TextFields are invalid. If so, they will be highlighted.
        checkTextFields();
        // The content of this dialog is the vBox which contains the labels and the TextFields.
        dialogPane.setContent(vBox);

        // When the user presses the confirm button, attempt to edit the transition if it is valid.
        dialogPane.lookupButton(confirm).addEventFilter(ActionEvent.ACTION, event -> {
            String newInputSymbol = inputSymbolTextField.getText();
            String newPopString = popTextField.getText();
            String newPushString = pushTextField.getText();
            PDATransition newTransition = new PDATransition(currentState, newInputSymbol,
                    newPopString, newPushString,
                    newState);

            // If the text fields are not valid, do not bother committing the edit at all and
            // show an error message.
            if (!textFieldsAreValid()) {
                errorLabel.setVisible(true);
                errorLabel.setText("The transition you specified does not follow the selected " +
                        "transition function");
                // Consume the event to prevent the dialog from closing so that the user can see
                // the error message.
                event.consume();
            } else {
                // Show an appropriate error message and prevent dialog closure if the edit is
                // invalid.
                if (!MainController.editTransition(transition, newTransition)) {
                    errorLabel.setVisible(true);
                    errorLabel.setText("The transition you specified already exists");
                    event.consume();
                }
                // No need to do anything if the edit is successful since the dialog closes and
                // edit occurs.
            }
        });

        // When the user presses the delete button, just delete the transition with the method in
        // MainController.
        dialogPane.lookupButton(delete).addEventFilter(ActionEvent.ACTION,
                event -> MainController.deleteTransition(transition));

        // Make sure the errorLabel is not visible when the dialog is opened
        dialog.setOnShown(dialogEvent -> errorLabel.setVisible(false));

        return dialog;
    }


    /**
     * Set up the three textFields such that pressing the enter key in either of the first two
     * automatically makes the next textField obtain focus. Also, initially make the first textField
     * the one with focus and unhighlight the pop and push TextFields in case they were highlighted.
     */
    public void initializeTextFields() {
        inputSymbolTextField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                popTextField.requestFocus();
                event.consume();
            }
        });
        popTextField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                checkTextFields();
                pushTextField.requestFocus();
                event.consume();
            }
        });

        Platform.runLater(() -> {
            inputSymbolTextField.requestFocus();
            inputSymbolTextField.selectAll();
        });

        MainController.unhighlightTextField(popTextField);
        MainController.unhighlightTextField(pushTextField);
    }


    /**
     * Returns a dialog for creating a new PDATransition object. See subsection 4.3.2 of the report
     * for further details.
     *
     * @param stateNode the PDAStateNode which is trying to create the transition.
     * @param newState  the name of the PDAStateNode the transition goes to.
     */
    public Dialog<PDATransition> getCreateTransitionDialog(PDAStateNode stateNode,
                                                           String newState) {
        Dialog<PDATransition> dialog = new Dialog<>();
        dialog.setTitle("Create transition");
        DialogPane dialogPane = dialog.getDialogPane();
        ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.getIcons().add(applicationIcon);

        currentStateLabel.setText(stateNode.getStateName());
        inputSymbolTextField.setText("");
        popTextField.setText("");
        pushTextField.setText("");
        newStateLabel.setText(newState);

        initializeTextFields();
        dialogPane.setContent(vBox);

        // When the user presses the confirm button, attempt to create a transition with the
        // content of the TextFields.
        dialogPane.lookupButton(confirm).addEventFilter(ActionEvent.ACTION, event -> {
            String currentState = stateNode.getStateName();
            String inputSymbol = inputSymbolTextField.getText();
            String popString = popTextField.getText();
            String pushString = pushTextField.getText();
            PDATransition transition = new PDATransition(currentState, inputSymbol, popString,
                    pushString, newState);
            boolean successful = MainController.addTransition(transition);
            // If the transition already exists, show the user an error message and consume the
            // event to prevent closure
            if (!successful) {
                errorLabel.setVisible(true);
                errorLabel.setText("The transition you attempted to add already exists.");
                event.consume();
            }
        });

        dialog.setOnShown(dialogEvent -> errorLabel.setVisible(false));

        return dialog;
    }

    /**
     * Create and return a pane that contains all the PDAStateNodes laid out in the same way but
     * made non-editable. This is for use by the AnimationController and the
     * StepByStepModeController.
     *
     * @return A pane that contains the non-editable PDAStatNodes
     */
    public Pane getNonEditablePDAPane() {
        // The PDA must have at least one PDAStateNode (the initial state) before it is possible
        // to run. Therefore, it is safe to do this as this method is only called when the PDA is
        // able to run.
        Pane canvas = (Pane) stateNodes.get(0).getParent();

        Pane pane = new Pane();
        pane.setMinHeight(canvas.getHeight());
        pane.setMinWidth(canvas.getWidth());

        ArrayList<PDAStateNode> nonEditableStateNodes = new ArrayList<>();

        for (PDAStateNode stateNode : stateNodes) {
            // Create an identical non-editable node
            PDAStateNode nonEditableStateNode = new PDAStateNode(stateNode.getStateName(), this,
                    false);
            nonEditableStateNodes.add(nonEditableStateNode);
            pane.getChildren().add(nonEditableStateNode);
            // Give the new non-editable node the same position as the original version of the node
            nonEditableStateNode.setLayoutX(stateNode.getLayoutX());
            nonEditableStateNode.setLayoutY(stateNode.getLayoutY());
            // Make this non-editable PDAStateNode an accepting state if necessary
            if (MainController.getAcceptingStates().contains(stateNode.getStateName())) {
                nonEditableStateNode.makeAcceptingState();
            }
        }

        // Now that the PDAStateNodes have been created and positioned correctly, all that needs
        // to be done is to add the transitions.
        for (PDATransition transition : MainController.getTransitions()) {
            // Find the PDAStateNode this transition goes to
            String currentState = transition.getCurrentState();
            String newState = transition.getNewState();
            PDAStateNode newStateNode = null;
            for (PDAStateNode stateNode : nonEditableStateNodes) {
                if (stateNode.getStateName().equals(newState)) {
                    newStateNode = stateNode;
                }
            }
            // Create a transition from the currentState of the transition to the newState
            for (PDAStateNode stateNode : nonEditableStateNodes) {
                if (stateNode.getStateName().equals(currentState)) {
                    stateNode.createExistingTransition(newStateNode, transition);
                }
            }
        }

        return pane;
    }

    /* Public methods to interact with the PDA for use by PDAStateNodes */

    /**
     * Attempts to rename the provided state to the provided new name.
     *
     * @param state   The state that is to be renamed.
     * @param newName The new name for the state that is to be renamed.
     * @return True if the renaming was successful and false otherwise.
     */
    public boolean renameState(String state, String newName) {
        return MainController.renameState(state, newName);
    }

    /**
     * Changes whether the given state is accepting or not based on whether it is currently
     * accepting or not.
     *
     * @param state The state being modified.
     */
    public void changeAcceptingState(String state) {
        MainController.changeAcceptingState(state);
    }

    /**
     * Changes the initial state of the PDA to the given state.
     *
     * @param newInitialState The new initial state of the PDA.
     */
    public void changeInitialState(String newInitialState) {
        MainController.changeInitialState(newInitialState);
    }

    /**
     * Deletes the given state from the PDA.
     *
     * @param state The state to be deleted.
     */
    public void deleteState(String state) {
        MainController.deleteState(state);
    }

    /**
     * Deselects all PDAStateNodes.
     */
    public void deselectAllNodes() {
        for (PDAStateNode node : stateNodes) {
            node.deselectNode();
        }
    }

    /**
     * Moves all currently selected nodes by the x and y offsets if and only if all nodes can be
     * moved without leaving the canvas area. Also resizes the canvas pane, if necessary.
     * @param xOffset The horizontal displacement
     * @param yOffset The vertical displacement
     * @param event   The MouseEvent created by dragging a node
     */
    public void moveSelectedNodes(double xOffset, double yOffset, MouseEvent event) {
        // Determine if the drag is valid by seeing if any selected node ends up with a new x or y
        // coordinate that is less than or equal to 0. This prevents nodes from being dragged out of
        // the canvas pane to the left or to the top.
        boolean validDrag = true;
        for (PDAStateNode node : stateNodes) {
            if (node.isSelected()) {
                double newX = node.getLayoutX() + xOffset;
                double newY = node.getLayoutY() + yOffset;

                if (newY <= 0 || newX <= 0) {
                    validDrag = false;
                }
            }
        }

        // If none of the nodes would leave the canvas to the top or to the left by performing this
        // drag, then perform the drag on all selected nodes.
        if (validDrag) {
            for (PDAStateNode node : stateNodes) {
                if (node.isSelected()) {
                    double newX = node.getLayoutX() + xOffset;
                    double newY = node.getLayoutY() + yOffset;

                    node.setLayoutX(newX);
                    node.setLayoutY(newY);

                    // Update the x and y variables of the PDAStateNode to the new coordinates of
                    // the mouse since the node has been     moved
                    node.setX(event.getSceneX());
                    node.setY(event.getSceneY());

                    // Resize the parent Pane if necessary
                    node.resizeParentPane();
                }
            }
        }
    }
}