package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDATransition;
import javafx.animation.FillTransition;
import javafx.animation.StrokeTransition;
import javafx.animation.Transition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Set;

/**
 * The PDATransitionArrow class represents the transition arrows in the PDA diagram. The class not
 * only provides the arrow shape along with the labels but also code to change the colour of the
 * transition arrow and/or label corresponding to a single PDATransition as well as code for
 * handling the positioning and orientation of the arrow components.
 * <p>
 * There are three kinds of transition arrow. Firstly, there are normal ones which are defined as
 * being the only transition arrow between two different states. Multiple PDATransitions that share
 * the same current and new state all share the same arrow. This is achieved by using a VBox that
 * stores all the Labels for the arrow beneath the line when it is oriented horizontally. The Labels
 * contain the input symbol, pop symbol/string and push symbol/string of the corresponding
 * PDATransition, separated by forward slashes.
 * <p>
 * There are also loop transitions where the start and end states of the arrow are the same. These
 * use a QuadCurve rather than a Line and have the VBox positioned above the arrow. Consequently,
 * it is necessary to shift the VBox up each time a new Label is added and to bring it back down
 * when a Label is removed.
 * <p>
 * Finally, there are double transitions. These have different start and end states like normal
 * transitions (and hence use a Line rather than a QuadCurve) but only exist if there is already
 * a transition arrow in the opposite direction. To prevent the two arrows from overlapping,
 * double transitions are positioned differently (and so too is the other arrow involved in the
 * double transition). Only the arrow involved in the double transition that appears at the top
 * when the arrows are roughly horizontal is considered a double transition though. This is because
 * it requires its VBox to be positioned above the arrow (unlike the arrow at the bottom of the
 * double transition) to prevent the Labels from overlapping with the arrow.
 *
 * @author Savraj Bassi
 */


public class PDATransitionArrow extends Group {
    // The default colour of transition arrows
    private final Color DARK_BLUE = Color.web("0x123edb");
    // The curve of the arrow (only used for loop transitions)
    private final QuadCurve curve = new QuadCurve();
    // The lin of the arrow (used by both normal and double transitions)
    private final Line line = new Line();
    // The arrowhead of the arrow (used by all kinds of transition)
    private final Polygon arrowhead = new Polygon();
    // VBox to store all the Labels for this arrow
    private final VBox vBox = new VBox();
    // The node this transition arrow starts from
    private final PDAStateNode startNode;
    // The node this transition arrow goes to
    private PDAStateNode endNode;
    // Whether this transition is a double transition or not
    private boolean isDoubleTransition;
    // Whether this transition is a loop transition or not
    private final boolean isLoopTransition;
    // Properties to store the coordinates of the midpoint of the newly created line
    private final DoubleProperty xMidpoint = new SimpleDoubleProperty();
    private final DoubleProperty yMidpoint = new SimpleDoubleProperty();
    // Property to store the required rotation of the arrowhead to point in the direction of the
    // Line. Not needed for loop transitions.
    private final DoubleProperty rotation = new SimpleDoubleProperty();
    // A listener that fires whenever the line's start or end points change.
    private ChangeListener<Number> changeListener;

    /**
     * Private constructor that contains the code common to both constructors.
     *
     * @param startNode        The node this transition arrow starts from.
     * @param isLoopTransition Whether this arrow is a loop transition or not.
     */
    private PDATransitionArrow(PDAStateNode startNode, boolean isLoopTransition) {
        super();
        this.startNode = startNode;
        this.isLoopTransition = isLoopTransition;

        line.setStroke(DARK_BLUE);
        line.setStrokeWidth(4);

        arrowhead.getPoints().addAll(-50.0, 40.0, 50.0, 40.0, 0.0, -60.0);
        arrowhead.setScaleX(0.15);
        arrowhead.setScaleY(0.15);
        arrowhead.setStrokeWidth(0);
        arrowhead.setFill(DARK_BLUE);

        vBox.setAlignment(Pos.CENTER);
        vBox.setPrefWidth(60);

        // If the transition is a loop transition, then call the createLoopTransition method before
        // adding the curve, arrowhead and VBox to the list of children of the Group. There is no
        // need to do anything else.
        if (isLoopTransition) {
            createLoopTransition();
            super.getChildren().addAll(curve, arrowhead, vBox);
            return;
        }

        // Calculate how much the arrowhead needs to be rotated based on the angle of the new line.
        double initialRequiredRotation = calculateRotation(line.getStartX(), line.getStartY(),
                line.getEndX(), line.getEndY());

        // Property to store the rotation of the arrowhead, initialised to the initial required
        // rotation
        rotation.set(initialRequiredRotation);

        // Bind the rotateProperty to the rotation DoubleProperty
        arrowhead.rotateProperty().bind(rotation);
        arrowhead.layoutXProperty().bind(line.endXProperty());
        arrowhead.layoutYProperty().bind(line.endYProperty().add(10));

        vBox.layoutXProperty().bind(xMidpoint);
        vBox.layoutYProperty().bind(yMidpoint);

        super.getChildren().addAll(line, arrowhead, vBox);
    }

