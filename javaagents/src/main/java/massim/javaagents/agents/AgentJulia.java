package massim.javaagents.agents;

import eis.iilang.*;
import massim.javaagents.MailService;
import massim.javaagents.agents.g2utils.*;

import java.util.*;

/**
 * A very basic agent.
 */
public class AgentJulia extends Agent {

	private String teamName;
	private int teamSize;
	private int stepsOverall;
	private List<Role> roles = new ArrayList<>();
	private String explorerMissionAgent = "";

	private int currentStep = -1;
	private long currentScore = 0;
	private int energyLevel = 0;
	private boolean currentlyActive = true;
	private Role currentRole = null;
	// TODO: fill this with maximum step number of agent's current role
	private int maxStepNum = 2;

	private long actionID = -1;
	private long timestamp = 0;
	private long deadline = -1;
	private String lastAction = "";
	private List<Object> lastActionParams = new ArrayList<>();
	private String lastActionResult = "";
	private RelativeCoordinate hitFrom = null;
	private List<Norm> violations = new ArrayList<>();

	private List<Percept> attachedThingsPercepts = new ArrayList<>();
	private List<Dispenser> dispensers = new ArrayList<>();
	private List<Block> blocks = new ArrayList<>();
	private List<Entity> entities = new ArrayList<>();
	private List<RelativeCoordinate> occupiedFields = new ArrayList<>();
	private List<RelativeCoordinate> goalZoneFields = new ArrayList<>();
	private List<RelativeCoordinate> roleZoneFields = new ArrayList<>();
	private List<Task> tasks = new ArrayList<>();
	private List<Norm> norms = new ArrayList<>();
	// Blocks that might(!) be directly attached to the agent (right next to agent)
	private List<Block> attachedBlocks = new ArrayList<>();

	private HashMap<String, RelativeCoordinate> seenTeamMembers = new HashMap<String, RelativeCoordinate>();
	private HashMap<RelativeCoordinate, Cell> map = new HashMap<RelativeCoordinate, Cell>(); //see map
	private RelativeCoordinate currentPos = new RelativeCoordinate(0, 0); // TODO delete if currentAbsolutePos works.
	private Orientation rotated = Orientation.NORTH;
	private HashMap<RelativeCoordinate, Cell> attachedBlocksWithPositions = new HashMap<>();
	private String roleName = ""; // TODO -> automatisch aktualisieren, wenn Rolle geändert wird

	private String lastMoveDir = "";
//	private MapOfAgent map = new MapOfAgent();
//	private RelativeCoordinate currentAbsolutePos = new RelativeCoordinate(0, 0);

	/**
	 * Constructor.
	 * 
	 * @param name    the agent's name
	 * @param mailbox the mail facility
	 */
	public AgentJulia(String name, MailService mailbox) {
		super(name, mailbox);
	}

	@Override
	public void handlePercept(Percept percept) {
	}

	@Override
	public void handleMessage(Percept message, String sender) {
	}

	public void handleMap(HashMap<RelativeCoordinate, Cell> mapSender, String from, RelativeCoordinate pos) {
		if (seenTeamMembers.containsKey(from)) {
			RelativeCoordinate posSender = seenTeamMembers.get(from);
			int xDiff = (posSender.getX() - pos.getX());
			int yDiff = (posSender.getY() - pos.getY());
			for (RelativeCoordinate key : mapSender.keySet()) {
				RelativeCoordinate adjusted = new RelativeCoordinate(key.getX() + xDiff, key.getY() + yDiff);
				if (!map.containsKey(adjusted) || mapSender.get(key).getLastSeen() > map.get(adjusted).getLastSeen()) {
					map.put(adjusted, mapSender.get(key));
				}
			}
		}
	}

	@Override
	public Action step() {
		boolean currentStepSet = setCurrentStep(getPercepts()); // must be set first, so agents knows currentStep for
																// sortPercepts()
		if (!currentStepSet) {
			say("I was called but did not receive a current step percept");
			// TODO: implement end of simulation percept handling
			return null;
		}
		sortPercepts(getPercepts());
		if (explorerMissionAgent.length() == 0) {
			if (teamSize > 1) {
				explorerMissionAgent = Mission.applyForExplorerMission(getName());
			}
		}
		if (!lastActionResult.equals("success")) {
			return handleError();
		}
		if (explorerMissionAgent.equals(getName())) {
			say("My mission: I am the explorer of the team!");
			return explorerStep();
		} else {
			say("My mission: I am just a normal worker :(");
			return workerStep();
		}
	}

	private boolean setCurrentStep(List<Percept> percepts) {
		for (Percept percept : percepts) {
			if (percept.getName().equals("step")) {
				currentStep = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				say("------------------ Current step: " + currentStep + " ------------------");
				return true;
			}
		}
		return false;
	}

