package PDA_Simulator;

import PDA_Simulator.Backend.PDAConfiguration;

import java.util.Stack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PDAConfigurationTest {

    /**
     * Resets the inputString of the PDAConfiguration class before each test.
     */
    @BeforeEach
    void setUp() {
        PDAConfiguration.setInputString(null);
    }

    /**
     * Test that setting the input string and then getting the input string works.
     */
    @Test
    void getAndSetInputString() {
        assertNull(PDAConfiguration.getInputString());
        PDAConfiguration.setInputString("");
        assertEquals(PDAConfiguration.getInputString(), "");
        PDAConfiguration.setInputString("aabb");
        assertEquals(PDAConfiguration.getInputString(), "aabb");
    }

    /**
     * Tests for getting the input symbol of a PDAConfiguration with different input strings and
     * indices.
     */
    @Test
    void getInputSymbol() {
        // When the input string is empty, the return value is null
        PDAConfiguration.setInputString("");
        PDAConfiguration configuration = new PDAConfiguration(new Stack<>(), "q0", 0);
        assertNull(configuration.getInputSymbol());

        // For a non-empty string and an index within the input string, the correct input symbol
        // is returned
        PDAConfiguration.setInputString("a");
        assertEquals(configuration.getInputSymbol(), "a");

        // When the index is past the end of the input string, null is returned
        configuration = new PDAConfiguration(new Stack<>(), "q0", 1);
        assertNull(configuration.getInputSymbol());

        // The behaviour is the same for input strings with length greater than 1
        PDAConfiguration.setInputString("aabb");
        configuration = new PDAConfiguration(new Stack<>(), "q0", 0);
        assertEquals(configuration.getInputSymbol(), "a");
        configuration = new PDAConfiguration(new Stack<>(), "q0", 1);
        assertEquals(configuration.getInputSymbol(), "a");
        configuration = new PDAConfiguration(new Stack<>(), "q0", 2);
        assertEquals(configuration.getInputSymbol(), "b");
        configuration = new PDAConfiguration(new Stack<>(), "q0", 3);
        assertEquals(configuration.getInputSymbol(), "b");
        configuration = new PDAConfiguration(new Stack<>(), "q0", 4);
        assertNull(configuration.getInputSymbol());
    }

    /**
     * Tests for PDAConfiguration equality.
     */
    @Test
    void PDAConfigurationEquals() {
        Stack<String> stack = new Stack<>();
        stack.push("#");
        PDAConfiguration configuration = new PDAConfiguration(new Stack<>(), "q0", 0);
        // Different stack
        PDAConfiguration configuration1 = new PDAConfiguration(stack, "q0", 0);
        // Different state
        PDAConfiguration configuration2 = new PDAConfiguration(new Stack<>(), "q1", 0);
        // Different index
        PDAConfiguration configuration3 = new PDAConfiguration(new Stack<>(), "q0", 1);

        // None of the above PDAConfigurations are equal
        assertEquals(configuration, configuration);
        assertNotEquals(configuration, configuration1);
        assertNotEquals(configuration, configuration2);
        assertNotEquals(configuration, configuration3);
        assertNotEquals(configuration1, configuration2);
        assertNotEquals(configuration1, configuration3);
        assertNotEquals(configuration2, configuration3);

        stack.pop();
        PDAConfiguration configuration4 = new PDAConfiguration(stack, "q0", 0);
        assertEquals(configuration, configuration4);

        // Two PDAConfigurations with different stacks with the same elements are considered
        // equal configurations as long as the states and indices match
        stack.push("#");
        stack.push("A");
        stack.push("B");

        Stack<String> stack1 = (Stack<String>) stack.clone();

        PDAConfiguration configuration5 = new PDAConfiguration(stack, "q0", 0);
        PDAConfiguration configuration6 = new PDAConfiguration(stack1, "q0", 0);

        assertEquals(configuration5, configuration6);

        assertFalse(configuration.equals(null));
    }
}
