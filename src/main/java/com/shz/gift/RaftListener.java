package com.shz.gift;

import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.AppendResponse;
import com.shz.gift.protocol.ClientRequest;
import com.shz.gift.protocol.MessageChannel;
import com.shz.gift.protocol.RequestForVote;
import com.shz.gift.protocol.VoteGranted;

public interface RaftListener {

	public void voteReceived(ClusterMember member, VoteGranted vote);

	public void append(ClusterMember source, AppendRequest entry);

	public void vote(ClusterMember clusterMember, RequestForVote vote);
	
	public void appendResponse(ClusterMember source, AppendResponse resp);
	
	public void handleClientRequest(MessageChannel client, ClientRequest req);
	
}
