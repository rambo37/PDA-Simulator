package PDA_Simulator.Backend;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;

/**
 * This class contains all the necessary functionality of the PDA object. Note that this class
 * does not contain any information about the transition function being used (pop symbol/push
 * symbol, pop symbol/push string or pop string/push string). Instead, the code for running the PDA
 * runs on the assumption that the PDA is always able to pop and push strings. This is because code
 * that can handle popping and pushing of strings of stack symbols is also able to handle popping
 * and pushing of just single stack symbols. Consequently, the distinction is purely made in the
 * frontend.
 * <p>
 * States of the PDA are simply modelled as strings while transitions have a dedicated class
 * (PDATransition). There is also a class that represents PDA configurations which are used when the
 * pushdown automaton is run. A computation is a sequence of PDA configurations and this is just
 * represented as an ArrayList of PDAConfiguration objects.
 *
 * @author Savraj Bassi
 */

public class PDA {
    // A boolean Property to monitor whether the PDA is deterministic or not
    private final SimpleBooleanProperty deterministic = new SimpleBooleanProperty(true);
    // The initial stack symbol of the PDA (null if the stack starts off empty)
    private String initialStackSymbol = null;
    // The PDATransitions of this PDA. Observable so changes can be detected automatically in the
    // controller
    private final ObservableList<PDATransition> transitions = FXCollections.observableArrayList();
    // The states of this PDA. Observable so changes can be detected automatically in the controller
    private final ObservableList<String> states = FXCollections.observableArrayList();
    // A String property that contains the initial state of the PDA
    private final SimpleStringProperty initialState = new SimpleStringProperty(null);
    // The set of accepting states of this PDA
    private final ArrayList<String> acceptingStates = new ArrayList<>();
    // A counter used to generate distinct state names automatically
    private int counter = -1;
    // The acceptance criterion of the PDA. Default acceptance criteria is accepting state.
    private AcceptanceCriteria acceptanceCriteria = AcceptanceCriteria.ACCEPTING_STATE;
    // A list to store all the accepting computations that are found when running the PDA on an
    // input string
    private final ArrayList<ArrayList<PDAConfiguration>> acceptingComputations = new ArrayList<>();
    // A boolean to indicate whether the computation step limit was reached for any individual
    // computation when searching for accepting computations
    private boolean hitMaxSteps = false;
    // A boolean to indicate whether the maximum number of total steps across all computations was
    // reached when searching for accepting computations.
    private boolean hitMaxTotalSteps = false;
    // The set of transitions that cause nondeterminism.
    private final HashSet<PDATransition> nondeterministicTransitions = new HashSet<>();

    /**
     * Creates a new PDA and adds a listener to the transitions ObservableList.
     */
    public PDA() {
        // Whenever any changes are made to the ObservableList of transitions, check if the
        // determinism changes
        transitions.addListener((ListChangeListener<PDATransition>) change -> checkDeterminism());
    }