    /**
     * Creates a PDATransitionArrow given the start and end nodes as well as whether this arrow is
     * a double transition or a loop transition.
     *
     * @param startNode          The node the transition arrow starts from.
     * @param endNode            The node the transition arrow goes to.
     * @param isDoubleTransition Whether this node is a double transition or not.
     * @param isLoopTransition   Whether this node is a loop transition or not.
     */
    public PDATransitionArrow(PDAStateNode startNode, PDAStateNode endNode,
                              boolean isDoubleTransition, boolean isLoopTransition) {
        this(startNode, isLoopTransition);

        this.endNode = endNode;
        this.isDoubleTransition = isDoubleTransition;

        // Whenever the line's end points change, this listener executes and updates both the
        // rotation and the two midpoints
        changeListener = (observableValue, oldValue, newValue) -> {
            // Figure out the required rotation for the arrowhead of the line to point in the
            // correct direction
            double requiredRotation = calculateRotation(line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY());

            rotation.setValue(requiredRotation);

            // Adjust the midpoint Properties so the Label which uses them is positioned
            // correctly
            adjustMidpoints();
        };

        // Calculate the required rotation and midpoint whenever the line changes
        line.startXProperty().addListener(changeListener);
        line.startYProperty().addListener(changeListener);
        line.endXProperty().addListener(changeListener);
        line.endYProperty().addListener(changeListener);

        // Call adjustMidpoints once to ensure the VBox is positioned correctly initially.
        adjustMidpoints();

        // Set the appropriate listener that fires when either of the PDAStateNodes this transition
        // arrow connects are moved. Different listeners are needed depending on whether this is a
        // double transition since a double transition requires two transition arrows to be moved.
        if (isDoubleTransition) {
            setDoubleTransitionPDAStateNodeMoveListener();
        } else {
            setSingleTransitionPDAStateNodeMoveListener();
        }
    }

    /**
     * Creates a PDATransitionArrow with just a start node but no end node. This is for use when
     * the user drags from the createTransitionSquare within a PDAStateNode.
     *
     * @param startNode The node this transition arrow starts from.
     */
    public PDATransitionArrow(PDAStateNode startNode) {
        this(startNode, false);

        // There is no need for the listener to update the midpoint since there is no VBox at this
        // point.
        changeListener = (observableValue, oldValue, newValue) -> {
            // Figure out the required rotation for the arrowhead of the line to point in the
            // correct direction
            double requiredRotation = calculateRotation(line.getStartX(), line.getStartY(),
                    line.getEndX(), line.getEndY());

            rotation.setValue(requiredRotation);
        };

        line.endXProperty().addListener(changeListener);
        line.endYProperty().addListener(changeListener);
    }

    /**
     * Creates the loop transition by creating the curve and positioning it along with the arrowhead
     * and the VBox. Also adds a listener to the VBox that makes the loop transition automatically
     * appear/disappear as needed.
     */
    private void createLoopTransition() {
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

        arrowhead.setRotate(167);
        arrowhead.setLayoutX(97);
        arrowhead.setLayoutY(3);

        vBox.setLayoutX(45);
        vBox.setLayoutY(-70);

        // Automatically hide/display the loop transition whenever a label is added/removed from the
        // VBox of the loop transition group.
        vBox.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    if (vBox.getChildren().isEmpty()) {
                        startNode.getChildren().remove(this);
                    }
                } else {
                    if (!startNode.getChildren().contains(this)) {
                        startNode.getChildren().add(this);
                    }
                }
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
     * Returns the Line of this transition arrow.
     *
     * @return The Line belonging to this transition arrow.
     */
    public Line getLine() {
        return line;
    }

    /**
     * Returns the VBox of this transition arrow.
     *
     * @return The VBox belonging to this transition arrow.
     */
    public VBox getVBox() {
        return vBox;
    }

