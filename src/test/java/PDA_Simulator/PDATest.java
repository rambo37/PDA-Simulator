package PDA_Simulator;

import PDA_Simulator.Backend.AcceptanceCriteria;
import PDA_Simulator.Backend.PDA;
import PDA_Simulator.Backend.PDAConfiguration;
import PDA_Simulator.Backend.PDATransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Stack;

import static org.junit.jupiter.api.Assertions.*;

class PDATest {
    private PDA pda;
    PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");

    /**
     * Modifies the pda variable to a new PDA with two states and a single transition.
     */
    @BeforeEach
    void setUp() {
        pda = new PDA();
        // The first addState creates the initial state, q0
        pda.addState();
        // This creates another state, q1
        pda.addState();
        pda.addTransition(t1);
    }

    /**
     * Test newly created empty PDA
     */
    @Test
    void initialisation() {
        PDA pda2 = new PDA();
        assertNull(pda2.getInitialState());
        assertEquals(pda2.getStates().size(), 0);
        assertTrue(pda2.getDeterministic());
        assertNull(pda2.getInitialStackSymbol());
        assertEquals(pda2.getAcceptingStates().size(), 0);
        assertEquals(pda2.getTransitions().size(), 0);
        assertEquals(pda2.getAcceptanceCriteria(), AcceptanceCriteria.ACCEPTING_STATE);
    }

    /**
     * Tests for changing the initial state of the PDA.
     */
    @Test
    void changeInitialState() {
        assertEquals(pda.getInitialState(), "q0");
        pda.changeInitialState("q1");
        assertEquals(pda.getInitialState(), "q1");
        pda.changeInitialState("q0");
        assertEquals(pda.getInitialState(), "q0");
    }

    /**
     * Tests for the addState() method that takes no arguments
     */
    @Test
    void addState() {
        // Size should be 2 after invoking addState once (this is done in setUp()) and new state
        // should be named "q1"
        assertEquals(pda.getStates().size(), 2);
        assertEquals(pda.getStates().get(1), "q1");

        // Creating a new state after deleting all states results in the newly created state
        // becoming the initial state
        pda.deleteState("q0");
        pda.deleteState("q1");
        pda.addState();
        assertEquals(pda.getInitialState(), "q2");

        // Adding a new state after renaming an existing state to the name that would be given to
        // the new state does not result in two states with the same name
        pda.renameState("q2", "q3");
        pda.addState();
        assertEquals(pda.getStates().get(0), "q3");
        assertEquals(pda.getStates().get(1), "q4");

        // Invoking addState() in a PDA without an initial state creates an initial state called q0
        PDA pda1 = new PDA();
        pda1.addState();
        assertEquals(pda1.getStates().get(0), "q0");
        assertEquals(pda1.getInitialState(), "q0");
    }

    /**
     * Tests for deleting a PDA state
     */
    @Test
    void deleteState() {
        // Deleting a state involved in a transition deletes the transition as well as the state
        assertEquals(pda.getTransitions().size(), 1);
        pda.deleteState("q1");
        assertEquals(pda.getStates().size(), 1);
        assertEquals(pda.getTransitions().size(), 0);

        pda.addState();
        PDATransition t2 = new PDATransition("q2", "a", "", "A", "q2");
        pda.addTransition(t2);
        assertEquals(pda.getTransitions().size(), 1);
        // Deleting the initial state makes initialState null
        pda.deleteState("q0");
        assertNull(pda.getInitialState());
        // Deleting a state that is not involved in a transition (q0) does not delete the transition
        assertEquals(pda.getTransitions().size(), 1);
        assertEquals(pda.getStates().size(), 1);
    }

    /**
     * Tests for renaming a state.
     */
    @Test
    void renameState() {
        // Cannot rename to an existing name, so no change to state q0 and transition remains
        // unmodified
        assertEquals(pda.getTransitions().get(0).getCurrentState(), "q0");
        assertFalse(pda.renameState("q0", "q1"));
        assertEquals(pda.getStates().get(0), "q0");
        assertEquals(pda.getTransitions().get(0).getCurrentState(), "q0");

        // Renaming to the same name does not change the state and returns true
        assertTrue(pda.renameState("q0", "q0"));
        assertEquals(pda.getStates().get(0), "q0");

        // Rename to a new name should work, so q0 becomes q5 and the transition should be
        // updated accordingly
        assertEquals(pda.getTransitions().get(0).getCurrentState(), "q0");
        assertTrue(pda.renameState("q0", "q5"));
        assertEquals(pda.getStates().get(0), "q5");
        assertEquals(pda.getTransitions().get(0).getCurrentState(), "q5");

        // Renaming initial state q0 changes initialState
        assertEquals(pda.getInitialState(), "q5");

        // q1 is now an initial state. Renaming it should also change the state in the set of
        // accepting states
        pda.changeAcceptingState("q1");
        assertEquals(pda.getTransitions().get(0).getNewState(), "q1");
        assertEquals(pda.getAcceptingStates().get(0), "q1");
        assertTrue(pda.renameState("q1", "q6"));
        assertEquals(pda.getAcceptingStates().get(0), "q6");
        assertEquals(pda.getTransitions().get(0).getNewState(), "q6");
    }

    /**
     * Tests for changing the initial stack symbol of the PDA.
     */
    @Test
    void setInitialStackSymbol() {
        // Stack symbol is initially not set (null)
        assertNull(pda.getInitialStackSymbol());
        pda.setInitialStackSymbol("a");
        assertEquals(pda.getInitialStackSymbol(), "a");
        pda.setInitialStackSymbol(null);
        assertNull(pda.getInitialStackSymbol());
    }

    /**
     * Tests for changing whether a PDA state is accepting or not.
     */
    @Test
    void changeAcceptingState() {
        // Initially, there are no accepting states
        assertEquals(pda.getAcceptingStates().size(), 0);
        pda.changeAcceptingState("q0");
        // Invoking changeAcceptingState with "q0" makes "q0" an accepting state
        assertEquals(pda.getAcceptingStates().size(), 1);
        assertEquals(pda.getAcceptingStates().get(0), "q0");
        // There can be more than 1 accepting state
        pda.changeAcceptingState("q1");
        assertEquals(pda.getAcceptingStates().size(), 2);
        assertEquals(pda.getAcceptingStates().get(1), "q1");

        // Invoking this method on an accepting state makes it no longer an accepting state
        pda.changeAcceptingState("q0");
        assertEquals(pda.getAcceptingStates().size(), 1);
        assertFalse(pda.getAcceptingStates().contains("q0"));
    }

    /**
     * Adding an existing transition returns false and does not change size of transitions
     */
    @Test
    void addTransition() {
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        assertFalse(pda.addTransition(t1));
        assertEquals(pda.getTransitions().size(), 1);
    }