    /**
     * Checks if the PDA is deterministic or not and updates the SimpleBooleanProperty, if
     * necessary. Nondeterminism can only be possible if there is at least one state with several
     * transitions. Therefore, iterate through all states and obtain all the transitions that
     * belong to that state. If there is more than just one transition, check for nondeterminism.
     * Also stores any transitions that cause nondeterminism as they are discovered.
     */
    private void checkDeterminism() {
        boolean nonDeterministic = false;
        // Clear the set of nondeterministic transitions to begin with
        nondeterministicTransitions.clear();
        ArrayList<PDATransition> stateTransitions = new ArrayList<>();
        // Find all transitions belonging to each state
        for (String state : states) {
            stateTransitions.clear();
            for (PDATransition transition : transitions) {
                if (transition.getCurrentState().equals(state)) {
                    stateTransitions.add(transition);
                }
            }
            // Check for nondeterminism in the set of state transitions.
            if (stateTransitions.size() > 1) {
                for (int i = 0; i < stateTransitions.size() - 1; i++) {
                    for (int j = i + 1; j < stateTransitions.size(); j++) {
                        String inputSymbolI = stateTransitions.get(i).getInputSymbol();
                        String inputSymbolJ = stateTransitions.get(j).getInputSymbol();
                        String popStringI = stateTransitions.get(i).getPopString();
                        String popStringJ = stateTransitions.get(j).getPopString();

                        // Test for condition 1 of nondeterminism
                        if (inputSymbolI.equals(inputSymbolJ)) {
                            if (popStringI.equals(popStringJ)) {
                                // More than one transition defined for current state, input
                                // symbol and pop string.  For example (q0, a, ε) -> (q1, ε) and
                                // (q0, a, ε) -> (q2, ε).
                                nonDeterministic = true;
                                nondeterministicTransitions.add(stateTransitions.get(i));
                                nondeterministicTransitions.add(stateTransitions.get(j));
                            }
                        }
                        // Test for condition 2 of nondeterminism (epsilon transitions)
                        if (inputSymbolI.isEmpty() || inputSymbolJ.isEmpty()) {
                            if (popStringI.equals(popStringJ)) {
                                // Both an epsilon transition and another transition with the
                                // same current state and the same pop string.
                                nonDeterministic = true;
                                nondeterministicTransitions.add(stateTransitions.get(i));
                                nondeterministicTransitions.add(stateTransitions.get(j));
                            }

                            // If at least one transition reads nothing from the input tape and
                            // at least one of them pops nothing from the stack, then the PDA is
                            // nondeterministic. E.g. (q0, a, ε) -> (q1, ε) and
                            // (q0, ε, A) -> (q1, ε) would be nondeterministic since if you have an
                            // 'A' on the top of the stack and the current input symbol is 'a', you
                            // can apply both transitions.
                            if (popStringI.isEmpty() || popStringJ.isEmpty()) {
                                nonDeterministic = true;
                                nondeterministicTransitions.add(stateTransitions.get(i));
                                nondeterministicTransitions.add(stateTransitions.get(j));
                            }
                        }
                        // Test for the third condition of nondeterminism
                        if (inputSymbolI.equals(inputSymbolJ) || inputSymbolI.isEmpty() ||
                                inputSymbolJ.isEmpty()) {

                            if (popStringI.startsWith(popStringJ) ||
                                    popStringJ.startsWith(popStringI)) {
                                nonDeterministic = true;
                                nondeterministicTransitions.add(stateTransitions.get(i));
                                nondeterministicTransitions.add(stateTransitions.get(j));
                            }
                            // If we have two transitions with the same current state and input
                            // symbol (or at least one of the transition input symbols is epsilon)
                            // but at least one of them pops nothing (i.e. it doesn't care
                            // what is on the stack), then we have nondeterminism. For example
                            // (q0, a, A) -> (q1, ε) and (q0, a, ε) -> (q1, ε). But this also
                            // applies to (q0, a, A) -> (q1, ε) and (q0, a, AB) -> (q1, ε), for
                            // example. If the second transition is applicable, then so is the first
                        }
                    }
                }
            }

        }
        if (nonDeterministic == deterministic.get()) {
            updateDeterminism();
        }
    }

    /**
     * Changes the deterministic SimpleBooleanProperty to the negation of its current value.
     */
    private void updateDeterminism() {
        deterministic.set(!deterministic.get());
    }

    /**
     * Changes the initial state of this PDA to the given state.
     *
     * @param newInitialState The new initial state of the PDA.
     */
    public void changeInitialState(String newInitialState) {
        initialState.set(newInitialState);
    }

    /**
     * Reconstructs a PDA from a string containing all of the relevant information.
     *
     * @param pda The string representation of the PDA.
     */
    public void loadPDAFromString(String pda) {
        int endOfStatesList = pda.indexOf("]");
        String statesString = pda.substring(9, endOfStatesList);
        String[] states = statesString.split(", ");
        // Create a state for each of the states in the string using the state names in the string
        for (String state : states) {
            if (!state.isEmpty()) {
                addState(state);
            }
        }

        String rest = pda.substring(endOfStatesList + 15);
        int endOfTransitionsList = rest.indexOf("]");
        String transitionsString = rest.substring(1, endOfTransitionsList);
        String[] transitions = transitionsString.split(", ");
        for (String transition : transitions) {
            if (!transition.isEmpty()) {
                // Use the constructor in the PDATransition class that reconstructs a
                // PDATransition from a string
                PDATransition pdaTransition = new PDATransition(transition);
                addTransition(pdaTransition);
            }
        }

        rest = rest.substring(endOfTransitionsList + 16);
        int commaIndex = rest.indexOf(",");
        String initialStateString = rest.substring(0, commaIndex);
        // Although a PDA without an initial state cannot be run, it can still be saved. The word
        // "null" cannot be used in the frontend to prevent ambiguity about whether there is an
        // initial state called null or there is just no initial state.
        if (initialStateString.equals("null")) {
            initialState.set(null);
        } else {
            initialState.set(initialStateString);
        }

        int openSquareParenthesisIndex = rest.indexOf("[");
        rest = rest.substring(openSquareParenthesisIndex + 1);
        int closeSquareParenthesisIndex = rest.indexOf("]");
        String acceptingStatesString = rest.substring(0, closeSquareParenthesisIndex);
        String[] acceptingStates = acceptingStatesString.split(", ");
        for (String state : acceptingStates) {
            if (!state.isEmpty()) {
                changeAcceptingState(state);
            }
        }

        int equalsIndex = rest.indexOf("=");
        rest = rest.substring(equalsIndex + 1);
        commaIndex = rest.indexOf(",");
        String initialStackSymbolString = rest.substring(0, commaIndex);
        if (initialStackSymbolString.equals("null")) {
            setInitialStackSymbol(null);
        } else {
            setInitialStackSymbol(initialStackSymbolString);
        }

        equalsIndex = rest.indexOf("=");
        rest = rest.substring(equalsIndex + 1);

        // Convert the string that contains the acceptance criteria to an AcceptanceCriteria enum.
        acceptanceCriteria = AcceptanceCriteria.valueOf(rest.substring(0, rest.length() - 1));
    }