    /**
     * Converts this transition from a double transition back to a regular transition. This involves
     * changing the isDoubleTransition boolean value to false and changing the listener that fires
     * when either of the nodes that this arrow is connected to are moved.
     */
    private void convertToNormalTransition() {
        isDoubleTransition = false;
        setSingleTransitionPDAStateNodeMoveListener();
    }

    /**
     * Sets the end node of this transition arrow to the given PDAStateNode.
     *
     * @param endNode The end node of this transition arrow.
     */
    public void setEndNode(PDAStateNode endNode) {
        this.endNode = endNode;
    }

    /**
     * Modifies the two DoubleProperty objects that correspond to the midpoint of a Line. This is
     * to ensure that the VBox containing all the transition labels is positioned in a way that
     * prevents the Labels overlapping the transition arrow line. See subsection 4.3.4 of the report
     * for further details. If the Line is a double transition, then the midpoints are positioned in
     * a different way to ensure the VBox is positioned well for the double transition. This is to
     * prevent the Labels overlapping with the other transition arrow. The positioning of the y
     * midpoint in particular for double transitions depends on the number of Labels already in the
     * VBox.
     */
    private void adjustMidpoints() {
        double angle = angleBetweenPDAStateNodes();
        double x = (line.getEndX() + line.getStartX()) / 2;
        double y = (line.getEndY() + line.getStartY()) / 2;

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

        // The midpoints require different adjustments at certain angles if this line is a double
        // transition.
        if (isDoubleTransition) {
            // The amount the yMidpoint needs to be shifted vertically
            int yDisplacement;

            // The yMidpoint needs to be raised (lower y value) if the line is a double transition
            // so that the Labels do not overlap with the line. This is only necessary though if
            // there is more than 1 Label in the VBox and only at specific angles (where the line
            // is close to horizontal).
            if (angle > 30 && angle < 135 || angle > 225 && angle < 315) {
                int numberOfLabels = vBox.getChildren().size();
                yDisplacement = 17 * numberOfLabels - 17;
                // Decrease the variable y by the necessary amount before it is used to adjust the
                // y midpoints based on the angle of the line. If the VBox has no children (as it
                // has just been created), then yDisplacement would be -17. This would shift the
                // VBox down which would position the single Label incorrectly. Therefore, y is only
                // decremented by 0 if yDisplacement is negative.
                y -= Math.max(yDisplacement, 0);
            }

            if (angle > 30 && angle < 70) {
                xMidpoint.setValue(x - 60);
                yMidpoint.setValue(y - 10);
            }
            if (angle >= 70 && angle < 75) {
                xMidpoint.setValue(x - 60);
                yMidpoint.setValue(y - 10);
            }
            if (angle >= 75 && angle <= 85) {
                xMidpoint.setValue(x - 40);
                yMidpoint.setValue(y - 20);
            }
            if (angle > 85 && angle < 100) {
                yMidpoint.setValue(y - 20);
            }
            if (angle >= 100 && angle < 135) {
                yMidpoint.setValue(y - 15);
                xMidpoint.setValue(x + 2);
            }

            if (angle > 225 && angle < 240) {
                xMidpoint.setValue(x - 70);
            }
            if (angle >= 240 && angle < 250) {
                xMidpoint.setValue(x - 60);
                yMidpoint.setValue(y - 10);
            }
            if (angle >= 250 && angle < 266) {
                xMidpoint.setValue(x - 50);
                yMidpoint.setValue(y - 20);
            }
            if (angle >= 266 && angle < 270) {
                yMidpoint.setValue(y - 20);
            }
            if (angle >= 270 && angle < 280) {
                yMidpoint.setValue(y - 20);
            }
            if (angle >= 280 && angle < 315) {
                xMidpoint.setValue(x);
                yMidpoint.setValue(y - 20);
            }
        }
    }

    /**
     * Sets the listener that fires when the start/end nodes are moved to a listener that calls the
     * adjustLine method.
     */
    public void setSingleTransitionPDAStateNodeMoveListener() {
        // Call adjustLine to lay the new line out nicely initially
        adjustLine();

        // A listener that fires whenever either of the PDAStateNodes involved in the transition
        // move. It adjusts the single line.
        ChangeListener<Number> PDAStateNodeMoveListener;
        PDAStateNodeMoveListener = (observableValue, oldValue, newValue) -> adjustLine();

        // Add the listener to the layoutX and layoutY Properties of both nodes.
        startNode.layoutXProperty().addListener(PDAStateNodeMoveListener);
        startNode.layoutYProperty().addListener(PDAStateNodeMoveListener);
        endNode.layoutXProperty().addListener(PDAStateNodeMoveListener);
        endNode.layoutYProperty().addListener(PDAStateNodeMoveListener);
    }

