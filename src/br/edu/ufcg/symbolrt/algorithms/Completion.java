/*
 * SYmbolic Model-Based test case generation toOL for Real-Time systems (SYMBOLRT)
 * (C) Copyright 2010-2013 Federal University of Campina Grande (UFCG)
 * 
 * This file is part of SYMBOLRT.
 *
 * SYMBOLRT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SYMBOLRT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SYMBOLRT.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * REVISION HISTORY:
 * Author                           Date           Brief Description
 * -------------------------------- -------------- ------------------------------
 * Wilkerson de Lucena Andrade      16/06/2011     An old class named TIOSTSPreprocessing (Version 1.2) was divided into two classes. This class only contains the completion algorithm (Version 1.0).
 *
 */
package br.edu.ufcg.symbolrt.algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import br.edu.ufcg.symbolrt.base.Action;
import br.edu.ufcg.symbolrt.base.ClockGuard;
import br.edu.ufcg.symbolrt.base.Location;
import br.edu.ufcg.symbolrt.base.SimpleClockGuard;
import br.edu.ufcg.symbolrt.base.TIOSTS;
import br.edu.ufcg.symbolrt.base.Transition;
import br.edu.ufcg.symbolrt.exception.ClockGuardException;
import br.edu.ufcg.symbolrt.util.Constants;


/**
 * <code>Completion</code> Class. <br>
 * This class contains an algorithm needed to process TIOSTSs before test case generation. The implemented algorithm here is used for completing
 * test purposes. 
 * 
 * @author Wilkerson de Lucena Andrade  ( <a href="mailto:wilkerson@computacao.ufcg.edu.br">wilkerson@computacao.ufcg.edu.br</a> )
 * 
 * @version 1.0
 * <br>
 * SYmbolic Model-Based test case generation toOL for Real-Time systems (SYMBOLRT)
 * <br>
 * (C) Copyright 2010-2013 Federal University of Campina Grande (UFCG)
 * <br>
 * <a href="https://sites.google.com/a/computacao.ufcg.edu.br/symbolrt">https://sites.google.com/a/computacao.ufcg.edu.br/symbolrt</a>
 */
public class Completion {

	private static Completion instance = null;


