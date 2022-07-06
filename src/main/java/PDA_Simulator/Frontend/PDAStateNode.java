package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDATransition;
import javafx.animation.FillTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Stack;

/**
 * The PDAStateNode class provides the functionality for the PDA nodes in the frontend. This
 * includes the ability to drag them, to rename them, to delete them, to make them
 * initial/accepting states or not and to create/edit transitions. It also contains some code to
 * allow specific transitions to be highlighted/unhighlighted and code to return Transitions that
 * do the same thing.
 * <p>
 * This class also provides a non-editable version of PDAStateNodes. These cannot be dragged,
 * renamed, deleted, have their acceptance changed or become/no longer become the initial state.
 * This is for the step by step and animation modes.
 *
 * @author Savraj Bassi
 */

public class PDAStateNode extends Group {
    // The controller used by all PDAStateNodes
    private final PDAStateNodeController pdaStateNodeController;
    // Whether this PDAStateNode is editable or not
    private final boolean editable;
    // The colour used by transition arrows, the createTransitionSquare, and the initial state
    // triangle
    private final Color DARK_BLUE = Color.web("0x123edb");
    // The colour used by the outer rectangle of the PDAStateNode and by labels/transitions when
    // hovered over
    private final Color LIGHT_BLUE = Color.web("0x1bb4e4");
    // The colour the outer rectangle of a PDAStateNode becomes when a new transition arrow is
    // dragged over it
    private final Color GREEN = Color.web("0x1be42f");
    // A mapping of PDATransitions to their respective transition arrow
    private final HashMap<PDATransition, PDATransitionArrow> transitionsMap = new HashMap<>();
    // A visual representation of the stack for use in the animation
    private final VBox stackVBox = new VBox();
    // The rounded light blue rectangle that contains the state name, checkbox and the
    // createTransitionSquare
    private Rectangle outerRectangle;
    // A dark blue square which is used to create transitions by clicking and dragging
    private Rectangle createTransitionSquare;
    // A label that contains the name of the state
    private Label stateNameLabel;
    // TextField to edit the name of the state
    private TextField stateNameTextField;
    // Checkbox that determines whether the state is accepting or not
    private CheckBox checkBox;
    // A blue triangle on the left-hand side of the outerRectangle that indicates a state is the
    // initial state
    private Polygon initialStateTriangle;
    // x coordinate of a mouse click
    private double x;
    // y coordinate of a mouse click
    private double y;
    // The name of this state
    private String stateName;
    // A menu that contains options to change the initial state and to delete the state
    private ContextMenu menu;
    // A listener for the checkbox
    private ChangeListener<Boolean> acceptingStateListener;
    // The checkbox used in the ContextMenu to make a state the initial state
    private CheckBox isInitialState;
    // A listener for the isInitialState checkbox
    private ChangeListener<Boolean> isInitialStateListener;
    // The arrow that is currently being created when the user drags from the createTransitionSquare
    private PDATransitionArrow newTransitionArrow;
    // A loop transition arrow for this PDAStateNode
    private PDATransitionArrow loopTransition;
    // Whether this PDAStateNode is selected or not
    private boolean selected = false;

    /**
     * Creates a PDAStateNode by creating all the individual components and adding them to the
     * children of the Group
     *
     * @param stateName              The name of the state this PDAStateNode is for
     * @param pdaStateNodeController The controller for PDAStateNodes
     * @param editable               Whether this PDAStateNode can be edited or not
     */
    public PDAStateNode(String stateName, PDAStateNodeController pdaStateNodeController,
                        boolean editable) {
        super();
        this.stateName = stateName;
        this.pdaStateNodeController = pdaStateNodeController;
        this.editable = editable;

        createOuterRectangle();
        createTransitionSquare();
        createStateNameLabel();
        createStateNameTextField();
        createAcceptingStateCheckBox();
        createInitialStateTriangle();
        createLoopTransition();
        createStackVBox();

        super.getChildren().add(outerRectangle);
        super.getChildren().add(createTransitionSquare);
        super.getChildren().add(stateNameLabel);
        super.getChildren().add(stateNameTextField);
        super.getChildren().add(checkBox);
        super.getChildren().add(initialStateTriangle);

        // Set the initial position of a newly created PDAStateNode to (50, 50) so it is not
        // right in the corner.
        // Note that the y-axis is inverted (it is positive in the down direction).
        setLayoutX(50);
        setLayoutY(50);

        // Create the menu for deleting/changing the initial state when a PDAStateNode is
        // right-clicked.
        createMenu();
    }

