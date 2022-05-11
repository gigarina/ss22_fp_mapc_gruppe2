package massim.javaagents.agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import eis.iilang.ParameterList;
import eis.iilang.Percept;
import massim.javaagents.ExtendedMailService;
import massim.javaagents.MailService;
import massim.javaagents.Orientation;
import massim.javaagents.world.Block;
import massim.javaagents.world.Cell;
import massim.javaagents.world.Dispenser;
import massim.javaagents.world.Obstacle;
import massim.javaagents.world.Position;

public class AgentDaniel extends Agent {
	
	private int counter;
	private int lastID = -1;

	private String teamName;
	private int teamSize;
	private int duration;
	
	private String role;
	private int vision;
	private int energy;
	private boolean deactivated;
	private ArrayList<String> actionList;
	private ArrayList<Integer> speed;
	private long clearChance;
	private int clearMaxDistance;
	
	private Position currentPos = new Position(0,0);
	private Orientation rotated = Orientation.NORTH;
	private String lastAction;
	private String lastActionResult;
	private ArrayList<Parameter> lastActionParameters;
	
	private HashMap<Position, Cell> map = new HashMap<Position, Cell>();
	private HashMap<Position, Cell> attachedBlocks = new HashMap<Position, Cell>();
	private HashMap<String, Position> seenTeamMembers = new HashMap<String, Position>();

	AgentDaniel(String name, ExtendedMailService mailbox) {
		super(name, mailbox);
		counter = 0; // TODO Neueinloggen des Agenten nach Disconnect - Abfrage des Counters bei anderen Agenten?
		// TODO Auto-generated constructor stub
	}
	
	public void handleMap(HashMap<Position, Cell> mapSender, String from, Position pos) {
		if (seenTeamMembers.containsKey(from)) {
			Position posSender = seenTeamMembers.get(from);
			int xDiff = (posSender.getX() - pos.getX());
			int yDiff = (posSender.getY() - pos.getY());
			for (Position key : mapSender.keySet()) {
				Position adjusted = new Position(key.getX() + xDiff, key.getY() + yDiff);
				if (!map.containsKey(adjusted) || mapSender.get(key).getLastSeen() > map.get(adjusted).getLastSeen()) {
					map.put(adjusted, mapSender.get(key));
				}
			}
		}	
	}	

	@Override
	public void handlePercept(Percept percept) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Action step() {
		
		// Percepts behandeln
		List<Percept> percepts = getPercepts();
		if (counter == 0) {
			this.readStartSim(percepts);
		}
		this.readRequestAction(percepts);
		
		// Auswertung der letzten Aktion mit Bestimmung der gegenwärtigen Position
		this.evaluateLastAction();
				
		
/*		if (!seenTeamMembers.isEmpty()) {
			Set<String> toInform = seenTeamMembers.keySet();
			Iterator<String> it = toInform.iterator();
			while (it.hasNext()) {
				String to = it.next();
				((ExtendedMailService) getMailService()).sendMap(map, to, getName(), getCurrentPos());
			} 
		}
*/		
		// TODO Auto-generated method stub
		return null;
		
		// Ja
	}

	@Override
	public void handleMessage(Percept message, String sender) {
		// TODO Auto-generated method stub
		
	}
	
	public Position getCurrentPos() {
		return currentPos;
	}
	
	public void setCurrentPosition(Position pos) {
		this.currentPos = pos;
	}
	
	public void handlePosition(Position pos, String from) {
		
	}
	
	private void readStartSim(List<Percept> percepts) {
		for (Percept percept : percepts) {
			switch (percept.getName()) {
			case "name":
				break;
			case "team":
				break;
			case "teamSize":
				Parameter size = percept.getParameters().get(0);
				if (size instanceof Numeral) {
                    int tSize = ((Numeral) size).getValue().intValue();
    				this.teamSize = tSize;
    				break;
				} else {
					break;
				}
			case "steps":
				Parameter st = percept.getParameters().get(0);
				if (st instanceof Numeral) {
                    int steps = ((Numeral) st).getValue().intValue();
                    this.duration = steps;
                    break;
				} else {
					break;
				}
			case "role":
				Parameter ro = percept.getParameters().get(0);
				if (ro instanceof Identifier) {
					String role = ((Identifier) ro).getValue();
					this.role = role;
				} else {
					break;
				}
				Parameter vi = percept.getParameters().get(1);
				if (vi instanceof Numeral) {
					int vision = ((Numeral) vi).getValue().intValue();
					this.vision = vision;
				}
				ArrayList<String> newActionList = new ArrayList<>();
				Parameter actions = percept.getParameters().get(2);
				if (actions instanceof ParameterList) {
					Iterator<Parameter> it = ((ParameterList) actions).iterator();
					while (it.hasNext()) {
						Parameter ac = it.next();
						if (ac instanceof Identifier) {
							String action = ((Identifier) ac).getValue();
							newActionList.add(action);
						}
					}
					this.actionList = newActionList;
				} else {
					break;
				}
				ArrayList<Integer> newSpeedList = new ArrayList<>();
				Parameter speeds = percept.getParameters().get(3);
				if (speeds instanceof ParameterList) {
					Iterator<Parameter> it = ((ParameterList) speeds).iterator();
					while (it.hasNext()) {
						Parameter sp = it.next();
						if (sp instanceof Numeral) {
							Integer speed = ((Numeral) sp).getValue().intValue();
							newSpeedList.add(speed);
						}
					}
					this.speed = newSpeedList;
				} else {
					break;
				}
				Parameter clear = percept.getParameters().get(4);
				if (clear instanceof Numeral) {
					long cChance = ((Numeral) clear).getValue().longValue();
					this.clearChance = cChance;
				} else {
					break;
				}
				Parameter maxClear = percept.getParameters().get(5);
				if (maxClear instanceof Numeral) {
					int maxDistance = ((Numeral) maxClear).getValue().intValue();
					this.clearMaxDistance = maxDistance;
					break;
				} else {
					break;
				}
			}
		}
	}
	
