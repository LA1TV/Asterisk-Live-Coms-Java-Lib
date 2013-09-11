package tjenkinson.asteriskLiveComsLib.program.events;

public abstract class AbstractChannelEvent  extends AsteriskLiveComsEvent {
	
	private int id;
	
	public AbstractChannelEvent(int id) {
		this.id = id;
	}
	
	public int getChannelId() {
		return id;
	}
}