    /**
     * Creates the outer rectangle of the PDAStateNode. Set the on mouse pressed and on mouse
     * dragged events for this rectangle so that the user can drag the PDAStateNode by clicking
     * and dragging the outer rectangle. Also, show the menu if the rectangle is right-clicked.
     */
    private void createOuterRectangle() {
        outerRectangle = new Rectangle(150, 50);
        outerRectangle.setArcHeight(20);
        outerRectangle.setArcWidth(20);
        outerRectangle.setFill(LIGHT_BLUE);
        // Do not add the mouse pressed/dragged functionality if this node is not editable
        if (editable) {
            setOnMousePressedEvent(outerRectangle);
            setOnMouseDraggedEvent(outerRectangle);
            // Change the cursor to indicate the node can be dragged
            outerRectangle.setOnMouseEntered(event -> getScene().setCursor(Cursor.MOVE));
            outerRectangle.setOnMouseExited(event -> getScene().setCursor(Cursor.DEFAULT));
        }
    }

    /**
     * Adds an onMousePressed event handler to the node passed as the argument which allows the
     * PDAStateNode to be moved if the mouse click is a left-click. If the mouse click is a right-
     * click, then display the menu instead. See subsection 4.3.1 of the report for further details.
     *
     * @param node The Node which we want to be able to click on to initiate dragging.
     */
    private void setOnMousePressedEvent(Node node) {
        node.setOnMousePressed((event) -> {
            // Create a save of the current simulator state as soon as the node is clicked on so
            // that if any drag occurs, it can be undone
            pdaStateNodeController.storeSimulatorState();
            if (event.isPrimaryButtonDown()) {
                // Update the x and y fields of this PDAStateNode to point to wherever the user
                // clicks.
                x = event.getSceneX();
                y = event.getSceneY();

                // Bring the node to the front so the user can still clearly see it if they drag it
                // over something else
                toFront();
                // Close the menu in case it was open
                menu.hide();

                // If this node was not already selected, select this node and also deselect all
                // other nodes.
                if (!selected) {
                    pdaStateNodeController.deselectAllNodes();
                    selectNode();
                }
            } else {
                // The menu will open on the right-hand side but the vertical position depends on
                // whether the loop transition for this PDAStateNode is present or not. If it is
                // present, shift the position the menu is shown at by 70 pixels down so that it
                // is consistent with PDAStateNodes with no loop transition.
                if (super.getChildren().contains(loopTransition)) {
                    menu.show(this, Side.RIGHT, 1, 70);
                } else {
                    menu.show(this, Side.RIGHT, 1, 1);
                }
            }
        });
    }

    /**
     * Adds an EventHandler to the node passed as the argument that allows for dragging of
     * PDAStateNodes.
     *
     * @param node The Node which can be dragged in order to drag the PDAStateNode.
     */
    private void setOnMouseDraggedEvent(Node node) {
        node.setOnMouseDragged((event) -> {
            // Only allow dragging if the mouse click is a left-click
            if (event.isPrimaryButtonDown()) {
                // Calculate the offsets based on the initial coordinates the drag started from
                // (x and y) and the current coordinates of the mouse. x and y are updated to the
                // new coordinates of the mouse as the node is dragged.
                double xOffset = event.getSceneX() - x;
                double yOffset = event.getSceneY() - y;

                // Move all currently selected nodes by the amount this particular node is moved
                pdaStateNodeController.moveSelectedNodes(xOffset, yOffset, event);
            }
        });
    }

    /**
     * Sets the mouse x coordinate.
     *
     * @param newX The new x coordinate of the mouse.
     */
    public void setX(double newX) {
        x = newX;
    }

    /**
     * Sets the mouse y coordinate.
     *
     * @param newY The new y coordinate of the mouse.
     */
    public void setY(double newY) {
        y = newY;
    }

    /**
     * Resizes the Pane in which all the PDAStateNodes are contained to the minimum size needed
     * for all PDAStateNodes to be visible.
     */
    public void resizeParentPane() {
        Pane canvas = (Pane) getParent();

        ObservableList<Node> children = canvas.getChildren();
        double maxX = 0;
        double maxY = 0;

        // Find the largest x and y coordinates of all PDAStateNodes
        for (Node child : children) {
            if (child.getLayoutX() > maxX) {
                maxX = child.getLayoutX();
            }
            if (child.getLayoutY() > maxY) {
                maxY = child.getLayoutY();
            }
        }
        // Increase the width of the pane to the minimum size required to fit all children. + 150
        // because width is 150
        canvas.setMinWidth(maxX + 150);

        // Increase the height of the pane to the minimum size required to fit all children. + 50
        // because height is 50
        canvas.setMinHeight(maxY + 50);
    }