    /**
     * Modifies the end points of the line based on the angle between the two PDAStateNodes to
     * ensure it is laid out nicely.
     */
    public void adjustLine() {
        unbindLineEndPoints(line);

        double angle = angleBetweenPDAStateNodes();

        if (angle >= 45 && angle <= 135) {
            line.startXProperty().bind(startNode.layoutXProperty().add(150));
            line.startYProperty().bind(startNode.layoutYProperty().add(25));
            line.endXProperty().bind(endNode.layoutXProperty().subtract(5));
            line.endYProperty().bind(endNode.layoutYProperty().add(25));
        } else if (angle > 135 && angle <= 225) {
            line.startXProperty().bind(startNode.layoutXProperty().add(75));
            line.startYProperty().bind(startNode.layoutYProperty().add(50));
            line.endXProperty().bind(endNode.layoutXProperty().add(75));
            line.endYProperty().bind(endNode.layoutYProperty().subtract(7));
        } else if (angle > 225 && angle <= 315) {
            line.startXProperty().bind(startNode.layoutXProperty().subtract(2));
            line.startYProperty().bind(startNode.layoutYProperty().add(25));
            line.endXProperty().bind(endNode.layoutXProperty().add(155));
            line.endYProperty().bind(endNode.layoutYProperty().add(25));
        } else {
            line.startXProperty().bind(startNode.layoutXProperty().add(75));
            line.startYProperty().bind(startNode.layoutYProperty());
            line.endXProperty().bind(endNode.layoutXProperty().add(75));
            line.endYProperty().bind(endNode.layoutYProperty().add(57));
        }
    }

    /**
     * Sets the listener that fires when the start/end nodes are moved to a listener that calls the
     * adjustLines method.
     */
    public void setDoubleTransitionPDAStateNodeMoveListener() {
        Line otherLine = getOtherPDATransitionArrow().getLine();
        // Call adjustLine to lay the new line out nicely initially
        adjustLines(otherLine);

        // A listener that fires whenever either of the PDAStateNodes involved in the transition
        // move. It adjusts both the lines in the double transition.
        ChangeListener<Number> PDAStateNodeMoveListener;
        PDAStateNodeMoveListener = (observableValue, oldValue, newValue) -> adjustLines(otherLine);

        // Add the listener to the layoutX and layoutY Properties of both nodes.
        startNode.layoutXProperty().addListener(PDAStateNodeMoveListener);
        startNode.layoutYProperty().addListener(PDAStateNodeMoveListener);
        endNode.layoutXProperty().addListener(PDAStateNodeMoveListener);
        endNode.layoutYProperty().addListener(PDAStateNodeMoveListener);
    }

    /**
     * Gets the other PDATransitionArrow involved in the double transition with this arrow.
     *
     * @return The other PDATransitionArrow in the double transition.
     */
    private PDATransitionArrow getOtherPDATransitionArrow() {
        PDATransitionArrow oppositeDirectionArrow = null;
        Set<PDATransition> endNodeTransitions = endNode.getTransitionsMap().keySet();
        for (PDATransition endNodeTransition : endNodeTransitions) {
            if (endNodeTransition.getNewState().equals(startNode.getStateName())) {
                oppositeDirectionArrow = endNode.getTransitionsMap().get(endNodeTransition);
            }
        }
        return oppositeDirectionArrow;
    }

