package PDA_Simulator.Backend;

import java.util.Objects;

/**
 * This class represents the transitions of a PDA. Each transition needs to store 5 pieces of
 * information - the current state, the current input symbol, the pop string, the push string and
 * the new state. Empty strings are used to denote epsilon (for inputSymbol, popString and
 * pushString).
 *
 * @author Savraj Bassi
 */

public class PDATransition {
    private final String currentState;
    private final String inputSymbol;
    private final String popString;
    private final String pushString;
    private final String newState;

    /**
     * Creates a PDATransition given the values of the five fields.
     *
     * @param currentState The current state of the PDATransition
     * @param inputSymbol  The input symbol of the PDATransition
     * @param popString    The pop string of the PDATransition
     * @param pushString   The push string of the PDATransition
     * @param newState     The new state of the PDATransition
     */
    public PDATransition(String currentState, String inputSymbol, String popString,
                         String pushString, String newState) {
        this.currentState = currentState;
        this.inputSymbol = inputSymbol;
        this.popString = popString;
        this.pushString = pushString;
        this.newState = newState;
    }

    /**
     * A secondary constructor to create a PDATransition from a single string.
     *
     * @param transitionString The string specifying the PDATransition.
     */
    public PDATransition(String transitionString) {
        // Remove the start and end parentheses
        StringBuilder transition = new StringBuilder(transitionString);
        transition.replace(0, 2, "");
        int endIndex = transition.length();
        transition.replace(endIndex - 2, endIndex, "");

        // Everything up to the first comma is part of the currentState
        int firstCommaIndex = transition.indexOf(",");
        currentState = transition.substring(0, firstCommaIndex);
        // Remove the current state part of the string and the comma.
        transition.replace(0, firstCommaIndex + 1, "");

        // Everything up to the second comma is part of the input symbol.
        int secondCommaIndex = transition.indexOf(",");
        inputSymbol = transition.substring(0, secondCommaIndex);
        // Remove the input symbol part of the string and the comma.
        transition.replace(0, secondCommaIndex + 1, "");

        // Everything up to the first closing parenthesis is part of the pop string.
        int firstClosingParenthesis = transition.indexOf(")");
        popString = transition.substring(0, firstClosingParenthesis);
        // Remove the pop string of the string and the ") -> (" part of the transition string.
        transition.replace(0, firstClosingParenthesis + 6, "");

        // Everything up to the last comma is part of the push string.
        int lastCommaIndex = transition.indexOf(",");
        pushString = transition.substring(0, lastCommaIndex);
        // Remove the push string part of the string and the comma.
        transition.replace(0, lastCommaIndex + 1, "");

        // What is left is the new state part of the string.
        newState = transition.toString();
    }

    /**
     * Gets the current state of this PDATransition.
     *
     * @return The current state of this PDATransition.
     */
    public String getCurrentState() {
        return currentState;
    }

    /**
     * Gets the current state of this PDATransition.
     *
     * @return The current state of this PDATransition.
     */
    public String getInputSymbol() {
        return inputSymbol;
    }

    /**
     * Gets the current state of this PDATransition.
     *
     * @return The current state of this PDATransition.
     */
    public String getPopString() {
        return popString;
    }

    /**
     * Gets the current state of this PDATransition.
     *
     * @return The current state of this PDATransition.
     */
    public String getPushString() {
        return pushString;
    }

    /**
     * Gets the current state of this PDATransition.
     *
     * @return The current state of this PDATransition.
     */
    public String getNewState() {
        return newState;
    }

    /**
     * Generates a string representing the PDATransition. An example of a string is:
     * "{(q0,,A) -> (,q1)}".
     *
     * @return A string representation of the PDATransition.
     */
    @Override
    public String toString() {
        return "{(" + currentState + "," + inputSymbol + "," + popString + ") -> (" +
                pushString + "," + newState + ")}";
    }

    /**
     * Checks if this PDATransition is equal to another object. If the other object is a
     * PDATransition, then this requires the other PDATransition to have the exact same values for
     * the five fields.
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

        PDATransition otherTransition = (PDATransition) other;
        return currentState.equals(otherTransition.currentState) &&
                inputSymbol.equals(otherTransition.inputSymbol) &&
                popString.equals(otherTransition.popString) &&
                pushString.equals(otherTransition.pushString) &&
                newState.equals(otherTransition.newState);
    }

    /**
     * Uses the relevant parts of this PDATransition to generate the hash code.
     *
     * @return The hash code for this PDATransition object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(currentState, inputSymbol, popString, pushString, newState);
    }
}
