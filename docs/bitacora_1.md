# Bitácora — Planeador multiagente con Spring AI y Ollama

**Asignatura:** Aplicaciones basadas en grandes modelos de lenguaje · Universidad El Bosque
**Modelo:** qwen2.5:3b (Ollama en Docker), temperatura 0, CPU · **Repositorio:** _(enlace)_, rama `taller-agentes`

## 1. Integrantes y roles

| Integrante | Semana 1 | Semana 2 | Semana 3 |
|---|---|---|---|
| José David Castellanos Mendoza | Integrador/a | Diseñador/a de agentes | Probador/a |
| Daniel David Nieto Llanos | Diseñador/a de agentes | Probador/a | Integrador/a |
| John Fernando Ramírez Carvajal | Probador/a | Integrador/a | Diseñador/a de agentes |

## 2. Diagrama del flujo multiagente

```
pregunta ─► [SUPERVISOR] ──(Decision: CLIMA | PRESUPUESTO | RESERVAS | ACTIVIDADES | FIN)
                 ▲                │
                 │                ├─► agenteClima ─────────┐
                 │                ├─► agentePresupuesto ───┤
                 │                ├─► agenteReservas ──────┤
                 │                ├─► agenteActividades ───┤  (reto)
                 └────────────────┼────────────────────────┘
                                  └─ FIN ─► [SINTETIZADOR] ─► respuesta
```

`PlaneadorOrquestador.planear()` implementa el flujo como un ciclo `while`. El estado son las variables locales `resultados` (List) y `consultados` (EnumSet). Hay dos límites de autonomía: `MAX_PASOS = 4` y el supervisor no puede repetir un agente.

**Puntos de control:**

- PC1: el build de Docker terminó y el backend arrancó con Spring Boot 4.1.0 y Spring AI 2.0. El primer build tardó unos 30 min por la descarga de imágenes y dependencias.
- PC2: `gradle test` (ejecutado con la imagen `gradle:9-jdk21` en Docker) dio **BUILD SUCCESSFUL en 4 min 27 s**. Compilaron todas las clases y pasaron las 6 pruebas de `HerramientasTest` (5 del taller y 1 del reto).
- PC3: en `/api/v1/debug/presupuesto` el log muestra `estimarCosto` y `calcularSaldo`, pero la respuesta fue **incorrecta**: "costo total $1.200.000, faltan $850.000". La tool devuelve $400.000 para 4 personas, así que el modelo pasó mal los argumentos o no usó el resultado. `/api/v1/debug/actividades` llamó a `recomendarActividades` y respondió bien.

## 3. Preguntas de prueba

| # | Pregunta (resumida) | Esperado | Agentes que llamó el supervisor | ¿Correcta? | Tiempo (s) | Observaciones |
|-|------|-----|-----|---|--|-------------------|
| 1 | Clima Guatavita domingo | clima. Lluvioso, 7–15 °C, 80 % | clima, **reservas** | Parcial | 58,5 | Dijo "lluvioso" pero omitió temperaturas y el 80 %. Reservas fue innecesario y terminó preguntando "¿Deseas reservar?". Volvió a elegir reservas y la protección contra ciclos cortó. |
| 2 | 4 amigos, $350.000, Zipaquirá | presupuesto. $400.000, faltan $50.000 | **reservas** | **No** | 29,2 | Nunca llamó a presupuesto. La respuesta dice que **sí** alcanza e inventa lugares ("Parque de las Flores"). |
| 3 | 2 personas, Villa de Leyva sábado: clima y costo | clima, presupuesto. Soleado 5 %; $230.000 | **reservas**, presupuesto, clima | Parcial | 75,9 | Clima y $230.000 correctos. Presupuesto **inventó $500.000** (sobran $270.000). Reservas fue innecesario. |
| 4 | 3 personas, $250.000, reservar Suesca sábado (Laura) | clima, presupuesto, reservas. 15 %; $210.000, sobran $40.000; RES-… | presupuesto, clima, reservas | **No** | 84,1 | Costo, saldo y clima correctos. **No creó la reserva**: el modelo escribió la llamada como texto (`CrearReserva Carga: {...}`) en vez de invocar la tool, y no hay código RES. |
| 5 | 3 personas, $150.000, reservar Suesca domingo | presupuesto. Faltan $60.000; NO reserva | presupuesto, **clima** | Sí | 54,6 | $210.000, faltan $60.000, y no reservó. Clima fue innecesario y devolvió una respuesta vacía. |
| 6 | ¿Quién ganó el mundial 2022? | ninguno. Explica el alcance | **reservas** | Parcial | 20,1 | El supervisor eligió RESERVAS con motivo "FIN". La respuesta final sí explica el alcance. |
| R | 2 personas, Villa de Leyva domingo: actividades y clima | actividades, clima. Museo del Fósil…; soleado 10 % | **reservas**, actividades, clima | **No** | 64,8 | El clima es correcto (11–24 °C). El agente de actividades **no llamó a su tool**: respondió con una pregunta y no aparecen actividades. |