    /**
     * Create a PDA state with a specific name.
     *
     * @param state The name of the state to be created/
     */
    private void addState(String state) {
        states.add(state);
    }

    /**
     * Create a new PDA state with an automatically generated name using the counter.
     */
    public void addState() {
        counter++;
        // If the next name is already in use, find the next available name
        if (states.contains("q" + counter)) {
            // Increment the counter until a fresh name can be generated.
            for (int i = counter + 1; i < Integer.MAX_VALUE; i++) {
                if (!states.contains("q" + i)) {
                    states.add("q" + i);
                    return;
                }
            }
        }
        // If the next name is not in use, then use that.
        states.add("q" + counter);
        // If there was no initial state, make this newly created state the initial state
        if (initialState.get() == null) {
            initialState.set("q" + counter);
        }
    }

    /**
     * This method handles the deletion of a PDA state. It first deletes all incoming and outgoing
     * transitions for the state that is to be deleted.
     *
     * @param state The state that is being deleted.
     */
    public void deleteState(String state) {
        // Delete all transitions involving the deleted state, whether they were transitioning to
        // or from that state.
        transitions.removeIf(t ->
                t.getCurrentState().equals(state) || t.getNewState().equals(state));

        states.remove(state);
        acceptingStates.remove(state);
        // If the state being deleted is the initial state, set the initialState Property's value
        // to null.
        if (state.equals(initialState.get())) {
            initialState.set(null);
        }
    }

    /**
     * Renames a state to the given new name and returns true/false to indicate whether the
     * operation was successful or not. If the new name is already in use, the renaming is not
     * done. All incoming/outgoing transitions need to be modified to use the new name as well if
     * the renaming is done.
     *
     * @param state   The original name of the state.
     * @param newName The desired name of the state.
     * @return True if the renaming was successful and false otherwise.
     */
    public boolean renameState(String state, String newName) {
        // If the state is renamed to its original name, change nothing and return true since
        // this is fine
        if (state.equals(newName)) {
            return true;
        }
        // If the new name of the state is already in use, then return false as this renaming is
        // invalid
        else if (hasState(newName)) {
            return false;
        } else {
            // Replace the old state with the new state. It is guaranteed that the state belongs
            // in the list of states.
            int index = states.indexOf(state);
            states.set(index, newName);
            // It is necessary to first check whether the given state is an accepting state
            // before attempting to set the element at the specified index to the newName since not
            // every state is an accepting state.
            index = acceptingStates.indexOf(state);
            if (index != -1) {
                acceptingStates.set(index, newName);
            }
            // Rename the initial state if necessary
            if (state.equals(initialState.get())) {
                initialState.set(newName);
            }

            // Find all the transitions using the old name of the state as these are no longer valid
            ArrayList<PDATransition> invalidTransitions = new ArrayList<>();

            for (PDATransition transition : transitions) {
                String currentState = transition.getCurrentState();
                String newState = transition.getNewState();
                if (currentState.equals(state) || newState.equals(state)) {
                    invalidTransitions.add(transition);
                }
            }
            // Delete the transitions with the old state name
            transitions.removeIf(invalidTransitions::contains);

            // Add transitions with the new state name
            for (PDATransition transition : invalidTransitions) {
                String currentState = transition.getCurrentState();
                if (currentState.equals(state)) {
                    currentState = newName;
                }

                String newState = transition.getNewState();
                if (newState.equals(state)) {
                    newState = newName;
                }

                String inputSymbol = transition.getInputSymbol();
                String popString = transition.getPopString();
                String pushString = transition.getPushString();

                transitions.add(new PDATransition(currentState, inputSymbol, popString, pushString,
                        newState));
            }

            return true;
        }
    }