    /**
     * Modifies the end points of both lines based on the angle between the two PDAStateNodes to
     * ensure they are laid out nicely.
     *
     * @param otherLine The Line from the endNode PDAStateNode
     */
    private void adjustLines(Line otherLine) {
        // Start by unbinding the endpoints of both lines
        unbindLineEndPoints(line);
        unbindLineEndPoints(otherLine);

        double angle = angleBetweenPDAStateNodes();

        if (angle >= 45 && angle <= 135) {
            line.startXProperty().bind(startNode.layoutXProperty().add(150));
            line.startYProperty().bind(startNode.layoutYProperty().add(10));
            line.endXProperty().bind(endNode.layoutXProperty().subtract(5));
            line.endYProperty().bind(endNode.layoutYProperty().add(10));

            otherLine.startXProperty().bind(endNode.layoutXProperty());
            otherLine.startYProperty().bind(endNode.layoutYProperty().add(40));
            otherLine.endXProperty().bind(startNode.layoutXProperty().add(155));
            otherLine.endYProperty().bind(startNode.layoutYProperty().add(40));
        } else if (angle > 135 && angle <= 225) {
            line.startXProperty().bind(startNode.layoutXProperty().add(25));
            line.startYProperty().bind(startNode.layoutYProperty().add(50));
            line.endXProperty().bind(endNode.layoutXProperty().add(25));
            line.endYProperty().bind(endNode.layoutYProperty().subtract(7));

            otherLine.startXProperty().bind(endNode.layoutXProperty().add(125));
            otherLine.startYProperty().bind(endNode.layoutYProperty());
            otherLine.endXProperty().bind(startNode.layoutXProperty().add(125));
            otherLine.endYProperty().bind(startNode.layoutYProperty().add(57));

        } else if (angle > 225 && angle <= 315) {
            line.startXProperty().bind(startNode.layoutXProperty());
            line.startYProperty().bind(startNode.layoutYProperty().add(10));
            line.endXProperty().bind(endNode.layoutXProperty().add(150).add(5));
            line.endYProperty().bind(endNode.layoutYProperty().add(10));

            otherLine.startXProperty().bind(endNode.layoutXProperty().add(150));
            otherLine.startYProperty().bind(endNode.layoutYProperty().add(40));
            otherLine.endXProperty().bind(startNode.layoutXProperty().subtract(5));
            otherLine.endYProperty().bind(startNode.layoutYProperty().add(40));
        } else {
            line.startXProperty().bind(startNode.layoutXProperty().add(25));
            line.startYProperty().bind(startNode.layoutYProperty());
            line.endXProperty().bind(endNode.layoutXProperty().add(25));
            line.endYProperty().bind(endNode.layoutYProperty().add(57));

            otherLine.startXProperty().bind(endNode.layoutXProperty().add(125));
            otherLine.startYProperty().bind(endNode.layoutYProperty().add(50));
            otherLine.endXProperty().bind(startNode.layoutXProperty().add(125));
            otherLine.endYProperty().bind(startNode.layoutYProperty().subtract(7));
        }
    }

    /**
     * Remove the startX, startY, endX and endY Property bindings of the given Line.
     */
    private void unbindLineEndPoints(Line line) {
        line.startXProperty().unbind();
        line.startYProperty().unbind();
        line.endXProperty().unbind();
        line.endYProperty().unbind();
    }

    /**
     * Returns the angle between the start and end PDAStateNodes of this transition arrow using the
     * centres of each node.
     *
     * @return The angle between this node and the other node
     */
    private double angleBetweenPDAStateNodes() {
        double centerX = startNode.getLayoutX() + 75;
        double centerY = startNode.getLayoutY() + 25;
        double otherCenterX = endNode.getLayoutX() + 75;
        double otherCenterY = endNode.getLayoutY() + 25;

        double angle = calculateRotation(centerX, centerY, otherCenterX, otherCenterY);
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Adds a new Label to the VBox of this transition arrow containing the relevant information.
     * Also adds EventHandlers to the Label so that it changes the colour of the text and of the
     * arrow on mouse entry and exit.
     *
     * @param newTransition The new PDATransition that requires a Label to be added to this arrow.
     */
    public void addTransition(PDATransition newTransition) {
        // Get a label for this new transition and add it to the VBox
        Label newTransitionLabel = PDATransitionArrow.getTransitionLabel(newTransition);
        vBox.getChildren().add(newTransitionLabel);
        // Ensure this label can be used to edit this transition and to change the colour of
        // the arrow on mouse entry and exit of the label.
        addLabelEventHandlers(newTransitionLabel, newTransition);
        adjustMidpoints();
    }

    /**
     * Returns a Label that describes a transition.
     *
     * @param transition The transition which requires a Label.
     * @return The Label with the correct text to describe the transition.
     */
    public static Label getTransitionLabel(PDATransition transition) {
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
     * @param label      The Label which is having the EventHandlers added to it.
     * @param transition The PDATransition that the Label is for.
     */
    private void addLabelEventHandlers(Label label, PDATransition transition) {
        // Do not add the event handlers to the Label if the PDAStateNode is not editable.
        if (startNode.isNotEditable()) {
            return;
        }

        Shape lineOrCurve = getLineOrCurve();

        Color LIGHT_BLUE = Color.web("0x1bb4e4");

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
            Dialog<PDATransition> editTransitionDialog;
            editTransitionDialog = startNode.getEditTransitionDialog(transition);
            editTransitionDialog.show();
        });
    }

