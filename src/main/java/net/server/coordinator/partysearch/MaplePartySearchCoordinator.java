/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.coordinator.partysearch;

import client.MapleCharacter;
import client.MapleJob;
import config.YamlConfig;
import connection.packets.CWvsContext;
import net.server.audit.locks.MonitoredLockType;
import net.server.audit.locks.MonitoredReadLock;
import net.server.audit.locks.MonitoredReentrantReadWriteLock;
import net.server.audit.locks.MonitoredWriteLock;
import net.server.audit.locks.factory.MonitoredReadLockFactory;
import net.server.audit.locks.factory.MonitoredWriteLockFactory;
import net.server.coordinator.world.MapleInviteCoordinator;
import net.server.coordinator.world.MapleInviteCoordinator.InviteType;
import net.server.world.MapleParty;
import provider.MapleData;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

/**
 * @author Ronan
 */
public class MaplePartySearchCoordinator {

    private static Map<Integer, Set<Integer>> mapNeighbors = fetchNeighbouringMaps();
    private static Map<Integer, MapleJob> jobTable = instantiateJobTable();
    private final MonitoredReentrantReadWriteLock leaderQueueLock = new MonitoredReentrantReadWriteLock(MonitoredLockType.WORLD_PARTY_SEARCH_QUEUE, true);
    private final MonitoredReadLock leaderQueueRLock = MonitoredReadLockFactory.createLock(leaderQueueLock);
    private final MonitoredWriteLock leaderQueueWLock = MonitoredWriteLockFactory.createLock(leaderQueueLock);
    private Map<MapleJob, PartySearchStorage> storage = new HashMap<>();
    private Map<MapleJob, PartySearchEchelon> upcomers = new HashMap<>();
    private List<MapleCharacter> leaderQueue = new LinkedList<>();
    private Map<Integer, MapleCharacter> searchLeaders = new HashMap<>();
    private Map<Integer, LeaderSearchMetadata> searchSettings = new HashMap<>();
    private Map<MapleCharacter, LeaderSearchMetadata> timeoutLeaders = new HashMap<>();
    private int updateCount = 0;

    public MaplePartySearchCoordinator() {
        for (MapleJob job : jobTable.values()) {
            storage.put(job, new PartySearchStorage());
            upcomers.put(job, new PartySearchEchelon());
        }
    }

