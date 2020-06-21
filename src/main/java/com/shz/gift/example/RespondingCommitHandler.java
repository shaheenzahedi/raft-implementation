package com.shz.gift.example;

import com.shz.gift.CommitHandler;
import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.ClientResponse;

public class RespondingCommitHandler implements CommitHandler {

	@Override
	public void commit(AppendRequest entry) {
		Object o = commitImpl(entry);
		if (entry.getClientChannel() != null) {
			entry.getClientChannel().send(null, o);
		}
	}

	protected Object commitImpl(AppendRequest entry) {
		return new ClientResponse(0, null);
	}

	@Override
	public void reject(AppendRequest entry) {
		if (entry.getClientChannel() != null) {
			entry.getClientChannel().send(null, new ClientResponse(1, null));
		}
	}

	
}
