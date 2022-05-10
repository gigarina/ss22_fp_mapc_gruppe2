package massim.javaagents.world;

public abstract class Cell {
	
	private int lastSeen;
	
	public Cell(int lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	public int getLastSeen() {
		return lastSeen;
	}

}