	private void readRequestAction(List<Percept> percepts) {
		for (Percept percept : percepts) {
			switch (percept.getName()) {
			case "actionID":
				Parameter aID = percept.getParameters().get(0);
				if (aID instanceof Numeral) {
					int actionID = ((Numeral) aID).getValue().intValue();
					if (actionID > lastID) {
						this.lastID = actionID;
					}
					break;
				} else {
					break;
				}
			case "timestamp":
				break;
			case "deadline":
				break;
			case "step":
				Parameter step = percept.getParameters().get(0);
				if (step instanceof Numeral) {
					int stepNo = ((Numeral) step).getValue().intValue();
					this.counter = stepNo;
					break;
				} else {
					break;
				}
			case "lastAction":
				Parameter lAction = percept.getParameters().get(0);
				if (lAction instanceof Identifier) {
					String lastAction = ((Identifier) lAction).getValue();
					this.lastAction = lastAction;
					break;
				} else {
					break;
				}
			case "lastActionResult":
				Parameter lActionResult = percept.getParameters().get(0);
				if (lActionResult instanceof Identifier) {
					String lastActionResult = ((Identifier) lActionResult).getValue();
					this.lastActionResult = lastActionResult;
					break;
				} else {
					break;
				}
			case "lastActionParams":
				this.lastActionParameters = new ArrayList<Parameter>();
				Parameter lActionParam = percept.getParameters().get(0);
				if (lActionParam instanceof ParameterList) {
					Iterator<Parameter> it = ((ParameterList) lActionParam).iterator();
					while (it.hasNext()) {
						this.lastActionParameters.add(it.next());
					}
					break;
				} else {
					break;
				}
			case "score":
				break;			
			case "thing":
				int xPos;
				int yPos;
				String details;
				String type;
				Parameter xCoordinate = percept.getParameters().get(0);
				if (xCoordinate instanceof Numeral) {
                    xPos = ((Numeral) xCoordinate).getValue().intValue();
				} else {
					break;
				}
				Parameter yCoordinate = percept.getParameters().get(1);
				if (yCoordinate instanceof Numeral) {
                    yPos = ((Numeral) yCoordinate).getValue().intValue();
				} else {
					break;
				}
				Position posThing = new Position(xPos + currentPos.getX(), yPos + currentPos.getY());
				Parameter detailParameter = percept.getParameters().get(3);
				if (detailParameter instanceof Identifier) {
					details = ((Identifier) detailParameter).getValue();
				} else {
					break;
				}
				Parameter typeParameter = percept.getParameters().get(2);
				if (typeParameter instanceof Identifier) {
					type = ((Identifier) typeParameter).getValue();
				} else {
					break;
				}
				switch (type) {
				case ("block"):
					map.put(posThing, new Block(counter, type));
					break;
				case ("obstacle"):
					map.put(posThing, new Obstacle(counter));
					break;
				case ("dispenser"):
					map.put(posThing, new Dispenser(counter, type));
					break;
				case ("entity"):
					if (details.equals(teamName)) {
						
					}
				}
			case "task":
				break;
			case "attached":
				break;
			case "energy":
				Parameter en = percept.getParameters().get(0);
				if (en instanceof Numeral) {
					int ener = ((Numeral) en).getValue().intValue();
					this.energy = ener;
					break;
				} else {
					break;
				}
			case "deactivated":
				Parameter stat = percept.getParameters().get(0);
				if (stat instanceof Identifier) {
					String status = ((Identifier) stat).getValue();
					if (status.equalsIgnoreCase("true")) {
						this.deactivated = true;
					} else {
						this.deactivated = false;
					}
					break;
				} else {
					break;
				}
			}
		}
	}
	
