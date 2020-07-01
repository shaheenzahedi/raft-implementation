package com.shz.gift.handler;

import com.shz.gift.protocol.AppendRequest;

public interface ICommitHandler {

	/**
	 * Commits the entry. Should NEVER fail. Retry should be handled by CommitHandler implementation
	 * @param entry
	 */
	void commit(AppendRequest entry);
	
	void reject(AppendRequest entry);
}
