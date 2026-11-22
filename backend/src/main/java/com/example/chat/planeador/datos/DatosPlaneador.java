package com.example.chat.planeador.datos;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Datos simulados (ficticios) del planeador.
 *
 * En un sistema real vendrían de una API de clima, una base de datos de precios
 * y un sistema de reservas. Aquí son mapas en memoria para que el taller funcione
 * sin internet y los resultados se puedan verificar a mano.
 */
@Component
public class DatosPlaneador {

    /** Costos por persona en pesos colombianos (valores ficticios). */
    public record Destino(String nombre, int transporte, int entrada, int alimentacion) {
        public int costoPorPersona() { return transporte + entrada + alimentacion; }
    }

    public record Pronostico(String cielo, int min, int max, int lluvia) {}

    public record Reserva(String codigo, String destino, String dia, int personas, String responsable) {}

    private final Map<String, Destino> destinos = new LinkedHashMap<>();
    private final Map<String, String> alias = new LinkedHashMap<>();
    private final Map<String, Pronostico> pronostico = new LinkedHashMap<>();
    private final Map<String, Integer> cupos = new ConcurrentHashMap<>();
    private final List<Reserva> reservas = new ArrayList<>();
    /** RETO (Parte 7): actividades recomendadas por destino (datos ficticios). */
    private final Map<String, List<String>> actividades = new LinkedHashMap<>();

    public DatosPlaneador() {
        destinos.put("guatavita", new Destino("Laguna de Guatavita", 30000, 25000, 35000));
        destinos.put("zipaquira", new Destino("Catedral de Sal de Zipaquirá", 20000, 50000, 30000));
        destinos.put("villa de leyva", new Destino("Villa de Leyva", 70000, 0, 45000));
        destinos.put("suesca", new Destino("Rocas de Suesca", 25000, 15000, 30000));
        destinos.put("chicaque", new Destino("Parque Natural Chicaque", 20000, 35000, 35000));

        alias.put("laguna", "guatavita");
        alias.put("catedral", "zipaquira");
        alias.put("zipa", "zipaquira");
        alias.put("leiva", "villa de leyva");
        alias.put("leyva", "villa de leyva");
        alias.put("rocas", "suesca");

        pronostico.put("guatavita|sabado", new Pronostico("parcialmente nublado", 8, 18, 30));
        pronostico.put("guatavita|domingo", new Pronostico("lluvioso", 7, 15, 80));
        pronostico.put("zipaquira|sabado", new Pronostico("nublado", 9, 19, 40));
        pronostico.put("zipaquira|domingo", new Pronostico("soleado", 8, 20, 10));
        pronostico.put("villa de leyva|sabado", new Pronostico("soleado", 12, 25, 5));
        pronostico.put("villa de leyva|domingo", new Pronostico("soleado", 11, 24, 10));
        pronostico.put("suesca|sabado", new Pronostico("soleado", 7, 21, 15));
        pronostico.put("suesca|domingo", new Pronostico("tormentas en la tarde", 8, 17, 70));
        pronostico.put("chicaque|sabado", new Pronostico("neblina en la mañana", 10, 19, 50));
        pronostico.put("chicaque|domingo", new Pronostico("parcialmente nublado", 10, 20, 35));

        cupos.put("guatavita|sabado", 12);
        cupos.put("guatavita|domingo", 4);
        cupos.put("zipaquira|sabado", 30);
        cupos.put("zipaquira|domingo", 25);
        cupos.put("villa de leyva|sabado", 2);
        cupos.put("villa de leyva|domingo", 10);
        cupos.put("suesca|sabado", 8);
        cupos.put("suesca|domingo", 6);
        cupos.put("chicaque|sabado", 0);
        cupos.put("chicaque|domingo", 15);

        actividades.put("guatavita", List.of("senderismo hasta el mirador de la laguna",
                "recorrido guiado por la leyenda de El Dorado", "almuerzo típico en el pueblo de Guatavita"));
        actividades.put("zipaquira", List.of("recorrido por la Catedral de Sal", "museo de la minería",
                "gastronomía en el centro histórico (fritanga y postres)"));
        actividades.put("villa de leyva", List.of("caminata por la plaza principal", "visita al Museo del Fósil",
                "pozos azules y viñedos", "gastronomía local"));
        actividades.put("suesca", List.of("escalada en roca", "senderismo por las rocas", "ciclismo de montaña"));
        actividades.put("chicaque", List.of("senderismo por el bosque de niebla", "avistamiento de aves", "cabalgata"));
    }

    /** Minúsculas, sin tildes y sin espacios sobrantes: "Zipaquirá " -> "zipaquira". */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    /** Devuelve la clave del destino (p. ej. "villa de leyva") o null si no existe. */
    public String buscarDestino(String texto) {
        String t = normalizar(texto);
        if (t.isEmpty()) return null;
        for (String clave : destinos.keySet()) {
            if (t.contains(clave) || (t.length() >= 4 && clave.contains(t))) return clave;
        }
        for (Map.Entry<String, String> a : alias.entrySet()) {
            if (t.contains(a.getKey())) return a.getValue();
        }
        return null;
    }

    /** Acepta "sábado", "Sabado", "sab"... y devuelve "sabado" o "domingo" (o null). */
    public static String normalizarDia(String texto) {
        String t = normalizar(texto);
        if (t.startsWith("sab")) return "sabado";
        if (t.startsWith("dom")) return "domingo";
        return null;
    }

    /** Formatea 90000 como "$90.000". */
    public static String pesos(int valor) {
        return "$" + String.format(Locale.ROOT, "%,d", valor).replace(',', '.');
    }

    public String destinosValidos() {
        return destinos.values().stream().map(Destino::nombre).collect(Collectors.joining(", "));
    }

    public Destino destino(String clave) { return destinos.get(clave); }

    public Pronostico pronostico(String clave, String dia) { return pronostico.get(clave + "|" + dia); }

    public int cupos(String clave, String dia) { return cupos.getOrDefault(clave + "|" + dia, 0); }

    /** RETO: actividades de un destino (lista vacía si no hay). */
    public List<String> actividades(String clave) { return actividades.getOrDefault(clave, List.of()); }

    /** Descuenta cupos y registra la reserva. synchronized: varias peticiones a la vez no se pisan. */
    public synchronized Reserva reservar(String clave, String dia, int personas, String responsable) {
        cupos.merge(clave + "|" + dia, -personas, Integer::sum);
        Reserva r = new Reserva("RES-%03d".formatted(reservas.size() + 1), clave, dia, personas, responsable);
        reservas.add(r);
        return r;
    }
}
