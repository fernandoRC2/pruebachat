package com.example.chat.impl;

import com.example.chat.ModelService;
import com.example.chat.dto.ChatResponse;
import com.example.chat.planeador.PlaneadorOrquestador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Nueva implementación de ModelService que usa el sistema multiagente.
 *
 * @Primary hace que el ChatController reciba ESTA implementación en lugar de
 * SpringAiModelService (que se conserva para comparar el chat simple vs. el agente).
 * El ChatController y el frontend no cambian: mismo contrato /api/v1/chat.
 *
 * Adaptación al proyecto: aquí ModelService recibe (sessionId, question) y devuelve
 * el DTO ChatResponse completo, así que envolvemos la respuesta del orquestador en él.
 * El orquestador hace varias llamadas al modelo, por eso los tokens se reportan en 0.
 */
@Service
@Primary
public class AgentesModelService implements ModelService {

    private static final Logger log = LoggerFactory.getLogger(AgentesModelService.class);

    private final PlaneadorOrquestador orquestador;

    public AgentesModelService(PlaneadorOrquestador orquestador) {
        this.orquestador = orquestador;
    }

    @Override
    public ChatResponse generateResponse(String sessionId, String question) {
        log.info("Planeador multiagente. sessionId={}", sessionId);
        long inicio = System.currentTimeMillis();
        String requestId = sessionId != null ? sessionId : UUID.randomUUID().toString();
        try {
            String respuesta = orquestador.planear(question);
            long elapsedMs = System.currentTimeMillis() - inicio;
            boolean ok = respuesta != null && !respuesta.isBlank();
            return new ChatResponse(requestId, ok ? respuesta : "", elapsedMs, ok ? "ok" : "error",
                    0, 0, 0, "planeador-multiagente", ok ? "stop" : "error", 0.0);
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - inicio;
            log.error("Error en el planeador multiagente", e);
            return new ChatResponse(requestId, "Error: " + e.getMessage(), elapsedMs, "error",
                    0, 0, 0, "error", "error", 0.0);
        }
    }
}
