package tjenkinson.asteriskLiveComsLib.program.events;

import java.util.Hashtable;
 
public class ChannelAddedEvent extends AsteriskLiveComsEvent {
	
	private Hashtable<String,Object> info;
	public ChannelAddedEvent(Hashtable<String,Object> info) {
		this.info = info;
	}
	
	public Hashtable<String,Object> getInfo() {
		return info;
	}
}
