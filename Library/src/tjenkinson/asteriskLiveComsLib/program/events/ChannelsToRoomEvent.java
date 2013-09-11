package tjenkinson.asteriskLiveComsLib.program.events;

import java.util.ArrayList;

public class ChannelsToRoomEvent extends AbstractChannelsEvent {

	public ChannelsToRoomEvent(ArrayList<Integer> ids) {
		super(ids);
	}
}
