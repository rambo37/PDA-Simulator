package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDAConfiguration;
import PDA_Simulator.Backend.PDATransition;
import javafx.animation.*;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This class is the controller for the animation mode of the simulator. The animation mode allows
 * the user to watch an animation of the PDA running on an input string without having to manually
 * step forwards each time, as in the step by step mode.
 * <p>
 * This class receives a computation via the setComputation method which it animates. In order to
 * animate a different computation, a new computation needs to be passed to this class. Pressing the
 * play button successively just plays the same computation animation again from the beginning.
 *
 * @author Savraj Bassi
 */

public class AnimationController {
    // The animation that will be played
    private final SequentialTransition animation = new SequentialTransition();
    // The Pane which contains the non-editable PDAStateNodes
    private Pane canvas;
    // The applied transition at each step. An ArrayList so it can be passed to TransitionTableCell
    private final ArrayList<PDATransition> appliedTransition = new ArrayList<>();
    // The input string the PDA is being run on
    private String inputString;
    // A triangle shape that represents the tape head of the automaton
    private Polygon tapeHead;
    // This is the index of the previous configuration in the computation
    private int previousIndex = 0;
    // The computation that is to be animated
    private ArrayList<PDAConfiguration> computation;

    // Below are all the components with fx:ids in animation-view.fxml.
    @FXML
    private ScrollPane pdaScrollPane;
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
    private Button play;
    @FXML
    private Slider speedSlider;
    @FXML
    private HBox inputTape;
    @FXML
    private Pane tapeHeadPane;

    /**
     * Initialises the non-editable transition table, the pdaScrollPane, the animation buttons,
     * the animation slider and creates the tape head.
     */
    @FXML
    public void initialize() {
        // No need to set onEditCommit EventHandlers since the table is not editable
        currentState.setCellValueFactory(new PropertyValueFactory<>("currentState"));
        currentState.setCellFactory(param -> new TransitionTableCell());

        inputSymbol.setCellValueFactory(new PropertyValueFactory<>("inputSymbol"));
        inputSymbol.setCellFactory(param -> new TransitionTableCell());

        pop.setCellValueFactory(new PropertyValueFactory<>("popString"));
        pop.setCellFactory(param -> new TransitionTableCell());

        push.setCellValueFactory(new PropertyValueFactory<>("pushString"));
        push.setCellFactory(param -> new TransitionTableCell());

        newState.setCellValueFactory(new PropertyValueFactory<>("newState"));
        newState.setCellFactory(param -> new TransitionTableCell());

        play.setOnAction(event -> {
            resetAnimation();
            playAnimation(computation);
        });

        createTapeHead();
        // Bind the speed of the animation to the valueProperty of the slider to control the speed
        // See subsection 4.12.3 of the report for further details.
        animation.rateProperty().bind(speedSlider.valueProperty());
    }

    /**
     * Resets the animation to prepare for the next animation.
     */
    private void resetAnimation() {
        // Stop the current animation if it was playing
        animation.stop();
        unhighlightStateNodes();
        unhighlightAllTransitions();
        // Unhighlight the transition table by setting the CellFactory again with null passed as
        // the parameter
        currentState.setCellFactory(param -> new TransitionTableCell(null));
        // Clear the contents of the animation for the next animation
        animation.getChildren().clear();
        previousIndex = 0;
        // Set the position of the tapeHead back to its original position
        tapeHead.setTranslateX(0);
        hideStackForAllStateNodes();
    }

