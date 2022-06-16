package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDATransition;
import javafx.animation.FillTransition;
import javafx.animation.StrokeTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
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
    // The colour used by transition arrows, thr createTransitionSquare, and the initial state
    // triangle
    private final Color DARK_BLUE = Color.web("0x123edb");
    // The colour used by the outer rectangle of the PDAStateNode and by labels/transitions when
    // hovered over
    private final Color LIGHT_BLUE = Color.web("0x1bb4e4");
    // The colour the outer rectangle of a PDAStateNode becomes when a new transition arrow is
    // dragged over it
    private final Color GREEN = Color.web("0x1be42f");
    // A mapping of PDATransitions to their respective transition arrow
    private final HashMap<PDATransition, Group> transitionsMap = new HashMap<>();
    // A list of all PDAStateNodes that have a transition in each direction with this PDAStateNode
    private final ArrayList<PDAStateNode> doubleTransitionNodes = new ArrayList<>();
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
    private Group newTransitionArrow;
    // A loop transition arrow for this PDAStateNode
    private Group loopTransition;
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
            outerRectangle.setOnMouseEntered(event -> getScene().setCursor(Cursor.HAND));
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

        createTransitionSquare.setOnMouseEntered(event -> getScene().setCursor(Cursor.HAND));

        // When the square is clicked on, set everything up for dragging to occur. This involves
        // creating a new transition arrow which starts from the centre of the square.
        createTransitionSquare.setOnMousePressed((event) -> {
            // Update the x and y coordinates to point to the new mouse coordinates
            x = event.getSceneX();
            y = event.getSceneY();

            // Create a new group for the newTransitionArrow field for this transition being created
            newTransitionArrow = new Group();
            Line line = new Line();
            line.setStroke(DARK_BLUE);
            line.setStrokeWidth(4);

            // Start the line from the center of the square
            line.setStartX(getLayoutX() + 128);
            line.setStartY(getLayoutY() + 24);
            line.setEndX(getLayoutX() + 128);
            line.setEndY(getLayoutY() + 24);

            // Create a property that contains the required rotation for the arrowhead
            DoubleProperty rotation = new SimpleDoubleProperty();
            ChangeListener<Number> changeListener = (observableValue, number, t1) -> {
                // Figure out the required rotation for the arrowhead to point in the correct
                // direction
                double requiredRotation = calculateRotation(line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY());
                rotation.setValue(requiredRotation);
            };

            // Calculate the required rotation of the arrowhead whenever the line's endpoint changes
            line.endXProperty().addListener(changeListener);
            line.endYProperty().addListener(changeListener);

            // This Polygon is the arrowhead of the transition arrow. It initially points upwards.
            Polygon arrowhead = new Polygon();
            arrowhead.getPoints().addAll(-50.0, 40.0, 50.0, 40.0, 0.0, -60.0);
            // Bind the rotateProperty to the rotation DoubleProperty
            arrowhead.rotateProperty().bind(rotation);
            arrowhead.setScaleX(0.15);
            arrowhead.setScaleY(0.15);
            arrowhead.layoutXProperty().bind(line.endXProperty());
            arrowhead.layoutYProperty().bind(line.endYProperty().add(10));
            arrowhead.setStrokeWidth(0);
            arrowhead.setFill(DARK_BLUE);

            newTransitionArrow.getChildren().add(line);
            newTransitionArrow.getChildren().add(arrowhead);

            Pane canvas = (Pane) getParent();
            canvas.getChildren().add(newTransitionArrow);
        });

        createTransitionSquare.setOnMouseDragged((event) -> {
            getScene().setCursor(Cursor.HAND);
            double xOffset = event.getSceneX() - x;
            double yOffset = event.getSceneY() - y;

            Line line = (Line) newTransitionArrow.getChildren().get(0);

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
            Pane canvas = (Pane) getParent();
            ObservableList<Node> children = canvas.getChildren();

            Line line = (Line) newTransitionArrow.getChildren().get(0);

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
                            // Check if there is already a transition arrow. If not, invoke
                            // adjustLine so that the transition is laid out in the same way it
                            // would be if it actually existed.
                            if (transition == null) {
                                needToRemoveNewTransitionArrow = false;
                                adjustLine(node, line);
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
     * Calculates the required rotation for the arrowhead of a transition. Since the arrowhead
     * points up by default, the returned rotation is the amount of clockwise rotation required from
     * the vertical.
     *
     * @param x1 The x coordinate of the start of the line
     * @param y1 The y coordinate of the start of the line
     * @param x2 The x coordinate of the end of the line
     * @param y2 The y coordinate of the end of the line
     * @return The required rotation to make the arrowhead point in the correct direction
     */
    private double calculateRotation(double x1, double y1, double x2, double y2) {
        // Multiply dy by -1 since the y-axis is inverted
        double dy = -(y2 - y1);
        double dx = x2 - x1;

        // Convert the value returned from Math.atan2 from radians to degrees
        return 90 - Math.atan2(dy, dx) * 57.2957795;
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
        VBox vBox = (VBox) loopTransition.getChildren().get(2);
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
        stateNameLabel.setOnMouseEntered(event -> getScene().setCursor(Cursor.HAND));
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
     * Creates a Group consisting of a QuadCurve, a Polygon and a VBox to represent a loop arrow in
     * the PDA.
     */
    private void createLoopTransition() {
        loopTransition = new Group();
        QuadCurve curve = new QuadCurve();
        curve.setControlX(-22);
        curve.setControlY(-97);
        curve.setStartX(-50);
        curve.setEndX((2));
        curve.setStartY(0);
        curve.setEndY(-11);
        curve.setLayoutX(96);
        curve.setLayoutY(0);
        curve.setFill(null);
        curve.setStroke(DARK_BLUE);
        curve.setStrokeWidth(4);
        curve.setStrokeType(StrokeType.INSIDE);

        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(-50.0, 40.0, 50.0, 40.0, 0.0, -60.0);
        arrowhead.setRotate(167);
        arrowhead.setScaleX(0.15);
        arrowhead.setScaleY(0.15);
        arrowhead.setLayoutX(97);
        arrowhead.setLayoutY(3);
        arrowhead.setStrokeWidth(0);
        arrowhead.setFill(DARK_BLUE);

        // VBox to store all the labels
        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setPrefWidth(60);
        vBox.setLayoutX(45);
        vBox.setLayoutY(-70);

        // Automatically hide/display the loop transition whenever a label is added/removed from the
        // VBox of the loop transition group.
        vBox.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    if (vBox.getChildren().isEmpty()) {
                        super.getChildren().remove(loopTransition);
                    }
                } else {
                    if (!super.getChildren().contains(loopTransition)) {
                        super.getChildren().add(loopTransition);
                    }
                }
            }
        });

        loopTransition.getChildren().addAll(curve, arrowhead, vBox);
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
            if (other.transitionBetween(this) != null) {
                createTransition(other, transition, true);
                doubleTransitionNodes.add(other);
                other.doubleTransitionNodes.add(this);
            } else {
                createTransition(other, transition, false);
            }
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
        VBox vBox = (VBox) loopTransition.getChildren().get(2);
        Label newTransition = getTransitionLabel(transition);
        vBox.getChildren().add(newTransition);
        // Shifting the VBox up is only necessary if there is more than one label
        if (vBox.getChildren().size() > 1) {
            vBox.setLayoutY(vBox.getLayoutY() - 17);
        }

        QuadCurve curve = (QuadCurve) loopTransition.getChildren().get(0);
        Polygon arrowhead = (Polygon) loopTransition.getChildren().get(1);
        // Add the EventHandlers to the new Label
        addLabelEventHandlers(newTransition, curve, arrowhead, transition);
        // Add to the map an entry which maps this PDATransition to the loopTransition
        transitionsMap.put(transition, loopTransition);
    }

    /**
     * Returns a Label that describes a transition.
     *
     * @param transition The transition which requires a Label.
     * @return The Label with the correct text to describe the transition.
     */
    private Label getTransitionLabel(PDATransition transition) {
        String inputSymbol = transition.getInputSymbol();
        String popString = transition.getPopString();
        String pushString = transition.getPushString();

        if (inputSymbol.isEmpty()) {
            inputSymbol = "ε";
        }
        if (popString.isEmpty()) {
            popString = "ε";
        }
        if (pushString.isEmpty()) {
            pushString = "ε";
        }

        return new Label(inputSymbol + " / " + popString + " / " + pushString);
    }

    /**
     * Adds three EventHandlers to a Label so that the colour of the Label text and the transition
     * arrow changes when the mouse enters/exits the Label and so that clicking the Label opens the
     * edit transition dialog. See subsection 4.3.5 of the report for further details.
     *
     * @param label       The Label which is having the EventHandlers added to it.
     * @param lineOrCurve The line (or the curve if it is a loopTransition) part of the transition
     *                    arrow.
     * @param arrowhead   The arrowhead part of the transition arrow.
     * @param transition  The PDATransition that the Label is for.
     */
    private void addLabelEventHandlers(Label label, Shape lineOrCurve, Polygon arrowhead,
                                       PDATransition transition) {
        // Do not add the event handlers to the Label if this PDAStateNode is not editable.
        if (!editable) {
            return;
        }

        // Change the colour of the label and the transition arrow on mouse entry
        label.setOnMouseEntered(event -> {
            lineOrCurve.setStroke(LIGHT_BLUE);
            arrowhead.setFill(LIGHT_BLUE);
            label.setTextFill(LIGHT_BLUE);
        });

        // Set the colours back to normal when the mouse exits
        label.setOnMouseExited(event -> {
            lineOrCurve.setStroke(DARK_BLUE);
            arrowhead.setFill(DARK_BLUE);
            label.setTextFill(Color.BLACK);
        });

        // Clicking on the label shows the edit transition dialog for the correct PDATransition
        label.setOnMouseClicked(event -> {
            Dialog<PDATransition> editTransitionDialog =
                    pdaStateNodeController.getEditTransitionDialog(transition);
            editTransitionDialog.show();
        });
    }

    /**
     * Handles the creation of non-loop transition arrows in the PDA diagram. Checks if there is
     * already an existing transition between the two nodes involved in the transition and if there
     * is one, then it just simply adds a Label to that existing transition group. Otherwise, it
     * creates a new group. The isDoubleTransition argument indicates whether there is a transition
     * from one state to another state and vice versa. These require a different layout from
     * ordinary transitions since they would otherwise overlap. See subsection 4.3.3 of the report
     * for further details.
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
            Group existingTransitionGroup = transitionsMap.get(existingTransition);
            Line line = (Line) existingTransitionGroup.getChildren().get(0);
            Polygon arrowhead = (Polygon) existingTransitionGroup.getChildren().get(1);
            VBox vBox = (VBox) existingTransitionGroup.getChildren().get(2);
            Label newTransition = getTransitionLabel(transition);
            vBox.getChildren().add(newTransition);
            // Ensure this label can be used to edit this transition and to change the colour of
            // the arrow on mouse entry and exit of the label.
            addLabelEventHandlers(newTransition, line, arrowhead, transition);
            // Update the map to include a mapping from this new transition to this same transition
            // group
            transitionsMap.put(transition, existingTransitionGroup);
            return;
        }

        // Since there is not already a transition arrow from this node to the other node, create
        // one.
        Group transitionArrow = new Group();
        Line line = new Line();
        line.setStroke(DARK_BLUE);
        line.setStrokeWidth(4);

        // A listener that fires whenever either of the PDAStateNodes involved in the transition
        // move
        ChangeListener<Number> PDAStateNodeMoveListener;

        // If this is a double transition, the listener needs to adjust both transition arrows
        // rather than just this new one
        if (isDoubleTransition) {
            // Find the line that goes from the other PDAStateNode to this PDAStateNode
            Line otherLine = null;
            Set<PDATransition> otherNodeTransitions = other.transitionsMap.keySet();
            for (PDATransition otherNodeTransition : otherNodeTransitions) {
                if (otherNodeTransition.getNewState().equals(stateName)) {
                    Group oppositeDirectionArrow = other.transitionsMap.get(otherNodeTransition);
                    otherLine = (Line) oppositeDirectionArrow.getChildren().get(0);
                }
            }

            // Call adjustLines to lay the two lines out nicely initially
            adjustLines(other, line, otherLine);
            // A final variable for use in the lambda expression
            Line finalOtherLine = otherLine;
            // The change listener needs to adjust both lines in the double transition when
            // either node is moved
            PDAStateNodeMoveListener = (observableValue, oldValue, newVal) -> adjustLines(other,
                    line, finalOtherLine);
        } else {
            // Call adjustLine to lay the new line out nicely initially
            adjustLine(other, line);
            // The change listener only needs to reposition the newly created line when either
            // node is moved
            PDAStateNodeMoveListener = (observableValue, oldValue, newValue) -> adjustLine(other,
                    line);
        }
        // Add the listener to the layoutX and layoutY Properties of both nodes.
        layoutXProperty().addListener(PDAStateNodeMoveListener);
        layoutYProperty().addListener(PDAStateNodeMoveListener);
        other.layoutXProperty().addListener(PDAStateNodeMoveListener);
        other.layoutYProperty().addListener(PDAStateNodeMoveListener);

        // Calculate how much the arrowhead needs to be rotated based on the angle of the new line.
        double initialRequiredRotation =
                calculateRotation(line.getStartX(), line.getStartY(), line.getEndX(),
                        line.getEndY());

        // Property to store the rotation of the arrowhead, initialised to the initial required
        // rotation
        DoubleProperty rotation = new SimpleDoubleProperty(initialRequiredRotation);
        // Properties to store the coordinates of the midpoint of the newly created line
        DoubleProperty xMidpoint =
                new SimpleDoubleProperty((line.getEndX() + line.getStartX()) / 2);
        DoubleProperty yMidpoint =
                new SimpleDoubleProperty((line.getEndY() + line.getStartY()) / 2);

        // A listener that fires whenever the line's start or end points change.
        ChangeListener<Number> changeListener = (observableValue, oldValue, newValue) -> {
            // Figure out the required rotation for the arrowhead of the line to point in the
            // correct direction
            double requiredRotation = calculateRotation(line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY());

            rotation.setValue(requiredRotation);

            // Update the midpoint of the line
            double x = (line.getEndX() + line.getStartX()) / 2;
            double y = (line.getEndY() + line.getStartY()) / 2;

            // Adjust the midpoint Properties so the Label which uses them is positioned correctly
            adjustMidpoints(other, x, y, xMidpoint, yMidpoint);
        };

        // Calculate the required rotation and midpoint whenever the line changes
        line.startXProperty().addListener(changeListener);
        line.startYProperty().addListener(changeListener);
        line.endXProperty().addListener(changeListener);
        line.endYProperty().addListener(changeListener);

        // Do this once manually so that the midpoints are correctly initialised before any
        // movement happens
        double x = (line.getEndX() + line.getStartX()) / 2;
        double y = (line.getEndY() + line.getStartY()) / 2;
        adjustMidpoints(other, x, y, xMidpoint, yMidpoint);


        Polygon arrowhead = new Polygon();
        arrowhead.getPoints().addAll(-50.0, 40.0, 50.0, 40.0, 0.0, -60.0);
        // Bind the rotateProperty of the arrowhead to the required rotation, so it rotates as the
        // line rotates
        arrowhead.rotateProperty().bind(rotation);
        arrowhead.setScaleX(0.15);
        arrowhead.setScaleY(0.15);
        // Bind the arrowhead to the end of the line, so it moves as the line moves
        arrowhead.layoutXProperty().bind(line.endXProperty());
        arrowhead.layoutYProperty().bind(line.endYProperty().add(10));
        arrowhead.setStrokeWidth(0);
        arrowhead.setFill(DARK_BLUE);

        VBox vBox = new VBox();
        vBox.setAlignment(Pos.CENTER);
        vBox.setPrefWidth(60);
        // Bind the x and y layout properties of the VBox to the midpoint of the line
        vBox.layoutXProperty().bind(xMidpoint);
        vBox.layoutYProperty().bind(yMidpoint);
        // Get a label for this new transition and add it to the VBox
        Label newTransitionLabel = getTransitionLabel(transition);
        vBox.getChildren().add(newTransitionLabel);

        // Add the EventHandlers to the Label.
        addLabelEventHandlers(newTransitionLabel, line, arrowhead, transition);
        transitionArrow.getChildren().add(line);
        transitionArrow.getChildren().add(arrowhead);
        transitionArrow.getChildren().add(vBox);

        Pane canvas = (Pane) getParent();
        canvas.getChildren().add(transitionArrow);

        // Map this new PDATransition to this transition arrow
        transitionsMap.put(transition, transitionArrow);
    }

    /**
     * Modifies the end points of both lines based on the angle between the two PDAStateNodes to
     * ensure they are laid out nicely. See subsection 4.3.4 of the report for further details.
     *
     * @param other     The other PDAStateNode that the Line is going to
     * @param line      The Line from this PDAStateNode to the other PDAStateNode
     * @param otherLine The Line from the other PDAStateNode to this PDAStateNode
     */
    private void adjustLines(PDAStateNode other, Line line, Line otherLine) {
        // Start by unbinding the endpoints of both lines
        unbindLineEndPoints(line);
        unbindLineEndPoints(otherLine);

        double angle = angleBetweenPDAStateNodes(other);

        if (angle >= 45 && angle <= 135) {
            line.startXProperty().bind(layoutXProperty().add(150));
            line.startYProperty().bind(layoutYProperty().add(10));
            line.endXProperty().bind(other.layoutXProperty().subtract(5));
            line.endYProperty().bind(other.layoutYProperty().add(10));

            otherLine.startXProperty().bind(other.layoutXProperty());
            otherLine.startYProperty().bind(other.layoutYProperty().add(40));
            otherLine.endXProperty().bind(layoutXProperty().add(155));
            otherLine.endYProperty().bind(layoutYProperty().add(40));
        } else if (angle > 135 && angle <= 225) {
            line.startXProperty().bind(layoutXProperty().add(25));
            line.startYProperty().bind(layoutYProperty().add(50));
            line.endXProperty().bind(other.layoutXProperty().add(25));
            line.endYProperty().bind(other.layoutYProperty().subtract(7));

            otherLine.startXProperty().bind(other.layoutXProperty().add(125));
            otherLine.startYProperty().bind(other.layoutYProperty());
            otherLine.endXProperty().bind(layoutXProperty().add(125));
            otherLine.endYProperty().bind(layoutYProperty().add(57));

        } else if (angle > 225 && angle <= 315) {
            line.startXProperty().bind(layoutXProperty());
            line.startYProperty().bind(layoutYProperty().add(10));
            line.endXProperty().bind(other.layoutXProperty().add(150).add(5));
            line.endYProperty().bind(other.layoutYProperty().add(10));

            otherLine.startXProperty().bind(other.layoutXProperty().add(150));
            otherLine.startYProperty().bind(other.layoutYProperty().add(40));
            otherLine.endXProperty().bind(layoutXProperty().subtract(5));
            otherLine.endYProperty().bind(layoutYProperty().add(40));
        } else {
            line.startXProperty().bind(layoutXProperty().add(25));
            line.startYProperty().bind(layoutYProperty());
            line.endXProperty().bind(other.layoutXProperty().add(25));
            line.endYProperty().bind(other.layoutYProperty().add(57));

            otherLine.startXProperty().bind(other.layoutXProperty().add(125));
            otherLine.startYProperty().bind(other.layoutYProperty().add(50));
            otherLine.endXProperty().bind(layoutXProperty().add(125));
            otherLine.endYProperty().bind(layoutYProperty().subtract(7));
        }
    }

    /**
     * Modifies the end points of the lines based on the angle between the two PDAStateNodes to
     * ensure it is laid out nicely. See subsection 4.3.4 of the report for further details.
     *
     * @param other The other PDAStateNode that the Line is going to
     * @param line  The Line between the two nodes.
     */
    private void adjustLine(PDAStateNode other, Line line) {
        unbindLineEndPoints(line);

        double angle = angleBetweenPDAStateNodes(other);

        if (angle >= 45 && angle <= 135) {
            line.startXProperty().bind(layoutXProperty().add(150));
            line.startYProperty().bind(layoutYProperty().add(25));
            line.endXProperty().bind(other.layoutXProperty().subtract(5));
            line.endYProperty().bind(other.layoutYProperty().add(25));
        } else if (angle > 135 && angle <= 225) {
            line.startXProperty().bind(layoutXProperty().add(75));
            line.startYProperty().bind(layoutYProperty().add(50));
            line.endXProperty().bind(other.layoutXProperty().add(75));
            line.endYProperty().bind(other.layoutYProperty().subtract(7));
        } else if (angle > 225 && angle <= 315) {
            line.startXProperty().bind(layoutXProperty().subtract(2));
            line.startYProperty().bind(layoutYProperty().add(25));
            line.endXProperty().bind(other.layoutXProperty().add(155));
            line.endYProperty().bind(other.layoutYProperty().add(25));
        } else {
            line.startXProperty().bind(layoutXProperty().add(75));
            line.startYProperty().bind(layoutYProperty());
            line.endXProperty().bind(other.layoutXProperty().add(75));
            line.endYProperty().bind(other.layoutYProperty().add(57));
        }
    }

    /**
     * Remove the startX, startY, endX and endY Property bindings of the given Line.
     *
     * @param line The Line which needs the bindings removed
     */
    private void unbindLineEndPoints(Line line) {
        line.startXProperty().unbind();
        line.startYProperty().unbind();
        line.endXProperty().unbind();
        line.endYProperty().unbind();
    }

    /**
     * Returns the angle between this PDAStateNode and another PDAStateNode using the centres of
     * each node.
     *
     * @param other The other PDAStateNode
     * @return The angle between this node and the other node
     */
    private double angleBetweenPDAStateNodes(PDAStateNode other) {
        double centerX = this.getLayoutX() + 75;
        double centerY = this.getLayoutY() + 25;
        double otherCenterX = other.getLayoutX() + 75;
        double otherCenterY = other.getLayoutY() + 25;

        double angle = calculateRotation(centerX, centerY, otherCenterX, otherCenterY);
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Modifies the two DoubleProperty objects that correspond to the midpoint of a Line. This is
     * to ensure that the VBox containing all the transition labels is positioned in a way that
     * prevents the Labels overlapping the transition arrow line. See subsection 4.3.4 of the report
     * for further details.
     *
     * @param other     The other PDAStateNode the Line connects to
     * @param x         The x midpoint of the Line
     * @param y         The y midpoint of the Line
     * @param xMidpoint The xMidpoint property
     * @param yMidpoint The yMidpoint property
     */
    private void adjustMidpoints(PDAStateNode other, Double x, Double y, DoubleProperty xMidpoint,
                                 DoubleProperty yMidpoint) {
        double angle = angleBetweenPDAStateNodes(other);

        xMidpoint.setValue(x);
        yMidpoint.setValue(y);

        if (angle > 80 && angle < 120) {
            xMidpoint.setValue(x - 30);
        }
        if (angle > 100 && angle < 120) {
            yMidpoint.setValue(y + 10);
        }
        if (angle > 110 && angle < 120) {
            xMidpoint.setValue(x - 35);
        }
        if (angle > 120 && angle < 180) {
            xMidpoint.setValue(x - 60);
        }
        if (angle > 170 && angle < 190) {
            yMidpoint.setValue(y - 10);
        }
        if (angle > 265 && angle <= 280) {
            xMidpoint.setValue(x - 30);
        }
        if (angle > 280 && angle < 290) {
            xMidpoint.setValue(x - 45);
        }
        if (angle > 290 && angle < 360) {
            xMidpoint.setValue(x - 60);
        }
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
     * Deletes a PDATransition in the frontend by deleting the Label and the transition arrow, if
     * necessary. If the PDATransition being deleted is within the map of transitions of this
     * PDAStateNode, it invokes the appropriate method to handle the removal of the transition in
     * the frontend. This depends on whether the transition being deleted is a loop transition.
     * See subsection 4.3.5 of the report for further details.
     *
     * @param transition The transition that has been deleted
     */
    public void deleteTransition(PDATransition transition) {
        Group transitionGroup = transitionsMap.get(transition);
        // If the transition that was deleted is a loopTransition, invoke the
        // deleteLoopTransition method
        if (transitionGroup == loopTransition) {
            deleteLoopTransition(transition);
        } else {
            // If the transition that was deleted is a non-loop transition, invoke the
            // deleteNonLoopTransition method
            deleteNonLoopTransition(transition);
        }
        transitionsMap.remove(transition);
    }

    /**
     * Handles the deletion of loop transitions (transitions that have the same current state and
     * next state). It removes the label of the specified transition and adjusts the positioning of
     * the VBox after the label has been removed, if necessary.
     *
     * @param transition The loop transition that has been deleted
     */
    private void deleteLoopTransition(PDATransition transition) {
        VBox vBox = (VBox) loopTransition.getChildren().get(2);
        int index = 0;
        for (Node child : vBox.getChildren()) {
            Label label = (Label) child;
            String labelText = label.getText();
            // Find the label for this transition by generating the Label with the correct text
            // for this transition and comparing the text to the text of each existing Label
            if (labelText.equals(getTransitionLabel(transition).getText())) {
                index = vBox.getChildren().indexOf(child);
            }
        }
        // If the VBox has more than 1 Label in it currently, then it has already been translated
        // vertically. Therefore, translate it by the same amount in the opposite direction since a
        // Label is being removed.
        if (vBox.getChildren().size() > 1) {
            vBox.setLayoutY(vBox.getLayoutY() + 17);
        }
        vBox.getChildren().remove(index);
    }

    /**
     * Handles the deletion of a non-loop transition (transitions that have different current and
     * next states). If after deleting this transition there are no more transitions from this
     * PDAStateNode to the other PDAStateNode in the transition, then the transition arrow is also
     * removed.
     *
     * @param transition The transition that has been deleted
     */
    private void deleteNonLoopTransition(PDATransition transition) {
        Group transitionGroup = transitionsMap.get(transition);
        // Remove the Label from the VBox
        VBox vBox = (VBox) transitionGroup.getChildren().get(2);
        int index = 0;
        for (Node child : vBox.getChildren()) {
            Label label = (Label) child;
            String labelText = label.getText();
            if (labelText.equals(getTransitionLabel(transition).getText())) {
                index = vBox.getChildren().indexOf(child);
            }
        }
        vBox.getChildren().remove(index);

        // If after removing the label the size of the VBox's children is 0, the transition arrow
        // needs to be removed
        if (vBox.getChildren().size() == 0) {
            Pane canvas = (Pane) getParent();
            canvas.getChildren().remove(transitionGroup);

            // Since we have just removed a transition, see if we need to convert a double
            // transition back to a normal transition.

            String newState = transition.getNewState();
            // The other PDAStateNode involved in the double transition
            PDAStateNode otherNode = null;

            // Iterate through the list of all PDAStateNodes that have a double transition with
            // this PDAStateNode
            for (PDAStateNode node : doubleTransitionNodes) {
                // If the name of a node matches the name of the newState of this transition,
                // this node needs to have the double transition converted into a normal transition
                if (node.stateName.equals(newState)) {
                    otherNode = node;
                    for (PDATransition otherNodeTransition : node.transitionsMap.keySet()) {
                        if (otherNodeTransition.getNewState().equals(stateName)) {
                            Line line =
                                    (Line) node.transitionsMap.get(otherNodeTransition).getChildren().get(0);
                            // Manually invoke the adjustLine method once so that the line is
                            // laid out correctly before it is moved
                            node.adjustLine(this, line);

                            // Change the way this line moves back to the normal way by giving it
                            // the ChangeListener that invokes the adjustLine method rather than the
                            // adjustLines method.
                            ChangeListener<Number> PDAStateNodeMoveListener =
                                    (observableValue, oldValue, newValue) ->
                                            node.adjustLine(this, line);

                            layoutXProperty().addListener(PDAStateNodeMoveListener);
                            layoutYProperty().addListener(PDAStateNodeMoveListener);
                            node.layoutXProperty().addListener(PDAStateNodeMoveListener);
                            node.layoutYProperty().addListener(PDAStateNodeMoveListener);
                        }
                    }
                }
            }

            // Remove the map entries in each object that indicate there exists a double
            // transition between the two nodes. Must be done outside the loop to prevent a
            // ConcurrentModificationException being thrown
            if (otherNode != null) {
                otherNode.doubleTransitionNodes.remove(this);
                doubleTransitionNodes.remove(otherNode);
            }
        }
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
     * Changes the colour of a transition to the specified colour. If this PDAStateNode is not
     * editable, then the EventHandler for mouse exit is not added. If the newColour is Color.BLACK,
     * then only the Label is set to the newColour and the transition arrow is set to its original
     * colour of DARK_BLUE.
     *
     * @param pdaTransition The PDATransition that needs the corresponding transition arrow's
     *                      colour changed
     * @param newColour     The new colour for the transition arrow
     */
    private void changeTransitionColour(PDATransition pdaTransition, Color newColour) {
        for (PDATransition transition : transitionsMap.keySet()) {
            if (transition.equals(pdaTransition)) {
                Group transitionGroup = transitionsMap.get(transition);
                VBox vBox = (VBox) transitionGroup.getChildren().get(2);
                for (Node node : vBox.getChildren()) {
                    Label label = (Label) node;
                    if (label.getText().equals(getTransitionLabel(transition).getText())) {
                        Shape lineOrCurve = (Shape) transitionGroup.getChildren().get(0);
                        Polygon arrowhead = (Polygon) transitionGroup.getChildren().get(1);
                        // The label needs the colour changed regardless of whether this
                        // PDAStateNode is editable or not
                        label.setTextFill(newColour);

                        // If this PDAStateNode is not editable, do not add the EventHandler -
                        // just change the colour
                        if (!editable) {
                            // If the colour we are changing to is black, then we are setting the
                            // transition back to normal
                            if (newColour.equals(Color.BLACK)) {
                                lineOrCurve.setStroke(DARK_BLUE);
                                arrowhead.setFill(DARK_BLUE);
                            } else {
                                lineOrCurve.setStroke(newColour);
                                arrowhead.setFill(newColour);
                            }
                            return;
                        }

                        label.setOnMouseExited(event -> {
                            lineOrCurve.setStroke(DARK_BLUE);
                            arrowhead.setFill(DARK_BLUE);
                            label.setTextFill(newColour);
                        });

                    }
                }
            }
        }
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
     * Gets a list of Transitions that highlights a transition arrow. As a transition arrow
     * consists of multiple parts, a single transition cannot highlight all the parts in one go.
     *
     * @param pdaTransition The PDATransition which needs to have its corresponding transition
     *                      arrow highlighted.
     * @param newColour     The colour the transition should be highlighted with.
     * @return An ArrayList of Transitions that highlights the transition arrow instantly
     * (duration of 0).
     */
    public ArrayList<Transition> getPDATransitionHighlightTransitions(PDATransition pdaTransition
            , String newColour) {
        // The list of transitions to be returned
        ArrayList<Transition> transitions = new ArrayList<>();
        for (PDATransition transition : transitionsMap.keySet()) {
            if (transition.equals(pdaTransition)) {
                // Get the corresponding group for the PDATransition
                Group transitionGroup = transitionsMap.get(transition);
                VBox vBox = (VBox) transitionGroup.getChildren().get(2);
                for (Node node : vBox.getChildren()) {
                    Label label = (Label) node;
                    // Find the Label that specifies this pdaTransition
                    if (label.getText().equals(getTransitionLabel(transition).getText())) {
                        Shape lineOrCurve = (Shape) transitionGroup.getChildren().get(0);
                        Polygon arrowhead = (Polygon) transitionGroup.getChildren().get(1);

                        // Custom transition to change the text fill of the Label
                        Transition labelTransition = new Transition() {
                            @Override
                            protected void interpolate(double v) {
                                label.setTextFill(Color.web(newColour));
                            }
                        };
                        transitions.add(labelTransition);

                        // Transition to change the colour of the Line or QuadCurve of the
                        // transition arrow
                        StrokeTransition strokeTransition = new StrokeTransition(Duration.ZERO,
                                lineOrCurve);
                        strokeTransition.setCycleCount(1);
                        transitions.add(strokeTransition);

                        // Transition to change the colour of the arrowhead
                        FillTransition fillTransition = new FillTransition(Duration.ZERO,
                                arrowhead);
                        fillTransition.setCycleCount(1);
                        transitions.add(fillTransition);

                        // If the colour we are changing to is black, then we are setting the
                        // transition back to normal
                        if (newColour.equals("0x000000")) {
                            strokeTransition.setToValue(DARK_BLUE);
                            fillTransition.setToValue(DARK_BLUE);
                        } else {
                            strokeTransition.setToValue(Color.web(newColour));
                            fillTransition.setToValue(Color.web(newColour));
                        }
                    }
                }
            }
        }
        return transitions;
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
