package com.max.messenger.controller;

import com.max.messenger.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RoomApiController {

    private final SessionService sessionService;

    public RoomApiController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/create-room")
    public ResponseEntity<Map<String, String>> createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        sessionService.createRoom(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("roomId", roomId);
        response.put("url", "/chat/" + roomId);
        return ResponseEntity.ok(response);
    }
}
