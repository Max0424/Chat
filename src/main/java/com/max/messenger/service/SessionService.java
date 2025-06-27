package com.max.messenger.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final Map<String, Integer> activeRooms = new ConcurrentHashMap<>();
    private final Set<String> lockedRooms = ConcurrentHashMap.newKeySet();

    public void createRoom(String roomId) {
        activeRooms.put(roomId, 0); // 0 connected clients initially
    }

    public boolean roomExists(String roomId) {
        return activeRooms.containsKey(roomId);
    }

    public boolean isRoomLocked(String roomId) {
        return lockedRooms.contains(roomId);
    }

    public void lockRoom(String roomId) {
        lockedRooms.add(roomId);
    }

    public void incrementClientCount(String roomId) {
        activeRooms.computeIfPresent(roomId, (id, count) -> count + 1);
    }

    public void decrementClientCount(String roomId) {
        activeRooms.computeIfPresent(roomId, (id, count) -> Math.max(0, count - 1));
    }

    public int getClientCount(String roomId) {
        return activeRooms.getOrDefault(roomId, 0);
    }

    public void removeRoomCompletely(String roomId) {
        activeRooms.remove(roomId);
        lockedRooms.remove(roomId);
    }
}

