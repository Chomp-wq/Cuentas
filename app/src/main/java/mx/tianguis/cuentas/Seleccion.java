package mx.tianguis.cuentas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Qué se va a exportar: qué días, de cuál artesano y cómo se ordena.
 *
 * Los días se guardan como un conjunto de fechas sueltas ("yyyy-MM-dd"), no
 * como un rango. Así da lo mismo pedir "esta semana", "del 1 al 15", "sólo
 * los sábados de agosto" o cinco días escogidos a mano: todo termina siendo
 * una lista de días y el reporte se arma igual.
 */
public class Seleccion {

    /** De quién son las piezas que entran en el reporte. */
    public static final int AMBOS = 0;
    public static final int SOLO_A = 1;
    public static final int SOLO_B = 2;

    /** Cómo se agrupa el resumen de arriba del reporte. */
    public static final int POR_DIA = 0;
    public static final int POR_SEMANA = 1;
    public static final int RESUMEN = 2;

    /** Días incluidos, ordenados solos por ser un TreeSet de "yyyy-MM-dd". */
    public final TreeSet<String> dias = new TreeSet<String>();
    public int quien = AMBOS;
    public int agrupacion = POR_DIA;
    /** Cómo se le dice a esta selección: "Esta semana", "5 días elegidos"... */
    public String etiqueta = "";

    // ------------------------------------------------------------- atajos

    public static Seleccion deUnDia(String fecha, String etiqueta) {
        Seleccion s = new Seleccion();
        s.dias.add(fecha);
        s.etiqueta = etiqueta;
        return s;
    }

    /** Todos los días entre dos fechas, ambas incluidas. */
    public static Seleccion deRango(String desde, String hasta, String etiqueta) {
        Seleccion s = new Seleccion();
        String a = desde;
        String b = hasta;
        if (a.compareTo(b) > 0) {
            String intercambio = a;
            a = b;
            b = intercambio;
        }
        String cursor = a;
        // Tope de seguridad: diez años de días es muchísimo más de lo que
        // cabría en un reporte, y evita un ciclo infinito si algo viniera mal.
        for (int i = 0; i < 3700 && cursor.compareTo(b) <= 0; i++) {
            s.dias.add(cursor);
            cursor = Fechas.sumarDias(cursor, 1);
        }
        s.etiqueta = etiqueta;
        return s;
    }

    /** La semana (lunes a domingo) a la que pertenece la fecha. */
    public static Seleccion deLaSemanaDe(String fecha, String etiqueta) {
        String lunes = Fechas.lunesDeLaSemana(fecha);
        Seleccion s = deRango(lunes, Fechas.sumarDias(lunes, 6), etiqueta);
        s.agrupacion = POR_DIA;
        return s;
    }

    /** Días escogidos a mano, sin que tengan que ser seguidos. */
    public static Seleccion deDias(Collection<String> fechas, String etiqueta) {
        Seleccion s = new Seleccion();
        s.dias.addAll(fechas);
        s.etiqueta = etiqueta;
        return s;
    }

    /** Varias semanas escogidas a mano: se agregan sus siete días. */
    public static Seleccion deSemanas(Collection<String> lunes, String etiqueta) {
        Seleccion s = new Seleccion();
        for (String inicio : lunes) {
            for (int d = 0; d < 7; d++) {
                s.dias.add(Fechas.sumarDias(inicio, d));
            }
        }
        s.etiqueta = etiqueta;
        s.agrupacion = POR_SEMANA;
        return s;
    }

    // ------------------------------------------------------------ consultas

    public boolean vacia() {
        return dias.isEmpty();
    }

    public String primerDia() {
        return dias.isEmpty() ? Fechas.hoy() : dias.first();
    }

    public String ultimoDia() {
        return dias.isEmpty() ? Fechas.hoy() : dias.last();
    }

    /** ¿Esta venta entra en el reporte? */
    public boolean incluye(Venta v) {
        if (!dias.contains(v.fecha)) {
            return false;
        }
        if (quien == SOLO_A) {
            return v.esDeA();
        }
        if (quien == SOLO_B) {
            return !v.esDeA();
        }
        return true;
    }

    public boolean incluyeA() {
        return quien != SOLO_B;
    }

    public boolean incluyeB() {
        return quien != SOLO_A;
    }

    /** Los lunes de las semanas que toca la selección, en orden. */
    public List<String> semanas() {
        TreeSet<String> lunes = new TreeSet<String>();
        for (String dia : dias) {
            lunes.add(Fechas.lunesDeLaSemana(dia));
        }
        return new ArrayList<String>(lunes);
    }

    /**
     * Cómo se describe la selección debajo del título del reporte. Si los días
     * van seguidos se dice como rango; si están sueltos, se cuentan.
     */
    public String descripcionDias() {
        if (dias.isEmpty()) {
            return "Sin días seleccionados";
        }
        if (dias.size() == 1) {
            return Fechas.completa(dias.first());
        }
        if (sonSeguidos()) {
            return "Del " + Fechas.diaMesAnio(primerDia()) + " al "
                    + Fechas.diaMesAnio(ultimoDia());
        }
        return dias.size() + " días elegidos, entre el " + Fechas.diaMesAnio(primerDia())
                + " y el " + Fechas.diaMesAnio(ultimoDia());
    }

    /** ¿Los días forman un tramo continuo, sin huecos? */
    public boolean sonSeguidos() {
        String esperado = primerDia();
        for (String dia : dias) {
            if (!dia.equals(esperado)) {
                return false;
            }
            esperado = Fechas.sumarDias(esperado, 1);
        }
        return true;
    }

    /** "Ambos artesanos", "Sólo Angel (A)"... */
    public String descripcionQuien(String nombreA, String nombreB) {
        if (quien == SOLO_A) {
            return "Sólo " + nombreA + " (A)";
        }
        if (quien == SOLO_B) {
            return "Sólo " + nombreB + " (B)";
        }
        return "Los dos artesanos";
    }

    /** Base del nombre del archivo que se comparte. */
    public String nombreArchivo() {
        StringBuilder sb = new StringBuilder("ventas");
        if (quien == SOLO_A) {
            sb.append("-A");
        } else if (quien == SOLO_B) {
            sb.append("-B");
        }
        if (dias.isEmpty()) {
            return sb.toString();
        }
        sb.append('-').append(primerDia());
        if (dias.size() > 1) {
            sb.append("_a_").append(ultimoDia());
        }
        return sb.toString();
    }

    /** Copia con los mismos días, para cambiar filtros sin rehacer la lista. */
    public Seleccion copia() {
        Seleccion s = new Seleccion();
        s.dias.addAll(dias);
        s.quien = quien;
        s.agrupacion = agrupacion;
        s.etiqueta = etiqueta;
        return s;
    }
}
