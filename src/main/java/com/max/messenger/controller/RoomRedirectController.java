package com.max.messenger.controller;

import com.max.messenger.service.SessionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class RoomRedirectController {

    private final SessionService sessionService;

    public RoomRedirectController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/chat/{roomId}")
    public Object serveChatPage(@PathVariable String roomId) {

        if (!sessionService.roomExists(roomId)) {
            return new RedirectView("/connecting.html");
        }

        return new RedirectView("/chat.html?roomId=" + roomId);
    }
}