    /**
     * Creates the dark blue square within the PDAStateNode which is used for creating
     * transitions. Transitions are created by clicking and dragging from the
     * createTransitionSquare. This creates an arrow which follows the mouse as the user moves it.
     * If the user wishes to create a transition from a state to itself (a loop transition), then
     * just clicking once is sufficient rather than dragging the arrow into the PDAStateNode.
     * See subsection 4.3.2 of the report for further details.
     */
    private void createTransitionSquare() {
        createTransitionSquare = new Rectangle(25, 25);
        createTransitionSquare.setArcHeight(5);
        createTransitionSquare.setArcWidth(5);
        createTransitionSquare.setFill(DARK_BLUE);
        createTransitionSquare.setStroke(Color.BLACK);
        createTransitionSquare.setStrokeType(StrokeType.INSIDE);
        createTransitionSquare.setLayoutX(116);
        createTransitionSquare.setLayoutY(12);

        // Do not add the event handlers to allow creating transitions if this PDAStateNode is not
        // editable
        if (!editable) {
            return;
        }

        createTransitionSquare.setOnMouseEntered(event -> getScene().setCursor(Cursor.CROSSHAIR));
        createTransitionSquare.setOnMouseExited(event -> getScene().setCursor(Cursor.DEFAULT));

        // When the square is clicked on, set everything up for dragging to occur. This involves
        // creating a new transition arrow which starts from the centre of the square.
        createTransitionSquare.setOnMousePressed((event) -> {
            // Update the x and y coordinates to point to the new mouse coordinates
            x = event.getSceneX();
            y = event.getSceneY();

            // Create a new PDATransitionArrow for this transition being created.
            newTransitionArrow = new PDATransitionArrow(this);
            Line line = newTransitionArrow.getLine();

            // Start the line from the center of the square
            line.setStartX(getLayoutX() + 128);
            line.setStartY(getLayoutY() + 24);
            line.setEndX(getLayoutX() + 128);
            line.setEndY(getLayoutY() + 24);

            Pane canvas = (Pane) getParent();
            canvas.getChildren().add(newTransitionArrow);
        });

        createTransitionSquare.setOnMouseDragged((event) -> {
            getScene().setCursor(Cursor.CROSSHAIR);
            double xOffset = event.getSceneX() - x;
            double yOffset = event.getSceneY() - y;

            Line line = newTransitionArrow.getLine();

            // Set the end coordinates of the line to the amount the mouse has moved. No need to do
            // anything for the arrowhead as that is bound to the end of the line. Also, no need to
            // prevent the line leaving the canvas Pane since if the user releases the mouse button,
            // the line will be removed.
            line.setEndX(xOffset + line.getEndX());
            line.setEndY(yOffset + line.getEndY());

            Pane canvas = (Pane) getParent();
            ObservableList<Node> children = canvas.getChildren();

            // Highlight PDAStateNodes when the line is dragged over them so that the user can
            // tell that releasing the left mouse button will add a transition. Set the colour
            // back to the original colour if the line is not within the PDAStateNode.
            for (Node child : children) {
                if (child.getClass() == PDAStateNode.class) {
                    PDAStateNode node = (PDAStateNode) child;
                    if (node.containsCoordinates(line.getEndX(), line.getEndY())) {
                        node.changeBackgroundColour(GREEN);
                    } else {
                        node.changeBackgroundColour(LIGHT_BLUE);
                    }
                }
            }

            // Update the x and y coordinates with the new mouse coordinates
            x = event.getSceneX();
            y = event.getSceneY();
        });

        // Upon mouse release, check if the newTransitionArrow was released inside a PDAStateNode
        // and if so, get and show the create transition dialog.
        createTransitionSquare.setOnMouseReleased(mouseEvent -> {
            getScene().setCursor(Cursor.DEFAULT);
            Pane canvas = (Pane) getParent();
            ObservableList<Node> children = canvas.getChildren();

            Line line = newTransitionArrow.getLine();

            // The needToRemoveNewTransitionArrow boolean indicates whether this newly created
            // transition arrow should be removed once this event finishes.
            boolean needToRemoveNewTransitionArrow = true;
            for (Node child : children) {
                if (child.getClass() == PDAStateNode.class) {
                    PDAStateNode node = (PDAStateNode) child;
                    if (node.containsCoordinates(line.getEndX(), line.getEndY())) {
                        // Create the dialog for creating transitions
                        Dialog<PDATransition> createTransitionDialog =
                                pdaStateNodeController.getCreateTransitionDialog(this,
                                        node.stateName);
                        // Invoke onAddTransitionDialogClose whenever the dialog closes, regardless
                        // of whether the transition was created or not. This is because the
                        // newTransitionArrow/loopTransition should be removed since a new one
                        // will be created anyway if the transition is created. If the transition
                        // is not created, then it also will need to be removed.
                        createTransitionDialog.setOnCloseRequest(dialogEvent ->
                                onAddTransitionDialogClose());

                        if (node != this) {
                            PDATransition transition = transitionBetween(node);
                            // Check if there is already a transition arrow. If not, set the start
                            // and end nodes of the newTransitionArrow so that adjustLine can be
                            // called. This is to ensure that the transition is laid out in the
                            // same way it would be if it actually existed.
                            if (transition == null) {
                                needToRemoveNewTransitionArrow = false;
                                newTransitionArrow.setEndNode(node);
                                newTransitionArrow.adjustLine();
                            } else {
                                // As there is already a transition arrow, the new one needs to be
                                // removed.
                                needToRemoveNewTransitionArrow = true;
                            }
                        } else {
                            // If the PDAStateNode the line was released in was this node, show
                            // the loop transition to the user if it was not already visible
                            if (!super.getChildren().contains(loopTransition)) {
                                super.getChildren().add(loopTransition);
                            }
                            // Set the boolean to true since the user should not see the
                            // newTransitionArrow as they can already see the loopTransition arrow.
                            needToRemoveNewTransitionArrow = true;
                        }
                        // Show the dialog
                        createTransitionDialog.show();
                        // Change the colour of the node back to normal on mouse release
                        node.changeBackgroundColour(LIGHT_BLUE);
                    }
                }
            }

            // Remove the newTransitionArrow if necessary. This will happen if either the
            // newTransitionArrow was released in the same PDAStateNode that initiated the drag
            // (it is a loop transition) or if the new arrow was not released in any PDAStateNode
            // at all. Doing this now means it is removed while the dialog to create the new
            // transition is showing rather than after it is closed.
            if (needToRemoveNewTransitionArrow) {
                canvas.getChildren().remove(newTransitionArrow);
            }
        });
    }