    /**
     * Updates the initial stack symbol to the provided symbol (which can be null)
     *
     * @param symbol The new initial stack symbol or null if the PDA will start with an empty stack.
     */
    public void setInitialStackSymbol(String symbol) {
        initialStackSymbol = symbol;
    }

    /**
     * Changes whether a state is accepting or not. If called with an accepting state, the state
     * ends up no longer being accepting. If called with a non-accepting state, the state is made
     * an accepting state.
     *
     * @param state The state that is being changed.
     */
    public void changeAcceptingState(String state) {
        int index = acceptingStates.indexOf(state);
        // If state was not already an accepting state, make it an accepting state
        if (index == -1) {
            acceptingStates.add(state);
        } else {
            acceptingStates.remove(state);
        }
    }

    /**
     * Attempts to add the given transition to this PDA.
     *
     * @param transition The transition to be added.
     * @return True if the transition was successfully added and false otherwise.
     */
    public boolean addTransition(PDATransition transition) {
        if (!hasTransition(transition)) {
            // Create new PDA states if the transition specifies states that do not already exist
            createTransitionStates(transition);
            transitions.add(transition);
            return true;
        }
        // Do not add duplicate transitions. Return false if the transition already exists.
        return false;
    }

    /**
     * Add a PDATransition at a specific index. This is to preserve the ordering of the transitions
     * ObservableList when a transition is edited.
     *
     * @param transition The transition being added.
     * @param index      The index the transition is being added at.
     */
    private void addTransition(PDATransition transition, int index) {
        // Do not add duplicate transitions.
        if (!hasTransition(transition)) {
            // Create new PDA states if the transition specifies states that do not already exist
            createTransitionStates(transition);
            transitions.add(index, transition);
        }
    }

    /**
     * Checks whether the PDA contains a particular PDATransition.
     *
     * @param transition The PDATransition that is being checked.
     * @return True if the PDA already has this PDATransition and false otherwise.
     */
    private boolean hasTransition(PDATransition transition) {
        return transitions.contains(transition);
    }

    /**
     * Creates and adds new states to the PDA if the given transition specifies states that do not
     * exist in the PDA.
     *
     * @param transition The transition being added to the PDA.
     */
    private void createTransitionStates(PDATransition transition) {
        String currentState = transition.getCurrentState();
        String newState = transition.getNewState();
        if (!hasState(currentState)) {
            addState(currentState);
        }
        if (!hasState(newState)) {
            addState(newState);
        }
    }

    /**
     * Checks whether the PDA contains a particular state.
     *
     * @param state The name of the state that is being checked.
     * @return True if the PDA already has this state and false otherwise.
     */
    private boolean hasState(String state) {
        return states.contains(state);
    }

    /**
     * Deletes a PDATransition from this PDA.
     *
     * @param transition The transition that needs to be deleted.
     */
    public void deleteTransition(PDATransition transition) {
        transitions.remove(transition);
    }

    /**
     * Attempts to edit a transition.
     *
     * @param oldTransition The transition to be edited.
     * @param newTransition The new desired transition.
     * @return True if the edit was successful and false otherwise.
     */
    public boolean editTransition(PDATransition oldTransition, PDATransition newTransition) {
        // If the transition is edited into itself, change nothing and return true since this is
        // fine to do.
        if (oldTransition.equals(newTransition)) {
            return true;
        }
        // If the transition already exists then do nothing and return false to indicate the edit
        // failed.
        if (hasTransition(newTransition)) {
            return false;
        }
        // Find the index of the oldTransition so that we add to the same index.
        int index = transitions.indexOf(oldTransition);
        deleteTransition(oldTransition);
        addTransition(newTransition, index);

        return true;
    }

    /**
     * Changes the acceptance criteria of the PDA.
     *
     * @param newAcceptanceCriteria The new acceptance criteria.
     */
    public void changeAcceptanceCriteria(AcceptanceCriteria newAcceptanceCriteria) {
        acceptanceCriteria = newAcceptanceCriteria;
    }

    /**
     * Adds a listener to the deterministic SimpleBooleanProperty.
     *
     * @param listener The listener to be added.
     */
    public void addDeterministicPropertyListener(ChangeListener<Boolean> listener) {
        deterministic.addListener(listener);
    }

    /**
     * Adds a listener to the deterministic SimpleStringProperty.
     *
     * @param listener The listener to be added.
     */
    public void addInitialStatePropertyListener(ChangeListener<String> listener) {
        initialState.addListener(listener);
    }

    /**
     * Gets the ObservableList of PDATransitions of this PDA object.
     *
     * @return The transitions (PDATransitions) ObservableList.
     */
    public ObservableList<PDATransition> getTransitions() {
        return transitions;
    }