    /**
     * Unhighlights all PDAStateNodes by setting the background colour back to normal
     */
    private void unhighlightStateNodes() {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                nonEditableStateNode.changeBackgroundColour(Color.web("0x1bb4e4"));
                // original colour
            }
        }
    }

    /**
     * Unhighlights all transition arrows in the PDA diagram.
     */
    private void unhighlightAllTransitions() {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;

                for (PDATransition transition : transitionTable.getItems()) {
                    if (nonEditableStateNode.hasTransition(transition)) {
                        nonEditableStateNode.resetTransitionColour(transition);
                    }
                }
            }
        }
    }

    /**
     * Constructs an animation for a given computation and then plays the animation. The animation
     * does not highlight all the applicable transitions as the step by step mode does. Instead, it
     * highlights just the transition that is applied at each step of the given computation. See
     * subsection 4.12.2 of the report for further details.
     *
     * @param computation The computation to animate.
     */
    private void playAnimation(ArrayList<PDAConfiguration> computation) {
        // Iterate through each configuration in the computation and build up the animation
        for (int i = 0; i < computation.size(); i++) {
            PDAConfiguration configuration = computation.get(i);
            // Unhighlight all state nodes
            SequentialTransition reset = new SequentialTransition();
            reset.getChildren().addAll(unhighlightAllStateNodes());
            // This PauseTransition is just added to make the animation work, serves no other
            // purpose
            reset.getChildren().add(new PauseTransition(Duration.millis(1)));
            // If this is not the first configuration, hide the stack after this transition if the
            // state of the current configuration is different to the state of the previous
            // configuration. This makes the stack disappear only if the state changes from one
            // configuration to the next.
            if (i > 0) {
                if (!configuration.getState().equals(computation.get(i - 1).getState())) {
                    reset.setOnFinished(event -> hideStackForAllStateNodes());
                }
            }
            animation.getChildren().add(reset);
            String stateName = configuration.getState();
            // This transition is to highlight the current state of the configuration green
            SequentialTransition highlightCurrentState = new SequentialTransition();
            FillTransition highlightState = highlightStateNode(stateName, "0x05d010");
            highlightCurrentState.getChildren().add(highlightState);
            // This PauseTransition is just added to make the animation work, serves no other
            // purpose
            highlightCurrentState.getChildren().add(new PauseTransition(Duration.millis(1)));
            // Show the stack after highlighting the current state
            highlightCurrentState.setOnFinished(event -> showStackForStateNode(stateName,
                    configuration.getStack()));
            animation.getChildren().add(highlightCurrentState);

            // After highlighting the current state of the configuration and displaying the stack,
            // the next step in the animation depends on whether input was consumed at the previous
            // step or not. If an input symbol was consumed from the tape, then the next step is to
            // move the tape head to point to the next input symbol. Otherwise, the next step is to
            // highlight the PDA transition that gets applied.

            // A final variable with the same value as i for use in the lambda expressions
            int finalI = i;
            // If the index of this configuration is different from the index of the previous
            // configuration, the tape head needs to be moved. Recall the configuration index is the
            // index into the inputString.
            if (configuration.getIndex() != previousIndex) {
                SequentialTransition tapeHeadTransition = new SequentialTransition();
                TranslateTransition translateTransition =
                        new TranslateTransition(Duration.millis(100));
                translateTransition.setNode(tapeHead);
                // Translate by 31 pixels to the right since each tape cell is 30 pixels wide and
                // 1-pixel apart
                translateTransition.setByX(31);
                tapeHeadTransition.getChildren().add(translateTransition);
                // Pause for 1 second to give the user time to see what happens next
                tapeHeadTransition.getChildren().add(new PauseTransition(Duration.millis(1000)));

                // After this transition is finished, highlight the row in the transition table
                // that contains the PDA transition that gets applied. The
                // highlightAppliedPDATransitionInTable method does not return a Transition, so it
                // needs to be invoked once the PauseTransition finishes.
                tapeHeadTransition.setOnFinished(event ->
                        highlightAppliedPDATransitionInTable(finalI, computation));
                animation.getChildren().add(tapeHeadTransition);
                // As the tape head moved, previousIndex needs to be updated for future iterations
                previousIndex = configuration.getIndex();
            } else {
                // If the tape head does not move, we still need to highlight the row in the table.
                // The pause also helps keep the animation speed consistent when the tape head does
                // not move.
                PauseTransition pauseTransition = new PauseTransition(Duration.millis(1000));
                pauseTransition.setOnFinished(event ->
                        highlightAppliedPDATransitionInTable(finalI, computation));
                animation.getChildren().add(pauseTransition);
            }

            // Only highlight the applied transition if this is not the final configuration in the
            // computation.
            if (i != computation.size() - 1) {
                // Find the transition that was applied and add it to the appliedTransition
                // ArrayList
                PDAConfiguration nextConfiguration = computation.get(i+1);
                appliedTransition.clear();
                appliedTransition.add(MainController.getAppliedTransition(configuration,
                        nextConfiguration));
                // As this transition to highlight the transition arrow in the PDA diagram is
                // instantaneous, the row in the transition table is highlighted at the same time
                // the arrow is highlighted
                SequentialTransition highlightPDATransitionArrow = new SequentialTransition();
                highlightPDATransitionArrow.getChildren().addAll(highlightAppliedTransition(true));
                // Pause for 1 second after highlighting the transition arrow of the applied
                // transition
                PauseTransition pause = new PauseTransition(Duration.millis(1000));
                highlightPDATransitionArrow.getChildren().add(pause);
                animation.getChildren().add(highlightPDATransitionArrow);

                // This next transition unhighlights the highlighted arrow and once it finishes, it
                // also unhighlights the table rows.
                SequentialTransition unhighlightTransitionArrow = new SequentialTransition();
                unhighlightTransitionArrow.getChildren().addAll(highlightAppliedTransition(false));
                // This PauseTransition is just added to make the animation work, serves no other
                // purpose
                pause = new PauseTransition(Duration.millis(1));
                unhighlightTransitionArrow.getChildren().add(pause);
                // Unhighlight the transition table rows by setting the CellFactory again with null
                // as the parameter
                unhighlightTransitionArrow.setOnFinished(event ->
                        currentState.setCellFactory(param ->
                                new TransitionTableCell(null))
                );

                animation.getChildren().add(unhighlightTransitionArrow);
                appliedTransition.clear();
            } else {
                // If this is the final configuration in the computation, then change the colour
                // of the PDAStateNode that is the current state to red if the configuration is not
                // an accepting configuration
                if (!MainController.isAcceptingConfiguration(computation.get(i))) {
                    SequentialTransition highlightNonAcceptingNode = new SequentialTransition();
                    highlightNonAcceptingNode.getChildren().add(highlightStateNode(stateName,
                            "0xff4d4d")); // Red
                    PauseTransition pause = new PauseTransition(Duration.millis(1000));
                    highlightNonAcceptingNode.getChildren().add(pause);
                    animation.getChildren().add(highlightNonAcceptingNode);
                }
            }
        }

        // Once the animation has been constructed, play it
        animation.play();
    }

    /**
     * Creates and returns a list of FillTransitions that unhighlight all the PDAStateNodes.
     *
     * @return An ArrayList of FillTransitions to unhighlight all the PDAStateNodes.
     */
    private ArrayList<FillTransition> unhighlightAllStateNodes() {
        ArrayList<FillTransition> fillTransitions = new ArrayList<>();
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                fillTransitions.add(nonEditableStateNode.getStateNodeUnhighlightTransition());
            }
        }
        return fillTransitions;
    }

    /**
     * Hides the stack for all PDAStateNodes.
     */
    private void hideStackForAllStateNodes() {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                nonEditableStateNode.hideStack();
            }
        }
    }

    /**
     * Creates and returns a FillTransition that instantaneously changes the colour of a certain
     * PDAStateNode.
     *
     * @param stateName The name of the state that needs to have its corresponding PDAStateNode's
     *                  colour changed.
     * @param colour    The new colour for the PDAStateNode with the state name of stateName.
     * @return The FillTransition to change the colour of the PDAStateNode.
     */
    private FillTransition highlightStateNode(String stateName, String colour) {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                if (nonEditableStateNode.getStateName().equals(stateName)) {
                    return nonEditableStateNode.getStateNodeHighlightTransition(Color.web(colour));
                }
            }
        }
        return null;
    }

    /**
     * Highlights the PDATransition in the transition table that was applied to generate the
     * current configuration of the computation.
     *
     * @param index       The index of the current configuration in the computation.
     * @param computation The computation the index is for.
     */
    private void highlightAppliedPDATransitionInTable(int index,
                                                      ArrayList<PDAConfiguration> computation) {
        // If index is pointing to the last configuration in the computation, there is no need to
        // do anything since no
        // transitions are applied in the final configuration.
        if (index != computation.size() - 1) {
            PDAConfiguration configuration = computation.get(index);
            // Find the transition that was applied and add it to the appliedTransition ArrayList
            PDAConfiguration nextConfiguration = computation.get(index+1);
            appliedTransition.clear();
            appliedTransition.add(MainController.getAppliedTransition(configuration,
                    nextConfiguration));
            // Force the table to refresh to highlight the row containing the applied transition
            currentState.setCellFactory(param -> new TransitionTableCell(appliedTransition));
        }
    }

    /**
     * Makes the specified PDAStateNode display the given stack.
     *
     * @param stateName The name of the state which needs its corresponding PDAStateNode to display
     *                  the stack.
     * @param stack     The stack that should be displayed.
     */
    private void showStackForStateNode(String stateName, Stack<String> stack) {
        ObservableList<Node> children = canvas.getChildren();
        PDAStateNode targetNode = null;
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                if (nonEditableStateNode.getStateName().equals(stateName)) {
                    // Cannot modify the nonEditableStateNode in here due to
                    // ConcurrentModificationExceptions
                    targetNode = nonEditableStateNode;
                }
            }
        }

        // Bring the node to the front so the stack is shown in front of everything else
        assert targetNode != null;
        targetNode.toFront();
        targetNode.showStack(stack);
    }

    /**
     * Highlights or unhighlights the applied transition depending on the highlight argument.
     *
     * @param highlight Whether to highlight or to unhighlight the applied transition arrow.
     * @return An ArrayList of Transitions to highlight/unhighlight the applied transition arrow.
     */
    private ArrayList<Transition> highlightAppliedTransition(boolean highlight) {
        ArrayList<Transition> transitions = new ArrayList<>();
        ObservableList<Node> children = canvas.getChildren();

        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode node = (PDAStateNode) child;
                for (PDATransition transition : appliedTransition) {
                    if (node.hasTransition(transition)) {
                        ArrayList<Transition> highlightTransitions;
                        if (highlight) {
                            highlightTransitions =
                                    node.getPDATransitionHighlightTransitions(transition,
                                            "0x05d010");
                        } else {
                            highlightTransitions =
                                    node.getPDATransitionHighlightTransitions(transition,
                                            "0x000000");
                        }
                        transitions.addAll(highlightTransitions);

                    }
                }
            }
        }
        return transitions;
    }

    /**
     * Creates the tape head and positions it in the correct place.
     */
    private void createTapeHead() {
        tapeHead = new Polygon(-10.0, 8.0, 10.0, 8.0, 0.0, -12.0);
        tapeHead.setFill(Color.web("0x05d010"));
        tapeHead.setLayoutX(15);
        tapeHead.setLayoutY(15);
        tapeHeadPane.getChildren().add(tapeHead);
    }

    /**
     * Sets the content of the ScrollPane to the canvas which contains the PDA diagram.
     *
     * @param canvas The Pane that contains the non-editable PDAStateNodes.
     */
    public void setPDAScrollPaneContent(Pane canvas) {
        pdaScrollPane.setContent(canvas);
        this.canvas = canvas;
    }

    /**
     * Sets the content of the transition table to the given list of PDATransitions.
     *
     * @param transitions The PDATransitions of the PDA.
     */
    public void setTransitionTableContent(List<PDATransition> transitions) {
        transitionTable.getItems().addAll(transitions);
    }

    /**
     * Sets the column names for the pop and push columns of the transition table.
     *
     * @param popColumn  The name of the popColumn.
     * @param pushColumn The name of the pushColumn.
     */
    public void setColumnNames(String popColumn, String pushColumn) {
        pop.setText(popColumn);
        push.setText(pushColumn);
    }

    /**
     * Sets the input string the animation is for.
     *
     * @param inputString The input string the PDA is to be run on.
     */
    public void setInputString(String inputString) {
        this.inputString = inputString;
    }

    /**
     * Sets the computation that the animation is for.
     *
     * @param computation The computation to be animated.
     */
    public void setComputation(ArrayList<PDAConfiguration> computation) {
        this.computation = computation;
    }

    /**
     * Creates the input tape using the input string. Each character of the input string is put
     * into a separate square which represents a tape cell. If the input string is empty, then just
     * a single tape cell is created with the epsilon character which denotes the empty string.
     * //See subsection 4.12.4 of the report for further details.
     */
    public void createInputTape() {
        if (inputString.isEmpty()) {
            Group tapeCell = getTapeCell("ε");
            inputTape.getChildren().add(tapeCell);
        }
        for (int i = 0; i < inputString.length(); i++) {
            char symbol = inputString.charAt(i);
            Group tapeCell = getTapeCell(String.valueOf(symbol));
            inputTape.getChildren().add(tapeCell);
        }
    }

    /**
     * Creates and returns a Group consisting of a Rectangle and a Label which represents a cell of
     * the input tape.
     *
     * @param inputSymbol The input symbol for this tape cell.
     * @return A Group which represents a single tape cell of the input tape.
     */
    private Group getTapeCell(String inputSymbol) {
        Group cell = new Group();
        Rectangle square = new Rectangle(30, 30);
        square.setFill(Color.WHITE);
        square.setStroke(Color.BLACK);
        square.setStrokeType(StrokeType.INSIDE);
        cell.getChildren().add(square);

        Label label = new Label(inputSymbol);
        label.setLayoutX(12);
        label.setLayoutY(5);
        cell.getChildren().add(label);

        return cell;
    }
}
