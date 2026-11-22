package com.example.chat.planeador;

/**
 * Salida ESTRUCTURADA del supervisor.
 *
 * En lugar de texto libre, el modelo debe devolver un JSON con estos campos.
 * Spring AI genera el JSON Schema a partir del record y convierte la respuesta
 * en un objeto Java con .entity(Decision.class).
 */
public record Decision(Agente siguiente, String motivo) {

    /** Agentes a los que el supervisor puede delegar, o FIN para responder. ACTIVIDADES = reto. */
    public enum Agente { CLIMA, PRESUPUESTO, RESERVAS, ACTIVIDADES, FIN }
}