    /**
     * Adds a listener to the transitions ObservableList.
     *
     * @param listener The listener to be added.
     */
    public void addTransitionsListener(ListChangeListener<PDATransition> listener) {
        transitions.addListener(listener);
    }

    /**
     * Gets the ObservableList of states of this PDA object.
     *
     * @return The states (Strings) ObservableList.
     */
    public ObservableList<String> getStates() {
        return states;
    }

    /**
     * Adds a listener to the state ObservableList.
     *
     * @param listener The listener to be added.
     */
    public void addStatesListener(ListChangeListener<String> listener) {
        states.addListener(listener);
    }

    /**
     * Returns the value of the SimpleBooleanProperty deterministic. Only necessary for testing
     * purposes since MainController adds a listener to this SimpleBooleanProperty and so never
     * needs to access the value.
     *
     * @return True if the PDA is deterministic and false otherwise.
     */
    public boolean getDeterministic() {
        return deterministic.get();
    }

    /**
     * Generates all possible accepting computations (a computation is an ArrayList of
     * PDAConfigurations) for the given input string which contain a number of configurations (or
     * steps) less than or equal to maxSteps. If the max total step limit is reached, then the
     * computation terminates prematurely. The return value of this method is a pair consisting of
     * the acceptingComputations ArrayList as well as a Boolean. If there cannot be any accepting
     * computations regardless of the length limit, then returns null instead. See subsection 4.9.1
     * of the report for further details.
     *
     * @param inputString   The input string the automaton is being run on.
     * @param maxSteps      The maximum number of steps the computations are allowed to go on for.
     * @param maxTotalSteps The maximum number of total steps across all computations.
     * @return A pair where the first element is an ArrayList of all accepting computations and the
     * second is a Boolean indicating whether the step limit or total step limit was reached. If no
     * accepting computations were found without reaching either limit, then null is returned.
     */
    public Pair<ArrayList<ArrayList<PDAConfiguration>>, Boolean> getAcceptingComputations(
            String inputString, int maxSteps, int maxTotalSteps) {
        // Start by clearing the previous acceptingComputations and resetting the booleans
        acceptingComputations.clear();
        hitMaxSteps = false;
        hitMaxTotalSteps = false;
        // Get the initial configuration of this PDA. This is the configuration that all
        // computations start with.
        PDAConfiguration initialConfiguration = getInitialConfiguration(inputString);
        // Create a computation consisting of just the initial configuration
        ArrayList<PDAConfiguration> computation = new ArrayList<>();
        computation.add(initialConfiguration);
        // Run the PDA with the initial computation and the step limit. This will add all
        // accepting computations that are discovered to the acceptingComputations ArrayList.
        runPDAOnInputString(computation, maxSteps, maxTotalSteps);
        // If the PDA has at least one accepting computation or hit either the individual
        // computation length limit or the total step limit, then return a Pair consisting of any
        // discovered accepting computations as well as a Boolean that indicates which of the two
        // limits was reached (if any). If a limit was reached, then it could be the case that there
        // are accepting computations but none were found with the step limit.
        if (hitMaxSteps || hitMaxTotalSteps || !acceptingComputations.isEmpty()) {
            // Sort the accepting computations so that the shortest computations are at the front
            Comparator<ArrayList<PDAConfiguration>> computationComparator =
                    Comparator.comparingInt(ArrayList::size);
            acceptingComputations.sort(computationComparator);
            // If hitMaxTotalSteps is true, return false as the second element of the pair so that
            // the calling method knows the total step limit was reached.
            if (hitMaxTotalSteps) {
                return new Pair<>(acceptingComputations, false);
            }
            // If hitMaxSteps is true, return true as the second element of the pair so that the
            // calling method knows the individual computation step limit was reached for at least
            // one computation.
            if (hitMaxSteps) {
                return new Pair<>(acceptingComputations, true);
            }
            return new Pair<>(acceptingComputations, null);
        }
        // If the PDA did not hit the maximum number of steps for any individual computation or the
        // maximum number of total steps across all computations, and did not find an accepting
        // computation, then that means the PDA had no more applicable transitions for all
        // computations. Therefore, return null to indicate that the PDA rejects the input string.
        return null;
    }

    /**
     * Create and return an initial configuration for this pushdown automaton based on the initial
     * state, initial stack symbol and the input string.
     *
     * @param inputString The input string this PDA is going to be run on.
     * @return The initial PDAConfiguration
     */
    public PDAConfiguration getInitialConfiguration(String inputString) {
        // Set the static variable of the PDAConfiguration class to the inputString. This allows
        // all instances of the PDAConfiguration class to share the same string while only a single
        // copy of it is stored.
        PDAConfiguration.setInputString(inputString);
        Stack<String> stack = new Stack<>();
        if (initialStackSymbol != null) {
            stack.push(initialStackSymbol);
        }
        // The returned configuration has the correct stack, the initial state and an index of 0
        // to denote that the input symbol this PDAConfiguration is currently at is the first one
        // of the input string.
        return new PDAConfiguration(stack, initialState.get(), 0);
    }

