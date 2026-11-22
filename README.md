# Pruebachat

This demo shows an Angular frontend and Spring Boot backend integrated with a local model runtime.

## Ollama (local model)
The project uses Ollama as the local model runtime. To start Ollama and related services:

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

To download and run a model inside the running ollama container (example using `llama3`):

```bash
docker exec -it pruebachat_ollama ollama run llama3
```

Note: model names and availability depend on Ollama's model repository and licensing. Ensure you
have the right to download and use the selected model.

## Configuration
The backend reads Ollama settings from `backend/src/main/resources/application.properties`:

```
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.default-model=llama3
```

Adjust the model and base URL as needed.

## Taller: planeador multiagente con Spring AI (rama `taller-agentes`)

Un supervisor (salida estructurada con `.entity(Decision.class)`) coordina cuatro agentes
especialistas (clima, presupuesto, reservas y **actividades**, el reto) y un sintetizador redacta
la respuesta. Se conecta detrás de `ModelService` con `AgentesModelService` (`@Primary`), así que el
ChatController y el frontend no cambian.

### Ejecutar
```bash
docker exec -it pruebachat_ollama ollama pull qwen2.5:3b
docker exec -it pruebachat_ollama ollama show qwen2.5:3b     # Capabilities: tools
docker compose -f docker/docker-compose.yml up -d --build
docker logs -f pruebachat_backend                           # traza del orquestador
```
Abrir http://localhost:4200.

### Probar
- Tools sin modelo: `cd backend && gradle test` (HerramientasTest, 6 pruebas).
- Un agente solo: `http://localhost:8080/api/v1/debug/{clima|presupuesto|reservas|actividades}?q=...`
- Sistema completo: las preguntas de prueba desde el frontend.

### Código
`backend/src/main/java/com/example/chat/planeador/` (datos, herramientas, agentes, Decision,
PlaneadorOrquestador) e `impl/AgentesModelService.java`.
