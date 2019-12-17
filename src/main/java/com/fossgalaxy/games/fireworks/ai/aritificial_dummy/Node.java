package com.fossgalaxy.games.fireworks.ai.aritificial_dummy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.rule.PlaySafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

public class Node {
	double maxScore = 25;
	public int agentId;
	public int depth;
	public int visits;
	public double score;
	public int wins;
	public GameState state;
	
	public Action move;
	public Node parent;
	public List<Node> childs;
	public Collection <Action> actions;
	public Random random;
	
	public Node(Node parent, int agentId, Action move, Collection<Action> actions,GameState state) {
		this.parent = parent;
		this.agentId = agentId;
		this.move = move;
		this.score = 0;
		this.visits = 0;
		this.wins = 0;
		this.childs = new ArrayList<>();
		if(actions !=null) {
			this.actions = new ArrayList<>(actions);	
		}
		this.state = state.getCopy();
	}
	//get utc value
	public double uct() {
		return (score / visits) + (Math.sqrt(2) * Math.sqrt( Math.log(parent.visits) / visits));
	}
	// get best node based on score
	public Node bestNode() {
		Node best = null;
		double bestScore = 0 ;
		for(Node child : childs) {
			double aux = child.score;
			if(aux > bestScore) {
				bestScore = aux;
				best = child;
			}
		}
		return best;
	}
	// looks for actions
	public boolean noActions(GameState state) {
		if(actions.isEmpty()) {
			return true;
		}
		for(Action action : actions) {
			for(int i=0; i < state.getPlayerCount(); i++) {
				if(i != agentId && action.isLegal(i, state)) {
					return false;
				}
			}
		}
		return true;
	}
	//expand all posible legal acctions
	public Node expand() {
		Collection<Action> moves = Utils.generateAllActions(this.agentId, this.state.getPlayerCount());
		this.actions = moves.stream().filter(action -> action.isLegal(this.agentId, this.state)).collect(Collectors.toList());
		for(Action action : actions) {
			GameState currentState = this.state.getCopy();
			List<GameEvent> events = action.apply(this.agentId, currentState);
			events.forEach(currentState::addEvent);
			currentState.tick();
			Node child = new Node(this,(this.agentId+1)%currentState.getPlayerCount(),action,null,currentState);
			this.childs.add(child);
		}
		return uctNode();
	}
	//get best node based on utc
	public Node uctNode() {
		double aux = 0;
		Node auxNode = null;
		for(Node child : this.childs) {
			if(child.visits == 0) {
				return child;
			}else {
				double uct = child.uct();
				if(aux < child.score) {
					aux = uct;
					auxNode = child;
				}
			}
		}
		return auxNode;
	}
	//adds random actions to the current state until game is over
	public void simulate(List<Rule> rules) {
		int currentPlayer = this.agentId;
		GameState currentState = this.state.getCopy();
		while(!currentState.isGameOver()) {
			double n =0;
			Action action = null;
			while(action == null) {
				action = fromRule(currentPlayer,currentState,rules);
				if(n >3) {
					break;
				}
				n++;
			}
			if(n >3) {
				break;
			}
			List<GameEvent> events = action.apply(currentPlayer, currentState);
			events.forEach(currentState::addEvent);
			currentState.tick();
			currentPlayer = (currentPlayer +1) %currentState.getPlayerCount();
		}
		this.score = (Double.valueOf(currentState.getScore())/maxScore);
		this.backProp();
	}
	
	//gets suitable action from rules
	private Action fromRule(int agentID, GameState state, List<Rule> rules) {
        for (Rule rule : rules) {
            if ( rule.canFire(agentID, state)) {
            	Action selected = rule.execute(agentID, state);
            	if (selected == null ) {
                    System.out.println("NULL RULE");
                    return null;
                }
            	if(selected.isLegal(agentID, state)) {
            		return selected;
            	}
            }
        }
        return null;
	}
	//back propagates the score and visits
	public void backProp() {
		this.visits++;
		Node current = this;
		double aux = current.score;
		current = current.parent;
		while(current != null) {
			current.score += aux;
			current.visits++;
			current = current.parent;
		}
	}
}