    /**
     * Run the PDA in a depth-first search manner up to the limit of maxSteps and add any
     * discovered accepting computations to the acceptingComputations field. Stop if the total step
     * limit is reached. See subsection 4.9.2 of the report for further details.
     *
     * @param computation   The initial computation the PDA starts with, containing just the
     *                      initial configuration
     * @param maxSteps      The step limit which this depth-first search can explore up to.
     * @param maxTotalSteps The total step limit across all computations
     */
    private void runPDAOnInputString(ArrayList<PDAConfiguration> computation, int maxSteps,
                                     int maxTotalSteps) {
        // Maintain a stack of computations for the depth-first search
        Stack<ArrayList<PDAConfiguration>> openList = new Stack<>();
        openList.push(computation);
        int totalSteps = 0;

        while (!openList.isEmpty()) {
            totalSteps++;
            ArrayList<PDAConfiguration> current = openList.pop();
            // Extract the last configuration of the computation as the current configuration
            PDAConfiguration currentConfiguration = current.get(current.size() - 1);

            // If the step limit for an individual computation has been reached, then set th
            // hitMaxSteps boolean to true and do not continue running the PDA on this particular
            // computation. Add 1 to maxSteps before comparing to ensure that computations can
            // contain the same number of configurations as the step limit. For example, if
            // maxSteps = 5, without the + 1, the longest computations would have a length of 4
            // configurations rather than 5.
            if (current.size() == maxSteps + 1) {
                hitMaxSteps = true;
            }
            // If the total step limit across all computations is reached, set the hitMaxTotalSteps
            // boolean to true and stop immediately.
            else if (totalSteps >= maxTotalSteps) {
                hitMaxTotalSteps = true;
                return;
            }
            else {
                // If the current configuration (at the end of the current computation) is an
                // accepting configuration according to the acceptance criteria, then add this
                // computation to acceptingComputations.
                if (isAcceptingConfiguration(currentConfiguration)) {
                    acceptingComputations.add(current);
                }

                // Gather all applicable transitions for the current configuration
                ArrayList<PDATransition> applicableTransitions =
                        getApplicableTransitions(currentConfiguration);

                // For each applicable transition, generate a new configuration from applying the
                // transition and a new computation that ends with the new configuration. Then,
                // add this new computation to the open list.
                for (PDATransition transition : applicableTransitions) {
                    PDAConfiguration newConfiguration = applyTransition(currentConfiguration,
                            transition);
                    ArrayList<PDAConfiguration> newComputation = new ArrayList<>(current);
                    newComputation.add(newConfiguration);
                    openList.push(newComputation);
                }
            }
        }
    }

    /**
     * Return a list of all PDATransitions that can be applied from a given configuration. This
     * takes into account the state, the current input symbol and the stack of the configuration
     * by comparing each of these things to each transition of the PDA.
     *
     * @param configuration The configuration for which we are generating the applicable transitions
     * @return The list of all applicable transitions for the given configuration
     */
    public ArrayList<PDATransition> getApplicableTransitions(PDAConfiguration configuration) {
        ArrayList<PDATransition> applicableTransitions = new ArrayList<>();

        for (PDATransition transition : transitions) {
            String currentState = configuration.getState();
            String inputSymbol = configuration.getInputSymbol();
            Stack<String> stack = configuration.getStack();
            // First check if the current state matches
            if (transition.getCurrentState().equals(currentState)) {
                // Then check if the input symbol matches - it can match either if it is empty or
                // if they are equal. If inputSymbol is null (due to the entire string being
                // consumed or the input string being the empty string), then a transition is
                // only potentially applicable if its input symbol is empty.
                if (transition.getInputSymbol().isEmpty() ||
                        transition.getInputSymbol().equals(inputSymbol)) {
                    String popString = transition.getPopString();
                    // Check if the current stack has the pop string of the transition
                    if (stackHasPopString(stack, popString)) {
                        applicableTransitions.add(transition);
                    }
                }
            }
        }

        return applicableTransitions;
    }

