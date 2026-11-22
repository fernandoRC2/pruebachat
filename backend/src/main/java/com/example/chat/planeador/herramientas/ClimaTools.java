package com.example.chat.planeador.herramientas;

import com.example.chat.planeador.datos.DatosPlaneador;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Tools del AGENTE DE CLIMA.
 *
 * @Tool convierte un método Java en una herramienta que el LLM puede pedir:
 * - el nombre del método es el nombre de la tool,
 * - description es lo que lee el modelo para decidir cuándo usarla,
 * - los parámetros (y @ToolParam) definen el esquema JSON de los argumentos.
 * El modelo solo PROPONE la llamada; quien ejecuta el método es Spring AI.
 */
@Component
public class ClimaTools {

    private final DatosPlaneador datos;

    public ClimaTools(DatosPlaneador datos) {
        this.datos = datos;
    }

    @Tool(description = "Lista los destinos de fin de semana disponibles cerca de Bogotá.")
    public String listarDestinos() {
        return "Destinos disponibles: " + datos.destinosValidos();
    }

    @Tool(description = "Consulta el pronóstico del clima de un destino para el sábado o el domingo.")
    public String consultarPronostico(
            @ToolParam(description = "Nombre del destino, por ejemplo Guatavita") String destino,
            @ToolParam(description = "sabado o domingo") String dia) {
        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s.".formatted(destino, datos.destinosValidos());
        }
        String d = DatosPlaneador.normalizarDia(dia);
        if (d == null) {
            return "Solo tengo pronóstico para 'sabado' o 'domingo'.";
        }
        DatosPlaneador.Pronostico p = datos.pronostico(clave, d);
        return "%s el %s: %s, entre %d °C y %d °C, probabilidad de lluvia %d %%."
                .formatted(datos.destino(clave).nombre(), d, p.cielo(), p.min(), p.max(), p.lluvia());
    }
}
