package massim.javaagents.world;

public class Block extends Cell {
	
	private String type;
	
	public Block(int lastSeen, String type) {
		super(lastSeen);
		this.type = type;
	}

}