    /**
     * Returns the PDAConfiguration obtained by applying the given transition on the given
     * configuration.
     *
     * @param configuration The original PDAConfiguration in which the transition is applied
     * @param transition    The transition that is being applied
     * @return The new PDAConfiguration
     */
    public PDAConfiguration applyTransition(PDAConfiguration configuration,
                                            PDATransition transition) {
        String newState = transition.getNewState();
        int newIndex = configuration.getIndex();
        // Use a copy of the stack to prevent modifying the actual stack of the configuration
        Stack<String> newStack = (Stack<String>) configuration.getStack().clone();

        // If this transition consumes an input symbol from the tape, then increase the index of
        // the new configuration to point to the next input symbol
        if (!transition.getInputSymbol().isEmpty()) {
            newIndex += 1;
        }
        // If the transition pops a non-empty string from the stack, pop the string from the stack
        if (!transition.getPopString().isEmpty()) {
            for (int i = 0; i < transition.getPopString().length(); i++) {
                newStack.pop();
            }
        }
        // If the transition pushes a non-empty string to the stack, push the string to the stack
        // in reverse order
        if (!transition.getPushString().isEmpty()) {
            for (int i = transition.getPushString().length() - 1; i >= 0; i--) {
                newStack.push(String.valueOf(transition.getPushString().charAt(i)));
            }
        }

        return new PDAConfiguration(newStack, newState, newIndex);
    }

    /**
     * Checks if a configuration is an accepting configuration. This depends on the acceptance
     * criteria as well as the configuration. A configuration cannot be an accepting one if there is
     * any input remaining, regardless of the acceptance criteria. If however all the input has been
     * consumed, then if the acceptance criteria is acceptance by accepting state, this
     * configuration is only accepting if the state of the configuration is an accepting state. If
     * the acceptance criteria is empty stack, then the configuration is only accepting if the stack
     * of the configuration is empty. Finally, the both acceptance criteria is accepting only if
     * both conditions are true.
     *
     * @param configuration The configuration being checked
     * @return True if the configuration is an accepting configuration and false otherwise.
     */
    public boolean isAcceptingConfiguration(PDAConfiguration configuration) {
        // If the input string has not been exhausted, then this cannot possibly be an accepting
        // configuration
        if (configuration.getIndex() < PDAConfiguration.getInputString().length()) {
            return false;
        }

        if (acceptanceCriteria == AcceptanceCriteria.ACCEPTING_STATE) {
            if (acceptingStates.contains(configuration.getState())) {
                return true;
            }
        }
        if (acceptanceCriteria == AcceptanceCriteria.EMPTY_STACK) {
            if (configuration.getStack().isEmpty()) {
                return true;
            }
        }
        if (acceptanceCriteria == AcceptanceCriteria.BOTH) {
            return acceptingStates.contains(configuration.getState()) &&
                    configuration.getStack().isEmpty();
        }

        return false;
    }

    /**
     * Checks if a stack contains a given string (the string to be popped from the stack). If the
     * pop string is "abc" for example, then the stack contains that pop string if the top of the
     * stack is "a", the second element of the stack is "b" and the third element is "c". When "abc"
     * would have been pushed to the stack, it would have been pushed in reverse order.
     *
     * @param stack     The stack
     * @param popString The string that needs to be popped from the stack
     * @return True if and only if the stack contains the pop string
     */
    private boolean stackHasPopString(Stack<String> stack, String popString) {
        // The contents of the stack are irrelevant if the pop string is empty
        if (popString.isEmpty()) {
            return true;
        }
        // Work on a copy of the stack to ensure the original stack is not altered
        Stack<String> copy = (Stack<String>) stack.clone();
        StringBuilder stackTopString = new StringBuilder();

        // The stack cannot possibly contain the pop string if it contains fewer elements than
        // the string's length
        if (copy.size() < popString.length()) {
            return false;
        }

        // Iterate through the pop string and build up the corresponding string of stack symbols
        // at the top of the stack
        for (int i = 0; i < popString.length(); i++) {
            String top = copy.pop();
            stackTopString.append(top);
        }

        return stackTopString.toString().equals(popString);
    }

    /**
     * Generates a random computation for a given input string. It works by repeatedly applying
     * applicable transitions for the current configuration until there are no more applicable
     * transitions left. If it is possible for there to be a computation of infinite length (due to
     * a cycle of epsilon transitions), then the computation is returned when the length of the
     * computation hits 50 or sooner if there are no applicable transitions. See subsection 4.12.1
     * of the report for further details.
     *
     * @param inputString The input string to generate a random computation for.
     * @return A random computation on the given input string.
     */
    public ArrayList<PDAConfiguration> getRandomComputation(String inputString) {
        ArrayList<PDAConfiguration> randomComputation = new ArrayList<>();
        randomComputation.add(getInitialConfiguration(inputString));
        PDAConfiguration current = randomComputation.get(0);
        ArrayList<PDATransition> applicableTransitions = getApplicableTransitions(current);
        boolean isInfinite = hasInfiniteComputation();

        while (!applicableTransitions.isEmpty()) {
            Random random = new Random();
            PDATransition selectedTransition =
                    applicableTransitions.get(random.nextInt(applicableTransitions.size()));
            PDAConfiguration newConfiguration = applyTransition(current, selectedTransition);
            randomComputation.add(newConfiguration);
            current = newConfiguration;
            applicableTransitions = getApplicableTransitions(newConfiguration);
            // If this PDA can have infinite length computations, return the computation after
            // the size reaches 50
            if (isInfinite && randomComputation.size() == 50) {
                return randomComputation;
            }
        }

        return randomComputation;
    }

