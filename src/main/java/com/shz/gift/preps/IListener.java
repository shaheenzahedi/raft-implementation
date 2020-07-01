package com.shz.gift.preps;
import com.shz.gift.protocol.AppendRequest;
import com.shz.gift.protocol.AppendResponse;
import com.shz.gift.protocol.ClientRequest;
import com.shz.gift.protocol.IMsg;
import com.shz.gift.protocol.RequestForVote;
import com.shz.gift.protocol.VoteGranted;
public interface IListener {
    void voteReceived(IMember member, VoteGranted vote);

    void append(IMember source, AppendRequest entry);

    void vote(IMember clusterMember, RequestForVote vote);

    void appendResponse(IMember source, AppendResponse resp);

    void handleClientRequest(IMsg client, ClientRequest req);
}
