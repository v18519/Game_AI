package com.fossgalaxy.games.fireworks.ai.aritificial_dummy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.rule.PlaySafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.random.TellRandomly;
import com.fossgalaxy.games.fireworks.ai.rule.CompleteTellUsefulCard;
import com.fossgalaxy.games.fireworks.ai.rule.DiscardOldestNoInfoFirst;
import com.fossgalaxy.games.fireworks.ai.rule.DiscardSafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;
import com.fossgalaxy.games.fireworks.ai.rule.TellAnyoneAboutUsefulCard;
import com.fossgalaxy.games.fireworks.ai.rule.TellMostInformation;
import com.fossgalaxy.games.fireworks.ai.rule.finesse.PlayFinesseTold;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardProbablyUselessCard;
import com.fossgalaxy.games.fireworks.ai.rule.random.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.ai.rule.simple.DiscardIfCertain;
import com.fossgalaxy.games.fireworks.ai.rule.simple.PlayIfCertain;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.ProductionRuleAgent;

public class artificial_dummy extends ProductionRuleAgent{
	int maxScore = 25;
	Random random = new Random();
	//cosntructor assigne rules
	public artificial_dummy() {
        addRule(new PlayProbablySafeCard());
        addRule(new PlaySafeCard());
        addRule(new CompleteTellUsefulCard());
        addRule(new TellAnyoneAboutUsefulCard());
        addRule(new DiscardSafeCard());
        addRule(new DiscardOldestNoInfoFirst());
        addRule(new PlayIfCertain());
        addRule(new DiscardIfCertain());
        addRule(new TellPlayableCardOuter());
        addRule(new PlayProbablySafeCard());
        addRule(new DiscardProbablyUselessCard());
        addRule(new PlayFinesseTold());
        addRule(new DiscardRandomly());
	}
	//if some information gone, use rules for try t play or discard save
	//if not use MCTS for
	@Override
	public Action doMove(int agentID, GameState state) {		
		if(isTableEmpty(state)) {
			return actionTableEmpty(agentID,state);
		}
		if(state.getInfomation()< 6) {
			return moveFromRule(agentID,state);
		}
		return moveFromTree(agentID, state);
	}
	//MCTS to get best node using uct
	public Action moveFromTree(int agentID, GameState state) {
		long startTime = System.currentTimeMillis(); 
		Node root = new Node(null, agentID, null, null,state);
		root.visits ++;
		Node current = root;
		Node aux = current;

		while(System.currentTimeMillis()-startTime < 500) {
			if(current == null) {
				break;
			}
	 		aux = select(current,root);	
			current = aux;
		}
		Action move = root.bestNode().move;
		return move;
	}
	//selects next MCTS node
	public Node select(Node current,Node root) {
		if(current.childs.isEmpty()) {
			if(current.visits == 0) {
				current.simulate(rules);
				return root;
			}else {
				return current.expand();
			}
		}else {
			return current.uctNode();
		}
	}
	//tells or play ones if table is empty
	private Action actionTableEmpty(int agentID, GameState state) {
		int hasOnes = calcAnyOnes(state);
		if(hasOnes == 9) {
			 Rule rule = new TellOnes();
			 if(rule == null) {
				 return moveFromTree(agentID,state);
			 }
			 Action action = rule.execute(agentID, state);
			 return action;
		}else if(hasOnes == agentID) {
			 Rule rule = new PlaySafeCard();
			 Action action = rule.execute(agentID, state);
			 return action;
		}
		return moveFromTree(agentID,state);
	}
	//compute table is empty
	private Boolean isTableEmpty(GameState state) {
		for(CardColour color : CardColour.values()) {
			if(state.getTableValue(color) > 0) {
				return false;
			}
		}
		return true;
	}
	//compute if anyones told
	private int calcAnyOnes(GameState state) {
		int player = 9;
		
		for(int i = 0; i < state.getPlayerCount(); i++) {
	        Hand hand = state.getHand(i);
	        for (int slot = 0; slot < hand.getSize(); slot++) {
	        	if(hand.getKnownValue(slot) != null){
	        		player = i;
	        	}
	        }
		}
		return player;
	}
	//selects legal action from rules
	public Action moveFromRule(int agentID, GameState state) {
        for (Rule rule : rules) {
            if (rule.canFire(agentID, state)) {
                Action selected = rule.execute(agentID, state);
                if (selected == null) {
                    System.out.println("NULL RULE");
                    continue;
                }
                return selected;
            }
        }
        return doDefaultBehaviour(agentID, state);
	}
	//prints table
    private String calcTable(GameState state){
        return String.format("B:%s\tG:%s\tO:%s\tR:%s\tY:%s",
                state.getTableValue(CardColour.BLUE),
                state.getTableValue(CardColour.GREEN),
                state.getTableValue(CardColour.ORANGE),
                state.getTableValue(CardColour.RED),
                state.getTableValue(CardColour.WHITE)
        );
    }
}