	private Completion() {
	}

	
	/**
	 * Method used to obtain an instance of this class.
	 * @return An instance of {@link Completion}.
	 */
	public static Completion getInstance() {
		if (instance == null) {
			instance = new Completion();
		}
		return instance;
	}
	
	
	/**
	 * This method is responsible for completing a TIOSTS model with respect to all actions of the model. During the test case
	 * generation this operation is used for completing test purposes. It is important to remark that the provided algorithm
	 * does not take internal actions into account because test purposes do not need this kind of action since test purposes 
	 * are used to indicate observable scenarios of the specification to be tested. <p> It is assumed that in each 
	 * transition of a test purpose one of the guards (data or clock guard) is true. This is important to avoid the introduction
	 * of disjunctions involving clock guards once this kind of guard is not supported by the theory. When a clock guard
	 * is defined, it is allowed only one constraint of the form c#i where c is a clock, i is an integer constant, and # &isin; 
	 * {<, <=, =>, >}. Moreover, the symbol = is not allowed in the constraint because its negation is not supported in 
	 * representation of zones. </p>
	 *  
	 * @return A complete TIOSTS model.
	 * @throws ClockGuardException If there is one transition with an unsupported clock guard.
	 */
	public TIOSTS complete(TIOSTS tiosts) throws ClockGuardException {
		TIOSTS tiostsComplete = tiosts.clone();
		tiostsComplete.setName(tiosts.getName() + "_complete");
		
		// Adds a Reject location if the tiosts does not have one
		Location rejectLocation = tiostsComplete.getLocation(Constants.LOCATION_LABEL_REJECT);
		if (rejectLocation == null) {
			rejectLocation = new Location(Constants.LOCATION_LABEL_REJECT);
			tiostsComplete.addLocation(rejectLocation);
		}
		
		for (Location loc : tiostsComplete.getLocations()) {
			if (!loc.isSpecialLocation() && !loc.isInitialLocation()) {
				for (Action action : getRemainingActions(loc, tiostsComplete.getInputOutputActions())) {
					tiostsComplete.createTransition(loc.getLabel(), Constants.GUARD_TRUE, Constants.GUARD_TRUE, action, null, null, loc.getLabel());
				}
				// The clone of outgoing transitions is used because the list of transitions will be changed during the iteration
				List<Transition> outTransitions = new ArrayList<Transition>(loc.getOutTransitions());
				for (Transition transition : outTransitions) {
					if (!transition.getAllGuard().equals(Constants.GUARD_TRUE)){
						String dataGuard = Constants.GUARD_TRUE;
						ClockGuard clockGuard = new ClockGuard();
						if (transition.getClockGuard().toString().equals(Constants.GUARD_TRUE)){
							dataGuard = Constants.GUARD_NEGATION + " (" + transition.getDataGuard() + ")";
						} else {
							clockGuard = negateClockGuard(transition.getClockGuard());
						}
						
						if (transition.getTarget().isRejectLocation()){
							// If target location is the reject location, then a self loop is created
							tiostsComplete.createTransition(loc.getLabel(), dataGuard, clockGuard.toString(), transition.getAction(), null, null, transition.getDeadline(), loc.getLabel());
						} else {
							// If target location is not the reject location, then a transition to the reject location is created
							tiostsComplete.createTransition(loc.getLabel(), dataGuard, clockGuard.toString(), transition.getAction(), null, null, transition.getDeadline(), rejectLocation.getLabel());
						}
					}
				}
			}
		}
		
		return tiostsComplete;
	}
	
	
	/**
	 * Given all actions of the model and a specific location, this method returns the actions that are not leaving the location. 
	 * @param loc The location to be analyzed. 
	 * @param allActions All actions present in the model.
	 * @return The remaining actions that are not leaving loc.
	 */
	private Collection<Action> getRemainingActions(Location loc, Collection<Action> allActions) { 
		Collection<Action> result = new ArrayList<Action>();
		Collection<Action> usedActions = loc.getOutActions();
		
		for (Action action : allActions) {
			if (!usedActions.contains(action)){
				result.add(action);
			}
		}
		
		return result;
	}
	
	
	/**
	 * Given a clock guard, this method returns its negation. It is assumed that the 
	 * parameter contains only one constraint of the form c#i where c is a clock, i is
	 * an integer constant, and # &isin; {<, <=, =>, >}. The symbol = is not allowed
	 * in the constraint because its negation is not supported in representation of zones.
	 * @param clockGuard The clock guard to negate.
	 * @return The negation of the clock guard.
	 * @throws ClockGuardException If the clock guard is not supported in test purposes.
	 */
	private ClockGuard negateClockGuard(ClockGuard clockGuard) throws ClockGuardException{
		if (clockGuard.getClockGuard().size() != 1) {
			throw new ClockGuardException("Clock guards of test purposes can have only one constraint.");
		}
		
		SimpleClockGuard simpleClockGuard = clockGuard.getClockGuard().get(0);
		
		if (simpleClockGuard.getRelation().equals(Constants.GUARD_EQUAL)) {
			throw new ClockGuardException("= is not allowed in clock guards of test purposes because its negation is not " +
					"supported in representation of zones.");
		}
		
		
		String relation = "";
		
		if (simpleClockGuard.getRelation().equals(Constants.GUARD_LEQ)){
			relation = Constants.GUARD_GT;
		} else if (simpleClockGuard.getRelation().equals(Constants.GUARD_GEQ)){
			relation = Constants.GUARD_LT;
		} else if (simpleClockGuard.getRelation().equals(Constants.GUARD_LT)){
			relation = Constants.GUARD_GEQ;
		} else if (simpleClockGuard.getRelation().equals(Constants.GUARD_GT)){
			relation = Constants.GUARD_LEQ;
		} else {
			throw new ClockGuardException("The clock guard \"" + clockGuard.toString() + "\" is not supported in test purposes.");
		}

		ClockGuard newClockGuard = new ClockGuard();
		SimpleClockGuard newSimpleClockGuard = new SimpleClockGuard(simpleClockGuard.getClockId(), simpleClockGuard.getClock(), relation, simpleClockGuard.getIntegerConstant());
		newClockGuard.addSimpleClockGuard(newSimpleClockGuard);
		
		return newClockGuard;
	}

	
}
