package PDA_Simulator.Frontend;

import PDA_Simulator.Backend.PDAConfiguration;
import PDA_Simulator.Backend.PDATransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is the controller for the step by step mode of the simulator. The step by step mode
 * allows the user to run the constructed PDA on input words and observe exactly what the PDA is
 * doing at each step. For nondeterministic PDAs, the user can select whichever parallel computation
 * they want to step forwards/backwards at any time. As the user steps forwards, two things are
 * done. Firstly, the applicable transition(s) are highlighted. And secondly, the applicable
 * transition(s) are applied. When the user steps backwards, the computation is taken back to the
 * previous  configuration.
 *
 * @author Savraj Bassi
 */

public class StepByStepController {
    // A mapping of computation boxes (ScrollPanes) to computations (ArrayList<PDAConfiguration>)
    private final HashMap<ScrollPane, ArrayList<PDAConfiguration>> computationBoxMap =
            new HashMap<>();
    // A map to maintain what the current index of each computation is. See subsection 4.11.2 of
    // the report for further details.
    private final HashMap<ArrayList<PDAConfiguration>, Integer> computationIndexMap =
            new HashMap<>();
    // The computation the user is currently focused on
    private final SimpleObjectProperty<ScrollPane> activeComputation = new SimpleObjectProperty<>();
    // The Pane which contains the non-editable PDAStateNodes
    private Pane canvas;
    // The applicable transition(s) for a configuration
    private ArrayList<PDATransition> applicableTransitions = null;
    // The width of the primary screen
    private double screenWidth;

    // Below are all the components with fx:ids in step-by-step-view.fxml.
    @FXML
    private ScrollPane pdaScrollPane;
    @FXML
    private TableView<PDATransition> transitionTable;
    @FXML
    private FlowPane flowPane;
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