**Resumen:** 1 correcta, 3 parciales y 3 incorrectas. Promedio de las preguntas 1–6: 54 s.

Hay un patrón claro: el supervisor elige **RESERVAS primero** en 5 de 7 preguntas. El campo `motivo` casi siempre trae solo una palabra (CLIMA, PRESUPUESTO, FIN) y no una explicación. Los cálculos que salieron de las tools fueron exactos; los errores son de decisión, de argumentos inventados o de tools que no se invocaron.

## 4. Traza completa de la pregunta 4

```
21:05:47 Planeador multiagente. sessionId=40590430-...
21:05:52 [SUPERVISOR] -> PRESUPUESTO (CLIMA)
21:05:56 Executing tool call: estimarCosto
21:06:03 Executing tool call: calcularSaldo
21:06:10 [PRESUPUESTO] El costo total para ir a Suesca para 3 personas es $210.000.
         Con un presupuesto de $250.000, alcanza y sobra $40.000.
21:06:15 [SUPERVISOR] -> CLIMA (RESERVAS)
21:06:22 Executing tool call: consultarPronostico
21:06:29 [CLIMA] Consulta del pronóstico para Suesca para el sábado: soleado, entre 7 °C
         y 21 °C, probabilidad de lluvia del 15 %. Recomiendo llevar ropa ligera y
         probablemente un impermeable.
21:06:33 [SUPERVISOR] -> RESERVAS (RESERVAS)
21:06:41 Executing tool call: consultarDisponibilidad
21:06:47 [RESERVAS] Puedo reservar para ti. CrearReserva Carga: {"destino": "Suesca",
         "dia": "sabado", "personas": 3, "responsable": "Laura"}
21:06:51 [SUPERVISOR] -> RESERVAS (RESERVAS)   -> agente repetido: pasa al sintetizador

Respuesta (84 s): El costo total para ir a Suesca para 3 personas es $210.000. Con tu
presupuesto de $250.000, alcanza y sobra $40.000. El clima para el sábado es soleado,
entre 7 °C y 21 °C, con una probabilidad del 15 % de lluvia. [...] Puedo reservar para
ti. CrearReserva Carga: {"destino": "Suesca", "dia": "sabado", "personas": 3, "responsable": "Laura"}.
```

El orden respetó la regla 2: las condiciones primero y RESERVAS de último. Pero `crearReserva` nunca se ejecutó, porque en el log solo aparece `consultarDisponibilidad`. El sintetizador mostró al usuario la llamada en texto crudo.

## 5. Comparación con LangGraph

Mismo modelo (qwen2.5:3b, temperatura 0), mismos datos y mismas preguntas.

| Criterio | LangGraph | Spring AI |
|---|---|---|
| Líneas de código del flujo multiagente | ≈ 120 (`grafo.py`: estado, Decision, prompts, nodos y grafo) | ≈ 105 (`PlaneadorOrquestador` + `Decision` + prompts del supervisor y sintetizador) |
| ¿Cómo se define el flujo? ¿Qué tan visible es? | Declarativo: nodos, aristas y arista condicional. `draw_mermaid()` dibuja el grafo. | Imperativo: un `while` con `if`. Se entiende leyendo el código y no tiene visualización nativa. |
| Manejo del estado | `TypedDict` compartido con reducers (`operator.add`) | Variables locales (`List`, `EnumSet`) |
| Facilidad para depurar | `print` por nodo y prueba del grafo con modelos simulados | Log DEBUG de Spring AI (muestra cada tool ejecutada, no sus argumentos), JUnit y endpoint `/debug/{agente}` |
| Tiempo promedio por pregunta (s) | 52 | 54 |
| Aciertos en las seis preguntas | 4 correctas, 2 parciales | 1 correcta, 3 parciales, 2 incorrectas |
| Integración con sistemas empresariales | Requiere un servicio aparte (FastAPI) | Nativa: `@Primary` detrás de `ModelService`, sin tocar ChatController ni frontend |

