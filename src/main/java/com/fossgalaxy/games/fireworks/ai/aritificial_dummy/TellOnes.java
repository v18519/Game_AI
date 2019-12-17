package com.fossgalaxy.games.fireworks.ai.aritificial_dummy;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

public class TellOnes extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {
    	int index = 1;
    	Action toDo = null;
        
    	while(toDo == null) {
        	int nextPlayer = (playerID + index) % state.getPlayerCount();

            Hand hand = state.getHand(nextPlayer);

            for (int slot = 0; slot < state.getHandSize(); slot++) {

                Card card = hand.getCard(slot);
                if (card == null || card.value != 1) {
                    continue;
                }

                toDo = tellMissingPrioritiseValue(hand, nextPlayer, slot);
                if (toDo != null) {
                    return toDo;
                }
            }
            if(index > 6) {
            	break;
            }
            index ++;
    	}
    	return toDo;
    }

}