	private void evaluateLastAction() {
		switch (lastAction) {
		case "skip":
			break;
		case "move":
			if (this.lastActionResult.equals("success")) {
				Iterator<Parameter> it = this.lastActionParameters.iterator();
				while (it.hasNext()) {
					Parameter dir = it.next();
					if (dir instanceof Identifier) {
						int x = this.currentPos.getX();
						int y = this.currentPos.getY();
						String dirString = ((Identifier) dir).getValue();
						if ((dirString.equals("n") && this.rotated.equals(Orientation.NORTH)) || (dirString.equals("e") && this.rotated.equals(Orientation.EAST)) || (dirString.equals("s") && this.rotated.equals(Orientation.SOUTH)) || (dirString.equals("w") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new Position(x, y+1));
						} else if  ((dirString.equals("s") && this.rotated.equals(Orientation.NORTH)) || (dirString.equals("w") && this.rotated.equals(Orientation.EAST)) || (dirString.equals("s") && this.rotated.equals(Orientation.NORTH)) || (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new Position(x, y+1));
						} else if ((dirString.equals("e") && this.rotated.equals(Orientation.NORTH)) || (dirString.equals("s") && this.rotated.equals(Orientation.EAST)) || (dirString.equals("w") && this.rotated.equals(Orientation.SOUTH)) || (dirString.equals("n") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new Position(x+1, y));
						} else if ((dirString.equals("w") && this.rotated.equals(Orientation.NORTH)) || (dirString.equals("n") && this.rotated.equals(Orientation.EAST)) || (dirString.equals("e") && this.rotated.equals(Orientation.SOUTH)) || (dirString.equals("s") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new Position(x-1, y));
						}
					}
				}
			} else if (this.lastActionResult.equals("partial_success")) {
				Parameter dir = this.lastActionParameters.get(0);
				if (dir instanceof Identifier) {
					int x = this.currentPos.getX();
					int y = this.currentPos.getY();
					String dirString = ((Identifier) dir).getValue();
					switch (dirString) {
					case "n":
						this.setCurrentPosition(new Position(x, y+1));
						break;
					case "s":
						this.setCurrentPosition(new Position(x, y-1));
						break;
					case "e":
						this.setCurrentPosition(new Position(x+1, y));
						break;
					case "w":
						this.setCurrentPosition(new Position(x-1, y));
						break;
					}
					// Fehlerbehandlung
				}
			} else if (this.lastActionResult.equals("failed_parameter")) {
				// Fehlerbehandlung
			} else {
				// Fehlerbehandlung
			}
			break;
		case "attach":
			switch (lastActionResult) {
			case "success":
				Parameter dir = this.lastActionParameters.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					Position pos;
					Cell cell;
					switch (dirString) {
					case "n":
						pos = new Position(this.currentPos.getX(), this.currentPos.getY()+1);
						cell = this.map.get(pos);
						this.attachedBlocks.put(pos, cell);
						break;
					case "s":
						pos = new Position(this.currentPos.getX(), this.currentPos.getY()-1);
						cell = this.map.get(pos);
						this.attachedBlocks.put(pos, cell);
						break;
					case "e":
						pos = new Position(this.currentPos.getX()+1, this.currentPos.getY());
						cell = this.map.get(pos);
						this.attachedBlocks.put(pos, cell);
						break;
					case "w":
						pos = new Position(this.currentPos.getX()-1, this.currentPos.getY());
						cell = this.map.get(pos);
						this.attachedBlocks.put(pos, cell);
						break;
					}
				}
			case "failed_parameter":
				break;
			case "failed_target":
				break;
			case "failed_blocked":
				break;
			case "failed":
				break;
			}
			break;
		case "detach":
			switch (lastActionResult) {
			case "success":
				Parameter dir = this.lastActionParameters.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					Position pos;
					switch (dirString) {
					case "n":
						pos = new Position(this.currentPos.getX(), this.currentPos.getY()+1);
						this.attachedBlocks.remove(pos);
						break;
					case "s":
						pos = new Position(this.currentPos.getX(), this.currentPos.getY()-1);
						this.attachedBlocks.remove(pos);
						break;
					case "e":
						pos = new Position(this.currentPos.getX()+1, this.currentPos.getY());
						this.attachedBlocks.remove(pos);
						break;
					case "w":
						pos = new Position(this.currentPos.getX()-1, this.currentPos.getY());
						this.attachedBlocks.remove(pos);
						break;
					}
			}
			break;
		case "rotate":
			switch (lastActionResult) {
			case "success":
				Parameter rot = this.lastActionParameters.get(0);
				if (rot instanceof Identifier) {
					String rotString = ((Identifier) rot).getValue();
					HashMap<Position, Cell> temp = new HashMap<Position, Cell>();
					if (rotString.equals("cw")) {
						this.rotated = Orientation.changeOrientation(rotated, 1);
						for (Position key : this.attachedBlocks.keySet()) {
							Cell cell = this.attachedBlocks.get(key);
							temp.put(new Position(key.getY(), -key.getX()), cell);
						}
					} else {
						this.rotated = Orientation.changeOrientation(rotated, -1);
						for (Position key : this.attachedBlocks.keySet()) {
							Cell cell = this.attachedBlocks.get(key);
							temp.put(new Position(-key.getY(), key.getX()), cell);
						}
					}
					this.attachedBlocks = temp;
				}
			}
			break;
		case "connect":
			break;
		case "disconnect":
			break;
		case "request":
			break;
		case "submit":
			break;
		case "clear":
			break;
		case "adopt":
			break;
		case "survey":
		
		}
		
	}
	}
}
