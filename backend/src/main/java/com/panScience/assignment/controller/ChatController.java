package com.panScience.assignment.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.panScience.assignment.service.ChatService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
public ResponseEntity<?> ask(@RequestBody Map<String, Object> request) {

    String question = (String) request.get("question");
    Long fileId = Long.valueOf(request.get("fileId").toString());

    return ResponseEntity.ok(
        chatService.askQuestion(fileId, question)
    );
}

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam Long fileId, @RequestParam String question) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout

        // delegate to service to write events asynchronously
        new Thread(() -> {
            try {
                chatService.askQuestionStream(fileId, question, emitter);
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }).start();

        return emitter;
    }
}