    /**
     * Returns either the Line or the Curve of this transition arrow, depending on whether this
     * arrow is a loop transition or not.
     *
     * @return The Line of this arrow or the Curve (if this transition is a loop transition).
     */
    private Shape getLineOrCurve() {
        if (isLoopTransition) {
            return curve;
        } else {
            return line;
        }
    }

    /**
     * Deletes the Label of the specified transition and adjusts the positioning of the VBox after
     * the Label has been removed, if necessary.
     *
     * @param transition The transition to be deleted.
     */
    public void deleteTransition(PDATransition transition) {
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

        vBox.getChildren().remove(index);

        if (isLoopTransition) {
            if (vBox.getChildren().size() > 0) {
                // If the VBox has at least one label after the deletion, then it has already been
                // translated vertically. Therefore, translate it by the same amount in the opposite
                // direction since a Label is being removed. Do not want to do this if the VBox is
                // now empty since otherwise it would be in the wrong place when a Label is added in
                // the future.
                vBox.setLayoutY(vBox.getLayoutY() + 17);
            }
        } else {
            adjustMidpoints();

            // If after removing the label the size of the VBox's children is 0, the transition
            // arrow needs to be removed
            if (vBox.getChildren().size() == 0) {
                Pane canvas = (Pane) getParent();
                canvas.getChildren().remove(this);

                // Since a transition arrow has just been removed, see if it is necessary to convert
                // a double transition back to a normal transition.
                PDATransitionArrow doubleTransition = getOtherPDATransitionArrow();
                if (doubleTransition != null) {
                    doubleTransition.convertToNormalTransition();
                }
            }
        }
    }

    /**
     * Changes the colour of this PDATransitionArrow. If the PDAStateNode this arrow belongs to is
     * not editable, then the EventHandler for mouse exit is not added. If the newColour is
     * Color.BLACK, then only the Label is set to the newColour and the transition arrow is set to
     * its original colour of DARK_BLUE.
     *
     * @param transition The transition that needs its corresponding arrow's colour changed.
     * @param newColour  The new colour for the transition arrow.
     */
    public void changeTransitionColour(PDATransition transition, Color newColour) {
        for (Node node : vBox.getChildren()) {
            Label label = (Label) node;
            if (label.getText().equals(getTransitionLabel(transition).getText())) {
                Shape lineOrCurve = getLineOrCurve();
                // The label needs the colour changed regardless of whether this
                // PDAStateNode is editable or not
                label.setTextFill(newColour);

                // If this PDAStateNode is not editable, do not add the EventHandler -
                // just change the colour
                if (startNode.isNotEditable()) {
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

    /**
     * Gets a list of Transitions that highlights this transition arrow. As a transition arrow
     * consists of multiple parts, a single transition cannot highlight all the parts in one go.
     *
     * @param transition The PDATransition which needs to have its corresponding transition
     *                   arrow highlighted.
     * @param newColour  The colour the transition should be highlighted with.
     * @return An ArrayList of Transitions that highlights the transition arrow instantly
     * (duration of 0).
     */
    public ArrayList<Transition> getPDATransitionHighlightTransitions(PDATransition transition
            , String newColour) {
        ArrayList<Transition> transitions = new ArrayList<>();

        for (Node node : vBox.getChildren()) {
            Label label = (Label) node;
            // Find the Label that specifies this transition
            if (label.getText().equals(getTransitionLabel(transition).getText())) {
                Shape lineOrCurve = getLineOrCurve();

                // Custom transition to change the text fill of the Label
                Transition labelTransition = new Transition() {
                    @Override
                    protected void interpolate(double v) {
                        label.setTextFill(Color.web(newColour));
                    }
                };
                transitions.add(labelTransition);

                // Transition to change the colour of the Line or QuadCurve of the transition arrow
                StrokeTransition strokeTransition = new StrokeTransition(Duration.ZERO,
                        lineOrCurve);
                strokeTransition.setCycleCount(1);
                transitions.add(strokeTransition);

                // Transition to change the colour of the arrowhead
                FillTransition fillTransition = new FillTransition(Duration.ZERO, arrowhead);
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

        return transitions;
    }
}
