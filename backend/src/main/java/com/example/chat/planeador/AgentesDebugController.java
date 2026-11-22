package com.example.chat.planeador;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de DEPURACIÓN: habla con UN agente especialista, sin supervisor.
 * Ejemplo: GET http://localhost:8080/api/v1/debug/presupuesto?q=Somos 4 con 350000, ¿alcanza para Zipaquirá?
 * Útil para probar cada agente por separado y ver en el log las llamadas a tools.
 */
@RestController
@RequestMapping("/api/v1/debug")
public class AgentesDebugController {

    private final Map<String, ChatClient> agentes;

    public AgentesDebugController(@Qualifier("agenteClima") ChatClient clima,
                                  @Qualifier("agentePresupuesto") ChatClient presupuesto,
                                  @Qualifier("agenteReservas") ChatClient reservas,
                                  @Qualifier("agenteActividades") ChatClient actividades) {
        this.agentes = Map.of("clima", clima, "presupuesto", presupuesto,
                "reservas", reservas, "actividades", actividades);
    }

    @GetMapping("/{agente}")
    public ResponseEntity<String> probar(@PathVariable String agente, @RequestParam String q) {
        ChatClient chatClient = agentes.get(agente);
        if (chatClient == null) {
            return ResponseEntity.badRequest()
                    .body("Agente desconocido: " + agente + ". Usa clima, presupuesto, reservas o actividades.");
        }
        return ResponseEntity.ok(chatClient.prompt().user(q).call().content());
    }
}