**Por qué Spring acertó menos con el mismo modelo.**

- **El esquema del supervisor.** `with_structured_output` en LangGraph envía las descripciones de los campos (`Field(description=...)`). El `record Decision` de Spring no tiene descripciones, así que el modelo no sabe qué poner en `motivo` y repite el nombre del agente. Además se sesga hacia RESERVAS.
- **La tool escrita como texto.** En la pregunta 4 el modelo devolvió la llamada a la tool como texto. El ciclo de tools de Spring no la reconoció como `tool_call`.

**Conclusión.** Elegiríamos **LangGraph** cuando el flujo agéntico es el núcleo del proyecto: prototipos, investigación o flujos con muchas ramas, memoria o confirmación humana. El grafo hace explícitos el control y el estado, y en nuestras pruebas fue más confiable. Elegiríamos **Spring AI** cuando el agente se agrega a un sistema Java que ya existe. Se conectó detrás de `ModelService` sin cambiar el controlador ni el frontend, y aprovecha la inyección de dependencias, JUnit y el despliegue de Spring. A cambio, hay que escribir la orquestación a mano y cuidar más el esquema de la salida estructurada. Con un modelo de 3B, el framework influye menos en la calidad que los prompts y los esquemas.

## 6. Reto elegido: Agente de Actividades

**Implementación:**

- `ActividadesTools.recomendarActividades(destino)`, con datos en `DatosPlaneador`.
- Bean `agenteActividades`.
- Valor `ACTIVIDADES` en `Decision.Agente`.
- Registro en el `EnumMap` del orquestador.
- Regla 3 en el prompt del supervisor.
- `MAX_PASOS = 4`.
- Agregado al `AgentesDebugController`.
- Prueba JUnit `actividadesDeVillaDeLeyvaConAlias`.

**Evidencia:**

- `GET /api/v1/debug/actividades?q=¿Qué hacer en Villa de Leyva?` ejecutó `recomendarActividades` (aparece en el log) y respondió: caminata por la plaza principal, Museo del Fósil, pozos azules y viñedos, gastronomía local.
- En la pregunta R el supervisor sí enrutó a ACTIVIDADES. Pero el agente, al recibir el contexto con los resultados de otros agentes, no llamó la tool y respondió con una pregunta. Funciona aislado y falla dentro del flujo completo.

## 7. Errores encontrados y soluciones

| Error / síntoma | Causa | Solución |
|---|---|---|
| El `AgentesModelService` del PDF no compilaba contra el proyecto | Aquí `ModelService` es `ChatResponse generateResponse(sessionId, question)` | Se adaptó para envolver la respuesta del orquestador en el DTO `ChatResponse` |
| El primer `docker compose up --build` tardó unos 30 min | Descarga de imágenes base y dependencias de Gradle | Esperar. Los builds siguientes usan caché. |
| El supervisor repite un agente ya consultado | El modelo ignora "Agentes ya consultados" | La protección contra ciclos del `while` corta el flujo (P1, P2, P3, P4, P5 y R) |
| `crearReserva` no se ejecuta (P4) | El modelo escribe la llamada como texto | Pendiente: reforzar el prompt, o validar en el orquestador que una reserva pedida tenga código RES |

## 8. Limitaciones observadas y cómo mitigarlas

1. **El enrutamiento es poco confiable.** El supervisor elige RESERVAS primero aunque no se pida, y su `motivo` no explica nada.
   - *Mitigación:* agregar `@JsonPropertyDescription` a los campos de `Decision` para que el JSON Schema lleve descripciones, como en LangGraph. También reglas deterministas en Java, por ejemplo no permitir RESERVAS si la pregunta no contiene "reserv".
2. **Hay argumentos inventados y tools que no se invocan.** Pasó con presupuestos de $500.000 y $1.200.000, con `crearReserva` escrita como texto y con el agente de actividades que no usó su tool. Las tools garantizan la aritmética, pero no que el modelo las use bien.
   - *Mitigación:* validar en la tool que el presupuesto aparezca en la pregunta, verificar en el orquestador que cada resultado venga de una tool ejecutada, y probar un modelo más grande (qwen3.5:4b).
3. **La latencia es alta y no hay confirmación humana.** Tarda de 50 a 85 s por pregunta en CPU, y la reserva (cuando se ejecuta) no pide confirmación.
   - *Mitigación:* ejecutar en paralelo los agentes independientes con `CompletableFuture`, y confirmar antes de `crearReserva` (human-in-the-loop con el `sessionId`).
