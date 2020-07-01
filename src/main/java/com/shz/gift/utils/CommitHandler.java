package com.shz.gift.utils;

import com.shz.gift.protocol.AppendRequest;

public interface CommitHandler {

	/**
	 * Commits the entry. Should NEVER fail. Retry should be handled by CommitHandler implementation
	 * @param entry
	 */
	public void commit(AppendRequest entry);
	
	public void reject(AppendRequest entry);
}