    private static Map<Integer, Set<Integer>> fetchNeighbouringMaps() {
        Map<Integer, Set<Integer>> mapLinks = new HashMap<>();

        MapleData data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "Etc.wz")).getData("MapNeighbors.img");
        if (data != null) {
            for (MapleData mapdata : data.getChildren()) {
                int mapid = Integer.parseInt(mapdata.getName());

                Set<Integer> neighborMaps = new HashSet<>();
                mapLinks.put(mapid, neighborMaps);

                for (MapleData neighbordata : mapdata.getChildren()) {
                    int neighborid = MapleDataTool.getInt(neighbordata, 999999999);

                    if (neighborid != 999999999) {
                        neighborMaps.add(neighborid);
                    }
                }
            }
        }

        return mapLinks;
    }

    public static boolean isInVicinity(int callerMapid, int calleeMapid) {
        Set<Integer> vicinityMapids = mapNeighbors.get(calleeMapid);

        if (vicinityMapids != null) {
            return vicinityMapids.contains(calleeMapid);
        } else {
            int callerRange = callerMapid / 10000000;
            if (callerRange >= 90) {
                return callerRange == (calleeMapid / 1000000);
            } else {
                return callerRange == (calleeMapid / 10000000);
            }
        }
    }

    private static Map<Integer, MapleJob> instantiateJobTable() {
        Map<Integer, MapleJob> table = new HashMap<>();

        List<Pair<Integer, Integer>> jobSearchTypes = new LinkedList<>() {{
            add(new Pair<>(MapleJob.MAPLELEAF_BRIGADIER.getId(), 0));
            add(new Pair<>(0, 0));
            add(new Pair<>(MapleJob.ARAN1.getId(), 0));
            add(new Pair<>(100, 3));
            add(new Pair<>(MapleJob.DAWNWARRIOR1.getId(), 0));
            add(new Pair<>(200, 3));
            add(new Pair<>(MapleJob.BLAZEWIZARD1.getId(), 0));
            add(new Pair<>(500, 2));
            add(new Pair<>(MapleJob.THUNDERBREAKER1.getId(), 0));
            add(new Pair<>(400, 2));
            add(new Pair<>(MapleJob.NIGHTWALKER1.getId(), 0));
            add(new Pair<>(300, 2));
            add(new Pair<>(MapleJob.WINDARCHER1.getId(), 0));
            add(new Pair<>(MapleJob.EVAN1.getId(), 0));
        }};

        int i = 0;
        for (Pair<Integer, Integer> p : jobSearchTypes) {
            table.put(i, MapleJob.getById(p.getLeft()).orElseThrow());
            i++;

            for (int j = 1; j <= p.getRight(); j++) {
                table.put(i, MapleJob.getById(p.getLeft() + 10 * j).orElseThrow());
                i++;
            }
        }

        return table;
    }

    private static MapleJob getPartySearchJob(MapleJob job) {
        if (job.getJobNiche() == 0) {
            return MapleJob.BEGINNER;
        } else if (job.getId() < 600) { // explorers
            return MapleJob.getById((job.getId() / 10) * 10).orElseThrow();
        } else if (job.getId() >= 1000) {
            return MapleJob.getById((job.getId() / 100) * 100).orElseThrow();
        } else {
            return MapleJob.MAPLELEAF_BRIGADIER;
        }
    }

    public void attachPlayer(MapleCharacter chr) {
        upcomers.get(getPartySearchJob(chr.getJob())).attachPlayer(chr);
    }

    public void detachPlayer(MapleCharacter chr) {
        MapleJob psJob = getPartySearchJob(chr.getJob());

        if (!upcomers.get(psJob).detachPlayer(chr)) {
            storage.get(psJob).detachPlayer(chr);
        }
    }

    public void updatePartySearchStorage() {
        for (Entry<MapleJob, PartySearchEchelon> psUpdate : upcomers.entrySet()) {
            storage.get(psUpdate.getKey()).updateStorage(psUpdate.getValue().exportEchelon());
        }
    }

    private Optional<MapleCharacter> fetchPlayer(int callerCid, int callerMapid, MapleJob job, int minLevel, int maxLevel) {
        return storage.get(getPartySearchJob(job)).callPlayer(callerCid, callerMapid, minLevel, maxLevel);
    }

    private void addQueueLeader(MapleCharacter leader) {
        leaderQueueRLock.lock();
        try {
            leaderQueue.add(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void removeQueueLeader(MapleCharacter leader) {
        leaderQueueRLock.lock();
        try {
            leaderQueue.remove(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    public void registerPartyLeader(MapleCharacter leader, int minLevel, int maxLevel, int jobs) {
        if (searchLeaders.containsKey(leader.getId())) {
            return;
        }

        searchSettings.put(leader.getId(), new LeaderSearchMetadata(minLevel, maxLevel, jobs));
        searchLeaders.put(leader.getId(), leader);
        addQueueLeader(leader);
    }

    private void registerPartyLeader(MapleCharacter leader, LeaderSearchMetadata settings) {
        if (searchLeaders.containsKey(leader.getId())) {
            return;
        }

        searchSettings.put(leader.getId(), settings);
        searchLeaders.put(leader.getId(), leader);
        addQueueLeader(leader);
    }

    public void unregisterPartyLeader(MapleCharacter leader) {
        MapleCharacter toRemove = searchLeaders.remove(leader.getId());
        if (toRemove != null) {
            removeQueueLeader(toRemove);
            searchSettings.remove(leader.getId());
        } else {
            unregisterLongTermPartyLeader(leader);
        }
    }

    private Optional<MapleCharacter> searchPlayer(MapleCharacter leader) {
        LeaderSearchMetadata settings = searchSettings.get(leader.getId());
        if (settings != null) {
            int minLevel = settings.minLevel, maxLevel = settings.maxLevel;
            Collections.shuffle(settings.searchedJobs);

            int leaderCid = leader.getId();
            int leaderMapid = leader.getMapId();
            for (MapleJob searchJob : settings.searchedJobs) {
                Optional<MapleCharacter> chr = fetchPlayer(leaderCid, leaderMapid, searchJob, minLevel, maxLevel);
                if (chr.isPresent()) {
                    return chr;
                }
            }
        }

        return Optional.empty();
    }

    private boolean sendPartyInviteFromSearch(MapleCharacter chr, MapleCharacter leader) {
        if (chr == null) {
            return false;
        }

        Optional<Integer> partyId = leader.getPartyId();
        if (partyId.isEmpty()) {
            return false;
        }

        if (MapleInviteCoordinator.createInvite(InviteType.PARTY, leader, partyId.get(), chr.getId())) {
            chr.disablePartySearchInvite(leader.getId());
            chr.announce(CWvsContext.partySearchInvite(leader));
            return true;
        } else {
            return false;
        }
    }

    private Pair<List<MapleCharacter>, List<MapleCharacter>> fetchQueuedLeaders() {
        List<MapleCharacter> queuedLeaders, nextLeaders;

        leaderQueueWLock.lock();
        try {
            int splitIdx = Math.min(leaderQueue.size(), 100);

            queuedLeaders = new LinkedList<>(leaderQueue.subList(0, splitIdx));
            nextLeaders = new LinkedList<>(leaderQueue.subList(splitIdx, leaderQueue.size()));
        } finally {
            leaderQueueWLock.unlock();
        }

        return new Pair<>(queuedLeaders, nextLeaders);
    }

    private void registerLongTermPartyLeaders(List<Pair<MapleCharacter, LeaderSearchMetadata>> recycledLeaders) {
        leaderQueueRLock.lock();
        try {
            for (Pair<MapleCharacter, LeaderSearchMetadata> p : recycledLeaders) {
                timeoutLeaders.put(p.getLeft(), p.getRight());
            }
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void unregisterLongTermPartyLeader(MapleCharacter leader) {
        leaderQueueRLock.lock();
        try {
            timeoutLeaders.remove(leader);
        } finally {
            leaderQueueRLock.unlock();
        }
    }

    private void reinstateLongTermPartyLeaders() {
        Map<MapleCharacter, LeaderSearchMetadata> timeoutLeadersCopy;
        leaderQueueWLock.lock();
        try {
            timeoutLeadersCopy = new HashMap<>(timeoutLeaders);
            timeoutLeaders.clear();
        } finally {
            leaderQueueWLock.unlock();
        }

        for (Entry<MapleCharacter, LeaderSearchMetadata> e : timeoutLeadersCopy.entrySet()) {
            registerPartyLeader(e.getKey(), e.getValue());
        }
    }

    public void runPartySearch() {
        Pair<List<MapleCharacter>, List<MapleCharacter>> queuedLeaders = fetchQueuedLeaders();

        List<MapleCharacter> searchedLeaders = new LinkedList<>();
        List<MapleCharacter> recalledLeaders = new LinkedList<>();
        List<MapleCharacter> expiredLeaders = new LinkedList<>();

        for (MapleCharacter leader : queuedLeaders.getLeft()) {
            Optional<MapleCharacter> chr = searchPlayer(leader);
            if (chr.isPresent() && sendPartyInviteFromSearch(chr.get(), leader)) {
                searchedLeaders.add(leader);
                return;
            }

            LeaderSearchMetadata settings = searchSettings.get(leader.getId());
            if (settings != null) {
                if (settings.reentryCount < YamlConfig.config.server.PARTY_SEARCH_REENTRY_LIMIT) {
                    settings.reentryCount += 1;
                    recalledLeaders.add(leader);
                } else {
                    expiredLeaders.add(leader);
                }
            }
        }

        leaderQueueRLock.lock();
        try {
            leaderQueue.clear();
            leaderQueue.addAll(queuedLeaders.getRight());

            try {
                leaderQueue.addAll(25, recalledLeaders);
            } catch (IndexOutOfBoundsException e) {
                leaderQueue.addAll(recalledLeaders);
            }
        } finally {
            leaderQueueRLock.unlock();
        }

        for (MapleCharacter leader : searchedLeaders) {
            Optional<MapleParty> party = leader.getParty();
            if (party.isPresent() && party.map(MapleParty::getMembers).map(Collection::size).orElse(6) < 6) {
                addQueueLeader(leader);
            } else {
                if (leader.isLoggedinWorld()) {
                    leader.dropMessage(5, "Your Party Search token session has finished as your party reached full capacity.");
                }
                searchLeaders.remove(leader.getId());
                searchSettings.remove(leader.getId());
            }
        }

        List<Pair<MapleCharacter, LeaderSearchMetadata>> recycledLeaders = new LinkedList<>();
        for (MapleCharacter leader : expiredLeaders) {
            searchLeaders.remove(leader.getId());
            LeaderSearchMetadata settings = searchSettings.remove(leader.getId());

            if (leader.isLoggedinWorld()) {
                if (settings != null) {
                    recycledLeaders.add(new Pair<>(leader, settings));
                    if (YamlConfig.config.server.USE_DEBUG && leader.isGM()) {
                        leader.dropMessage(5, "Your Party Search token session is now on waiting queue for up to 7 minutes, to get it working right away please stop your Party Search and retry again later.");
                    }
                } else {
                    leader.dropMessage(5, "Your Party Search token session expired, please stop your Party Search and retry again later.");
                }
            }
        }

        if (!recycledLeaders.isEmpty()) {
            registerLongTermPartyLeaders(recycledLeaders);
        }

        updateCount++;
        if (updateCount % 77 == 0) {
            reinstateLongTermPartyLeaders();
        }
    }

    private class LeaderSearchMetadata {
        private int minLevel;
        private int maxLevel;
        private List<MapleJob> searchedJobs;

        private int reentryCount;

        private LeaderSearchMetadata(int minLevel, int maxLevel, int jobs) {
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.searchedJobs = decodeSearchedJobs(jobs);
            this.reentryCount = 0;
        }

        private List<MapleJob> decodeSearchedJobs(int jobsSelected) {
            List<MapleJob> searchedJobs = new LinkedList<>();

            int topByte = (int) ((Math.log(jobsSelected) / Math.log(2)) + 1e-5);

            for (int i = 0; i <= topByte; i++) {
                if (jobsSelected % 2 == 1) {
                    MapleJob job = jobTable.get(i);
                    if (job != null) {
                        searchedJobs.add(job);
                    }
                }

                jobsSelected = jobsSelected >> 1;
                if (jobsSelected == 0) {
                    break;
                }
            }

            return searchedJobs;
        }

    }

}