	private void sortPercepts(List<Percept> percepts) {
		if (percepts == null) { // Error handling if no percepts are available
			return;
		}
		// Allocate initial percepts to variables
		if (currentStep == 0) {
			saveInitialPercepts(percepts);
		}

		// Delete previous step percepts
		lastActionParams = new ArrayList<>();
		hitFrom = null;
		attachedThingsPercepts = new ArrayList<>();
		dispensers = new ArrayList<>();
		blocks = new ArrayList<>();
		entities = new ArrayList<>();
		occupiedFields = new ArrayList<>();
		goalZoneFields = new ArrayList<>();
		roleZoneFields = new ArrayList<>();
		tasks = new ArrayList<>();
		norms = new ArrayList<>();
		attachedBlocks = new ArrayList<>();

		// Allocate step percepts to variables
		for (Percept percept : percepts) {
			String perceptName = percept.getName();
			switch (perceptName) {
			case "actionID" -> {
				long id = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				if (id > actionID) {
					actionID = id;
				}
				break;
			}
			case "timestamp" -> {
				timestamp = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}
			case "deadline" -> {
				deadline = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}

			case "score" -> {
				currentScore = ((Numeral) percept.getParameters().get(0)).getValue().longValue();
				break;
			}
			case "energy" -> {
				energyLevel = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				break;
			}
			case "deactivated" -> {
				String value = ((Identifier) percept.getParameters().get(0)).getValue();
				if (value.equals("true")) {
					currentlyActive = false;
				} else {
					currentlyActive = true;
				}
				break;
			}
			case "lastAction" -> {
				lastAction = ((Identifier) percept.getParameters().get(0)).getValue();
				break;
			}
			case "lastActionResult" -> {
				lastActionResult = ((Identifier) percept.getParameters().get(0)).getValue();
				break;
			}
			case "lastActionParams" -> {
				Parameter lastParams = percept.getParameters().get(0);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) lastParams).size(); i++) {
					params.add(((ParameterList) lastParams).get(i));
				}
				for (Parameter param : params) {
					if (param instanceof Identifier) {
						String stringParam = ((Identifier) param).getValue();
						lastActionParams.add(stringParam);
						continue;
					}
					if (param instanceof Numeral) {
						Integer intParam = ((Numeral) param).getValue().intValue();
						lastActionParams.add(intParam);
					}
				}
				break;
			}

