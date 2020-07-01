package com.shz.gift.algotest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.shz.gift.preps.IMember;
import com.shz.gift.preps.Algo;
import com.shz.gift.preps.EventQueue;
import com.shz.gift.handler.SequentialExecutor;
import com.shz.gift.log.LogImpl;
import com.shz.gift.protocol.ClientRequest;
import com.shz.gift.utils.ElectionLogic;
import com.shz.gift.utils.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trigger {
    Logger logger;

    private final ScheduledExecutorService executor = new SequentialExecutor(new ScheduledThreadPoolExecutor(10));

    private final Random random = new Random();
    List<Member> members = new ArrayList<>();
    private final AtomicInteger successCounter = new AtomicInteger();
    private final AtomicInteger errCounter = new AtomicInteger();
    long nextFail = System.currentTimeMillis() + random.nextInt(10000);

    public void start() throws InterruptedException {
        ElectionLogic.DEFAULT_ELECTION_TIMEOUT = 1000L;
        ElectionLogic.DEFAULT_LEADER_TIMEOUT = 1000L;
        ElectionLogic.PING_LOOP = 100L;
        ElectionLogic.DEFAULT_STALE_MEMBER_TIMEOUT = 1000L;

        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
        System.setProperty("org.slf4j.simpleLogger.logFile", "raft.log");
        logger = LoggerFactory.getLogger(getClass());
        for (int i = 0; i < 3; i++) {
            Algo r = new Algo();
            r.setId("" + i);
            r.setCommitHandler(new HashSetCommits());
            EventQueue q = new EventQueue(r, executor);
            Member ms = new Member(q);
            members.add(ms);
        }

        for (Member m : members) {
            List<IMember> remoteMembers = new ArrayList<>();
            for (Member remoteMember : members) {
                if (m != remoteMember) {
                    RemoteStub ms = new RemoteStub(remoteMember.getRaftListener(), logger, executor);
                    remoteMembers.add(ms);
                    m.getRemotes().add(ms);
                }
            }
            m.getRaftListener().setMembers(remoteMembers);
        }
        for (Member m : members)
            for (RemoteStub rs : m.getRemotes())
                for (Member rm : members) {
                    if (m == rm) continue;
                    if (rm.getRaftListener() == rs.getRaftListener())
                        for (RemoteStub rrs : rm.getRemotes())
                            if (rrs.getRaftListener() == m.getRaftListener())
                                rs.setRouteBack(rrs);

                }
        for (Member m : members) m.getRaftListener().init();

        Member leader = null;
        List<Request> requests = new ArrayList<>();
        int i;
        for (i = 0; i < 3000; i++) {
            chaos();
            leader = getLeader(leader);

            if (leader == null) {
                i--;
                Thread.sleep(1);
                continue;
            }
            Request r = new Request(successCounter, errCounter);
            requests.add(r);
            leader.getRaftListener().handleClientRequest(r, new ClientRequest(r.getPayload()));
            Thread.sleep(leader.getRaftListener().getQueueSize() + 1);
            if (i % 10 == 0)
                System.out.printf("Done: %d %d %d%n", i, successCounter.get(), errCounter.get());

            Thread.sleep(random.nextInt(30));
        }
        for (Member m : members)
            if (!m.getRaftListener().isRunning())
                m.getRaftListener().init();

        for (int c = 0; c < 10; c++) {
            System.out.printf("Done: %d %d %d%n", i, successCounter.get(), errCounter.get());
            Thread.sleep(1000);
        }
        leader = getLeader(null);
        assert leader != null;
        HashSetCommits commits = (HashSetCommits) ((LogImpl) leader.getRaftListener().getAlgo().getILog()).getHandler();
        for (Request r : requests) {
            if (!commits.getCommitSet().contains(r.getPayload())) System.out.println();
            assert r.getErr() != 0 || commits.getCommitSet().contains(r.getPayload()) : "" + r.getErr() + " missing: " + r.getPayload();
        }
    }
    private void chaos() {
        if (System.currentTimeMillis() > nextFail) {
            nextFail = System.currentTimeMillis() + random.nextInt(30000);
            int i = random.nextInt(members.size());
            if (members.get(i).getRaftListener().getAlgo().getRole() != Role.LEADER) {
                i = random.nextInt(members.size());
                if (members.get(i).getRaftListener().getAlgo().getRole() != Role.LEADER) {
                    i = random.nextInt(members.size());
                }
            }
            final Member m = members.get(i);
            System.out.println("stopping: " + m.getRaftListener().getAlgo());
            m.getRaftListener().stop();
            long time = 1;
            executor.schedule(() -> {
                System.out.println("starting: " + m.getRaftListener().getAlgo());
                m.getRaftListener().init();

            }, random.nextInt(6000) + time, TimeUnit.MILLISECONDS);
        }
    }


    private Member getLeader(Member leader) {
        if (leader != null && leader.getRaftListener().getAlgo().getRole() == Role.LEADER && leader.getRaftListener().isRunning()) {
            return leader;
        }
        for (Member m : members) {
            if (m.getRaftListener().getAlgo().getRole() == Role.LEADER && m.getRaftListener().isRunning()) {
                return m;
            }
        }
        return null;
    }
}