    /**
     * Adding a transition that causes nondeterminism not only increases the size of the set of
     * transitions, but also causes the PDA to update the nondeterministic field so that
     * getDeterministic returns false.
     */
    @Test
    void addTransition2() {
        // PDA is deterministic to begin with
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 1);
        // Adding a new transition returns true and increments size of transitions by 1
        PDATransition t2 = new PDATransition("q0", "a", "", "A", "q0");
        assertTrue(pda.addTransition(t2));
        assertEquals(pda.getTransitions().size(), 2);
        // Adding the new transition makes the PDA nondeterministic as there is more than 1
        // transition defined for the same (current state, input symbol, pop string) triple.
        assertFalse(pda.getDeterministic());
    }

    /**
     * Tests for adding various transitions and observing whether the determinism changes correctly.
     */
    @Test
    void addTransition3() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t2 = new PDATransition("q0", "", "B", "A", "q0");
        pda.addTransition(t2);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,,B) -> (A,q0)}]
        // Adding this epsilon transition makes the PDA nondeterministic
        assertFalse(pda.getDeterministic());
        pda.deleteTransition(t2);
        // transitions: [{(q0,,) -> (A,q1)}]

        // PDA is deterministic again once the epsilon transition is removed
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Adding this epsilon transition also makes the PDA nondeterministic
        PDATransition t3 = new PDATransition("q0", "", "", "A", "q1");
        pda.addTransition(t3);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,,) -> (A,q1)}]
        assertFalse(pda.getDeterministic());

        // PDA is deterministic if t3 is the only transition it has from state q0
        pda.deleteTransition(pda.getTransitions().get(0));
        // transitions: [{(q0,,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Adding this transition does not cause nondeterminism since it is the only transition
        // from state q1
        PDATransition t4 = new PDATransition("q1", "", "", "A", "q1");
        pda.addTransition(t4);
        // transitions: [{(q0,,) -> (A,q1)}, {(q1,,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());

        // PDA is no longer deterministic since it has a full epsilon transition and another
        // transition from state q0. Full epsilon transition meaning that both the input symbol and
        // pop string are epsilon. This makes the transition always applicable, irrespective of the
        // input symbol and the current state of the stack. Therefore, having any transition along
        // with a full epsilon transition means the PDA will be nondeterministic.
        PDATransition t5 = new PDATransition("q0", "A", "B", "A", "q1");
        pda.addTransition(t5);
        // transitions: [{(q0,,) -> (A,q1)}, {(q1,,) -> (A,q1)}, {(q0,A,B) -> (A,q1)}]
        assertFalse(pda.getDeterministic());

        pda.getTransitions().clear();
        assertEquals(pda.getTransitions().size(), 0);
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Two transitions with the same current state, input symbol and pop string but a
        // different push string makes the PDA nondeterministic
        PDATransition t6 = new PDATransition("q0", "a", "#", "A", "q1");
        PDATransition t7 = new PDATransition("q0", "a", "#", "B", "q1");

        pda.addTransition(t6);
        pda.addTransition(t7);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#) -> (B,q1)}]
        assertFalse(pda.getDeterministic());

        pda.deleteTransition(t7);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Two transitions with the same current state, input symbol and pop string but a
        // different new state makes the PDA nondeterministic
        PDATransition t8 = new PDATransition("q0", "a", "#", "A", "q0");
        pda.addTransition(t8);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#) -> (A,q0)}]
        assertFalse(pda.getDeterministic());

        pda.deleteTransition(t8);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Adding this transition causes nondeterminism since if you are in state q0, the input
        // symbol is 'a' and there is a '#' on the top of the stack, you can apply either transition
        PDATransition t9 = new PDATransition("q0", "a", "", "A", "q0");
        pda.addTransition(t9);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,) -> (A,q0)}]
        assertFalse(pda.getDeterministic());

        pda.deleteTransition(t9);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Adding this transition causes nondeterminism since if you are in state q0, the input
        // symbol is 'a', the top of the stack is '#' and the element beneath that is 'A', you can
        // apply either transition
        PDATransition t10 = new PDATransition("q0", "a", "#A", "A", "q0");
        pda.addTransition(t10);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        pda.deleteTransition(t10);
        assertTrue(pda.getDeterministic());
        // ----------------------------------------------------------------------------------------
        // Same as the previous case but with epsilon as the input symbol for the transition with
        // the multiple character pop string
        PDATransition t11 = new PDATransition("q0", "", "#A", "A", "q0");
        pda.addTransition(t11);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        pda.getTransitions().clear();
        // ----------------------------------------------------------------------------------------
        // Same as the previous case but with epsilon as the input symbol for the transition with
        // the single character pop string
        PDATransition t12 = new PDATransition("q0", "", "#", "A", "q1");
        PDATransition t13 = new PDATransition("q0", "a", "#A", "A", "q0");
        pda.addTransition(t12);
        pda.addTransition(t13);
        // transitions: [{(q0,,#) -> (A,q1)}, {(q0,a,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
    }

    /**
     * Adding transitions that do not cause the PDA to become nondeterministic do not change the
     * nondeterministic field of PDA.
     */
    @Test
    void addTransition4() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t2 = new PDATransition("q0", "b", "A", "A", "q0");
        pda.addTransition(t2);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,A) -> (A,q0)}]
        // Adding this transition does not make the PDA nondeterministic
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 2);

        PDATransition t3 = new PDATransition("q0", "b", "#", "A", "q0");
        pda.addTransition(t3);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,A) -> (A,q0)}, {(q0,b,#) -> (A,q0)}]
        // Adding this transition does not make the PDA nondeterministic
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 3);
    }

    /**
     * Adding a transition that contains a state which does not exist results in that state being
     * added
     */
    @Test
    void addTransition5() {
        // states: [q0, q1]
        assertEquals(pda.getStates().size(), 2);
        assertEquals(pda.getTransitions().size(), 1);
        // Adding a transition which specifies a newState that does not exist results in that
        // state being created
        PDATransition t2 = new PDATransition("q0", "a", "", "A", "q5");
        assertTrue(pda.addTransition(t2));
        assertEquals(pda.getTransitions().size(), 2);
        assertEquals(pda.getStates().size(), 3);
        assertTrue(pda.getStates().contains("q5"));
        // states: [q0, q1, q5]

        // Adding a transition which specifies a currentState that does not exist results in that
        // state being created
        PDATransition t3 = new PDATransition("q2", "a", "", "A", "q1");
        assertTrue(pda.addTransition(t3));
        assertEquals(pda.getTransitions().size(), 3);
        assertEquals(pda.getStates().size(), 4);
        assertTrue(pda.getStates().contains("q2"));
        // states: [q0, q1, q5, q2]

        // Adding a transition which specifies both a currentState and a newState that do not
        // exist results in both of the states being created
        PDATransition t4 = new PDATransition("q3", "a", "", "A", "q4");
        assertTrue(pda.addTransition(t4));
        assertEquals(pda.getTransitions().size(), 4);
        assertEquals(pda.getStates().size(), 6);
        assertTrue(pda.getStates().contains("q3"));
        assertTrue(pda.getStates().contains("q4"));
        // states: [q0, q1, q5, q2, q3, q4]
    }


    /**
     * Deleting a transition that does not cause nondeterminism decreases the size of transitions
     * by 1 and does not modify the determinism of the PDA.
     */
    @Test
    void deleteTransition() {
        // PDA is deterministic to begin with
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        pda.deleteTransition(t1);
        assertEquals(pda.getTransitions().size(), 0);
        // PDA is still deterministic
        assertTrue(pda.getDeterministic());
    }

    /**
     * Deleting a transition that causes nondeterminism decreases the size of transitions by 1
     * and makes the PDA no longer nondeterministic.
     */
    @Test
    void deleteTransition2() {
        // PDA is deterministic to begin with
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t2 = new PDATransition("q0", "a", "", "A", "q0");
        assertTrue(pda.addTransition(t2));
        assertEquals(pda.getTransitions().size(), 2);
        // PDA is no longer deterministic after this transition is added
        assertFalse(pda.getDeterministic());

        pda.deleteTransition(t2);
        assertEquals(pda.getTransitions().size(), 1);
        // PDA is deterministic again once the transition causing nondeterminism is deleted
        assertTrue(pda.getDeterministic());
    }

    /**
     * Deleting an epsilon transition that adds nondeterminism decreases the size of transitions
     * by 1 and makes the PDA no longer nondeterministic.
     */
    @Test
    void deleteTransition3() {
        // PDA is deterministic to begin with
        assertTrue(pda.getDeterministic());
        assertEquals(pda.getTransitions().size(), 1);
        PDATransition t2 = new PDATransition("q0", "", "", "A", "q1");
        pda.addTransition(t2);
        assertEquals(pda.getTransitions().size(), 2);
        // Adding this epsilon transition makes the PDA nondeterministic
        assertFalse(pda.getDeterministic());

        pda.deleteTransition(t2);
        assertEquals(pda.getTransitions().size(), 1);
        // PDA is deterministic again once the transition causing nondeterminism is deleted
        assertTrue(pda.getDeterministic());
    }

    /**
     * Basic test for editing a transition.
     */
    @Test
    void editTransition() {
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        PDATransition t2 = new PDATransition("q1", "b", "A", "B", "q0");
        assertEquals(pda.getTransitions().size(), 1);
        assertEquals(pda.getTransitions().get(0), t1);
        assertNotEquals(pda.getTransitions().get(0), t2);

        assertTrue(pda.editTransition(t1, t2));
        assertEquals(pda.getTransitions().size(), 1);
        assertEquals(pda.getTransitions().get(0), t2);
        assertNotEquals(pda.getTransitions().get(0), t1);

        // Editing a transition into itself returns true
        assertTrue(pda.editTransition(t2, t2));
    }

    /**
     * Editing a transition into an existing transition does not change anything and returns false.
     */
    @Test
    void editTransition2() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        // PDA already contains t1
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        PDATransition t2 = new PDATransition("q0", "b", "", "B", "q1");
        pda.addTransition(t2);
        assertEquals(pda.getTransitions().size(), 2);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,) -> (B,q1)}]
        assertEquals(pda.getTransitions().get(0), t1);
        assertEquals(pda.getTransitions().get(1), t2);

        // Editing a transition into an existing transition does nothing
        assertFalse(pda.editTransition(t1, t2));
        assertEquals(pda.getTransitions().get(0), t1);
        assertEquals(pda.getTransitions().get(1), t2);
        assertEquals(pda.getTransitions().size(), 2);
    }

    /**
     * Editing transitions updates determinism correctly.
     */
    @Test
    void editTransition3() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        // PDA already contains t1
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        PDATransition t2 = new PDATransition("q0", "b", "", "A", "q1");
        PDATransition t3 = new PDATransition("q0", "b", "", "B", "q1");
        pda.addTransition(t3);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,) -> (B,q1)}]
        // PDA is deterministic to begin with
        assertTrue(pda.getDeterministic());

        // Editing t1 into t2 so that it reads the same input symbol as t3 makes the PDA
        // nondeterministic.
        assertTrue(pda.editTransition(t1, t2));
        // transitions: [{(q0,b,) -> (A,q1)}, {(q0,b,) -> (B,q1)}]
        assertFalse(pda.getDeterministic());

        // Changing the transition back reverts the nondeterminism
        assertTrue(pda.editTransition(t2, t1));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,) -> (B,q1)}]
        assertTrue(pda.getDeterministic());
    }

    /**
     * Editing a transition to include states that do not exist adds the states
     */
    @Test
    void editTransition4() {
        // PDA already contains t1
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        PDATransition t2 = new PDATransition("q3", "a", "", "A", "q4");
        assertEquals(pda.getStates().size(), 2);
        assertTrue(pda.editTransition(t1, t2));
        assertEquals(pda.getStates().size(), 4);
    }

    /**
     * Tests for changing the acceptance criteria of the PDA. The default is ACCEPTING_STATE.
     */
    @Test
    void changeAcceptanceCriteria() {
        assertEquals(pda.getAcceptanceCriteria(), AcceptanceCriteria.ACCEPTING_STATE);

        pda.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        assertEquals(pda.getAcceptanceCriteria(), AcceptanceCriteria.EMPTY_STACK);

        pda.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
        assertEquals(pda.getAcceptanceCriteria(), AcceptanceCriteria.BOTH);
    }

    /**
     * Test that a listener can be added that fires whenever the deterministic Property of the
     * PDA changes.
     */
    @Test
    void addDeterministicPropertyListener() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        // Initial stack symbol is null to begin with
        assertNull(pda.getInitialStackSymbol());
        // This listener will change the initial stack symbol of the pda to "#" if it is null and
        // if it is not null, it will set it back to null
        pda.addDeterministicPropertyListener((observableValue, oldValue, newValue) -> {
            if (pda.getInitialStackSymbol() == null) {
                pda.setInitialStackSymbol("#");
            } else {
                pda.setInitialStackSymbol(null);
            }
        });

        PDATransition t1 = new PDATransition("q0", "a", "", "", "q1");
        pda.addTransition(t1);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,a,) -> (,q1)}]
        // Adding this transition makes the PDA nondeterministic
        assertFalse(pda.getDeterministic());
        // The initial stack symbol of the PDA was changed which means the listener fired, as
        // required
        assertEquals(pda.getInitialStackSymbol(), "#");

        // Deleting the transition makes the PDA deterministic again
        pda.deleteTransition(t1);
        assertTrue(pda.getDeterministic());
        // Initial stack symbol is null again which means the listener fired again
        assertNull(pda.getInitialStackSymbol());
    }

    /**
     * Test that a listener can be added that fires whenever the initial state Property of the
     * PDA changes.
     */
    @Test
    void addInitialStatePropertyListener() {
        assertEquals(pda.getInitialState(), "q0");
        // Initial stack symbol is null to begin with
        assertNull(pda.getInitialStackSymbol());
        // This listener will change the initial stack symbol of the pda to "#" if it is null and
        // if it is not null, it will set it back to null
        pda.addInitialStatePropertyListener((observableValue, oldValue, newValue) -> {
            if (pda.getInitialStackSymbol() == null) {
                pda.setInitialStackSymbol("#");
            } else {
                pda.setInitialStackSymbol(null);
            }
        });

        pda.changeInitialState("q1");
        assertEquals(pda.getInitialState(), "q1");
        // The initial stack symbol of the PDA was changed which means the listener fired, as
        // required
        assertEquals(pda.getInitialStackSymbol(), "#");

        pda.changeInitialState("q0");
        assertEquals(pda.getInitialState(), "q0");
        // Initial stack symbol is null again which means the listener fired again
        assertNull(pda.getInitialStackSymbol());
    }

    /**
     * Test that a listener can be added that fires whenever the ObservableList of PDATransitions
     * changes.
     */
    @Test
    void addTransitionsListener() {
        // Initial stack symbol is null to begin with
        assertNull(pda.getInitialStackSymbol());
        // This listener will change the initial stack symbol of the pda to "#" if it is null and
        // if it is not null, it will set it back to null
        pda.addTransitionsListener(change -> {
            if (pda.getInitialStackSymbol() == null) {
                pda.setInitialStackSymbol("#");
            } else {
                pda.setInitialStackSymbol(null);
            }
        });

        // Adding a transition causes the transitions ObservableList to change and thus the
        // listener fires
        PDATransition t1 = new PDATransition("q0", "a", "", "", "q1");
        pda.addTransition(t1);
        // The initial stack symbol of the PDA was changed which means the listener fired, as
        // required
        assertEquals(pda.getInitialStackSymbol(), "#");

        // Deleting the transition changes the ObservableList again
        pda.deleteTransition(t1);
        // Initial stack symbol is null again which means the listener fired again
        assertNull(pda.getInitialStackSymbol());
    }

    /**
     * Test that a listener can be added that fires whenever the ObservableList of states changes.
     */
    @Test
    void addStatesListener() {
        // Initial stack symbol is null to begin with
        assertNull(pda.getInitialStackSymbol());
        // This listener will change the initial stack symbol of the pda to "#" if it is null and
        // if it is not null, it will set it back to null
        pda.addStatesListener(change -> {
            if (pda.getInitialStackSymbol() == null) {
                pda.setInitialStackSymbol("#");
            } else {
                pda.setInitialStackSymbol(null);
            }
        });

        // Adding a state causes the transitions ObservableList to change and thus the listener
        // fires
        pda.addState();
        // The initial stack symbol of the PDA was changed which means the listener fired, as
        // required
        assertEquals(pda.getInitialStackSymbol(), "#");

        // Deleting a state causes the transitions ObservableList to change again
        pda.deleteState("q2");
        // The initial stack symbol of the PDA was changed which means the listener fired, as
        // required
        assertNull(pda.getInitialStackSymbol());
    }

    /**
     * Test the toString method for a freshly created PDA with an initial state
     */
    @Test
    void pdaToString() {
        String initialPDAString = "{states=[q0], transitions=[], initialState=q0, " +
                "acceptingStates=[], initialStackSymbol=null, acceptanceCriteria=ACCEPTING_STATE}";
        PDA pda2 = new PDA();
        pda2.addState();

        assertEquals(pda2.toString(), initialPDAString);
    }

    /**
     * Test the toString method for a freshly created PDA without an initial state
     */
    @Test
    void pdaToString2() {
        String initialPDAString = "{states=[], transitions=[], initialState=null, " +
                "acceptingStates=[], initialStackSymbol=null, acceptanceCriteria=ACCEPTING_STATE}";
        PDA pda2 = new PDA();

        assertEquals(pda2.toString(), initialPDAString);
    }

    /**
     * Additional tests for the toString method which test multiple things like null initial
     * state, initial stack symbol, multiple states, multiple transitions and different acceptance
     * criteria.
     */
    @Test
    void pdaToString3() {
        String pdaString = "{states=[q0, q1], transitions=[{(q0,a,) -> (A,q1)}], initialState=q0," +
                " acceptingStates=[], initialStackSymbol=null, acceptanceCriteria=ACCEPTING_STATE}";
        assertEquals(pda.toString(), pdaString);

        pda.changeAcceptingState("q0");
        pdaString = "{states=[q0, q1], transitions=[{(q0,a,) -> (A,q1)}], initialState=q0, " +
                "acceptingStates=[q0], initialStackSymbol=null, " +
                "acceptanceCriteria=ACCEPTING_STATE}";
        assertEquals(pda.toString(), pdaString);

        pda.changeInitialState(null);
        pdaString = "{states=[q0, q1], transitions=[{(q0,a,) -> (A,q1)}], initialState=null, " +
                "acceptingStates=[q0], initialStackSymbol=null, " +
                "acceptanceCriteria=ACCEPTING_STATE}";
        assertEquals(pda.toString(), pdaString);

        pda.setInitialStackSymbol("#");
        pdaString = "{states=[q0, q1], transitions=[{(q0,a,) -> (A,q1)}], initialState=null, " +
                "acceptingStates=[q0], initialStackSymbol=#, acceptanceCriteria=ACCEPTING_STATE}";
        assertEquals(pda.toString(), pdaString);

        pda.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
        pdaString = "{states=[q0, q1], transitions=[{(q0,a,) -> (A,q1)}], initialState=null, " +
                "acceptingStates=[q0], initialStackSymbol=#, acceptanceCriteria=BOTH}";
        assertEquals(pda.toString(), pdaString);

        PDATransition t2 = new PDATransition("q2", "", "", "", "q3");
        pda.addTransition(t2);
        pdaString = "{states=[q0, q1, q2, q3], transitions=[{(q0,a,) -> (A,q1)}, {(q2,,) -> (,q3)" +
                "}], initialState=null, acceptingStates=[q0], initialStackSymbol=#, " +
                "acceptanceCriteria=BOTH}";
        assertEquals(pda.toString(), pdaString);
    }

    /**
     * Tests for the PDA equality method.
     */
    @Test
    void pdaEquals() {
        assertEquals(pda, pda);
        PDA pda2 = new PDA();
        assertEquals(pda2, pda2);
        pda2.addState();
        assertEquals(pda2, pda2);
        assertNotEquals(pda, pda2);
        pda2.addState();
        assertNotEquals(pda, pda2);
        PDATransition t1 = new PDATransition("q0", "a", "", "A", "q1");
        pda2.addTransition(t1);
        assertEquals(pda, pda2);

        pda.changeInitialState(null);
        assertNotEquals(pda, pda2);
        pda2.changeInitialState(null);
        assertEquals(pda, pda2);

        pda.changeAcceptingState("q0");
        assertNotEquals(pda, pda2);
        pda2.changeAcceptingState("q0");
        assertEquals(pda, pda2);

        pda.setInitialStackSymbol("#");
        assertNotEquals(pda, pda2);
        pda2.setInitialStackSymbol("#");
        assertEquals(pda, pda2);

        pda.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
        assertNotEquals(pda, pda2);
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
        assertEquals(pda, pda2);

        pda.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        assertNotEquals(pda, pda2);
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        assertEquals(pda, pda2);

        assertFalse(pda.equals(null));
    }

    /**
     * Test that a PDA can be correctly loaded from a string.
     */
    @Test
    void loadPDA() {
        PDA pda2 = new PDA();
        assertNull(pda2.getInitialState());
        assertEquals(pda2.getStates().size(), 0);
        assertTrue(pda2.getDeterministic());
        assertNull(pda2.getInitialStackSymbol());
        assertEquals(pda2.getAcceptingStates().size(), 0);
        assertEquals(pda2.getTransitions().size(), 0);
        assertEquals(pda2.getAcceptanceCriteria(), AcceptanceCriteria.ACCEPTING_STATE);

        pda2.loadPDAFromString("{states=[q0, q1], transitions=[{(q0,a,) -> (1,q0)}, {(q0,,) -> (," +
                "q1)}, {(q1,b,1) -> (,q1)}], initialState=q0, acceptingStates=[q1], " +
                "initialStackSymbol=null, acceptanceCriteria=BOTH}");

        assertEquals(pda2.getInitialState(), "q0");
        assertEquals(pda2.getStates().size(), 2);
        assertFalse(pda2.getDeterministic());
        assertNull(pda2.getInitialStackSymbol());
        assertEquals(pda2.getAcceptingStates().size(), 1);
        assertEquals(pda2.getTransitions().size(), 3);
        assertEquals(pda2.getAcceptanceCriteria(), AcceptanceCriteria.BOTH);

        PDA pda3 = new PDA();

        pda3.loadPDAFromString("{states=[q0, q1], transitions=[], initialState=null, " +
                "acceptingStates=[], initialStackSymbol=#, acceptanceCriteria=EMPTY_STACK}");

        assertNull(pda3.getInitialState());
        assertEquals(pda3.getStates().size(), 2);
        assertTrue(pda3.getDeterministic());
        assertEquals(pda3.getInitialStackSymbol(), "#");
        assertEquals(pda3.getAcceptingStates().size(), 0);
        assertEquals(pda3.getTransitions().size(), 0);
        assertEquals(pda3.getAcceptanceCriteria(), AcceptanceCriteria.EMPTY_STACK);
    }

    /**
     * Test that the initial configuration returned for different PDAs is correct for different
     * PDAs and input strings.
     */
    @Test
    void getInitialConfiguration() {
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("abc");
        // Configuration uses the initial state of the PDA
        assertEquals(initialConfiguration.getState(), "q0");
        // The inputString has been correctly set
        assertEquals(PDAConfiguration.getInputString(), "abc");
        // Index is 0 as required
        assertEquals(initialConfiguration.getIndex(), 0);
        // The inputSymbol for the configuration is the first character of the string
        assertEquals(initialConfiguration.getInputSymbol(), "a");
        // Stack is initially empty (initial stack symbol is null)
        assertEquals(initialConfiguration.getStack().size(), 0);

        pda.changeInitialState("q1");
        pda.setInitialStackSymbol("#");

        initialConfiguration = pda.getInitialConfiguration("aaa");
        // The state of the configuration is now the new initial state of the PDA
        assertEquals(initialConfiguration.getState(), "q1");
        // The inputString has been updated
        assertEquals(PDAConfiguration.getInputString(), "aaa");
        assertEquals(initialConfiguration.getIndex(), 0);
        // The stack is no longer empty - it has a single element which is the initial stack
        // symbol of the PDA
        assertEquals(initialConfiguration.getStack().size(), 1);
        assertEquals(initialConfiguration.getStack().get(0), "#");

        initialConfiguration = pda.getInitialConfiguration("");
        // No input symbol for the initial configuration if the input string is null
        assertNull(initialConfiguration.getInputSymbol());
    }

    /**
     * Tests for the getApplicableTransitions method for various PDAs and configurations.
     */
    @Test
    void getApplicableTransitions() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        // Test a configuration with an empty stack and a non-empty inputString
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("abc");
        ArrayList<PDATransition> applicableTransitions =
                pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        // The transition is applicable since the states match, the input symbol of the
        // transition matches the input symbol of the configuration ("a" because index is 0) and the
        // transition does not pop from the stack.

        // The transition is not applicable as the input symbol of the configuration does not
        // match the input symbol of the transition
        initialConfiguration = pda.getInitialConfiguration("bc");
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 0);

        // After adding this transition, now there is an applicable transition for the configuration
        PDATransition t1 = new PDATransition("q0", "b", "", "B", "q1");
        pda.addTransition(t1);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,) -> (B,q1)}]
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 1);

        // With the addition of this epsilon transition, we now have two applicable transitions
        // for the initial configuration for the input string "bc"
        PDATransition t2 = new PDATransition("q0", "", "", "", "q1");
        pda.addTransition(t2);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,) -> (B,q1)}, {(q0,,) -> (,q1)}]
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 2);

        // After deleting the epsilon transition and changing the popString of the other applicable
        // transition to "#", there are no longer any applicable transitions. This is because the
        // stack of the initial configuration is empty due to the initial stack symbol being null.
        pda.deleteTransition(t2);
        PDATransition t3 = new PDATransition("q0", "b", "#", "B", "q1");
        pda.editTransition(t1, t3);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,b,#) -> (B,q1)}]
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 0);

        // Changing the initial stack symbol to "#" makes the transition applicable
        pda.setInitialStackSymbol("#");
        initialConfiguration = pda.getInitialConfiguration("bc");
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 1);


        pda.getTransitions().clear();
        PDATransition t4 = new PDATransition("q0", "", "#", "ABC", "q0");
        PDATransition t5 = new PDATransition("q0", "", "ABC", "B", "q1");

        pda.addTransition(t4);
        pda.addTransition(t5);
        // transitions: [{(q0,,#) -> (ABC,q0)}, {(q0,,ABC) -> (B,q1)}]

        // Transition t4 is the only applicable transition for the empty string since the initial
        // stack symbol is "#"
        initialConfiguration = pda.getInitialConfiguration("");
        applicableTransitions = pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        assertEquals(applicableTransitions.get(0), t4);

        // After applying t4, the stack now has "ABC" on the top which makes t5 applicable
        PDAConfiguration newConfiguration = pda.applyTransition(initialConfiguration,
                applicableTransitions.get(0));
        applicableTransitions = pda.getApplicableTransitions(newConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        assertEquals(applicableTransitions.get(0), t5);


        pda.getTransitions().clear();
        PDATransition t6 = new PDATransition("q0", "", "A", "", "q0");
        PDATransition t7 = new PDATransition("q0", "", "AB", "", "q0");
        PDATransition t8 = new PDATransition("q0", "", "ABC", "", "q0");

        pda.addTransition(t6);
        pda.addTransition(t7);
        pda.addTransition(t8);
        // transitions: [{(q0,,A) -> (,q0)}, {(q0,,AB) -> (,q0)}, {(q0,,ABC) -> (,q0)}]

        Stack<String> stack = new Stack<>();
        stack.push("C");
        stack.push("B");
        stack.push("A");

        // With a configuration that has a stack containing "ABC" (where C is on the bottom) of
        // the stack, a state of "q0" and an index of 0, all three transitions are applicable.
        PDAConfiguration configuration = new PDAConfiguration(stack, "q0", 0);
        applicableTransitions = pda.getApplicableTransitions(configuration);
        assertEquals(applicableTransitions.size(), 3);
    }

    /**
     * Tests for the applyTransition method with various configurations.
     */
    @Test
    void applyTransition() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("abc");
        ArrayList<PDATransition> applicableTransitions =
                pda.getApplicableTransitions(initialConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        PDAConfiguration newConfiguration = pda.applyTransition(initialConfiguration,
                applicableTransitions.get(0));
        // Applying this transition should give a new configuration with a stack containing one
        // element ("A"), a state of "q1" and an index of 1.
        assertEquals(newConfiguration.getState(), "q1");
        // The input string remains unchanged in the new configuration
        assertEquals(PDAConfiguration.getInputString(), "abc");
        assertEquals(newConfiguration.getIndex(), 1);
        // Index of 1 means the current input symbol for this configuration is "b" - the "a" has
        // been consumed
        assertEquals(newConfiguration.getStack().size(), 1);
        assertEquals(newConfiguration.getStack().get(0), "A");

        pda.addTransition(new PDATransition("q1", "b", "A", "", "q2"));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q1,b,A) -> (,q2)}]
        applicableTransitions = pda.getApplicableTransitions(newConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        // This transition should pop the "A" from the stack leaving it empty, change the state
        // to q2 and increase the index.
        newConfiguration = pda.applyTransition(newConfiguration, applicableTransitions.get(0));
        assertEquals(newConfiguration.getState(), "q2");
        assertEquals(PDAConfiguration.getInputString(), "abc");
        assertEquals(newConfiguration.getIndex(), 2);
        // Index of 2 means the current input symbol for this configuration is "c" - the "ab" has
        // been consumed
        assertEquals(newConfiguration.getStack().size(), 0);
        // Stack is empty as the transition popped the "A" but didn't push anything


        pda.addTransition(new PDATransition("q2", "", "", "ABC", "q3"));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q1,b,A) -> (,q2)}, {(q2,,) -> (Z,q3)}]
        applicableTransitions = pda.getApplicableTransitions(newConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        // The effect of applying this transition should be a new configuration with a stack
        // containing "ABC", a state of "q3" and the same index of 2.
        newConfiguration = pda.applyTransition(newConfiguration, applicableTransitions.get(0));
        assertEquals(newConfiguration.getState(), "q3");
        assertEquals(PDAConfiguration.getInputString(), "abc");
        assertEquals(newConfiguration.getIndex(), 2);
        // Index remains at 2 since this transition does not consume any input symbols
        assertEquals(newConfiguration.getStack().size(), 3);
        assertEquals(newConfiguration.getStack().get(2), "A");
        assertEquals(newConfiguration.getStack().get(1), "B");
        assertEquals(newConfiguration.getStack().get(0), "C");

        pda.addTransition(new PDATransition("q3", "c", "AB", "DE", "q3"));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q1,b,A) -> (,q2)}, {(q2,,) -> (ABC,q3)}, {(q3,c,
        // AB) -> (DE,q3)}]
        applicableTransitions = pda.getApplicableTransitions(newConfiguration);
        assertEquals(applicableTransitions.size(), 1);
        // This transition will produce a configuration with the same state, an increased index.
        // As this transition pops "AB" from the stack, the stack of the new configuration would end
        // up with just a "C". However, this transition also pushes "DE" onto the stack so the stack
        // ends up as "DEC". This is because we push in reverse order.
        newConfiguration = pda.applyTransition(newConfiguration, applicableTransitions.get(0));
        assertEquals(newConfiguration.getState(), "q3");
        assertEquals(PDAConfiguration.getInputString(), "abc");
        assertEquals(newConfiguration.getIndex(), 3);
        // Index increased to 3 since this transition consumes the "c" from the tape.
        assertEquals(newConfiguration.getStack().size(), 3);
        assertEquals(newConfiguration.getStack().get(2), "D");
        assertEquals(newConfiguration.getStack().get(1), "E");
        assertEquals(newConfiguration.getStack().get(0), "C");
    }


    /**
     * Tests for the isAcceptingConfiguration method using the default acceptance criteria of
     * ACCEPTING_STATE
     */
    @Test
    void isAcceptingConfiguration1() {
        // Initial configuration with the empty string is not an accepting configuration as q0 is
        // not an accepting state
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));

        pda.changeAcceptingState("q0");
        // Now that q0 is an accepting state, the configuration is considered an accepting
        // configuration
        assertTrue(pda.isAcceptingConfiguration(initialConfiguration));

        // A non-empty stack is still accepting with the "Accepting state" acceptance criteria
        pda.setInitialStackSymbol("#");
        initialConfiguration = pda.getInitialConfiguration("");
        assertTrue(pda.isAcceptingConfiguration(initialConfiguration));

        // transitions: [{(q0,a,) -> (A,q1)}]
        // A configuration in which the input string has been consumed but does not have an
        // accepting state is not an accepting configuration.
        initialConfiguration = pda.getInitialConfiguration("a");
        PDAConfiguration newConfiguration = pda.applyTransition(initialConfiguration,
                pda.getTransitions().get(0));
        assertFalse(pda.isAcceptingConfiguration(newConfiguration));
        pda.changeAcceptingState("q1");
        // Now that the state of the configuration is an accepting state, the configuration is
        // deemed an accepting one
        assertTrue(pda.isAcceptingConfiguration(newConfiguration));

        // Configuration cannot be accepting if there are any remaining input symbols, even if it
        // is in an accepting state
        initialConfiguration = pda.getInitialConfiguration("z");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));

    }

    /**
     * Tests for the isAcceptingConfiguration method using the acceptance criteria of EMPTY_STACK
     */
    @Test
    void isAcceptingConfiguration2() {
        // Initial configuration is accepting since it has an empty stack, even though it is not
        // in an accepting state
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("");
        pda.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        assertTrue(pda.isAcceptingConfiguration(initialConfiguration));

        // A configuration with a non-empty stack is not considered accepting with the "Empty
        // stack" acceptance criteria
        pda.setInitialStackSymbol("#");
        initialConfiguration = pda.getInitialConfiguration("");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));

        pda.setInitialStackSymbol(null);
        // transitions: [{(q0,a,) -> (A,q1)}]
        // A configuration in which the input string has been consumed but does not have an empty
        // stack is not an accepting configuration.
        initialConfiguration = pda.getInitialConfiguration("a");
        PDAConfiguration newConfiguration = pda.applyTransition(initialConfiguration,
                pda.getTransitions().get(0));
        assertFalse(pda.isAcceptingConfiguration(newConfiguration));
        pda.addTransition(new PDATransition("q1", "", "A", "", "q1"));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q1,,A) -> (,q1)}]
        newConfiguration = pda.applyTransition(newConfiguration, pda.getTransitions().get(1));
        // Now that another transition is applied that empties the stack by popping the "A", this
        // is an accepting configuration
        assertTrue(pda.isAcceptingConfiguration(newConfiguration));

        // Configuration cannot be accepting if there are any remaining input symbols, even if
        // the stack is empty
        initialConfiguration = pda.getInitialConfiguration("z");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));
    }

    /**
     * Tests for the isAcceptingConfiguration method using the default acceptance criteria of BOTH
     */
    @Test
    void isAcceptingConfiguration3() {
        // Configuration is not an accepting one because the state of the configuration is not an
        // accepting state
        PDAConfiguration initialConfiguration = pda.getInitialConfiguration("");
        pda.changeAcceptanceCriteria(AcceptanceCriteria.BOTH);
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));

        // This is now an accepting configuration as both the stack is empty and the state is an
        // accepting state
        pda.changeAcceptingState("q0");
        assertTrue(pda.isAcceptingConfiguration(initialConfiguration));

        // No longer an accepting configuration due to the non-empty stack
        pda.setInitialStackSymbol("#");
        initialConfiguration = pda.getInitialConfiguration("");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));

        pda.setInitialStackSymbol(null);
        // transitions: [{(q0,a,) -> (A,q1)}]
        // A configuration in which the input string has been consumed but does not have an empty
        // stack nor a state which is an accepting state is not an accepting configuration.
        initialConfiguration = pda.getInitialConfiguration("a");
        PDAConfiguration newConfiguration = pda.applyTransition(initialConfiguration,
                pda.getTransitions().get(0));
        assertFalse(pda.isAcceptingConfiguration(newConfiguration));
        pda.addTransition(new PDATransition("q1", "", "A", "", "q1"));
        // transitions: [{(q0,a,) -> (A,q1)}, {(q1,,A) -> (,q1)}]
        newConfiguration = pda.applyTransition(newConfiguration, pda.getTransitions().get(1));
        // Even after applying another transition which empties the stack, the configuration is
        // not accepting since the state q1 is not an accepting state
        assertFalse(pda.isAcceptingConfiguration(newConfiguration));
        pda.changeAcceptingState("q1");
        // Now that the stack is empty, the state is an accepting state and the entire input
        // string has been consumed, this is considered an accepting configuration.
        assertTrue(pda.isAcceptingConfiguration(newConfiguration));

        // Configuration cannot be accepting if there are any remaining input symbols, even if
        // the stack is empty and the state is an accepting state
        initialConfiguration = pda.getInitialConfiguration("z");
        assertFalse(pda.isAcceptingConfiguration(initialConfiguration));
    }

    /**
     * Tests for a PDA with acceptance criteria of ACCEPTING_STATE and no transitions that use
     * the stack (basically a finite automaton).
     */
    @Test
    void getAcceptingComputations1() {
        PDA pda2 = new PDA();
        pda2.addState();
        pda2.changeAcceptingState("q0");
        pda2.addTransition(new PDATransition("q0", "a", "", "", "q0"));
        // Input string "a" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 1);
        // Input string "aaaaa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aaaaa", 50, 5000).getKey().size(), 1);
        // Empty string has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 1);
        // Input string "b" has no solutions
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));

        pda2 = new PDA();
        pda2.addState();
        pda2.addTransition(new PDATransition("q0", "a", "", "", "q1"));
        pda2.addTransition(new PDATransition("q1", "b", "", "", "q1"));
        pda2.changeAcceptingState("q1");
        // Input string "a" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 1);
        // Input string "abbb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("abbb", 50, 5000).getKey().size(), 1);
        // Input string "b" has no solutions
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "ba" has no solutions
        assertNull(pda2.getAcceptingComputations("ba", 50, 5000));
        // Empty string has no solutions
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));

        // Tests for PDA with infinite length computations
        pda2 = new PDA();
        pda2.addState();
        pda2.changeAcceptingState("q0");
        pda2.addTransition(new PDATransition("q0", "", "", "", "q0"));
        // 50 accepting computations for the empty string and a step limit of 50
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 50);
        // 100 accepting computations for the empty string and a step limit of 100
        assertEquals(pda2.getAcceptingComputations("", 100, 5000).getKey().size(), 100);
        // No accepting computations found for the input string "a"
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 0);

        pda2.addTransition(new PDATransition("q0", "", "", "", "q1"));
        pda2.changeAcceptingState("q1");
        // 99 accepting computations for the empty string and a step limit of 50
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 99);
        // No accepting computations found for the input string "a"
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 0);
    }

    /**
     * Tests for a PDA with acceptance criteria of EMPTY_STACK.
     */
    @Test
    void getAcceptingComputations2() {
        PDA pda2 = new PDA();
        pda2.addState();
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        pda2.addTransition(new PDATransition("q0", "a", "", "", "q0"));
        // Input string "a" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 1);
        // Input string "aaaaa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aaaaa", 50, 5000).getKey().size(), 1);
        // Empty string has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 1);
        // Input string "b" has no solutions
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));

        pda2 = new PDA();
        pda2.addState();
        pda2.setInitialStackSymbol("#");
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        pda2.addTransition(new PDATransition("q0", "a", "#", "", "q1"));
        pda2.addTransition(new PDATransition("q0", "b", "", "", "q1"));
        pda2.addTransition(new PDATransition("q1", "b", "#", "", "q1"));
        pda2.addTransition(new PDATransition("q1", "b", "", "", "q1"));
        // Input string "a" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("a", 50, 5000).getKey().size(), 1);
        // Input string "b" has no solutions
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "bbb" has 2 accepting computations
        assertEquals(pda2.getAcceptingComputations("bbb", 50, 5000).getKey().size(), 2);


        pda2 = new PDA();
        pda2.addState();
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        pda2.addTransition(new PDATransition("q0", "a", "", "A", "q1"));
        pda2.addTransition(new PDATransition("q0", "b", "", "B", "q1"));
        pda2.addTransition(new PDATransition("q1", "a", "A", "", "q1"));
        pda2.addTransition(new PDATransition("q1", "b", "", "", "q1"));
        // Input string "a" has no accepting computations
        assertNull(pda2.getAcceptingComputations("a", 50, 5000));
        // Input string "b" has no accepting computations
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "aa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aa", 50, 5000).getKey().size(), 1);
        // Input string "bb" has no accepting computations
        assertNull(pda2.getAcceptingComputations("bb", 50, 5000));
    }

    /**
     * This PDA has no accepting computations and also has an epsilon transition cycle. However, the
     * getAcceptingComputations method should still terminate and produce false as the value of the
     * Pair object since the total step limit is reached.
     */
    @Test
    void getAcceptingComputations3() {
        PDA pda2 = new PDA();
        pda2.addState();
        pda2.setInitialStackSymbol("#");
        pda2.changeAcceptanceCriteria(AcceptanceCriteria.EMPTY_STACK);
        pda2.addTransition(new PDATransition("q0", "", "", "", "q0"));
        pda2.addTransition(new PDATransition("q0", "", "", "", "q1"));
        pda2.addTransition(new PDATransition("q1", "", "", "", "q0"));
        assertFalse(pda2.getAcceptingComputations("", 50, 5000).getValue());
    }

    /**
     * The PDA that sample 1 contains accepts the language a^n b^n over the alphabet {a, b}.
     */
    @Test
    void runSample1PDA() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1, q2], transitions=[{(q0,a,) -> (A,q0)}, {(q0,b,A)" +
                " -> (,q1)}, {(q1,,#) -> (,q2)}, {(q1,b,A) -> (,q1)}], initialState=q0," +
                " acceptingStates=[], initialStackSymbol=#, acceptanceCriteria=EMPTY_STACK}");

        // Input string "a" has no accepting computations
        assertNull(pda2.getAcceptingComputations("a", 50, 5000));
        // Input string "b" has no accepting computations
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "ba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("ba", 50, 5000));
        // Input string "aba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("aba", 50, 5000));
        // Input string "abab" has no accepting computations
        assertNull(pda2.getAcceptingComputations("abab", 50, 5000));
        // Input string consisting of 25 as followed by 25bs  has no accepting computations of
        // length 50 or less
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbb",
                50, 5000).getKey().size(), 0);
        // The second element of the pair should be true since the step limit was reached
        assertTrue(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbb",
                50, 5000).getValue());

        // Input string "ab" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("ab", 50, 5000).getKey().size(), 1);
        // Input string "aaaaaaaaaabbbbbbbbbb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaabbbbbbbbbb", 50, 5000).getKey().size(), 1);
        // Input string consisting of 24 as followed by 24bs has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbb",
                50, 5000).getKey().size(), 1);
        // Input string consisting of 25 as followed by 25bs  has 1 accepting computations when
        // maxSteps is 100
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbb",
                100, 5000).getKey().size(), 1);
    }

    /**
     * Sample 2 accepts the same language as sample 1 but is constructed differently. It is
     * nondeterministic and only has 2 states rather than 3 states. The acceptance criteria used by
     * sample 2 is BOTH rather than ACCEPTING_STATE (though sample 1 could use Both instead).
     */
    @Test
    void runSample2PDA() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1], transitions=[{(q0,a,) -> (1,q0)}, {(q0,,1) -> " +
                "(1,q1)}, {(q1,b,1) -> (,q1)}], initialState=q0, acceptingStates=[q1], " +
                "initialStackSymbol=null, acceptanceCriteria=BOTH}");

        // Input string "a" has no accepting computations
        assertNull(pda2.getAcceptingComputations("a", 50, 5000));
        // Input string "b" has no accepting computations
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "ba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("ba", 50, 5000));
        // Input string "aba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("aba", 50, 5000));
        // Input string "abab" has no accepting computations
        assertNull(pda2.getAcceptingComputations("abab", 50, 5000));
        // Input string consisting of 25 as followed by 25bs  has no accepting computations of
        // length 50 or less
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbb",
                50, 5000).getKey().size(), 0);

        // Input string "ab" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("ab", 50, 5000).getKey().size(), 1);
        // Input string "aaaaaaaaaabbbbbbbbbb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaabbbbbbbbbb", 50, 5000).getKey().size(), 1);
        // Input string consisting of 24 as followed by 24bs has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbb",
                50, 5000).getKey().size(), 1);
        // Input string consisting of 25 as followed by 25bs  has 1 accepting computations when
        // maxSteps is 100
        assertEquals(pda2.getAcceptingComputations(
                "aaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbb",
                100, 5000).getKey().size(), 1);
    }

    /**
     * This sample matches strings over the alphabet {a, b} of the form: w w^r where w^r denotes
     * the reverse of w.
     */
    @Test
    void runSample3PDA() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1], transitions=[{(q0,a,) -> (a,q0)}, {(q0,b,) -> " +
                "(b,q0)}, {(q0,,) -> (,q1)}, {(q1,a,a) -> (,q1)}, {(q1,b,b) -> (,q1)}]," +
                " initialState=q0, acceptingStates=[q1], initialStackSymbol=null," +
                " acceptanceCriteria=BOTH}");

        // Input string "a" has no accepting computations
        assertNull(pda2.getAcceptingComputations("a", 50, 5000));
        // Input string "b" has no accepting computations
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "ab" has no accepting computations
        assertNull(pda2.getAcceptingComputations("ab", 50, 5000));
        // Input string "ba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("ba", 50, 5000));
        // Input string "aba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("aba", 50, 5000));
        // Input string "abab" has no accepting computations
        assertNull(pda2.getAcceptingComputations("abab", 50, 5000));

        // Input string "aa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aa", 50, 5000).getKey().size(), 1);
        // Input string "bb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("bb", 50, 5000).getKey().size(), 1);
        // Input string "aabbaa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aabbaa", 50, 5000).getKey().size(), 1);
        // Input string "aaaaaa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aaaaaa", 50, 5000).getKey().size(), 1);
        // Input string "aabbaaaabbaa" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aabbaaaabbaa", 50, 5000).getKey().size(), 1);
    }

    /**
     * Sample 4 accepts all strings over the alphabet {A, B} with an equal numbers of As and Bs
     * (including 0), in any order.
     */
    @Test
    void runSample4PDA() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1, q2, q3, q4, q5], transitions=[{(q0,,) -> (#,q1)" +
                "}, {(q1,A,) -> (A,q2)}, {(q2,A,) -> (A,q2)}, {(q2,B,A) -> (,q3)}, {(q3,B,A) " +
                "-> (,q3)}, {(q3,A,) -> (A,q2)}, {(q3,,#) -> (#,q1)}, {(q1,B,) -> (B,q4)}," +
                " {(q4,B,) -> (B,q4)}, {(q4,A,B) -> (,q5)}, {(q5,B,) -> (B,q4)}, {(q5,A,B) ->" +
                " (,q5)}, {(q5,,#) -> (#,q1)}], initialState=q0, acceptingStates=[q1]," +
                " initialStackSymbol=null, acceptanceCriteria=ACCEPTING_STATE}");

        // Input string "A" has no accepting computations
        assertNull(pda2.getAcceptingComputations("A", 50, 5000));
        // Input string "B" has no accepting computations
        assertNull(pda2.getAcceptingComputations("B", 50, 5000));
        // Input string "AAB" has no accepting computations
        assertNull(pda2.getAcceptingComputations("AAB", 50, 5000));
        // Input string "AABABBA" has no accepting computations
        assertNull(pda2.getAcceptingComputations("AABABBA", 50, 5000));

        // Empty string has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 1);
        // Input string "AB" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("AB", 50, 5000).getKey().size(), 1);
        // Input string "BA" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("BA", 50, 5000).getKey().size(), 1);
        // Input string "ABAB" has 2 accepting computations
        assertEquals(pda2.getAcceptingComputations("ABAB", 50, 5000).getKey().size(), 2);
        // Input string "BABA" has 2 accepting computations
        assertEquals(pda2.getAcceptingComputations("BABA", 50, 5000).getKey().size(), 2);
        // Input string "ABABAB" has 4 accepting computations
        assertEquals(pda2.getAcceptingComputations("ABABAB", 50, 5000).getKey().size(), 4);
        // Input string "ABABABAB" has 8 accepting computations
        assertEquals(pda2.getAcceptingComputations("ABABABAB", 50, 5000).getKey().size(), 8);
        // Input string "AAAABBBB" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("AAAABBBB", 50, 5000).getKey().size(), 1);
        // Input string "BBBBAAAA" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("BBBBAAAA", 50, 5000).getKey().size(), 1);
        // Input string "AABAABBB" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("AABAABBB", 50, 5000).getKey().size(), 1);
    }

    /**
     * This sample PDA accepts the language a^n b^m where n is less than or equal to m and m is
     * less than or equal to 2 times n.
     */
    @Test
    void runSample5PDA() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1], transitions=[{(q0,a,) -> (1,q0)}, {(q0,a,) -> " +
                "(11,q0)}, {(q0,,) -> (,q1)}, {(q1,b,1) -> (,q1)}], initialState=q0," +
                " acceptingStates=[q1], initialStackSymbol=null, acceptanceCriteria=BOTH}");

        // Input string "a" has no accepting computations
        assertNull(pda2.getAcceptingComputations("a", 50, 5000));
        // Input string "b" has no accepting computations
        assertNull(pda2.getAcceptingComputations("b", 50, 5000));
        // Input string "ba" has no accepting computations
        assertNull(pda2.getAcceptingComputations("ba", 50, 5000));
        // Input string "abbb" has no accepting computations
        assertNull(pda2.getAcceptingComputations("abbb", 50, 5000));

        // Input string "ab" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("ab", 50, 5000).getKey().size(), 1);
        // Input string "abb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("abb", 50, 5000).getKey().size(), 1);
        // Input string "aabb" has 1 accepting computation
        assertEquals(pda2.getAcceptingComputations("aabb", 50, 5000).getKey().size(), 1);
        // Input string "aabbb" has 2 accepting computations
        assertEquals(pda2.getAcceptingComputations("aabbb", 50, 5000).getKey().size(), 2);
        // Input string "aabbbb" has 2 accepting computations
        assertEquals(pda2.getAcceptingComputations("aabbbb", 50, 5000).getKey().size(), 1);
        // Input string "aaabbbb" has 3 accepting computations
        assertEquals(pda2.getAcceptingComputations("aaabbbb", 50, 5000).getKey().size(), 3);
    }

    /**
     * Tests for the getRandomComputation method when the behaviour is not random (as the PDA is
     * deterministic).
     */
    @Test
    void randomComputationTest() {
        PDA pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1, q2], transitions=[{(q0,a,) -> (A,q0)}, {(q0,b,A)" +
                " -> (,q1)}, {(q1,,#) -> (,q2)}, {(q1,b,A) -> (,q1)}], initialState=q0," +
                " acceptingStates=[], initialStackSymbol=#, acceptanceCriteria=EMPTY_STACK}");

        // The random computation returns the same computation as the computation returned by
        // getAcceptingComputations
        // when the PDA has only one accepting computation
        assertEquals(pda2.getAcceptingComputations("ab", 50, 5000).getKey().get(0),
                pda2.getRandomComputation("ab"));

        pda2 = new PDA();
        pda2.addState();
        pda2.changeAcceptingState("q0");
        pda2.addTransition(new PDATransition("q0", "", "", "", "q0"));

        // 50 accepting computations are generated for this PDA with an epsilon transition cycle.
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().size(), 50);

        // The longest accepting computation (the last one) is the same as the randomly generated
        // computation
        assertEquals(pda2.getAcceptingComputations("", 50, 5000).getKey().get(49),
                pda2.getRandomComputation(""));

        // PDA with an epsilon cycle returns a random computation that does not exceed length 50
        pda2 = new PDA();
        pda2.addTransition(new PDATransition("q0", "", "", "", "q0"));
        pda2.changeInitialState("q0");
        assertEquals(50, pda2.getRandomComputation("").size());

        // PDA with an epsilon cycle spanning multiple states returns a random computation with
        // length no more than 50
        pda2 = new PDA();
        pda2.loadPDAFromString("{states=[q0, q1, q2, q3], transitions=[{(q0,a,) -> (,q1)}, {(q1,," +
                ") -> (,q2)}, {(q2,,) -> (,q3)}, {(q3,,) -> (,q1)}], initialState=q0," +
                " acceptingStates=[q1], initialStackSymbol=null," +
                " acceptanceCriteria=ACCEPTING_STATE}");
        assertEquals(50, pda2.getRandomComputation("a").size());
    }

    @Test
    void getNonDeterministicTransitionsTest() {
        // transitions: [{(q0,a,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        PDATransition t2 = new PDATransition("q0", "", "B", "A", "q0");
        pda.addTransition(t2);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,,B) -> (A,q0)}]
        // Adding this epsilon transition makes the PDA nondeterministic
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t1));
        assertTrue(pda.getNondeterministicTransitions().contains(t2));
        pda.deleteTransition(t2);
        // transitions: [{(q0,,) -> (A,q1)}]

        // PDA is deterministic again once the epsilon transition is removed
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Adding this epsilon transition also makes the PDA nondeterministic
        PDATransition t3 = new PDATransition("q0", "", "", "A", "q1");
        pda.addTransition(t3);
        // transitions: [{(q0,a,) -> (A,q1)}, {(q0,,) -> (A,q1)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t1));
        assertTrue(pda.getNondeterministicTransitions().contains(t3));

        // PDA is deterministic if t3 is the only transition it has from state q0
        pda.deleteTransition(t1);
        // transitions: [{(q0,,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Adding this transition does not cause nondeterminism since it is the only transition
        // from state q1
        PDATransition t4 = new PDATransition("q1", "", "", "A", "q1");
        pda.addTransition(t4);
        // transitions: [{(q0,,) -> (A,q1)}, {(q1,,) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());

        // PDA is no longer deterministic since it has a full epsilon transition and another
        // transition from state q0. Full epsilon transition meaning that both the input symbol and
        // pop string are epsilon. This makes the transition always applicable, irrespective of the
        // input symbol and the current state of the stack. Therefore, having any transition along
        // with a full epsilon transition means the PDA will be nondeterministic.
        PDATransition t5 = new PDATransition("q0", "A", "B", "A", "q1");
        pda.addTransition(t5);
        // transitions: [{(q0,,) -> (A,q1)}, {(q1,,) -> (A,q1)}, {(q0,A,B) -> (A,q1)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t3));
        assertTrue(pda.getNondeterministicTransitions().contains(t5));

        pda.getTransitions().clear();
        assertEquals(pda.getTransitions().size(), 0);
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Two transitions with the same current state, input symbol and pop string but a
        // different push string makes the PDA nondeterministic
        PDATransition t6 = new PDATransition("q0", "a", "#", "A", "q1");
        PDATransition t7 = new PDATransition("q0", "a", "#", "B", "q1");

        pda.addTransition(t6);
        pda.addTransition(t7);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#) -> (B,q1)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t6));
        assertTrue(pda.getNondeterministicTransitions().contains(t7));

        pda.deleteTransition(t7);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Two transitions with the same current state, input symbol and pop string but a
        // different new state makes the PDA nondeterministic
        PDATransition t8 = new PDATransition("q0", "a", "#", "A", "q0");
        pda.addTransition(t8);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t6));
        assertTrue(pda.getNondeterministicTransitions().contains(t8));

        pda.deleteTransition(t8);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Adding this transition causes nondeterminism since if you are in state q0, the input
        // symbol is 'a' and there is a '#' on the top of the stack, you can apply either transition
        PDATransition t9 = new PDATransition("q0", "a", "", "A", "q0");
        pda.addTransition(t9);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t6));
        assertTrue(pda.getNondeterministicTransitions().contains(t9));

        pda.deleteTransition(t9);
        // transitions: [{(q0,a,#) -> (A,q1)}]
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Adding this transition causes nondeterminism since if you are in state q0, the input
        // symbol is 'a', the top of the stack is '#' and the element beneath that is 'A', you can
        // apply either transition
        PDATransition t10 = new PDATransition("q0", "a", "#A", "A", "q0");
        pda.addTransition(t10);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,a,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t6));
        assertTrue(pda.getNondeterministicTransitions().contains(t10));

        pda.deleteTransition(t10);
        assertTrue(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Same as the previous case but with epsilon as the input symbol for the transition with
        // the multiple character pop string
        PDATransition t11 = new PDATransition("q0", "", "#A", "A", "q0");
        pda.addTransition(t11);
        // transitions: [{(q0,a,#) -> (A,q1)}, {(q0,,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t6));
        assertTrue(pda.getNondeterministicTransitions().contains(t11));

        pda.getTransitions().clear();
        assertTrue(pda.getNondeterministicTransitions().isEmpty());
        // ----------------------------------------------------------------------------------------
        // Same as the previous case but with epsilon as the input symbol for the transition with
        // the single character pop string
        PDATransition t12 = new PDATransition("q0", "", "#", "A", "q1");
        PDATransition t13 = new PDATransition("q0", "a", "#A", "A", "q0");
        pda.addTransition(t12);
        pda.addTransition(t13);
        // transitions: [{(q0,,#) -> (A,q1)}, {(q0,a,#A) -> (A,q0)}]
        assertFalse(pda.getDeterministic());
        assertTrue(pda.getNondeterministicTransitions().contains(t12));
        assertTrue(pda.getNondeterministicTransitions().contains(t13));

        // This transition does not contribute to the PDA being nondeterministic
        PDATransition t14 = new PDATransition("q1", "", "", "", "q1");
        pda.addTransition(t14);
        assertFalse(pda.getNondeterministicTransitions().contains(t14));
        assertEquals(pda.getNondeterministicTransitions().size(), 2);
    }
}