			case "violations" -> {
				// TODO
				break;
			}
			case "thing" -> {
				String thingType = ((Identifier) percept.getParameters().get(2)).getValue();
				// Maybe Check if x and y are Numeral first
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				RelativeCoordinate relativeCoordinate = new RelativeCoordinate(x, y);
				if (thingType.equals("dispenser")) {
					String type = ((Identifier) percept.getParameters().get(3)).getValue();
					Dispenser dispenser = new Dispenser(relativeCoordinate, type, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, dispenser);
					map.put(relativeCoordinate, dispenser);
					dispensers.add(dispenser);
					break;
				}
				if (thingType.equals("block")) {
					String blockType = ((Identifier) percept.getParameters().get(3)).getValue();
					Block block = new Block(relativeCoordinate, blockType, currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, block);
					map.put(relativeCoordinate, block);
					blocks.add(block);
					occupiedFields.add(relativeCoordinate);
					break;
				}
				if (thingType.equals("entity")) {
					String teamName = ((Identifier) percept.getParameters().get(3)).getValue();
					Entity entity = new Entity(relativeCoordinate, teamName);
					entities.add(entity);
					occupiedFields.add(relativeCoordinate);
					break;
				}
				if (thingType.equals("obstacle")) {
					occupiedFields.add(relativeCoordinate);
					Obstacle obstacle = new Obstacle(currentStep);
//					map.putThisStep(currentAbsolutePos, relativeCoordinate, obstacle); //TODO @Carina -> make current AbsolutePos work. Then we can make a Map. 
					map.put(relativeCoordinate, obstacle);
					break;
				}
				if (thingType.equals("marker")) {
					String info = ((Identifier) percept.getParameters().get(3)).getValue();
					// TODO
					break;
				}
				break;
			}
			case "task" -> {
				String name = ((Identifier) percept.getParameters().get(0)).getValue();
				int deadline = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				int reward = ((Numeral) percept.getParameters().get(2)).getValue().intValue();

				Parameter paramRequirements = percept.getParameters().get(3);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
					params.add(((ParameterList) paramRequirements).get(i));
				}
				List<TaskRequirement> requirements = new ArrayList<>();
				for (Parameter param : params) {
					Parameter paramCoordinateX = ((Function) param).getParameters().get(0);
					Parameter paramCoordinateY = ((Function) param).getParameters().get(1);
					Parameter paramBlockType = ((Function) param).getParameters().get(2);

					int x = ((Numeral) paramCoordinateX).getValue().intValue();
					int y = ((Numeral) paramCoordinateY).getValue().intValue();
					String blockType = ((Identifier) paramBlockType).getValue();

					TaskRequirement requirement = new TaskRequirement(new RelativeCoordinate(x, y), blockType);
					requirements.add(requirement);
				}
				Task task = new Task(name, deadline, reward, requirements);
				tasks.add(task);
				break;
			}
			case "norm" -> {
				String normName = ((Identifier) percept.getParameters().get(0)).getValue();
				int firstStep = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				int lastStep = ((Numeral) percept.getParameters().get(2)).getValue().intValue();
				int punishment = ((Numeral) percept.getParameters().get(4)).getValue().intValue();

				Parameter paramRequirements = percept.getParameters().get(3);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramRequirements).size(); i++) {
					params.add(((ParameterList) paramRequirements).get(i));
				}
				List<NormRequirement> requirements = new ArrayList<>();
				for (Parameter param : params) {
					Parameter paramType = ((Function) param).getParameters().get(0);
					Parameter paramName = ((Function) param).getParameters().get(1);
					Parameter paramQuantity = ((Function) param).getParameters().get(2);
					Parameter paramDetails = ((Function) param).getParameters().get(3);

					String type = ((Identifier) paramType).getValue();
					String name = ((Identifier) paramName).getValue();
					int quantity = ((Numeral) paramQuantity).getValue().intValue();
					String details = ((Identifier) paramDetails).getValue();

					NormRequirement requirement = new NormRequirement(type, name, quantity, details);
					requirements.add(requirement);
				}
				Norm norm = new Norm(normName, firstStep, lastStep, requirements, punishment);
				norms.add(norm);
				break;
			}
			case "attached" -> {
				attachedThingsPercepts.add(percept);
				break;
			}
			case "goalZone" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate goalZoneField = new RelativeCoordinate(x, y);
				goalZoneFields.add(goalZoneField);
				break;
			}
			case "roleZone" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate roleZoneField = new RelativeCoordinate(x, y);
				roleZoneFields.add(roleZoneField);
				break;
			}
			case "role" -> {
				if (percept.getParameters().size() == 1) {
					String roleName = ((Identifier) percept.getParameters().get(0)).getValue();
					if (currentRole == null || !currentRole.getName().equals(roleName)) {
						Role newRole = Role.getRole(roles, roleName);
						currentRole = newRole;
					}
					if (currentRole != null) {
						say("My current role: " + currentRole.getName());
					}
				}
				break;
			}
			case "surveyed" -> {
				// TODO
				break;
			}
			case "hit" -> {
				int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();

				RelativeCoordinate origin = new RelativeCoordinate(x, y);
				hitFrom = origin;
				break;
			}
			}
		}

		// Saves the last Direction where the Agent successfully walked towards
		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
			if (lastAction.equals("move") && lastActionResult.equals("success")) {
				String lastParamsString = lastActionParams.get(0) + "";
				lastParamsString = lastParamsString.replace("[", "");
				lastParamsString = lastParamsString.replace("]", "");
				this.lastMoveDir = lastParamsString;
			}
		}
//		setCurrentAbsolutePos();
		// Identify if agent has blocks directly attached (next to agent) - TODO: needs
		// to be improved since they could be attached to another entity
		for (Percept percept : attachedThingsPercepts) {
			int x = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
			int y = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
			RelativeCoordinate relativeCoordinateAttachedThing = new RelativeCoordinate(x, y);
			if (!relativeCoordinateAttachedThing.isNextToAgent()) {
				continue;
			}

			for (Block block : blocks) {
				RelativeCoordinate relativeCoordinate = block.getRelativeCoordinate();
				if (relativeCoordinate.equals(relativeCoordinateAttachedThing)) {
					say("I probably have a block attached on position " + relativeCoordinate.getX() + "|"
							+ relativeCoordinate.getY());
					attachedBlocks.add(block);
				}
			}
			// TODO: Same should be done with entity and obstacle once variables are
			// implemented
		}
	}

//	We only need this, if we want to use currentAbsolutePos.
	// updates the current absolute Position of the Agent.
