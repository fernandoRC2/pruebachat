package com.example.chat.planeador.herramientas;

import com.example.chat.planeador.datos.DatosPlaneador;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** Tools del AGENTE DE RESERVAS. crearReserva MODIFICA datos: es la tool "peligrosa". */
@Component
public class ReservasTools {

    private final DatosPlaneador datos;

    public ReservasTools(DatosPlaneador datos) {
        this.datos = datos;
    }

    @Tool(description = "Consulta cuántos cupos quedan para un destino el sábado o el domingo.")
    public String consultarDisponibilidad(
            @ToolParam(description = "Nombre del destino") String destino,
            @ToolParam(description = "sabado o domingo") String dia) {
        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s.".formatted(destino, datos.destinosValidos());
        }
        String d = DatosPlaneador.normalizarDia(dia);
        if (d == null) {
            return "Solo se puede reservar para 'sabado' o 'domingo'.";
        }
        return "%s el %s: quedan %d cupos.".formatted(datos.destino(clave).nombre(), d, datos.cupos(clave, d));
    }

    @Tool(description = "Crea una reserva para un grupo. Úsala SOLO si el usuario pidió reservar "
            + "y dio el nombre del responsable.")
    public String crearReserva(
            @ToolParam(description = "Nombre del destino") String destino,
            @ToolParam(description = "sabado o domingo") String dia,
            @ToolParam(description = "Número de personas") int personas,
            @ToolParam(description = "Nombre de la persona responsable de la reserva") String responsable) {
        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s.".formatted(destino, datos.destinosValidos());
        }
        String d = DatosPlaneador.normalizarDia(dia);
        if (d == null) {
            return "Solo se puede reservar para 'sabado' o 'domingo'.";
        }
        if (responsable == null || responsable.isBlank()) {
            return "Falta el nombre del responsable de la reserva. Pídeselo al usuario.";
        }
        int cupos = datos.cupos(clave, d);
        if (personas > cupos) {
            return "No hay cupo suficiente: se piden %d y quedan %d.".formatted(personas, cupos);
        }
        DatosPlaneador.Reserva r = datos.reservar(clave, d, personas, responsable.trim());
        return "Reserva confirmada %s: %s, %s, %d personas, a nombre de %s."
                .formatted(r.codigo(), datos.destino(clave).nombre(), d, personas, r.responsable());
    }
}
