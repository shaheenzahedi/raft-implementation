package com.shz.gift.protocol;

import com.shz.gift.preps.IMember;

public class RaftEvent {

	public enum EventType {
		APPEND,
		APPEND_RESPONSE,
		REQUEST_VOTE,
		VOTE_GRANTED,
		LOOP, 
		CLIENT_REQUEST,
		;
	}
	
	private IMember source;
	
	private IMsg clientSource;
	
	private EventType type;
	
	private Object event;

	public RaftEvent(IMember source, EventType type, Object event) {
		super();
		this.source = source;
		this.type = type;
		this.event = event;
	}

	public RaftEvent(IMsg clientSource, EventType type, Object event) {
		super();
		this.clientSource = clientSource;
		this.type = type;
		this.event = event;
	}



	public IMember getSource() {
		return source;
	}

	public EventType getType() {
		return type;
	}

	public Object getEvent() {
		return event;
	}

	public IMsg getClientSource() {
		return clientSource;
	}

	@Override
	public String toString() {
		return "RaftEvent ["
				+ (source != null ? "source=" + source + ", " : "")
				+ (clientSource != null ? "clientSource=" + clientSource + ", "
						: "") + (type != null ? "type=" + type + ", " : "")
				+ (event != null ? "event=" + event : "") + "]";
	}


	
}
