package com.techhub.service.notification.impl;

import com.techhub.model.dto.response.notification.NotificationResponse;
import com.techhub.service.notification.NotificationStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationStreamServiceImpl implements NotificationStreamService {

    private static final Long DEFAULT_TIMEOUT = 1_800_000L; // 30 phút

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        log.info("Subscribing to SSE notification stream for userId={}", userId);
        SseEmitter emitter = createEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for userId={}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE emitter timed out for userId={}", userId);
            emitter.complete();
            removeEmitter(userId, emitter);
        });

        emitter.onError(e -> {
            log.warn("SSE emitter error for userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        emittersMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("Connected to notification stream"));
            log.info("SSE connection initialized successfully for userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to send initial CONNECT event for userId={}", userId, e);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    @Override
    public void sendNotification(UUID userId, NotificationResponse notification) {
        if (userId == null || notification == null) {
            log.warn("Cannot send notification: userId or notification payload is null");
            return;
        }

        CopyOnWriteArrayList<SseEmitter> emitters = emittersMap.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE emitters found for userId={}", userId);
            return;
        }

        log.info("Broadcasting notification {} to {} SSE emitter(s) for userId={}", notification.id(), emitters.size(), userId);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("NOTIFICATION")
                        .data(notification));
            } catch (Exception e) {
                log.warn("Failed to send SSE notification to userId={}, removing disconnected emitter: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        }
    }

    @Override
    public void broadcastToAllActive(NotificationResponse notification) {
        if (notification == null || emittersMap.isEmpty()) {
            return;
        }
        log.info("Broadcasting notification {} to all active SSE users ({} connected users)", notification.id(), emittersMap.size());
        for (UUID activeUserId : emittersMap.keySet()) {
            sendNotification(activeUserId, notification);
        }
    }

    @Override
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        if (emittersMap.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, CopyOnWriteArrayList<SseEmitter>> entry : emittersMap.entrySet()) {
            UUID userId = entry.getKey();
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    log.debug("Heartbeat failed for userId={}, removing disconnected emitter: {}", userId, e.getMessage());
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    public int getActiveEmitterCount(UUID userId) {
        CopyOnWriteArrayList<SseEmitter> list = emittersMap.get(userId);
        return (list != null) ? list.size() : 0;
    }

    protected SseEmitter createEmitter(Long timeout) {
        return new SseEmitter(timeout);
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        if (userId == null || emitter == null) {
            return;
        }
        emittersMap.computeIfPresent(userId, (k, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
