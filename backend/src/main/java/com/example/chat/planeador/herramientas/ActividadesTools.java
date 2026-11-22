package com.example.chat.planeador.herramientas;

import com.example.chat.planeador.datos.DatosPlaneador;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** RETO (Parte 7): Tools del AGENTE DE ACTIVIDADES. */
@Component
public class ActividadesTools {

    private final DatosPlaneador datos;

    public ActividadesTools(DatosPlaneador datos) {
        this.datos = datos;
    }

    @Tool(description = "Recomienda actividades para hacer en un destino de fin de semana "
            + "(senderismo, gastronomía, museos, deportes...).")
    public String recomendarActividades(
            @ToolParam(description = "Nombre del destino, por ejemplo Suesca") String destino) {
        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s.".formatted(destino, datos.destinosValidos());
        }
        return "Actividades en %s: %s."
                .formatted(datos.destino(clave).nombre(), String.join("; ", datos.actividades(clave)));
    }
}