    /**
     * Checks if it is possible for this PDA to have computations of infinite length. These can
     * only exist if there is at least one cycle of epsilon transitions. Works by performing a
     * depth-first search from each state and seeing if any of the nodes reached in the depth-first
     * search with epsilon transitions can be  reached again.
     *
     * @return True if the PDA has at least one possible infinitely long computation.
     */
    public boolean hasInfiniteComputation() {
        ArrayList<PDATransition> epsilonTransitions = getEpsilonTransitions();

        for (String pdaState : states) {
            Stack<String> openList = new Stack<>();
            openList.push(pdaState);
            ArrayList<String> visitedStates = new ArrayList<>();

            while (!openList.isEmpty()) {
                String state = openList.pop();
                if (!visitedStates.contains(state)) {
                    visitedStates.add(state);
                }

                for (PDATransition epsilonTransition : epsilonTransitions) {
                    if (epsilonTransition.getCurrentState().equals(state)) {
                        String newState = epsilonTransition.getNewState();
                        if (visitedStates.contains(newState)) {
                            return true;
                        }
                        openList.push(newState);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gets all the epsilon transitions from the list of all transitions.
     *
     * @return The epsilon transitions in this PDA.
     */
    private ArrayList<PDATransition> getEpsilonTransitions() {
        ArrayList<PDATransition> epsilonTransitions = new ArrayList<>();

        for (PDATransition transition : transitions) {
            if (transition.getInputSymbol().isEmpty()) {
                epsilonTransitions.add(transition);
            }
        }

        return epsilonTransitions;
    }

    /**
     * Generates a string containing all the necessary information to reconstruct this PDA.
     *
     * @return A string representation of this PDA object.
     */
    @Override
    public String toString() {
        return "{states=" + states + ", transitions=" + transitions + ", initialState=" +
                initialState.get() + ", acceptingStates=" + acceptingStates +
                ", initialStackSymbol=" + initialStackSymbol +
                ", acceptanceCriteria=" + acceptanceCriteria + '}';
    }

    /**
     * Checks if this PDA is equal to the given object. Checks all the relevant pieces of
     * information (those used in the toString() method).
     *
     * @param other The other object being checked for equality.
     * @return True if the given object is an identical PDA and false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PDA pda = (PDA) other;

        return Objects.equals(initialStackSymbol, pda.initialStackSymbol) &&
                transitions.equals(pda.transitions) && states.equals(pda.states) &&
                Objects.equals(initialState.get(), pda.initialState.get()) &&
                acceptingStates.equals(pda.acceptingStates) &&
                acceptanceCriteria.equals(pda.acceptanceCriteria);
    }

    /**
     * Uses the relevant parts of this PDA to generate the hash code.
     *
     * @return The hash code for this PDA object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(initialStackSymbol, transitions, states, initialState,
                acceptingStates, acceptanceCriteria);
    }

    /**
     * Gets the initial state of this PDA from the SimpleStringProperty initialState.
     *
     * @return The initial state of this PDA.
     */
    public String getInitialState() {
        return initialState.get();
    }

    /**
     * Gets the initial stack symbol of this PDA. If initialStackSymbol is null, the PDA starts
     * with an empty stack.
     *
     * @return The initial stack symbol of this PDA.
     */
    public String getInitialStackSymbol() {
        return initialStackSymbol;
    }

    /**
     * Gets the list of accepting states of this PDA.
     *
     * @return The list of accepting states of this PDA.
     */
    public ArrayList<String> getAcceptingStates() {
        return acceptingStates;
    }

    /**
     * Gets the acceptance criteria of this PDA.
     *
     * @return The acceptance criteria of this PDA.
     */
    public AcceptanceCriteria getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    /**
     * Gets the transitions that cause nondeterminism.
     *
     * @return The set of transitions that cause nondeterminism.
     */
    public HashSet<PDATransition> getNondeterministicTransitions() {
        return nondeterministicTransitions;
    }
}
