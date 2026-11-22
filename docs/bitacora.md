# Bitácora — Planeador multiagente con Spring AI y Ollama

**Asignatura:** Aplicaciones basadas en grandes modelos de lenguaje · Universidad El Bosque
**Modelo:** qwen2.5:3b (Ollama en Docker), temperatura 0, CPU · **Repositorio:** _(enlace)_, rama `taller-agentes`

## 1. Integrantes y roles

| Integrante | Semana 1 | Semana 2 | Semana 3 |
|---|---|---|---|
| Daniel David Nieto Llanos | Integrador + Probador | Diseñador de agentes | Integrador + Probador |
| John Fernando Ramírez Carvajal | Diseñador de agentes | Integrador + Probador | Diseñador de agentes |

Grupo de dos integrantes: los tres roles sugeridos se repartieron y rotaron entre ambos.

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

- PC1: el frontend muestra "ollama activo" con qwen2.5:3b. _(evidencia)_
- PC2: `HerramientasTest` pasa las 6 pruebas (las 5 del taller y 1 del reto). _(captura)_
- PC3: `/api/v1/debug/presupuesto` responde $400.000, faltan $50.000. _(captura del log con estimarCosto y calcularSaldo)_

## 3. Preguntas de prueba

| # | Pregunta (resumida) | Esperado | Agentes que llamó el supervisor | ¿Correcta? | Tiempo (s) | Observaciones |
|-|------|-----|-----|---|--|-------------------|
| 1 | Clima Guatavita domingo | clima. Lluvioso, 7–15 °C, 80 % | | | | |
| 2 | 4 amigos, $350.000, Zipaquirá | presupuesto. $400.000, faltan $50.000 | | | | |
| 3 | 2 personas, Villa de Leyva sábado: clima y costo | clima, presupuesto. Soleado 5 %; $230.000 | | | | |
| 4 | 3 personas, $250.000, reservar Suesca sábado (Laura) | clima, presupuesto, reservas. 15 %; $210.000, sobran $40.000; RES-… | | | | |
| 5 | 3 personas, $150.000, reservar Suesca domingo | presupuesto. Faltan $60.000; NO reserva | | | | |
| 6 | ¿Quién ganó el mundial 2022? | ninguno. Explica el alcance | | | | |
| R | 2 personas, Villa de Leyva domingo: actividades y clima | actividades, clima. Museo del Fósil…; soleado 10 % | | | | Reto |

## 4. Traza completa de la pregunta 4

```
(pegar aquí las líneas de docker logs -f pruebachat_backend: [SUPERVISOR] -> ..., [CLIMA] ...,
 [PRESUPUESTO] ..., [RESERVAS] ..., y la respuesta final del frontend)
```

## 5. Comparación con LangGraph

Mismo modelo, mismos datos, mismas preguntas y mismo frontend. La columna de LangGraph sale del taller anterior.

| Criterio | LangGraph | Spring AI |
|---|---|---|
| Líneas de código del flujo multiagente | ≈ 120 (`grafo.py`: estado, Decision, prompts, nodos y grafo) | ≈ 105 (`PlaneadorOrquestador` + `Decision` + prompts del supervisor y sintetizador) |
| ¿Cómo se define el flujo? ¿Qué tan visible es? | Declarativo: nodos, aristas y arista condicional. `draw_mermaid()` dibuja el grafo. | Imperativo: un `while` con `if`. Se entiende leyendo el código y no tiene visualización nativa. |
| Manejo del estado | `TypedDict` compartido con reducers (`operator.add`) | Variables locales (`List`, `EnumSet`) dentro del método |
| Facilidad para depurar | `print` por nodo, pruebas con `.invoke()` y grafo simulado | Logs SLF4J, `HerramientasTest` con JUnit y endpoint `/debug/{agente}` por agente |
| Tiempo promedio por pregunta (s) | 52 (preguntas 1–6) | _(completar)_ |
| Aciertos en las seis preguntas | 4 de 6 (P3 y P6 parciales) | _(completar)_ |
| Integración con sistemas empresariales | Requiere un servicio aparte (FastAPI) | Nativa: inyección de dependencias, `@Primary` detrás de `ModelService`, mismo backend, seguridad y monitoreo de Spring |

**Conclusión:** _(ajustar según sus resultados)_ LangGraph conviene en proyectos donde el flujo agéntico es el producto: prototipos, investigación o flujos con muchas ramas, memoria, `interrupt` y visualización, porque el grafo hace explícito el control y el estado. Spring AI conviene cuando el agente se agrega a un sistema Java empresarial que ya existe: aprovecha la inyección de dependencias, las pruebas JUnit, la seguridad y el despliegue de Spring sin otro servicio. El costo es escribir a mano la orquestación, que con más ramas se vuelve difícil de mantener. En ambos casos la calidad la limita el modelo de 3B y no el framework.

## 6. Reto elegido: Agente de Actividades

**Implementación:**

- `ActividadesTools.recomendarActividades(destino)`, con datos en `DatosPlaneador`.
- Bean `agenteActividades`.
- Valor `ACTIVIDADES` en el enum `Decision.Agente`.
- Registro en el `EnumMap` del orquestador.
- Regla 3 en el prompt del supervisor.
- `MAX_PASOS = 4`.
- Agregado al `AgentesDebugController`.
- Prueba `actividadesDeVillaDeLeyvaConAlias` en `HerramientasTest`.

**Evidencia:** _(captura de `/api/v1/debug/actividades?q=¿Qué hacer en Villa de Leyva?` y traza de la pregunta R)._

## 7. Errores encontrados y soluciones

| Error / síntoma | Causa | Solución |
|---|---|---|
| | | |

## 8. Limitaciones observadas y mitigación

_(Ajustar con lo que observen. En LangGraph se vieron estas tres; verifiquen si se repiten.)_

1. **El enrutamiento es poco confiable con un modelo de 3B.** El supervisor llama agentes innecesarios.
   - *Mitigación:* reglas deterministas en Java antes de consultar al modelo, o un modelo más grande solo para el supervisor.
2. **Hay alucinaciones en los argumentos.** Por ejemplo, un presupuesto inventado que se pasa a `calcularSaldo`, o códigos de reserva inventados por el sintetizador.
   - *Mitigación:* validar en la tool que los valores aparezcan en la pregunta, y verificar que los códigos del sintetizador existan en `DatosPlaneador`.
3. **La latencia es alta y la reserva no tiene confirmación humana.**
   - *Mitigación:* ejecutar en paralelo los agentes independientes (`CompletableFuture`), definir un umbral explícito de lluvia, y confirmar antes de `crearReserva` (human-in-the-loop).