    /**
     * Initialises the non-editable transition table, the screenWidth, the flowPane and the
     * activeComputation Property.
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

        // Subtract 10 from the actual screenWidth as a safety net
        screenWidth = Screen.getPrimary().getVisualBounds().getWidth() - 10;
        // Set the gap between the flowPane children as 4% of the screenWidth
        flowPane.setHgap(screenWidth / 25);
        // The vertical gap between the flowPane children is fixed at 10 pixels, regardless of
        // screen size
        flowPane.setVgap(10);
        flowPane.setPrefWidth(screenWidth);
        // Left and right padding of 4% of the screenWidth to ensure the flowPane fills the
        // screenWidth well
        flowPane.setPadding(new Insets(10, screenWidth / 25, 0, screenWidth / 25));

        // This listener fires whenever the active computation changes. This happens whenever a
        // computation box (which is a ScrollPane) is clicked. See subsection 4.11.3 of the report
        // for further details.
        activeComputation.addListener((observableValue, previous, current) -> {
            // If the activeComputation changes while the applicable transitions of a computation
            // have been generated, unhighlight the applicable transitions in the table and in
            // the diagram and set applicableTransitions to null.
            if (applicableTransitions != null) {
                highlightApplicableTransitions(false);
            }

            // Set the CSS ID of the previous active computation back to the normal non-selected ID.
            for (ScrollPane scrollPane : computationBoxMap.keySet()) {
                if (scrollPane.getContent().getId().equals("selectedPDAConfiguration")) {
                    scrollPane.getContent().setId("pdaConfiguration");
                }
            }

            // Unhighlight state nodes since the active computation has changed
            unhighlightStateNodes();

            // Current can become null if you delete the active computation so check that it is
            // not null first
            if (current != null) {
                VBox configurationVBox = (VBox) current.getContent();
                // Only make this VBox appear selected (yellow background) if its previous ID was
                // pdaConfiguration. This is to prevent changing the background colour of
                // terminal configurations (accepting or non-accepting).
                if (configurationVBox.getId().equals("pdaConfiguration")) {
                    configurationVBox.setId("selectedPDAConfiguration");
                }

                ArrayList<PDAConfiguration> computation = computationBoxMap.get(current);
                // Highlight the current state for the current configuration of the active
                // computation. Use the computationIndexMap to get the current configuration for a
                // computation.
                String stateName = computation.get(computationIndexMap.get(computation)).getState();
                highlightStateNode(stateName);
            }
        });
    }

    /**
     * Unhighlights all PDAStateNodes by setting the colour of each node back to normal.
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
     * Highlights the specific PDAStateNode which has the correct name.
     *
     * @param stateName The name of the PDA state that needs to be highlighted.
     */
    private void highlightStateNode(String stateName) {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                if (nonEditableStateNode.getStateName().equals(stateName)) {
                    nonEditableStateNode.changeBackgroundColour(Color.web("0x05d010"));
                    // green colour
                }
            }
        }
    }

    /**
     * Highlights or unhighlights all the PDATransitions in the applicableTransitions ArrayList.
     * If the highlight argument is false, it also sets applicableTransitions to null to prepare for
     * the next step. See subsection 4.11.6 of the report for further details.
     *
     * @param highlight Whether to highlight or to unhighlight the transitions in
     *                  applicableTransitions.
     */
    private void highlightApplicableTransitions(boolean highlight) {
        ObservableList<Node> children = canvas.getChildren();
        for (Node child : children) {
            if (child.getClass() == PDAStateNode.class) {
                PDAStateNode nonEditableStateNode = (PDAStateNode) child;
                for (PDATransition transition : applicableTransitions) {
                    if (nonEditableStateNode.hasTransition(transition)) {
                        if (highlight) {
                            nonEditableStateNode.highlightTransitionGreen(transition);
                        } else {
                            nonEditableStateNode.resetTransitionColour(transition);
                        }
                    }
                }
            }
        }

        if (!highlight) {
            applicableTransitions = null;
        }
        currentState.setCellFactory(param -> new TransitionTableCell(applicableTransitions));
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
     * Creates the initial computation box, updates the maps and the active computation and adds
     * the computation box to the flowPane.
     *
     * @param computation The initial computation for the step by step mode to work on.
     */
    public void createInitialComputation(ArrayList<PDAConfiguration> computation) {
        // As this is the initial computation, it contains only one configuration, so set the
        // Index to 0
        int index = 0;
        computationIndexMap.put(computation, index);
        // Create a ScrollPane computationBox for this computation
        ScrollPane computationBox = createComputationBox(computation);
        computationBoxMap.put(computationBox, computation);
        activeComputation.set(computationBox);
        // If the initial configuration is an accepting one, then make the background of the
        // computationBox green to indicate it is an accepting computation.
        if (MainController.isAcceptingConfiguration(computation.get(0))) {
            computationBox.getContent().setId("acceptingPDAConfiguration");
        }
        flowPane.getChildren().add(computationBox);
    }

    /**
     * Creates a computationBox for a given computation. This computationBox is a ScrollPane which
     * contains a VBox that shows all the relevant information of a single PDAConfiguration. In
     * addition, it contains buttons to step the computation forwards/backwards and a button to
     * remove the computation from the flowPane.
     *
     * @param computation The computation which this computationBox is for.
     * @return A ScrollPane computationBox for the computation.
     */
    private ScrollPane createComputationBox(ArrayList<PDAConfiguration> computation) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.getStylesheets().add("CSS/configuration.css");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setPrefHeight(130);
        // Set the width of the computationBox to 20% of the screenWidth. Due to the flowPane
        // having left and right padding of 4% and a horizontal space between children of 4%, four
        // computation boxes can be shown per row.
        scrollPane.setPrefWidth(screenWidth / 5);

        // Get the index for this computation from the map so the current configuration can be
        // obtained.
        int index = computationIndexMap.get(computation);
        PDAConfiguration configuration = computation.get(index);
        // Use the MainController static method to get a configurationVBox for the current
        // configuration.
        VBox configurationVBox = MainController.getConfigurationVBox(configuration);
        // Create three buttons of equal width
        TilePane buttons = new TilePane(Orientation.HORIZONTAL);
        Button stepForwards = new Button("Step forwards");
        stepForwards.setMaxWidth(Double.MAX_VALUE);
        Button stepBackwards = new Button("Step backwards");
        stepBackwards.setMaxWidth(Double.MAX_VALUE);
        Button remove = new Button("Remove");
        remove.setMaxWidth(Double.MAX_VALUE);

        // Disable the step forwards button if this configuration is accepting and there is no
        // way out of it (i.e. via an epsilon transition).
        if (MainController.getApplicableTransitions(configuration).isEmpty())
            if (MainController.isAcceptingConfiguration(configuration)) {
                stepForwards.setDisable(true);
            }

        // If the index of this computation is 0, then there is no previous configuration, so
        // disable stepBackwards.
        if (index == 0) {
            stepBackwards.setDisable(true);
        }

        buttons.setHgap(20);
        buttons.setPrefWidth((screenWidth / 5) - 20);
        // Top margin of 10 from the configurationVBox to ensure adequate spacing
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        buttons.getChildren().addAll(stepBackwards, stepForwards, remove);

        configurationVBox.getChildren().add(buttons);
        configurationVBox.setMinWidth((screenWidth / 5) - 2);

        //See subsection 4.11.4 of the report for further details.
        stepForwards.addEventFilter(ActionEvent.ACTION, event -> {
            // Update the active computation when the button is pressed
            activeComputation.set(scrollPane);

            // If applicableTransitions is null, then pressing the step forwards button should
            // generate and highlight the applicable transition(s)
            if (applicableTransitions == null) {
                // If index is not equal to computation.size() - 1, then that means the user
                // previously stepped backwards. Therefore, figure out which transition was
                // previously applied to generate the next configuration in the computation and
                // highlight just that transition rather than all applicable ones.
                if (index < computation.size() - 1) {
                    // Get the next configuration in the computation using the current index
                    PDAConfiguration nextConfiguration = computation.get(index + 1);

                    // Figure out which applicable transition was applied to generate the next
                    // configuration
                    applicableTransitions = new ArrayList<>();
                    applicableTransitions.add(MainController.getAppliedTransition(configuration,
                            nextConfiguration));
                    // Highlight just this applied transition in the table and PDA diagram
                    highlightApplicableTransitions(true);
                } else {
                    // Since the index is pointing to the last configuration in the computation,
                    // generate and highlight all applicable transitions.
                    applicableTransitions = MainController.getApplicableTransitions(configuration);

                    // As there are no applicable transitions, change the colour of the
                    // configuration box. This lets the user know the PDA rejects the input word
                    // as it has gotten stuck.
                    if (applicableTransitions.isEmpty()) {
                        configurationVBox.setId("nonAcceptingPDAConfiguration");
                        stepForwards.setDisable(true);
                    } else {
                        // Highlight all the applicable transitions in the table and PDA diagram
                        highlightApplicableTransitions(true);
                    }
                }
                // If applicableTransitions is not null, then the next step is to apply the
                // applicable transition(s)
            } else {
                if (applicableTransitions.size() == 1) {
                    // As there is only 1 applicable transition, replace the existing computation
                    // with a new computation ending with the configuration obtained by applying the
                    // single applicable transition
                    int originalScrollPaneIndex = flowPane.getChildren().indexOf(scrollPane);
                    flowPane.getChildren().remove(scrollPane);
                    computationBoxMap.remove(scrollPane);
                    PDAConfiguration newConfiguration;

                    int newIndex = index + 1;
                    // If index is not at the final configuration in the computation, then the
                    // newConfiguration can be obtained by just using the computation
                    if (index < computation.size() - 1) {
                        newConfiguration = computation.get(newIndex);
                    } else {
                        // Since we are at the end of the computation, we need to apply the
                        // transition to get the new configuration and add it to the computation
                        // since it was not already in it
                        newConfiguration = MainController.applyTransition(configuration,
                                applicableTransitions.get(0));
                        computation.add(newConfiguration);
                    }

                    // Update the index of this computation
                    computationIndexMap.put(computation, newIndex);
                    ScrollPane computationBox = createComputationBox(computation);
                    computationBoxMap.put(computationBox, computation);
                    // Add this new computationBox at the same index the old one was at, so it
                    // does not seem to move
                    flowPane.getChildren().add(originalScrollPaneIndex, computationBox);
                    // Making the new computationBox the active computation handles all the
                    // highlighting/unhighlighting and makes it appear as the selected computation
                    activeComputation.set(computationBox);

                    // Check whether the computation ends with an accepting configuration and
                    // change the CSS if required
                    if (MainController.isAcceptingConfiguration(newConfiguration)) {
                        computationBox.getContent().setId("acceptingPDAConfiguration");
                    }
                } else {
                    // If there is more than 1 applicable transition (nondeterminism) disable the
                    // buttons to step forwards and backwards for this computation box.
                    stepForwards.setDisable(true);
                    stepBackwards.setDisable(true);

                    for (PDATransition transition : applicableTransitions) {
                        // Generate a new computation for each applicable transition that shares
                        // the original computation but differs by only the final configuration
                        PDAConfiguration newConfiguration =
                                MainController.applyTransition(configuration, transition);
                        ArrayList<PDAConfiguration> newComputation = new ArrayList<>(computation);
                        newComputation.add(newConfiguration);

                        // Map this new computation to index + 1 since a configuration has been
                        // added to the end of the old computation.
                        int newIndex = index + 1;
                        computationIndexMap.put(newComputation, newIndex);
                        ScrollPane computationBox = createComputationBox(newComputation);
                        computationBoxMap.put(computationBox, newComputation);
                        flowPane.getChildren().add(computationBox);
                        // Check whether the computation ends with an accepting configuration and
                        // change the CSS if required
                        if (MainController.isAcceptingConfiguration(newConfiguration)) {
                            computationBox.getContent().setId("acceptingPDAConfiguration");
                        }
                    }
                    // Set activeComputation to null since there is no activeComputation at this
                    // point. This handles the unhighlighting of the states and transitions as well
                    // as the colour of the old computationBox.
                    activeComputation.set(null);
                }
            }
        });

        // Stepping backwards deletes the computationBox and replaces it with a new one that is
        // at the previous step and that is at the same position in the flowPane. There is no need
        // to check that the index > 0 since the stepBackwards button is disabled if index is 0 when
        // the computationBox is created. See subsection 4.11.5 of the report for further details.
        stepBackwards.addEventFilter(ActionEvent.ACTION, event -> {
            // Update the active computation when the button is pressed.
            activeComputation.set(scrollPane);

            int originalScrollPaneIndex = flowPane.getChildren().indexOf(scrollPane);
            flowPane.getChildren().remove(scrollPane);
            computationBoxMap.remove(scrollPane);
            // When stepping backwards, the previousConfiguration will always be the second last
            // one in the computation since a new computation with index - 1 is generated each time
            // the user steps backwards.
            PDAConfiguration previousConfiguration = computation.get(computation.size() - 2);
            int newIndex = index - 1;

            // Map this same computation to a new index which is 1 lower (adding this entry replaces
            // the old one).
            computationIndexMap.put(computation, newIndex);
            // Create a new computationBox for the computation
            ScrollPane computationBox = createComputationBox(computation);

            // Map this new computationBox to the same computation.
            computationBoxMap.put(computationBox, computation);
            // Add the computationBox to the same index of the flowPane the original computationBox
            // was at.
            flowPane.getChildren().add(originalScrollPaneIndex, computationBox);
            // Update the activeComputation to the computationBox that replaces the old
            // computationBox.
            activeComputation.set(computationBox);

            if (MainController.isAcceptingConfiguration(previousConfiguration)) {
                computationBox.getContent().setId("acceptingPDAConfiguration");
            }
        });

        // When the remove button is clicked, the maps and flowPane remove this computation /
        // computationBox and the activeComputation is set to null.
        remove.addEventFilter(ActionEvent.ACTION, event -> {
            computationBoxMap.remove(scrollPane);
            computationIndexMap.remove(computation);
            activeComputation.set(null);
            flowPane.getChildren().remove(scrollPane);
        });

        scrollPane.setContent(configurationVBox);
        // This computationBox becomes the activeComputation whenever it is clicked.
        scrollPane.setOnMousePressed(event -> activeComputation.set(scrollPane));

        return scrollPane;
    }
}
