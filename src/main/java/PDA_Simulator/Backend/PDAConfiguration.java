package PDA_Simulator.Backend;

import java.util.Objects;
import java.util.Stack;

/**
 * This class represents a configuration of a PDA. It contains the current state, the current stack,
 * the full input string the PDA is being run on and an index into the input string to indicate the
 * current input symbol of the configuration.
 *
 * @author Savraj Bassi
 */
public class PDAConfiguration {
    // The stack of this PDA configuration
    private final Stack<String> stack;
    // The current state of this PDA configuration
    private final String state;
    // The index of the current input symbol of this PDA configuration
    private final int index;
    // Static variable holding the inputString so that all PDAConfiguration objects can share the
    // same string
    private static String inputString;

    /**
     * Creates a PDAConfiguration given the stack, state and index.
     *
     * @param stack The stack for this configuration.
     * @param state The state for this configuration.
     * @param index The index for this configuration.
     */
    public PDAConfiguration(Stack<String> stack, String state, int index) {
        this.stack = stack;
        this.state = state;
        this.index = index;
    }

    /**
     * Gets the stack of this configuration.
     *
     * @return The stack of this configuration.
     */
    public Stack<String> getStack() {
        return stack;
    }

    /**
     * Gets the state of this configuration.
     *
     * @return The state of this configuration.
     */
    public String getState() {
        return state;
    }

    /**
     * Gets the index of this configuration.
     *
     * @return The index of this configuration.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the current input symbol of this configuration by using the index and the static
     * inputString field.
     *
     * @return The current input symbol of the input string or null if the string has been
     * exhausted.
     */
    public String getInputSymbol() {
        if (index < inputString.length()) {
            return String.valueOf(inputString.charAt(index));
        }
        // If the index is not less than the length of the input string, the entire input string
        // has been consumed (and so there is no current input symbol) or the input string is the
        // empty string, so return null.
        else {
            return null;
        }
    }

    /**
     * Sets the static inputString field so all PDAConfigurations can use it.
     *
     * @param inputString The input string the PDA is to be run on.
     */
    public static void setInputString(String inputString) {
        PDAConfiguration.inputString = inputString;
    }

    /**
     * Gets the input string the PDA is to be run on.
     *
     * @return The input string the PDA is to be run on.
     */
    public static String getInputString() {
        return inputString;
    }

    /**
     * Checks if this PDAConfiguration is equal to another object.
     *
     * @param other The other object being checked for equality.
     * @return True if the other object is an equivalent PDAConfiguration and false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        PDAConfiguration pdaConfiguration = (PDAConfiguration) other;
        return index == pdaConfiguration.index && Objects.equals(stack, pdaConfiguration.stack) &&
                Objects.equals(state, pdaConfiguration.state);
    }

    /**
     * Uses the relevant parts of this PDAConfiguration to generate the hash code.
     *
     * @return The hash code for this PDAConfiguration object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(stack, state, index);
    }
}
