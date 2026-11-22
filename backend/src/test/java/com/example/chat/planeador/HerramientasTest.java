package com.example.chat.planeador;

import com.example.chat.planeador.datos.DatosPlaneador;
import com.example.chat.planeador.herramientas.ActividadesTools;
import com.example.chat.planeador.herramientas.ClimaTools;
import com.example.chat.planeador.herramientas.PresupuestoTools;
import com.example.chat.planeador.herramientas.ReservasTools;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las tools son métodos Java normales: se prueban SIN modelo y SIN levantar Spring.
 * Si estas pruebas fallan, el agente nunca podrá responder bien.
 */
class HerramientasTest {

    private final DatosPlaneador datos = new DatosPlaneador();

    @Test
    void pronosticoAceptaTildesYMayusculas() {
        String r = new ClimaTools(datos).consultarPronostico("Guatavita", "Domingo");
        assertTrue(r.contains("lluvioso"), r);
    }

    @Test
    void costoDeZipaquiraParaCuatro() {
        String r = new PresupuestoTools(datos).estimarCosto("Zipaquirá", 4);
        assertTrue(r.contains("$400.000"), r);
    }

    @Test
    void saldoNegativoDiceCuantoFalta() {
        String r = new PresupuestoTools(datos).calcularSaldo(350000, 400000);
        assertTrue(r.contains("faltan $50.000"), r);
    }

    @Test
    void noReservaSinCupos() {
        String r = new ReservasTools(datos).crearReserva("Chicaque", "sabado", 2, "Ana");
        assertTrue(r.startsWith("No hay cupo"), r);
    }

    @Test
    void destinoDesconocidoListaLosValidos() {
        String r = new ClimaTools(datos).consultarPronostico("Medellín", "sabado");
        assertTrue(r.contains("Destinos válidos"), r);
    }

    /** RETO (Parte 7). */
    @Test
    void actividadesDeVillaDeLeyvaConAlias() {
        String r = new ActividadesTools(datos).recomendarActividades("Leiva");
        assertTrue(r.contains("Museo del Fósil"), r);
    }
}