    /**
     * Checks if the coordinate (x, y) is within this PDAStateNode. Note that the
     * initialStateTriangle is not considered to be within a PDAStateNode - rather it is just the
     * outer rectangle that is used.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return true if this PDAStateNode contains the coordinate and false otherwise.
     */
    public boolean containsCoordinates(double x, double y) {
        double minX = getLayoutX();
        double maxX = minX + 150;
        double minY = getLayoutY();
        double maxY = minY + 50;

        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    /**
     * Changes the colour of the outer rectangle part of the PDAStateNode to the specified colour
     *
     * @param colour The new colour
     */
    public void changeBackgroundColour(Color colour) {
        outerRectangle.setFill(colour);
    }

    /**
     * Removes the new transition arrow and the loopTransition if there is not at least one Label
     * in the VBox of the loopTransition group. This method always executes when the add
     * transition dialog closes - regardless of whether a transition was created. This is done
     * because a new transition arrow will be created automatically if a transition is
     * successfully created. Therefore, the newTransitionArrow/loopTransition needs to be removed to
     * prevent two overlapping arrows being created.
     */
    private void onAddTransitionDialogClose() {
        Pane canvas = (Pane) getParent();
        canvas.getChildren().remove(newTransitionArrow);
        VBox vBox = loopTransition.getVBox();
        if (vBox.getChildren().size() == 0) {
            super.getChildren().remove(loopTransition);
        }
    }

    /**
     * Creates the label which contains the name of this PDAStateNode. If the label is
     * double-clicked, it is replaced with a text field to allow the user to edit the name of the
     * state. The label has the same behaviour as the outer rectangle when clicked or dragged. That
     * is, on right-click display the edit menu, on left-click start the dragging of the
     * PDAStateNode and on mouse drag, move the PDAStateNode.
     */
    private void createStateNameLabel() {
        stateNameLabel = new Label(stateName);
        stateNameLabel.setLayoutX(36);
        stateNameLabel.setLayoutY(15);
        stateNameLabel.setAlignment(Pos.CENTER);
        stateNameLabel.setTextAlignment(TextAlignment.CENTER);
        stateNameLabel.setPrefHeight(17);
        stateNameLabel.setPrefWidth(78);

        // Do not add the event handlers to the stateNameLabel if this PDAStateNode is not editable.
        if (!editable) {
            return;
        }
        // Replace the stateNameLabel with the  stateNameTextField on double click
        stateNameLabel.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                if (mouseEvent.getClickCount() == 2) {
                    stateNameTextField.setVisible(true);
                    stateNameTextField.setText(stateName);
                    Platform.runLater(() -> {
                        stateNameTextField.requestFocus();
                        stateNameTextField.selectAll();
                    });
                    stateNameLabel.setVisible(false);
                }
            }
        });

        // Allow the PDAStateNode to be moved by clicking and dragging on the label and also for
        // the menu to open by right-clicking on the label.
        setOnMousePressedEvent(stateNameLabel);
        setOnMouseDraggedEvent(stateNameLabel);
        stateNameLabel.setOnMouseEntered(event -> getScene().setCursor(Cursor.MOVE));
        stateNameLabel.setOnMouseExited(event -> getScene().setCursor(Cursor.DEFAULT));
    }

    /**
     * Creates the stateNameTextField which is used to edit the name of the PDA state represented
     * by this PDAStateNode. It is initially invisible and is made visible when the user
     * double-clicks on the stateNameLabel. See subsection 4.3.6 of the report for further details.
     */
    private void createStateNameTextField() {
        stateNameTextField = new TextField();
        stateNameTextField.setVisible(false);
        stateNameTextField.setLayoutX(32);
        stateNameTextField.setLayoutY(13);
        stateNameTextField.setPrefHeight(17);
        stateNameTextField.setPrefWidth(78);
        stateNameTextField.setTextFormatter(MainController.stringTextFormatter());

        // If the user presses the enter key, check if the input is valid.
        stateNameTextField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // State names cannot be empty so highlight the textField red to inform the user
                if (stateNameTextField.getText().length() == 0) {
                    stateNameTextField.setId("error");
                } else {
                    // Create a save of the current simulator state before attempting to commit the
                    // state renaming so that it can be undone
                    pdaStateNodeController.storeSimulatorState();

                    // Set the state name string of this PDAStateNode to the name in the textField
                    // before renaming the state so that the initialState property listener in
                    // MainController can use the new name to decide whether to make this node an
                    // initial state. If the renaming is invalid, then revert stateName.
                    stateName = stateNameTextField.getText();
                    if (pdaStateNodeController.renameState(stateNameLabel.getText(),
                            stateNameTextField.getText())) {
                        stateNameLabel.setText(stateNameTextField.getText());
                        cancelStateRename();
                    } else {
                        // If the name of this state is already in use, then highlight the textField
                        stateNameTextField.setId("error");
                        stateName = stateNameLabel.getText();
                    }
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelStateRename();
            }
        });

        // Cancel the state renaming if the textField loses focus
        stateNameTextField.focusedProperty().addListener((observableValue, oldPropertyValue,
                                                          newPropertyValue) -> {
            if (!newPropertyValue) {
                cancelStateRename();
            }
        });
    }

    /**
     * Return the name of the state represented by this PDAStateNode. Use the stateName field
     * rather than the text of the stateNameLabel since this stateName field uses the new name of
     * the state in the event that it is renamed.
     *
     * @return The name of this state.
     */
    public String getStateName() {
        return stateName;
    }

    /**
     * Makes the stateNameLabel visible again and hides the stateNameTextField. Also clears the CSS
     * since the textField will start off valid the next time it is made visible.
     */
    private void cancelStateRename() {
        stateNameTextField.setVisible(false);
        stateNameTextField.setId("");
        stateNameLabel.setVisible(true);
    }

    /**
     * Creates the CheckBox within the PDAStateNode that allows the user to change whether this
     * state is an accepting state or not. See subsection 4.3.7 of the report for further details.
     */
    private void createAcceptingStateCheckBox() {
        checkBox = new CheckBox();
        checkBox.setLayoutX(12);
        checkBox.setLayoutY(15);

        acceptingStateListener = (observableValue, oldPropertyValue, newPropertyValue) -> {
            // If this PDAStateNode is not editable, do not let the change to the checkBox go
            // through by removing the listener, setting the selected value to the value before the
            // event and then adding the lister back. It repeatedly in a loop.
            if (!editable) {
                checkBox.selectedProperty().removeListener(acceptingStateListener);
                checkBox.setSelected(oldPropertyValue);
                checkBox.selectedProperty().addListener(acceptingStateListener);
            } else {
                // Create a save of the current simulator state whenever this listener fires so that
                // the change made to the accepting states can be undone
                pdaStateNodeController.storeSimulatorState();

                // If the node is editable, then change whether this state is accepting or not
                pdaStateNodeController.changeAcceptingState(stateName);
            }
        };

        checkBox.selectedProperty().addListener(acceptingStateListener);
    }

    /**
     * Selects the checkBox of this PDAStateNode to indicate it is an accepting state. Before
     * doing so, the listener of the checkBox is removed to prevent the listener firing and updating
     * the PDA incorrectly. The listener is then added back so that the user can make states
     * accepting or not accepting as normal.
     */
    public void makeAcceptingState() {
        checkBox.selectedProperty().removeListener(acceptingStateListener);
        checkBox.setSelected(true);
        checkBox.selectedProperty().addListener(acceptingStateListener);
    }

    /**
     * Creates the initial state triangle which lets the user tell which state is the initial state
     * easily. It is initially hidden.
     */
    private void createInitialStateTriangle() {
        initialStateTriangle = new Polygon();
        initialStateTriangle.getPoints().addAll(-50.0, 40.0, 50.0, 40.0, 0.0, -60.0);
        initialStateTriangle.setRotate(90);
        initialStateTriangle.setScaleX(0.4);
        initialStateTriangle.setScaleY(0.3);
        initialStateTriangle.setFill(DARK_BLUE);
        initialStateTriangle.setStroke(Color.BLACK);
        initialStateTriangle.setStrokeType(StrokeType.INSIDE);
        initialStateTriangle.setLayoutX(-16);
        initialStateTriangle.setLayoutY(36);
        initialStateTriangle.setVisible(false);
    }

    /**
     * Shows the initial state triangle for this node by making it visible.
     */
    public void showInitialStateTriangle() {
        initialStateTriangle.setVisible(true);
    }

    /**
     * Creates a PDATransitionArrow consisting of a QuadCurve, a Polygon and a VBox to represent a
     * loop arrow in the PDA.
     */
    private void createLoopTransition() {
        loopTransition = new PDATransitionArrow(this, this, false, true);
    }

    /**
     * Creates the menu that is shown when right-clicking on a PDAStateNode. The menu consists of
     * two MenuItems. The first contains a CheckBox which allows the user to make this state of
     * the PDA the initialState. The second MenuItem allows the user to delete this state from
     * the PDA. See subsections 4.3.8 and 4.3.9 of the report for further details.
     */
    private void createMenu() {
        menu = new ContextMenu();
        MenuItem initialState = new MenuItem("Initial state");
        isInitialState = new CheckBox();

        // This listener updates the initial state of the PDA to the state represented by this
        // PDAStateNode if the new value is true. If the new value is false, then it sets the
        // initial state to null.
        isInitialStateListener = (observableValue, oldValue, newValue) -> {
            // Create a save of the current simulator state as soon as the initial state is changed
            // so that any change to the initial state can be undone
            pdaStateNodeController.storeSimulatorState();
            if (newValue) {
                pdaStateNodeController.changeInitialState(stateName);
            } else {
                pdaStateNodeController.changeInitialState(null);
            }
        };
        // Add the listener to the selectedProperty of the isInitialState checkbox. This means it
        // will fire whenever the checkBox is selected or deselected.
        isInitialState.selectedProperty().addListener(isInitialStateListener);

        // Set the graphic of the initialState MenuItem so that it contains the checkbox in addition
        // to the text
        initialState.setGraphic(isInitialState);

        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(event -> {
            // Create a save of the current simulator state as soon as the delete menu item is
            // clicked on so that the state deletion can be undone
            pdaStateNodeController.storeSimulatorState();

            pdaStateNodeController.deleteState(stateName);
            Pane canvas = (Pane) getParent();
            canvas.getChildren().remove(this);
        });

        menu.getItems().addAll(initialState, delete);
    }

    /**
     * Makes this PDAStateNode the initial state. Remove listener before changing the selected
     * property so that the listener does not fire. Add it back afterwards so that it can be used to
     * respond to the user changing the initial state in the future. Show the initialStateTriangle
     * since this state is now the initial state.
     */
    public void makeInitialState() {
        isInitialState.selectedProperty().removeListener(isInitialStateListener);
        isInitialState.setSelected(true);
        isInitialState.selectedProperty().addListener(isInitialStateListener);
        initialStateTriangle.setVisible(true);
    }

    /**
     * Makes this PDAStateNode not the initial state. As with making a PDAStateNode the initial
     * state, first remove the listener and then add the listener back afterwards. Hide the
     * initialStateTriangle since this state is no longer the initial state.
     */
    public void makeNotInitialState() {
        isInitialState.selectedProperty().removeListener(isInitialStateListener);
        isInitialState.setSelected(false);
        isInitialState.selectedProperty().addListener(isInitialStateListener);
        initialStateTriangle.setVisible(false);
    }

    /**
     * Creates a transition arrow between the two PDAStateNodes. This method is called externally
     * whenever a transition is created. See subsection 4.3.3 of the report for further details.
     *
     * @param other      The PDAStateNode that this new transition arrow needs to connect to
     * @param transition The PDATransition
     */
    public void createExistingTransition(PDAStateNode other, PDATransition transition) {
        // Create a loop transition if the other node is this node
        if (other == this) {
            addNewLoopTransition(transition);
        } else {
            // If there is already a transition from the other state to this state, do not create
            // a normal transition since it would overlap with the other transition. Instead, create
            // a double transition.
            createTransition(other, transition, other.transitionBetween(this) != null);
        }
    }

    /**
     * Checks if this PDAStateNode already has an existing transition to another PDAStateNode.
     *
     * @param other The other PDAStateNode
     * @return The existing PDATransition if there is one and null otherwise.
     */
    private PDATransition transitionBetween(PDAStateNode other) {
        Set<PDATransition> transitions = transitionsMap.keySet();
        for (PDATransition transition : transitions) {
            if (transition.getCurrentState().equals(stateName) &&
                    transition.getNewState().equals(other.stateName)) {
                return transition;
            }
        }
        return null;
    }

    /**
     * Adds to the loop transition of this PDAStateNode a label which represents the loop transition
     * that has just been added to this PDAStateNode. Shifts the VBox in which the label will be
     * stored up so that the labels do not end up overlapping the loop arrow.
     *
     * @param transition The PDATransition that was just added to this state.
     */
    private void addNewLoopTransition(PDATransition transition) {
        VBox vBox = loopTransition.getVBox();
        loopTransition.addTransition(transition);
        // Shifting the VBox up is only necessary if there is more than one label
        if (vBox.getChildren().size() > 1) {
            vBox.setLayoutY(vBox.getLayoutY() - 17);
        }

        // Add to the map an entry which maps this PDATransition to the loopTransition
        transitionsMap.put(transition, loopTransition);
    }

    /**
     * Returns whether this PDAStateNode is editable.
     * @return Whether this PDAStateNode is editable or not,
     */
    public boolean isNotEditable() {
        return !editable;
    }

    /**
     * Returns the mapping between PDATransitions and their corresponding PDATransitionArrows.
     * @return Returns the map of PDATransitions to PDATransitionArrows.
     */
    public HashMap<PDATransition, PDATransitionArrow> getTransitionsMap() {
        return transitionsMap;
    }

    /**
     * Returns the dialog used to edit a particular transition.
     * @param transition The transition that is to be edited with this dialog.
     * @return A dialog to edit the given transition.
     */
    public Dialog<PDATransition> getEditTransitionDialog(PDATransition transition) {
        return pdaStateNodeController.getEditTransitionDialog(transition);
    }

    /**
     * Handles the creation of non-loop transition arrows in the PDA diagram, creating a new
     * PDATransitionArrow, if necessary. The isDoubleTransition argument indicates whether there is
     * a transition from one state to another state and vice versa. These require a different layout
     * from ordinary transitions since they would otherwise overlap. See subsection 4.3.3 of the
     * report for further details.
     *
     * @param other              The PDAStateNode that this PDAStateNode has a transition to (and
     *                           vice versa)
     * @param transition         The PDATransition from this PDAStateNode to the other node
     * @param isDoubleTransition Whether this is a double transition or not
     */
    private void createTransition(PDAStateNode other, PDATransition transition,
                                  boolean isDoubleTransition) {
        PDATransition existingTransition = transitionBetween(other);
        // If there is already a transition from this PDAStateNode to the other one, then just
        // add another Label to the VBox for that existing transition arrow rather than creating a
        // new one.
        if (existingTransition != null) {
            PDATransitionArrow existingTransitionArrow = transitionsMap.get(existingTransition);
            existingTransitionArrow.addTransition(transition);

            // Update the map to include a mapping from this new transition to this same transition
            // arrow
            transitionsMap.put(transition, existingTransitionArrow);
            return;
        }

        // Since there is not already a transition arrow from this node to the other node, create
        // one.
        PDATransitionArrow transitionArrow = new PDATransitionArrow(this, other, isDoubleTransition,
                false);

        // Invoke the addTransition method to create a Label for this newly added transition arrow
        transitionArrow.addTransition(transition);

        Pane canvas = (Pane) getParent();
        canvas.getChildren().add(transitionArrow);

        // Map this new PDATransition to this transition arrow
        transitionsMap.put(transition, transitionArrow);
    }

    /**
     * Creates the VBox which represents the stack of the pushdown automaton during the animation.
     * It is initially not visible.
     */
    private void createStackVBox() {
        stackVBox.setSpacing(1);
        stackVBox.setLayoutX(63.5);
        stackVBox.setLayoutY(60);
        stackVBox.setVisible(false);
        super.getChildren().add(stackVBox);
    }

    /**
     * Deletes a PDATransition in the frontend by invoking the deleteTransition method of the
     * PDATransitionArrow that corresponds to the given PDATransition object.
     *
     * @param transition The transition that has been deleted
     */
    public void deleteTransition(PDATransition transition) {
        PDATransitionArrow transitionArrow = transitionsMap.get(transition);
        transitionArrow.deleteTransition(transition);
        transitionsMap.remove(transition);
    }

    /**
     * Highlights a specific invalid transition by changing the colour of the Label of that
     * transition to red.
     *
     * @param invalidTransition The transition that needs to be highlighted.
     */
    public void highlightTransitionRed(PDATransition invalidTransition) {
        changeTransitionColour(invalidTransition, Color.RED);
    }

    /**
     * Sets the given transition's appearance back to normal.
     *
     * @param validTransition The transition that needs to have its highlight removed.
     */
    public void resetTransitionColour(PDATransition validTransition) {
        changeTransitionColour(validTransition, Color.BLACK);
    }

    /**
     * Highlights a specific transition by changing the colour of the Label of that transition to
     * green. This is used by the step-by-step mode to highlight applicable transitions. As
     * PDAStateNodes are non-editable in the step-by-step mode, the transition arrow also becomes
     * green.
     *
     * @param transition The transition that needs to be highlighted.
     */
    public void highlightTransitionGreen(PDATransition transition) {
        // This is a slightly duller green than the colour specified by GREEN
        changeTransitionColour(transition, Color.web("0x05d010"));
    }

    /**
     * Changes the colour of a transition to the specified colour.
     *
     * @param transition The PDATransition that needs the corresponding transition arrow's colour
     *                   changed
     * @param newColour  The new colour for the transition arrow
     */
    private void changeTransitionColour(PDATransition transition, Color newColour) {
        PDATransitionArrow transitionArrow = transitionsMap.get(transition);
        transitionArrow.changeTransitionColour(transition, newColour);
    }

    /**
     * Checks whether this PDAStateNode has a transition.
     *
     * @param transition The transition being checked.
     * @return True if this PDAStateNode has this transition and false otherwise.
     */
    public boolean hasTransition(PDATransition transition) {
        return transitionsMap.containsKey(transition);
    }

    /**
     * Gets a FillTransition that instantly highlights the outerRectangle with the specified colour.
     *
     * @param colour The colour the outerRectangle should be coloured with.
     * @return The FillTransition that highlights the outerRectangle.
     */
    public FillTransition getStateNodeHighlightTransition(Color colour) {
        FillTransition fillTransition = new FillTransition(Duration.ZERO, outerRectangle);
        fillTransition.setToValue(colour);
        fillTransition.setCycleCount(1);
        return fillTransition;
    }

    /**
     * Gets a FillTransition that instantly resets the colour of the outerRectangle to the normal
     * colour.
     *
     * @return The FillTransition that resets the colour of the outerRectangle.
     */
    public FillTransition getStateNodeUnhighlightTransition() {
        FillTransition fillTransition = new FillTransition(Duration.ZERO, outerRectangle);
        fillTransition.setToValue(LIGHT_BLUE);
        fillTransition.setCycleCount(1);
        return fillTransition;
    }

    /**
     * Gets a list of Transitions that highlights a transition arrow.
     *
     * @param transition The PDATransition which needs to have its corresponding transition
     *                   arrow highlighted.
     * @param newColour  The colour the transition should be highlighted with.
     * @return An ArrayList of Transitions that highlights the transition arrow.
     */
    public ArrayList<Transition> getPDATransitionHighlightTransitions(PDATransition transition
            , String newColour) {
        // Get the corresponding group for the PDATransition
        PDATransitionArrow transitionArrow = transitionsMap.get(transition);
        return transitionArrow.getPDATransitionHighlightTransitions(transition, newColour);
    }

    /**
     * Shows the given stack by this PDAStateNode with the elements of the stack in 5 individual
     * squares. If the given stack has fewer than 5 elements, the remaining stack squares are blank.
     * If the stack has exactly 5 elements, then the entire stack is displayed with 1 element per
     * cell. And if the stack has more than 5 elements, then the top 4 are shown in the top 4
     * cells and the bottom cell contains an ellipsis to denote there are more elements below.
     */
    public void showStack(Stack<String> stack) {
        // Start by clearing the previous stack
        stackVBox.getChildren().clear();

        // The stackVBox does not behave like a stack, so we add the elements of the stack in a
        // top first manner

        if (stack.size() < 5) {
            int requiredEmptyCells = 5 - stack.size();
            // First add the required number of empty cells to the VBox (these appear at the top
            // of the VBox)
            for (int i = 0; i < requiredEmptyCells; i++) {
                stackVBox.getChildren().add(getStackCell(""));
            }
            // Then, add however many stack elements are left. The top element of the stack is
            // stored at the highest index of the stack object, so it is necessary to iterate
            // backwards.
            for (int i = stack.size() - 1; i >= 0; i--) {
                stackVBox.getChildren().add(getStackCell(stack.get(i)));
            }
        } else if (stack.size() == 5) {
            for (int i = stack.size() - 1; i >= 0; i--) {
                stackVBox.getChildren().add(getStackCell(stack.get(i)));
            }
        } else {
            // Create stack cells for only the top 4 elements of the stack
            for (int i = 0; i < 4; i++) {
                stackVBox.getChildren().add(getStackCell(stack.get(stack.size() - 1 - i)));
            }
            // Last cell of the stack contains an ellipsis
            stackVBox.getChildren().add(getStackCell("..."));
        }

        stackVBox.setVisible(true);
    }

    /**
     * Creates and returns a Group that represents a single cell of the stack. The Group consists
     * of a square and a Label within the square containing the stack symbol (or an ellipsis).
     *
     * @param inputSymbol The symbol that belongs to this particular stack cell.
     * @return The stack cell Group.
     */
    private Group getStackCell(String inputSymbol) {
        Group cell = new Group();
        Rectangle square = new Rectangle(25, 25);
        square.setFill(Color.WHITE);
        square.setStroke(Color.BLACK);
        square.setStrokeType(StrokeType.INSIDE);
        cell.getChildren().add(square);

        Label label = new Label(inputSymbol);
        label.setLayoutX(8);
        label.setLayoutY(4);
        cell.getChildren().add(label);

        return cell;
    }

    /**
     * Hides the stack by setting visible to false.
     */
    public void hideStack() {
        stackVBox.setVisible(false);
    }

    /**
     * Selects this node by giving it a visible border (stroke) and sets the boolean variable
     * selected to true.
     */
    public void selectNode() {
        outerRectangle.setStroke(Color.BLUE);
        outerRectangle.setStrokeWidth(2);
        selected = true;
    }

    /**
     * Deselects this node by removing its border and sets the boolean variable selected to false.
     */
    public void deselectNode() {
        outerRectangle.setStroke(null);
        selected = false;
    }

    /**
     * Checks whether this node is currently selected.
     * @return True if this node is currently selected and false otherwise.
     */
    public boolean isSelected() {
        return selected;
    }
}
