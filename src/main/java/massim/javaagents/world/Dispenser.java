package massim.javaagents.world;

public class Dispenser extends Cell {
	
	private String type;
	
	public Dispenser(int lastSeen, String type) {
		super(lastSeen);
		this.type = type;
	}

}
