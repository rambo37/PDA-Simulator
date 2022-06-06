package PDA_Simulator;

import PDA_Simulator.Backend.PDATransition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PDATransitionTest {

    /**
     * Tests for the second constructor which takes a single string.
     */
    @Test
    void secondConstructor() {
        // Complete PDATransition works (no empty strings anywhere)
        PDATransition transition = new PDATransition("{(q0,a,A) -> (#,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "a");
        assertEquals(transition.getPopString(), "A");
        assertEquals(transition.getPushString(), "#");
        assertEquals(transition.getNewState(), "q1");

        // Empty string for just inputSymbol works
        transition = new PDATransition("{(q0,,A) -> (#,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "A");
        assertEquals(transition.getPushString(), "#");
        assertEquals(transition.getNewState(), "q1");

        // Empty string for just popString works
        transition = new PDATransition("{(q0,a,) -> (#,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "a");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "#");
        assertEquals(transition.getNewState(), "q1");

        // Empty strings for inputSymbol and popString works
        transition = new PDATransition("{(q0,,) -> (#,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "#");
        assertEquals(transition.getNewState(), "q1");

        // Empty string for just pushString works
        transition = new PDATransition("{(q0,a,A) -> (,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "a");
        assertEquals(transition.getPopString(), "A");
        assertEquals(transition.getPushString(), "");
        assertEquals(transition.getNewState(), "q1");

        // Empty strings for inputSymbol, popString and pushString works
        transition = new PDATransition("{(q0,,) -> (,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "");
        assertEquals(transition.getNewState(), "q1");

        // Single character state names work
        transition = new PDATransition("{(q,,) -> (,q)}");
        assertEquals(transition.getCurrentState(), "q");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "");
        assertEquals(transition.getNewState(), "q");

        // More than 2 character state names work
        transition = new PDATransition("{(q00001,,) -> (,q00002)}");
        assertEquals(transition.getCurrentState(), "q00001");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "");
        assertEquals(transition.getNewState(), "q00002");

        // Multiple character pop string works
        transition = new PDATransition("{(q0,,abc) -> (,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "abc");
        assertEquals(transition.getPushString(), "");
        assertEquals(transition.getNewState(), "q1");

        // Multiple character push string works
        transition = new PDATransition("{(q0,,) -> (abc,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "");
        assertEquals(transition.getPushString(), "abc");
        assertEquals(transition.getNewState(), "q1");

        // Multiple character pop and push strings work together
        transition = new PDATransition("{(q0,,abc) -> (abc,q1)}");
        assertEquals(transition.getCurrentState(), "q0");
        assertEquals(transition.getInputSymbol(), "");
        assertEquals(transition.getPopString(), "abc");
        assertEquals(transition.getPushString(), "abc");
        assertEquals(transition.getNewState(), "q1");
    }

    /**
     * Tests for the equals method of the PDATransition class.
     */
    @Test
    void pdaTransitionEquals() {
        PDATransition transition = new PDATransition("{(q0,a,A) -> (#,q1)}");
        PDATransition transition0 = new PDATransition("q0", "a", "A", "#", "q1");
        PDATransition transition1 = new PDATransition("q0", "a", "A", "#", "q1");
        PDATransition transition2 = new PDATransition("q1", "a", "A", "#", "q1");
        PDATransition transition3 = new PDATransition("q0", "", "A", "#", "q1");
        PDATransition transition4 = new PDATransition("q0", "a", "", "#", "q1");
        PDATransition transition5 = new PDATransition("q0", "a", "A", "", "q1");
        PDATransition transition6 = new PDATransition("q0", "a", "A", "#", "q0");

        assertEquals(transition, transition);
        // transition and transition1 are identical even though they used different constructors
        assertEquals(transition, transition0);
        assertEquals(transition, transition1);
        assertEquals(transition0, transition1);
        // Neither transition nor transition1 are equal to transition2 which differs just by the
        // currentState
        assertNotEquals(transition, transition2);
        assertNotEquals(transition1, transition2);

        // None of the transitions are equal to transition3 which differs just by the inputSymbol
        assertNotEquals(transition, transition3);
        assertNotEquals(transition1, transition3);
        assertNotEquals(transition2, transition3);

        // None of the transitions are equal to transition4 which differs just by the popString
        assertNotEquals(transition, transition4);
        assertNotEquals(transition1, transition4);
        assertNotEquals(transition2, transition4);
        assertNotEquals(transition3, transition4);

        // None of the transitions are equal to transition5 which differs just by the pushString
        assertNotEquals(transition, transition5);
        assertNotEquals(transition1, transition5);
        assertNotEquals(transition2, transition5);
        assertNotEquals(transition3, transition5);
        assertNotEquals(transition4, transition5);

        // None of the transitions are equal to transition6 which differs just by the newState
        assertNotEquals(transition, transition6);
        assertNotEquals(transition1, transition6);
        assertNotEquals(transition2, transition6);
        assertNotEquals(transition3, transition6);
        assertNotEquals(transition4, transition6);
        assertNotEquals(transition5, transition6);

        assertFalse(transition.equals(null));
    }

    /**
     * Tests for the toString() method of the PDATransition class.
     */
    @Test
    void pdaTransitionToString() {
        // Complete PDATransition works (no empty strings anywhere)
        PDATransition transition = new PDATransition("q0", "a", "A", "#", "q1");
        String transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,a,A) -> (#,q1)}");

        // Empty string for just inputSymbol works
        transition = new PDATransition("q0", "", "A", "#", "q1");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,,A) -> (#,q1)}");

        // Empty string for just popString works
        transition = new PDATransition("q0", "a", "", "#", "q1");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,a,) -> (#,q1)}");

        // Empty string for just pushString works
        transition = new PDATransition("q0", "a", "A", "", "q1");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,a,A) -> (,q1)}");

        // Empty string for both inputSymbol and popString works
        transition = new PDATransition("q0", "", "", "#", "q1");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,,) -> (#,q1)}");

        // Empty string for all three of inputSymbol, popString and pushString works
        transition = new PDATransition("q0", "", "", "", "q1");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q0,,) -> (,q1)}");

        // Single character states works
        transition = new PDATransition("q", "", "", "", "q");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q,,) -> (,q)}");

        // More than 2 character state names works
        transition = new PDATransition("q00001", "", "", "", "q00002");
        transitionString = transition.toString();
        assertEquals(transitionString, "{(q00001,,) -> (,q00002)}");
    }
}
