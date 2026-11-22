package com.example.chat.planeador.agentes;

import com.example.chat.planeador.herramientas.ActividadesTools;
import com.example.chat.planeador.herramientas.ClimaTools;
import com.example.chat.planeador.herramientas.PresupuestoTools;
import com.example.chat.planeador.herramientas.ReservasTools;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Definición de los agentes.
 *
 * Cada agente = ChatClient + instrucciones (defaultSystem) + herramientas (defaultTools).
 * El ToolCallingAdvisor que Spring AI registra automáticamente ejecuta el ciclo:
 * el modelo pide una tool -> Spring la ejecuta -> el resultado vuelve al modelo...
 * hasta que el modelo responde sin pedir más tools.
 *
 * ChatClient.Builder es un bean "prototype": cada método @Bean recibe un builder NUEVO,
 * por eso podemos crear varios ChatClient independientes a partir del mismo modelo.
 */
@Configuration
public class AgentesConfig {

    static final String PROMPT_CLIMA = """
            Eres el AGENTE DE CLIMA de un planeador de salidas de fin de semana cerca de Bogotá.
            Tu único trabajo es informar el pronóstico del tiempo.
            - Usa SIEMPRE la herramienta consultarPronostico. Nunca inventes datos del clima.
            - Si el usuario no dice el día, consulta sábado y domingo.
            - Ignora las partes de la pregunta que no son de clima (dinero, reservas).
            - Responde en español, en máximo 3 frases, con una recomendación práctica (ropa, impermeable...).
            """;

    static final String PROMPT_PRESUPUESTO = """
            Eres el AGENTE DE PRESUPUESTO de un planeador de salidas de fin de semana.
            Tu único trabajo es calcular cuánto cuesta el plan y si el dinero alcanza.
            - Usa estimarCosto para obtener el costo total. Nunca hagas las cuentas de memoria.
            - Si el usuario dio un presupuesto, usa calcularSaldo con ese presupuesto y el costo total.
            - Escribe los valores en pesos como números enteros (300000, no 300.000) al llamar las herramientas.
            - Ignora las partes de la pregunta que no son de dinero.
            - Responde en español, en máximo 3 frases.
            """;

    static final String PROMPT_RESERVAS = """
            Eres el AGENTE DE RESERVAS de un planeador de salidas de fin de semana.
            Tu único trabajo es consultar cupos y crear reservas.
            - Usa consultarDisponibilidad antes de reservar.
            - Usa crearReserva SOLO si el usuario pidió reservar explícitamente y dio el nombre del responsable.
            - Si el usuario condicionó la reserva (por ejemplo "si no llueve" o "si nos alcanza") y los
              resultados de otros agentes muestran que la condición NO se cumple, NO reserves y explica por qué.
            - Responde en español, en máximo 3 frases. Si reservaste, incluye el código de la reserva.
            """;

    /** RETO (Parte 7): nuevo especialista. */
    static final String PROMPT_ACTIVIDADES = """
            Eres el AGENTE DE ACTIVIDADES de un planeador de salidas de fin de semana cerca de Bogotá.
            Tu único trabajo es recomendar qué hacer en el destino.
            - Usa SIEMPRE la herramienta recomendarActividades. Nunca inventes actividades.
            - Si el usuario menciona un interés (caminar, comer, museos, deporte), prioriza esas actividades.
            - Ignora las partes de la pregunta que no son de actividades (clima, dinero, reservas).
            - Responde en español, en máximo 3 frases.
            """;

    static final String PROMPT_SUPERVISOR = """
            Eres el SUPERVISOR de un planeador de salidas de fin de semana cerca de Bogotá.
            Coordinas a cuatro agentes especialistas:
            - CLIMA: pronóstico del tiempo de un destino para el sábado o el domingo.
            - PRESUPUESTO: costo del plan para un grupo y si el dinero alcanza.
            - RESERVAS: cupos disponibles y creación de reservas.
            - ACTIVIDADES: qué hacer en el destino (senderismo, gastronomía, museos, deportes).

            Lee la pregunta del usuario y los resultados que ya existen, y decide quién trabaja ahora.
            Reglas:
            1. Elige solo agentes que la pregunta realmente necesita y que no hayan sido consultados.
            2. Si el usuario quiere reservar con una condición (clima, dinero), consulta primero esos agentes
               y deja RESERVAS de último.
            3. Elige ACTIVIDADES solo si el usuario pregunta qué hacer, qué visitar o qué actividades hay.
            4. Si ya hay información suficiente, o la pregunta no trata de estos temas, responde FIN.
            """;

    static final String PROMPT_SINTETIZADOR = """
            Eres el asistente final de un planeador de salidas de fin de semana.
            Redacta la respuesta para el usuario usando SOLO los resultados de los agentes.
            - No inventes datos que no estén en los resultados.
            - Organiza la respuesta en un párrafo corto o en viñetas, en español y con tono amable.
            - Si se hizo una reserva, muestra el código.
            - Si no hay resultados, explica que puedes ayudar con clima, presupuesto, reservas y actividades
              para Guatavita, Zipaquirá, Villa de Leyva, Suesca y Chicaque.
            """;

    @Bean
    ChatClient agenteClima(ChatClient.Builder builder, ClimaTools tools) {
        return builder.defaultSystem(PROMPT_CLIMA).defaultTools(tools).build();
    }

    @Bean
    ChatClient agentePresupuesto(ChatClient.Builder builder, PresupuestoTools tools) {
        return builder.defaultSystem(PROMPT_PRESUPUESTO).defaultTools(tools).build();
    }

    @Bean
    ChatClient agenteReservas(ChatClient.Builder builder, ReservasTools tools) {
        return builder.defaultSystem(PROMPT_RESERVAS).defaultTools(tools).build();
    }

    /** RETO (Parte 7). */
    @Bean
    ChatClient agenteActividades(ChatClient.Builder builder, ActividadesTools tools) {
        return builder.defaultSystem(PROMPT_ACTIVIDADES).defaultTools(tools).build();
    }

    /** Sin tools. Con salida estructurada NATIVA: Ollama obliga al modelo a respetar el JSON Schema. */
    @Bean
    ChatClient supervisor(ChatClient.Builder builder) {
        return builder.defaultSystem(PROMPT_SUPERVISOR)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }

    @Bean
    ChatClient sintetizador(ChatClient.Builder builder) {
        return builder.defaultSystem(PROMPT_SINTETIZADOR).build();
    }
}
