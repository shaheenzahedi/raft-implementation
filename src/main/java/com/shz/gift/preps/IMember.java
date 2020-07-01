package com.shz.gift.preps;

import com.shz.gift.protocol.IMsg;

public interface IMember {

    public IMsg getChannel();

    public long getNextIndex();

    public void setNextIndex(long index);

    public long getMatchIndex();

    public void setMatchIndex(long matchIndex);

    public long getLastCommandReceived();

    public void setLastCommandReceived(long time);
}
