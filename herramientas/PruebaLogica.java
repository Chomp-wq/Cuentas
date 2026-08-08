import java.util.ArrayList;
import java.util.List;

import mx.tianguis.cuentas.Dinero;
import mx.tianguis.cuentas.ExportTexto;
import mx.tianguis.cuentas.Fechas;
import mx.tianguis.cuentas.Reporte;
import mx.tianguis.cuentas.Reportes;
import mx.tianguis.cuentas.Seleccion;
import mx.tianguis.cuentas.Totales;
import mx.tianguis.cuentas.Venta;

/**
 * Pruebas de la logica que no depende de Android: el manejo del dinero, las
 * fechas de la semana y el armado de los cortes.
 *
 * Se ejecutan en la computadora, sin emulador, con herramientas/probar.sh
 */
public class PruebaLogica {

    private static int fallos = 0;
    private static int pruebas = 0;

    public static void main(String[] args) {
        dinero();
        fechas();
        totales();
        corteDelDia();
        corteDeLaSemana();
        seleccionDeDias();
        reporteAMedida();

        System.out.println();
        if (fallos == 0) {
            System.out.println("OK — " + pruebas + " comprobaciones pasaron.");
        } else {
            System.out.println("FALLARON " + fallos + " de " + pruebas + " comprobaciones.");
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ dinero

    private static void dinero() {
        titulo("Dinero");
        igual("entero", 15000L, Dinero.aCentavos("150"));
        igual("con decimales", 15050L, Dinero.aCentavos("150.50"));
        igual("un decimal", 15050L, Dinero.aCentavos("150.5"));
        igual("con signo", 15000L, Dinero.aCentavos("$150"));
        igual("con espacios", 15000L, Dinero.aCentavos("  150  "));
        igual("coma decimal", 15050L, Dinero.aCentavos("150,50"));
        igual("coma de miles", 120000L, Dinero.aCentavos("1,200"));
        igual("miles y decimales", 120050L, Dinero.aCentavos("1,200.50"));
        igual("cero", 0L, Dinero.aCentavos("0"));
        igual("vacio se rechaza", -1L, Dinero.aCentavos(""));
        igual("letras se rechazan", -1L, Dinero.aCentavos("abc"));
        igual("dos puntos se rechazan", -1L, Dinero.aCentavos("1.2.3"));
        igual("nulo se rechaza", -1L, Dinero.aCentavos(null));

        igual("formato", "$150.00", Dinero.formato(15000L));
        igual("formato con miles", "$1,200.50", Dinero.formato(120050L));
        igual("formato sin signo", "1,200.50", Dinero.formatoSinSigno(120050L));
        igual("ida y vuelta", "$150.50", Dinero.formato(Dinero.aCentavos("150.50")));

        // Lo importante: sumar muchos precios "rotos" no debe perder centavos.
        long suma = 0;
        for (int i = 0; i < 1000; i++) {
            suma += Dinero.aCentavos("0.10");
        }
        igual("mil veces $0.10", "$100.00", Dinero.formato(suma));
    }

    // ------------------------------------------------------------------ fechas

    private static void fechas() {
        titulo("Fechas");
        // 8 de agosto de 2026 es sabado; su semana empieza el lunes 3.
        igual("lunes de un sabado", "2026-08-03", Fechas.lunesDeLaSemana("2026-08-08"));
        igual("lunes de un domingo", "2026-08-03", Fechas.lunesDeLaSemana("2026-08-09"));
        igual("lunes de un lunes", "2026-08-03", Fechas.lunesDeLaSemana("2026-08-03"));
        igual("lunes siguiente", "2026-08-10", Fechas.lunesDeLaSemana("2026-08-10"));

        igual("sumar dias", "2026-08-09", Fechas.sumarDias("2026-08-08", 1));
        igual("restar dias", "2026-07-31", Fechas.sumarDias("2026-08-01", -1));
        igual("cambio de anio", "2027-01-01", Fechas.sumarDias("2026-12-31", 1));
        igual("febrero bisiesto", "2028-02-29", Fechas.sumarDias("2028-02-28", 1));

        igual("nombre del dia", "Sabado", Fechas.nombreDia("2026-08-08"));
        igual("dia y mes", "8 de agosto", Fechas.diaYMes("2026-08-08"));
        igual("fecha completa", "Sabado 8 de agosto de 2026", Fechas.completa("2026-08-08"));
        igual("rango de la semana", "3 – 9 de agosto", Fechas.rangoSemana("2026-08-03"));
        igual("rango entre meses", "29 de junio – 5 de julio",
                Fechas.rangoSemana("2026-06-29"));
        igual("anio", 2026, Fechas.anio("2026-08-08"));

        // Recorrer la semana completa desde el lunes debe dar los siete dias.
        String lunes = Fechas.lunesDeLaSemana("2026-08-08");
        igual("septimo dia es domingo", "Domingo", Fechas.nombreDia(Fechas.sumarDias(lunes, 6)));
    }

    // ----------------------------------------------------------------- totales

    private static void totales() {
        titulo("Totales");
        Totales t = new Totales();
        t.sumar(venta("2026-08-08", "A", "Aretes", 2, 12000));
        t.sumar(venta("2026-08-08", "B", "Pulsera", 1, 8000));
        t.sumar(venta("2026-08-08", "A", "Collar", 1, 25000));

        igual("total de A", 49000L, t.a);
        igual("total de B", 8000L, t.b);
        igual("total del dia", 57000L, t.total());
        igual("piezas de A", 3, t.piezasA);
        igual("piezas de B", 1, t.piezasB);
        igual("piezas totales", 4, t.piezas());
        cierto("con ventas no esta vacio", !t.vacio());
        cierto("sin ventas esta vacio", new Totales().vacio());
    }

    // ------------------------------------------------------------ corte del dia

    private static void corteDelDia() {
        titulo("Corte del dia");
        List<Venta> ventas = new ArrayList<Venta>();
        ventas.add(venta("2026-08-08", "A", "Aretes de chaquira", 2, 12000));
        ventas.add(venta("2026-08-08", "B", "Pulsera tejida", 1, 8000));
        ventas.add(venta("2026-08-08", "A", "Collar de semillas", 1, 25000));

        Reporte r = Reportes.delDia(ventas, "2026-08-08", "Angel", "Bryan", "Artesanias");

        igual("titulo", "CORTE DEL DÍA", r.titulo);
        igual("subtitulo", "Sabado 8 de agosto de 2026", r.subtitulo);
        igual("nombre de archivo", "cuenta-2026-08-08", r.nombreArchivo);
        igual("dos secciones", 2, r.secciones.size());
        igual("piezas de Angel en su seccion", 2, r.secciones.get(0).filas.size());
        igual("piezas de Bryan en su seccion", 1, r.secciones.get(1).filas.size());
        igual("subtotal de Angel", 49000L, r.secciones.get(0).pieMonto);
        igual("subtotal de Bryan", 8000L, r.secciones.get(1).pieMonto);
        igual("gran total", 57000L, r.granTotal());
        igual("tres renglones de total", 3, r.totales.size());
        cierto("el ultimo total va destacado", r.totales.get(2).destacado);

        String texto = ExportTexto.armar(r);
        contiene("el texto trae el titulo", texto, "CORTE DEL DÍA");
        contiene("el texto trae a Angel", texto, "ANGEL (A)");
        contiene("el texto trae a Bryan", texto, "BRYAN (B)");
        contiene("el texto trae una pieza", texto, "2 × Aretes de chaquira ($120.00) = $240.00");
        contiene("el texto trae el subtotal", texto, "Subtotal Angel · 3 piezas: $490.00");
        contiene("el texto trae el total", texto, "TOTAL DEL DÍA: $570.00");

        // Un dia sin ventas tambien debe poder compartirse.
        Reporte vacio = Reportes.delDia(new ArrayList<Venta>(), "2026-08-08",
                "Angel", "Bryan", "Artesanias");
        igual("dia vacio suma cero", 0L, vacio.granTotal());
        contiene("dia vacio lo dice", ExportTexto.armar(vacio), "No se vendió nada de Angel.");
    }

    // --------------------------------------------------------- corte de semana

    private static void corteDeLaSemana() {
        titulo("Corte de la semana");
        List<Venta> ventas = new ArrayList<Venta>();
        // Lunes
        ventas.add(venta("2026-08-03", "A", "Aretes de chaquira", 2, 12000));
        ventas.add(venta("2026-08-03", "B", "Pulsera tejida", 1, 8000));
        // Miercoles
        ventas.add(venta("2026-08-05", "A", "Aretes de chaquira", 3, 12000));
        // Sabado
        ventas.add(venta("2026-08-08", "B", "Bolsa bordada", 1, 45000));
        ventas.add(venta("2026-08-08", "A", "Collar de semillas", 1, 25000));

        Reporte r = Reportes.deLaSemana(ventas, "2026-08-03", "Angel", "Bryan", "Artesanias");

        igual("titulo", "CORTE DE LA SEMANA", r.titulo);
        igual("subtitulo", "Del 3 de agosto de 2026 al 9 de agosto de 2026", r.subtitulo);
        igual("tres secciones", 3, r.secciones.size());
        igual("solo los dias con venta", 3, r.secciones.get(0).filas.size());

        long esperadoA = 2 * 12000 + 3 * 12000 + 25000;
        long esperadoB = 8000 + 45000;
        igual("total de Angel", esperadoA, r.totales.get(0).monto);
        igual("total de Bryan", esperadoB, r.totales.get(1).monto);
        igual("total de la semana", esperadoA + esperadoB, r.granTotal());

        // Los aretes del lunes y del miercoles se juntan en un solo renglon.
        Reporte.Seccion detalleA = r.secciones.get(1);
        igual("dos piezas distintas de Angel", 2, detalleA.filas.size());
        Reporte.Fila aretes = buscarFila(detalleA, "Aretes de chaquira");
        cierto("los aretes aparecen agrupados", aretes != null);
        if (aretes != null) {
            igual("piezas de aretes en la semana", "5", aretes.c2);
            igual("importe de aretes en la semana", 60000L, aretes.monto);
        }

        String texto = ExportTexto.armar(r);
        contiene("el texto trae el dia por dia", texto, "DÍA POR DÍA");
        contiene("el texto trae un dia", texto, "• Lunes 3 ago — A: $240.00 · B: $80.00 = $320.00");
        contiene("el texto trae el total", texto, "TOTAL DE LA SEMANA: $1,380.00");

        // Una semana sin nada tampoco debe reventar.
        Reporte vacio = Reportes.deLaSemana(new ArrayList<Venta>(), "2026-08-03",
                "Angel", "Bryan", "Artesanias");
        igual("semana vacia suma cero", 0L, vacio.granTotal());
        contiene("semana vacia lo dice", ExportTexto.armar(vacio),
                "No hubo ventas en esta semana.");
    }

    // ------------------------------------------------------ seleccion de dias

    private static void seleccionDeDias() {
        titulo("Seleccion de dias");

        Seleccion unDia = Seleccion.deUnDia("2026-08-08", "Hoy");
        igual("un dia trae un dia", 1, unDia.dias.size());
        igual("un dia se describe completo", "Sabado 8 de agosto de 2026",
                unDia.descripcionDias());

        Seleccion rango = Seleccion.deRango("2026-08-03", "2026-08-09", "");
        igual("el rango trae los siete dias", 7, rango.dias.size());
        igual("el rango empieza donde debe", "2026-08-03", rango.primerDia());
        igual("el rango termina donde debe", "2026-08-09", rango.ultimoDia());
        cierto("el rango es continuo", rango.sonSeguidos());
        igual("el rango se describe", "Del 3 de agosto de 2026 al 9 de agosto de 2026",
                rango.descripcionDias());

        // Al reves da lo mismo: se ordena solo.
        Seleccion alReves = Seleccion.deRango("2026-08-09", "2026-08-03", "");
        igual("el rango al reves trae lo mismo", 7, alReves.dias.size());
        igual("el rango al reves empieza igual", "2026-08-03", alReves.primerDia());

        Seleccion cruzaMes = Seleccion.deRango("2026-07-30", "2026-08-02", "");
        igual("cruza el cambio de mes", 4, cruzaMes.dias.size());

        List<String> sueltos = new ArrayList<String>();
        sueltos.add("2026-08-08");
        sueltos.add("2026-08-01");
        sueltos.add("2026-08-15");
        Seleccion elegidos = Seleccion.deDias(sueltos, "");
        igual("los dias sueltos se guardan", 3, elegidos.dias.size());
        igual("los dias sueltos se ordenan", "2026-08-01", elegidos.primerDia());
        cierto("los dias sueltos no son continuos", !elegidos.sonSeguidos());
        contiene("los dias sueltos se cuentan", elegidos.descripcionDias(), "3 días elegidos");

        List<String> lunes = new ArrayList<String>();
        lunes.add("2026-08-03");
        lunes.add("2026-08-17");
        Seleccion semanas = Seleccion.deSemanas(lunes, "");
        igual("dos semanas son catorce dias", 14, semanas.dias.size());
        igual("dos semanas se agrupan por semana", Seleccion.POR_SEMANA, semanas.agrupacion);
        igual("las semanas escogidas se ven", 2, semanas.semanas().size());

        // El filtro por artesano.
        Seleccion soloA = Seleccion.deRango("2026-08-03", "2026-08-09", "");
        soloA.quien = Seleccion.SOLO_A;
        cierto("solo A deja pasar a A", soloA.incluye(venta("2026-08-05", "A", "x", 1, 100)));
        cierto("solo A no deja pasar a B", !soloA.incluye(venta("2026-08-05", "B", "x", 1, 100)));
        cierto("solo A rechaza un dia de fuera",
                !soloA.incluye(venta("2026-09-01", "A", "x", 1, 100)));
        cierto("solo A incluye a A", soloA.incluyeA());
        cierto("solo A excluye a B", !soloA.incluyeB());
        contiene("solo A se describe", soloA.descripcionQuien("Angel", "Bryan"), "Sólo Angel (A)");
        contiene("el archivo dice de quien es", soloA.nombreArchivo(), "ventas-A-2026-08-03");
    }

    // ------------------------------------------------------- reporte a medida

    private static void reporteAMedida() {
        titulo("Reporte a la medida");
        List<Venta> ventas = new ArrayList<Venta>();
        ventas.add(venta("2026-08-03", "A", "Aretes de chaquira", 2, 12000));
        ventas.add(venta("2026-08-03", "B", "Pulsera tejida", 1, 8000));
        ventas.add(venta("2026-08-05", "A", "Aretes de chaquira", 3, 12000));
        ventas.add(venta("2026-08-08", "B", "Bolsa bordada", 1, 45000));
        ventas.add(venta("2026-08-08", "A", "Collar de semillas", 1, 25000));
        // Fuera del rango que se va a pedir.
        ventas.add(venta("2026-08-20", "A", "Aretes de chaquira", 9, 12000));

        // --- Dias sueltos: solo el lunes y el sabado.
        List<String> sueltos = new ArrayList<String>();
        sueltos.add("2026-08-03");
        sueltos.add("2026-08-08");
        Seleccion s = Seleccion.deDias(sueltos, "");
        Reporte r = Reportes.personalizado(ventas, s, "Angel", "Bryan", "Artesanias");

        igual("titulo generico", "REPORTE DE VENTAS", r.titulo);
        igual("suma solo los dias elegidos", 24000L + 8000L + 45000L + 25000L, r.granTotal());
        contiene("dice cuantos dias", ExportTexto.armar(r), "2 días");
        contiene("no se cuela el dia de fuera", ExportTexto.armar(r), "Angel (A): $490.00");

        // --- Un solo artesano.
        Seleccion soloB = Seleccion.deRango("2026-08-03", "2026-08-09", "");
        soloB.quien = Seleccion.SOLO_B;
        Reporte rb = Reportes.personalizado(ventas, soloB, "Angel", "Bryan", "Artesanias");
        igual("solo B suma lo de B", 8000L + 45000L, rb.granTotal());
        igual("solo B trae un renglon de total mas el destacado", 2, rb.totales.size());
        String textoB = ExportTexto.armar(rb);
        contiene("solo B se anuncia", textoB, "Sólo Bryan (B)");
        cierto("solo B no menciona a Angel", !textoB.contains("Angel (A):"));
        contiene("solo B trae su pieza", textoB, "Bolsa bordada");
        cierto("solo B no trae piezas de Angel", !textoB.contains("Collar de semillas"));

        // --- Una semana completa se sigue llamando corte de la semana.
        Seleccion semana = Seleccion.deLaSemanaDe("2026-08-08", "");
        Reporte rs = Reportes.personalizado(ventas, semana, "Angel", "Bryan", "Artesanias");
        igual("una semana completa se reconoce", "CORTE DE LA SEMANA", rs.titulo);
        igual("la semana suma bien", 138000L, rs.granTotal());

        // --- Un solo dia se lee pieza por pieza.
        Seleccion dia = Seleccion.deUnDia("2026-08-08", "");
        Reporte rd = Reportes.personalizado(ventas, dia, "Angel", "Bryan", "Artesanias");
        igual("un dia se llama corte del dia", "CORTE DEL DÍA", rd.titulo);
        igual("un dia suma lo suyo", 45000L + 25000L, rd.granTotal());
        igual("un dia trae las dos secciones", 2, rd.secciones.size());

        // --- Agrupado por semana sobre varias semanas.
        Seleccion dosSemanas = Seleccion.deRango("2026-08-03", "2026-08-23", "");
        dosSemanas.agrupacion = Seleccion.POR_SEMANA;
        Reporte rw = Reportes.personalizado(ventas, dosSemanas, "Angel", "Bryan", "Artesanias");
        contiene("agrupa por semana", ExportTexto.armar(rw), "SEMANA POR SEMANA");
        igual("suma las tres semanas", 138000L + 108000L, rw.granTotal());
        igual("solo las semanas con venta salen", 2, rw.secciones.get(0).filas.size());

        // --- Sin agrupacion: solo el detalle de cada artesano.
        Seleccion resumen = Seleccion.deRango("2026-08-03", "2026-08-09", "");
        resumen.agrupacion = Seleccion.RESUMEN;
        Reporte rr = Reportes.personalizado(ventas, resumen, "Angel", "Bryan", "Artesanias");
        igual("el resumen trae solo los dos detalles", 2, rr.secciones.size());
        igual("el resumen suma igual", 138000L, rr.granTotal());

        // --- Nada seleccionado no revienta.
        Reporte vacio = Reportes.personalizado(ventas, new Seleccion(), "Angel", "Bryan", "A");
        igual("sin dias suma cero", 0L, vacio.granTotal());
    }

    // ---------------------------------------------------------------- utiles

    private static Venta venta(String fecha, String artesano, String descripcion,
                               int cantidad, long precio) {
        Venta v = new Venta();
        v.fecha = fecha;
        v.artesano = artesano;
        v.descripcion = descripcion;
        v.cantidad = cantidad;
        v.precio = precio;
        return v;
    }

    private static Reporte.Fila buscarFila(Reporte.Seccion s, String descripcion) {
        for (int i = 0; i < s.filas.size(); i++) {
            if (descripcion.equals(s.filas.get(i).c1)) {
                return s.filas.get(i);
            }
        }
        return null;
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("── " + t + " ──");
    }

    private static void igual(String que, Object esperado, Object obtenido) {
        pruebas++;
        boolean bien = esperado == null ? obtenido == null : esperado.equals(obtenido);
        if (bien) {
            System.out.println("  ok   " + que);
        } else {
            fallos++;
            System.out.println("  FALLA " + que + ": esperaba <" + esperado
                    + "> y llego <" + obtenido + ">");
        }
    }

    private static void cierto(String que, boolean condicion) {
        pruebas++;
        if (condicion) {
            System.out.println("  ok   " + que);
        } else {
            fallos++;
            System.out.println("  FALLA " + que);
        }
    }

    private static void contiene(String que, String texto, String fragmento) {
        pruebas++;
        if (texto.contains(fragmento)) {
            System.out.println("  ok   " + que);
        } else {
            fallos++;
            System.out.println("  FALLA " + que + ": no encontre <" + fragmento + ">");
            System.out.println("        en:\n" + texto);
        }
    }
}
