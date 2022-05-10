package massim.javaagents;

import java.util.HashMap;
import java.util.logging.Logger;

import eis.iilang.Percept;
import massim.javaagents.agents.Agent;
import massim.javaagents.agents.AgentDaniel;
import massim.javaagents.world.Cell;
import massim.javaagents.world.Position;

public class ExtendedMailService extends MailService {
	
   /* public void sendMap(HashMap<Position, Cell> map, String to, String from, Position pos){

        AgentDaniel recipient = (AgentDaniel) getRecipient(to);

        if (recipient == null) {
        	Logger logger = getLogger();
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        }
        else {
            recipient.handleMap(map, from, pos);
        }
    }
    
    public void sendPosition(Position pos, String to, String from){

        AgentDaniel recipient = (AgentDaniel) getRecipient(to);

        if (recipient == null) {
        	Logger logger = getLogger();
            logger.warning("Cannot deliver message to " + to + "; unknown target,");
        }
        else {
            recipient.handlePosition(pos, from);
        }
    }
    
    public void handshakeSYN(Position pos, String sender) {
        getAgentsByTeam().get(getTeamForAgent().get(sender)).stream()
        	.map(Agent::getName)
        	.filter(ag -> !ag.equals(sender))
        	.forEach(ag -> sendPosition(pos, ag, sender));
    }
*/
}