//	private void setCurrentAbsolutePos() {
//		if (lastAction != null && lastActionResult != null && lastActionParams != null) {
//			if (lastAction.equals("move") && lastActionResult.equals("success")) {
//				int x = 0;
//				int y = 0;
//				switch (lastMoveDir) {
//				case "n": {
//					y = 1;
//					break;
//				}
//				case "e": {
//					x = 1;
//					break;
//				}
//				case "s": {
//					y = -1;
//					break;
//				}
//				case "w": {
//					x = -1;
//					break;
//				}
//				}
//				currentAbsolutePos = new RelativeCoordinate(currentAbsolutePos.getX() + x,
//						currentAbsolutePos.getY() + y);
//			}
//		}
//
//	}

	private void saveInitialPercepts(List<Percept> percepts) {
		for (Percept percept : percepts) {
			if (percept.getName().equals("team")) {
				say("Saving team name...");
				teamName = ((Identifier) percept.getParameters().get(0)).getValue();
				continue;
			}
			if (percept.getName().equals("teamSize")) {
				say("Saving team size...");
				teamSize = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				continue;
			}
			if (percept.getName().equals("steps")) {
				say("Saving simulation step number...");
				stepsOverall = ((Numeral) percept.getParameters().get(0)).getValue().intValue();
				continue;
			}
			if (percept.getName().equals("role") && percept.getParameters().size() == 6) {
				String roleName = ((Identifier) percept.getParameters().get(0)).getValue();
				int roleVision = ((Numeral) percept.getParameters().get(1)).getValue().intValue();
				double clearChance = ((Numeral) percept.getParameters().get(4)).getValue().doubleValue();
				int clearMaxDist = ((Numeral) percept.getParameters().get(5)).getValue().intValue();

				Parameter paramActions = percept.getParameters().get(2);
				List<Parameter> params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramActions).size(); i++) {
					params.add(((ParameterList) paramActions).get(i));
				}
				List<String> actions = new ArrayList<>();
				for (Parameter param : params) {
					String action = ((Identifier) param).getValue();
					actions.add(action);
				}

				Parameter paramSpeeds = percept.getParameters().get(3);
				params = new ArrayList<>();
				for (int i = 0; i < ((ParameterList) paramSpeeds).size(); i++) {
					params.add(((ParameterList) paramSpeeds).get(i));
				}
				List<Integer> speeds = new ArrayList<>();
				for (Parameter param : params) {
					int speed = ((Numeral) param).getValue().intValue();
					speeds.add(speed);
				}
				Role role = new Role(roleName, roleVision, actions, speeds, clearChance, clearMaxDist);
				say("Saving information for role '" + roleName + "'...");
				roles.add(role);
				continue;
			}
		}
	}

	// Verarbeitet die Informationen zur letzten Aktion und passt die Annahmen des
	// Agenten an
	// TODO: eigentliche Evaluation
	private void evaluateLastAction() {
		switch (lastAction) {
		case "skip":
			break;
		case "move":
			if (this.lastActionResult.equals("success")) {
				Iterator<Object> it = this.lastActionParams.iterator();
				while (it.hasNext()) {
					Parameter dir = (Parameter) it.next();
					if (dir instanceof Identifier) {
						int x = this.currentPos.getX();
						int y = this.currentPos.getY();
						String dirString = ((Identifier) dir).getValue();
						if ((dirString.equals("n") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("w") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x, y + 1));
						} else if ((dirString.equals("s") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("w") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x, y + 1));
						} else if ((dirString.equals("e") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("w") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("n") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x + 1, y));
						} else if ((dirString.equals("w") && this.rotated.equals(Orientation.NORTH))
								|| (dirString.equals("n") && this.rotated.equals(Orientation.EAST))
								|| (dirString.equals("e") && this.rotated.equals(Orientation.SOUTH))
								|| (dirString.equals("s") && this.rotated.equals(Orientation.WEST))) {
							this.setCurrentPosition(new RelativeCoordinate(x - 1, y));
						}
					}
				}
			} else if (this.lastActionResult.equals("partial_success")) {
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					int x = this.currentPos.getX();
					int y = this.currentPos.getY();
					String dirString = ((Identifier) dir).getValue();
					switch (dirString) {
					case "n":
						this.setCurrentPosition(new RelativeCoordinate(x, y + 1));
						break;
					case "s":
						this.setCurrentPosition(new RelativeCoordinate(x, y - 1));
						break;
					case "e":
						this.setCurrentPosition(new RelativeCoordinate(x + 1, y));
						break;
					case "w":
						this.setCurrentPosition(new RelativeCoordinate(x - 1, y));
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
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					RelativeCoordinate pos;
					Cell cell;
					switch (dirString) {
					case "n":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() + 1);
						cell = this.map.get(pos);
						this.attachedBlocksWithPositions.put(pos, cell);
						break;
					case "s":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() - 1);
						cell = this.map.get(pos);
						this.attachedBlocksWithPositions.put(pos, cell);
						break;
					case "e":
						pos = new RelativeCoordinate(this.currentPos.getX() + 1, this.currentPos.getY());
						cell = this.map.get(pos);
						this.attachedBlocksWithPositions.put(pos, cell);
						break;
					case "w":
						pos = new RelativeCoordinate(this.currentPos.getX() - 1, this.currentPos.getY());
						cell = this.map.get(pos);
						this.attachedBlocksWithPositions.put(pos, cell);
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
			default:
				break;
			}
			break;
		case "detach":
			switch (lastActionResult) {
			case "success":
				Parameter dir = (Parameter) this.lastActionParams.get(0);
				if (dir instanceof Identifier) {
					String dirString = ((Identifier) dir).getValue();
					RelativeCoordinate pos;
					switch (dirString) {
					case "n":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() + 1);
						this.attachedBlocks.remove(pos);
						break;
					case "s":
						pos = new RelativeCoordinate(this.currentPos.getX(), this.currentPos.getY() - 1);
						this.attachedBlocks.remove(pos);
						break;
					case "e":
						pos = new RelativeCoordinate(this.currentPos.getX() + 1, this.currentPos.getY());
						this.attachedBlocks.remove(pos);
						break;
					case "w":
						pos = new RelativeCoordinate(this.currentPos.getX() - 1, this.currentPos.getY());
						this.attachedBlocks.remove(pos);
						break;
					default:
						break;
					}
				}
				break;
			case "rotate":
				switch (lastActionResult) {
				case "success":
					Parameter rot = (Parameter) this.lastActionParams.get(0);
					if (rot instanceof Identifier) {
						String rotString = ((Identifier) rot).getValue();
						HashMap<RelativeCoordinate, Cell> temp = new HashMap<RelativeCoordinate, Cell>();
						if (rotString.equals("cw")) {
							this.rotated = Orientation.changeOrientation(rotated, 1);
							for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
								Cell cell = this.attachedBlocksWithPositions.get(key);
								temp.put(new RelativeCoordinate(key.getY(), -key.getX()), cell);
							}
						} else {
							this.rotated = Orientation.changeOrientation(rotated, -1);
							for (RelativeCoordinate key : this.attachedBlocksWithPositions.keySet()) {
								Cell cell = this.attachedBlocksWithPositions.get(key);
								temp.put(new RelativeCoordinate(-key.getY(), key.getX()), cell);
							}
						}
						this.attachedBlocksWithPositions = temp;
					}
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "connect":
				switch (lastActionResult) {
				case "success":
					// Behandlung
					break;
				case "failed_parameter":
					// FEhlerbehandlung
					break;
				case "failed_partner":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				case "failed":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "disconnect":
				switch (lastActionResult) {
				case "success":
					Parameter xCellA = (Parameter) this.lastActionParams.get(0);
					Parameter yCellA = (Parameter) this.lastActionParams.get(1);
					Parameter xCellB = (Parameter) this.lastActionParams.get(2);
					Parameter yCellB = (Parameter) this.lastActionParams.get(3);
					// Bestimmung, ob Blöcke aus der attached-Liste entfernt werden müssen
					break;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "request":
				break;
			case "submit":
				break;
			case "clear":
				switch (lastActionResult) {
				case "success":
					int xCoor = 0;
					int yCoor = 0;
					Parameter xCell = (Parameter) this.lastActionParams.get(0);
					if (xCell instanceof Numeral) {
						xCoor = ((Numeral) xCell).getValue().intValue();
					} else {
						break;
					}
					Parameter yCell = (Parameter) this.lastActionParams.get(1);
					if (yCell instanceof Numeral) {
						yCoor = ((Numeral) yCell).getValue().intValue();
					} else {
						break;
					}
					this.map.remove(new RelativeCoordinate(xCoor, yCoor));
					break;
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_target":
					// Fehlerbehandlung
					break;
				case "failed_resources":
					// Fehlerbehandlung
					break;
				case "failed_location":
					// Fehlerbehandlung
					break;
				case "failed_random":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "adopt":
				switch (lastActionResult) {
				case "success":
					Parameter role = (Parameter) this.lastActionParams.get(0);
					if (role instanceof Identifier) {
						this.roleName = ((Identifier) role).getValue();
					}
				case "failed_parameter":
					// Fehlerbehandlung
					break;
				case "failed_location":
					// Fehlerbehandlung
					break;
				default:
					break;
				}
				break;
			case "survey":
				break;
			default:
				break;
			}

		}
	}

	private void setCurrentPosition(RelativeCoordinate relativeCoordinate) {
		this.currentPos = relativeCoordinate;

	}

	private Action handleError() {

		if (lastAction.equals("move") && !lastActionResult.equals("success") && lastActionParams.size() == 1) {
			// Get direction
			String direction = (String) lastActionParams.get(0);
			say("I got stuck when trying to walk '" + direction + "'");

			RelativeCoordinate desiredField = RelativeCoordinate.getRelativeCoordinate(direction);
			for (RelativeCoordinate relativeCoordinate : occupiedFields) {
				if (relativeCoordinate.equals(desiredField)) {
					say("Reason: field towards direction '" + direction + "' is already occupied");
					// This will ensure that agent will try all possible directions if one step
					// after another fails due to occupied fields
					switch (direction) {
					case "n":
						say("Moving one step east...");
						return new Action("move", new Identifier("e"));

					case "e":
						say("Moving one step south...");
						return new Action("move", new Identifier("s"));

					case "s":
						say("Moving one step west...");
						return new Action("move", new Identifier("w"));

					case "w":
						say("Moving one step north...");
						return new Action("move", new Identifier("n"));
					}
				}
			}
		}
		if (lastAction.equals("attach") && !lastActionResult.equals("success")) {
			String direction = (String) lastActionParams.get(0);
			say("Last attempt to attach failed. Trying to attach...");
			return new Action("attach", new Identifier(direction));
		}
		if (lastAction.equals("rotate") && !lastActionResult.equals("success")) {
			say("Rotation was not succesfull.");
			return moveRandomly(1);
		}
		// TODO: expand error handling
		return moveRandomly(maxStepNum);
	}

	private Action workerStep() {
		// If a block has been requested in the last step, then attach this block
		if (lastAction.equals("request") && lastActionResult.equals("success")) {
			String direction = (String) lastActionParams.get(0);
			say("Block had been successfully requested. Trying to attach...");
			return new Action("attach", new Identifier(direction));
		}
		// This only works for tasks with one block
		// If the agent has a block attached, then either rotate, look for goal zone or
		// submit
		if (!attachedBlocks.isEmpty()) {
			if (!checkIfTaskComplete(tasks.get(0))) {
				// TODO: Add check in which direction rotation is possible (due to obstacles)
				say("Block(s) attached, but task incomplete.");
				say("Rotating in clockwise direction...");
				return new Action("rotate", new Identifier("cw"));
			}
			say("Task '" + tasks.get(0).getName() + "' is complete");
			if (taskSubmissionPossible(tasks.get(0))) {
				String taskName = tasks.get(0).getName();
				say("Submitting task '" + taskName + "'...");
				return new Action("submit", new Identifier(taskName));
			}
			say("Need to look for goal zone");
			// Walk to goal zone
			if (!goalZoneFields.isEmpty()) {
				say("Goal zone identified");
				List<RelativeCoordinate> adjacentGoalZoneFields = getAdjacentGoalZoneFields();
				if (!agentInGoalZone() && adjacentGoalZoneFields.isEmpty()) {
					RelativeCoordinate closestGoalZoneField = RelativeCoordinate.getClosestCoordinate(goalZoneFields);
					int x = closestGoalZoneField.getX();
					int y = closestGoalZoneField.getY();
					// TODO: improve this
					if (y < 0) {
						say("Moving one step north towards goal zone...");
						return new Action("move", new Identifier("n"));
					}
					if (y > 0) {
						say("Moving one step south towards goal zone...");
						return new Action("move", new Identifier("s"));
					}
					if (x < 0) {
						say("Moving one step west towards goal zone...");
						return new Action("move", new Identifier("w"));
					}
					if (x > 0) {
						say("Moving one step east towards goal zone...");
						return new Action("move", new Identifier("e"));
					}
				}
				// Agent is at one of the four corners of a goal zone (outside)
				if (!agentInGoalZone() && adjacentGoalZoneFields.size() == 1) {
					RelativeCoordinate adjacentGoalZoneField = adjacentGoalZoneFields.get(0);
					String direction = adjacentGoalZoneField.getDirectDirection();
					say("Entering goal zone via corner...");
					return new Action("move", new Identifier(direction));
				}
				// Agent is somewhere along the edges of the goal zone (outside)
				if (!agentInGoalZone() && adjacentGoalZoneFields.size() == 2) {
					List<String> directions = new ArrayList<>();
					for (RelativeCoordinate adjacentGoalZoneField : adjacentGoalZoneFields) {
						directions.add(adjacentGoalZoneField.getDirectDirection());
					}
					if (directions.contains("n") && directions.contains("e")) {
						say("Entering goal zone via edge...");
						return new Action("move", new Identifier("e"), new Identifier("n"));
					}
					if (directions.contains("n") && directions.contains("w")) {
						say("Entering goal zone via edge...");
						return new Action("move", new Identifier("w"), new Identifier("n"));
					}
					if (directions.contains("s") && directions.contains("e")) {
						say("Entering goal zone via edge...");
						return new Action("move", new Identifier("e"), new Identifier("s"));
					}
					if (directions.contains("s") && directions.contains("w")) {
						say("Entering goal zone via edge...");
						return new Action("move", new Identifier("w"), new Identifier("s"));
					}
				}
				// This statement is only reached if the task is complete, but cannot be
				// submitted yet and the agent is already inside the goal zone
				if (agentInGoalZone()) {
					say("Already in goal zone");
					// TODO: move in such a way that all blocks are inside the goal zone
					List<String> allowedDirections = new ArrayList<>();
					for (RelativeCoordinate adjacentGoalZoneField : adjacentGoalZoneFields) {
						allowedDirections.add(adjacentGoalZoneField.getDirectDirection());
					}
					return moveRandomly(1, allowedDirections);
				}
			}
			return moveRandomly(maxStepNum);
		}
		// At the moment limited to one type of dispenser/block ('b0')
		String result = checkIfDispenserNext("b0");
		// TODO: add check whether block already lies on top of dispenser
		if (!result.equals("x")) {
			return requestBlock(result);
		}
		String dispenserDirection = lookFor("dispenser", "b0");
		if (!dispenserDirection.equals("x")) {
			say("Dispenser identified. Moving towards dispenser...");
			return new Action("move", new Identifier(dispenserDirection));
		}
		return moveRandomly(maxStepNum);
	}

	private Action explorerStep() {
		ArrayList<String> possibleDirs = getPossibleDirs();
		ArrayList<String> prefDirs = getPreferredDirs();
		say("this is my map: " + map);
		if (possibleDirs != null && possibleDirs.size() != 0) {
			if (prefDirs != null) {
				// flip coin
				// return new Action("move", new Identifier(dir));
				return null;
			}
			if (possibleDirs.size() > 1) { // remove Dir where you came from
				if (lastMoveDir != null) {
					possibleDirs.remove(getOppositeDirection(lastMoveDir));
				}
			}
			return moveRandomly(1, possibleDirs); // TODO adjust 1 to current step length
		}
		return null;
	}

	private ArrayList<String> getPossibleDirs() {
		ArrayList<String> possibleDirs = new ArrayList<String>();
		boolean blockedWayNorth = false;
		boolean blockedWayEast = false;
		boolean blockedWaySouth = false;
		boolean blockedWayWest = false;
		int minDistance = 1000; // TODO set higher than possible sight
		for (RelativeCoordinate pos : occupiedFields) {
			int distance = pos.distanceFromAgent();
			if (distance < minDistance) {
				minDistance = distance;
			}
			String dir = pos.getDirectDirection();
			switch (dir) {
			case ("n"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayNorth = true;
				}
				break;
			}
			case ("e"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayEast = true;
				}
				break;
			}
			case ("s"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWaySouth = true;
				}
				break;
			}
			case ("w"): {
				if (pos.distanceFromAgent() == minDistance) {
					blockedWayWest = true;
				}
				break;
			}
			}
		}
		if (!blockedWayNorth) {
			possibleDirs.add("n");
		}
		if (!blockedWayEast) {
			possibleDirs.add("e");
		}
		if (!blockedWaySouth) {
			possibleDirs.add("s");
		}
		if (!blockedWayWest) {
			possibleDirs.add("w");
		}

		return possibleDirs;
	}

	private ArrayList<String> getPreferredDirs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Checks in clockwise-sequence if there is a dispenser of the required block
	 * type directly next to the agent
	 * 
	 * @param blockType block type of dispenser agent is looking for
	 * @return direction of the dispenser or 'x' if no dispenser of the required
	 *         block type next to the agent
	 */
	private String checkIfDispenserNext(String blockType) {
		if (!dispensers.isEmpty()) {
			for (Dispenser dispenser : dispensers) {
				String type = dispenser.getType();
				if (type.equals(blockType)) {
					RelativeCoordinate relativeCoordinate = dispenser.getRelativeCoordinate();

					if (relativeCoordinate.isOneStepNorth()) {
						return "n";
					}
					if (relativeCoordinate.isOneStepEast()) {
						return "e";
					}
					if (relativeCoordinate.isOneStepSouth()) {
						return "s";
					}
					if (relativeCoordinate.isOneStepWest()) {
						return "w";
					}
				}
			}
		}
		return "x";
	}

	private List<RelativeCoordinate> getAdjacentGoalZoneFields() {
		List<RelativeCoordinate> adjacentGoalZoneFields = new ArrayList<>();
		for (RelativeCoordinate relativeCoordinate : goalZoneFields) {
			if (relativeCoordinate.distanceFromAgent() == 1) {
				adjacentGoalZoneFields.add(relativeCoordinate);
			}
		}
		return adjacentGoalZoneFields;
	}

	private boolean agentInGoalZone() {
		RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
		for (RelativeCoordinate goalZoneField : goalZoneFields) {
			if (goalZoneField.equals(agentPosition)) {
				return true;
			}
		}
		return false;
	}

	private Action requestBlock(String direction) {
		say("Requesting block...");
		return new Action("request", new Identifier(direction));
	}

	private Action moveRandomly(int stepNum) {
		List<String> allowedDirections = new ArrayList<String>(Arrays.asList("n", "e", "s", "w"));
		return moveRandomly(stepNum, allowedDirections);
	}

	private Action moveRandomly(int stepNum, List<String> allowedDirections) {
		Random rand = new Random();
		List<String> randomDirections = new ArrayList<>();
		for (int i = 1; i <= stepNum; i++) {
			String randomDirection = allowedDirections.get(rand.nextInt(allowedDirections.size()));
			randomDirections.add(randomDirection);
		}

		switch (stepNum) {
		case 1 -> {
			String direction = randomDirections.get(0);
			say("Moving one step randomly...");
			return new Action("move", new Identifier(direction));
		}
		case 2 -> {
			String direction1 = randomDirections.get(0);
			String direction2 = randomDirections.get(1);
			if (oppositeDirections(direction1, direction2)) {
				allowedDirections.remove(direction2);
				direction2 = allowedDirections.get(rand.nextInt(allowedDirections.size()));
			}
			say("Moving two steps randomly...");
			return new Action("move", new Identifier(direction1), new Identifier(direction2));
		}
		default -> {
			return null;
		}
		}
	}

	private boolean checkIfTaskComplete(Task task) {
		// Check if all required blocks are attached
		List<TaskRequirement> requirements = task.getRequirements();
		for (TaskRequirement requirement : requirements) {
			boolean requirementFulfilled = false;
			for (Block attachedBlock : attachedBlocks) {
				if (requirement.isFulfilledBy(attachedBlock)) {
					requirementFulfilled = true;
					break;
				}
			}
			if (requirementFulfilled == false) {
				return false;
			}
		}

		// Check if all attached blocks are really needed
		for (Block attachedBlock : attachedBlocks) {
			boolean blockIsRequired = false;
			for (TaskRequirement requirement : requirements) {
				if (requirement.isFulfilledBy(attachedBlock)) {
					blockIsRequired = true;
					break;
				}
			}
			if (blockIsRequired == false) {
				return false;
			}
		}
		return true;
	}

	private boolean agentIsLocked() {
		// TODO
		return false;
	}

	private boolean taskSubmissionPossible(Task task) {
		if (!checkIfTaskComplete(task)) {
			return false;
		}
		List<TaskRequirement> requirements = task.getRequirements();
		for (TaskRequirement requirement : requirements) {
			RelativeCoordinate relativeCoordinateR = requirement.getRelativeCoordinate();

			boolean inGoalZone = false;
			for (RelativeCoordinate goalZoneField : goalZoneFields) {
				if (relativeCoordinateR.equals(goalZoneField)) {
					inGoalZone = true;
					break;
				}
			}
			if (!inGoalZone) {
				return false;
			}
		}
		for (RelativeCoordinate goalZoneField : goalZoneFields) {
			RelativeCoordinate agentPosition = new RelativeCoordinate(0, 0);
			if (goalZoneField.equals(agentPosition)) {
				return true;
			}
		}
		return false;
	}

	private String lookFor(String type, String additionalInfo) {
		switch (type) {
		case "dispenser" -> {
			// Look for dispensers of the required type
			List<Dispenser> dispenserCandidates = new ArrayList<>();
			for (Dispenser dispenser : dispensers) {
				String dispenserType = dispenser.getType();
				if (dispenserType.equals(additionalInfo)) {
					dispenserCandidates.add(dispenser);
				}
			}
			// Select closest dispenser
			if (dispenserCandidates.isEmpty()) {
				return "x";
			} else {
				Dispenser closestDispenser = Dispenser.getClosestDispenser(dispenserCandidates);
				int x = closestDispenser.getRelativeCoordinate().getX();
				int y = closestDispenser.getRelativeCoordinate().getY();
				// TODO: improve this
				if (y < 0) {
					return "n";
				}
				if (y > 0) {
					return "s";
				}
				if (x < 0) {
					return "w";
				}
				if (x > 0) {
					return "e";
				}
			}
		}
		}
		return "x";
	}

	private boolean oppositeDirections(String direction1, String direction2) {
		if ((direction1.equals("n") && direction2.equals("s")) || (direction1.equals("s") && direction2.equals("n"))) {
			return true;
		}
		if ((direction1.equals("e") && direction2.equals("w")) || (direction1.equals("w") && direction2.equals("e"))) {
			return true;
		}
		if ((direction1.equals("cw") && direction2.equals("ccw"))
				|| (direction1.equals("ccw") && direction2.equals("cw"))) {
			return true;
		}
		return false;
	}

	private String getOppositeDirection(String direction) {
		switch (direction) {
		case "n":
			return "s";
		case "e":
			return "w";
		case "s":
			return "n";
		case "w":
			return "e";
		case "cw":
			return "ccw";
		case "ccw":
			return "cw";
		default:
			return null;
		}
	}
}
