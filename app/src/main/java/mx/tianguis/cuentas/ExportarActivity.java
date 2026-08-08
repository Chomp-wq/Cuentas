package mx.tianguis.cuentas;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

/**
 * Pantalla para armar un reporte a la medida: se eligen los días (uno, un
 * tramo, varios sueltos, semanas enteras o todo), de cuál artesano y cómo se
 * ordena. Abajo se ve al momento cuánto va a salir, y de ahí se comparte en
 * texto, imagen o PDF.
 */
public class ExportarActivity extends Activity {

    public static final String EXTRA_FECHA = "fecha";

    private static final String ESTADO_DIAS = "dias";
    private static final String ESTADO_QUIEN = "quien";
    private static final String ESTADO_ORDEN = "orden";
    private static final String ESTADO_ETIQUETA = "etiqueta";

    private Db db;
    private Seleccion seleccion = new Seleccion();
    /** Día desde el que se abrió la pantalla; sirve de referencia. */
    private String fechaBase;

    /** Los botones de periodo, para poder apagarlos todos al elegir uno. */
    private final List<TextView> opcionesPeriodo = new ArrayList<TextView>();
    private TextView quienAmbos;
    private TextView quienA;
    private TextView quienB;
    private TextView ordenDia;
    private TextView ordenSemana;
    private TextView ordenResumen;
    private TextView txtResumen;
    private TextView txtDetalle;
    private TextView txtTotal;
    private View tarjetaOrden;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.activity_exportar);
        setTitle(R.string.titulo_exportar);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = Db.obtener(this);
        fechaBase = getIntent().getStringExtra(EXTRA_FECHA);
        if (fechaBase == null) {
            fechaBase = Fechas.hoy();
        }

        enlazar();
        prepararPeriodos();
        prepararQuien();
        prepararOrden();

        if (estado != null) {
            restaurar(estado);
        } else {
            elegir(findViewById(R.id.op_esta_semana),
                    Seleccion.deLaSemanaDe(fechaBase, getString(R.string.rango_esta_semana)));
        }

        findViewById(R.id.btn_compartir_reporte).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compartir();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle estado) {
        super.onSaveInstanceState(estado);
        estado.putStringArray(ESTADO_DIAS,
                seleccion.dias.toArray(new String[seleccion.dias.size()]));
        estado.putInt(ESTADO_QUIEN, seleccion.quien);
        estado.putInt(ESTADO_ORDEN, seleccion.agrupacion);
        estado.putString(ESTADO_ETIQUETA, seleccion.etiqueta);
    }

    private void restaurar(Bundle estado) {
        String[] dias = estado.getStringArray(ESTADO_DIAS);
        Seleccion s = new Seleccion();
        if (dias != null) {
            for (int i = 0; i < dias.length; i++) {
                s.dias.add(dias[i]);
            }
        }
        s.quien = estado.getInt(ESTADO_QUIEN, Seleccion.AMBOS);
        s.agrupacion = estado.getInt(ESTADO_ORDEN, Seleccion.POR_DIA);
        s.etiqueta = estado.getString(ESTADO_ETIQUETA, "");
        seleccion = s;
        refrescar();
    }

    private void enlazar() {
        quienAmbos = (TextView) findViewById(R.id.quien_ambos);
        quienA = (TextView) findViewById(R.id.quien_a);
        quienB = (TextView) findViewById(R.id.quien_b);
        ordenDia = (TextView) findViewById(R.id.orden_dia);
        ordenSemana = (TextView) findViewById(R.id.orden_semana);
        ordenResumen = (TextView) findViewById(R.id.orden_resumen);
        txtResumen = (TextView) findViewById(R.id.txt_resumen);
        txtDetalle = (TextView) findViewById(R.id.txt_detalle);
        txtTotal = (TextView) findViewById(R.id.txt_total);
        tarjetaOrden = findViewById(R.id.tarjeta_orden);

        quienA.setText("A · " + Prefs.nombreA(this));
        quienB.setText("B · " + Prefs.nombreB(this));
    }

    // -------------------------------------------------------------- periodos

    private void prepararPeriodos() {
        periodoFijo(R.id.op_hoy, R.string.rango_hoy, 0);
        periodoFijo(R.id.op_ayer, R.string.rango_ayer, -1);

        opcion(R.id.op_esta_semana, new Runnable() {
            @Override
            public void run() {
                elegir(findViewById(R.id.op_esta_semana), Seleccion.deLaSemanaDe(
                        Fechas.hoy(), getString(R.string.rango_esta_semana)));
            }
        });
        opcion(R.id.op_semana_pasada, new Runnable() {
            @Override
            public void run() {
                elegir(findViewById(R.id.op_semana_pasada), Seleccion.deLaSemanaDe(
                        Fechas.sumarDias(Fechas.hoy(), -7),
                        getString(R.string.rango_semana_pasada)));
            }
        });
        opcion(R.id.op_este_mes, new Runnable() {
            @Override
            public void run() {
                elegir(findViewById(R.id.op_este_mes),
                        mes(0, getString(R.string.rango_este_mes)));
            }
        });
        opcion(R.id.op_mes_pasado, new Runnable() {
            @Override
            public void run() {
                elegir(findViewById(R.id.op_mes_pasado),
                        mes(-1, getString(R.string.rango_mes_pasado)));
            }
        });
        opcion(R.id.op_rango, new Runnable() {
            @Override
            public void run() {
                pedirRango();
            }
        });
        opcion(R.id.op_dias_sueltos, new Runnable() {
            @Override
            public void run() {
                pedirDiasSueltos();
            }
        });
        opcion(R.id.op_semanas, new Runnable() {
            @Override
            public void run() {
                pedirSemanas();
            }
        });
        opcion(R.id.op_todo, new Runnable() {
            @Override
            public void run() {
                elegirTodo();
            }
        });
    }

    private void periodoFijo(final int id, final int etiqueta, final int diasAtras) {
        opcion(id, new Runnable() {
            @Override
            public void run() {
                String fecha = Fechas.sumarDias(Fechas.hoy(), diasAtras);
                elegir(findViewById(id), Seleccion.deUnDia(fecha, getString(etiqueta)));
            }
        });
    }

    /** Registra un botón de periodo y lo deja listo para encenderse. */
    private void opcion(int id, final Runnable accion) {
        TextView v = (TextView) findViewById(id);
        opcionesPeriodo.add(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View x) {
                accion.run();
            }
        });
    }

    /** El mes de hoy, o el anterior si se pide -1. */
    private Seleccion mes(int desplazamiento, String etiqueta) {
        Calendar c = Fechas.calendarioHoy();
        c.add(Calendar.MONTH, desplazamiento);
        c.set(Calendar.DAY_OF_MONTH, 1);
        String primero = Fechas.clave(c);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return Seleccion.deRango(primero, Fechas.clave(c), etiqueta);
    }

    /** Deja encendido sólo el botón elegido y actualiza el reporte. */
    private void elegir(View boton, Seleccion nueva) {
        nueva.quien = seleccion.quien;
        if (nueva.agrupacion == Seleccion.POR_DIA) {
            nueva.agrupacion = seleccion.agrupacion;
        }
        seleccion = nueva;
        for (int i = 0; i < opcionesPeriodo.size(); i++) {
            TextView v = opcionesPeriodo.get(i);
            prender(v, v == boton);
        }
        refrescar();
    }

    // ----------------------------------------------------- elegir a la medida

    /** Dos calendarios seguidos: desde qué día y hasta qué día. */
    private void pedirRango() {
        Calendar inicio = Fechas.calendario(fechaBase);
        DatePickerDialog dialogo = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker picker, int anio, int mes, int dia) {
                        Calendar c = Fechas.calendarioHoy();
                        c.set(anio, mes, dia);
                        pedirFin(Fechas.clave(c));
                    }
                }, inicio.get(Calendar.YEAR), inicio.get(Calendar.MONTH),
                inicio.get(Calendar.DAY_OF_MONTH));
        dialogo.setTitle(R.string.desde_fecha);
        dialogo.show();
    }

    private void pedirFin(final String desde) {
        Calendar inicio = Fechas.calendario(desde);
        DatePickerDialog dialogo = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker picker, int anio, int mes, int dia) {
                        Calendar c = Fechas.calendarioHoy();
                        c.set(anio, mes, dia);
                        elegir(findViewById(R.id.op_rango),
                                Seleccion.deRango(desde, Fechas.clave(c), ""));
                    }
                }, inicio.get(Calendar.YEAR), inicio.get(Calendar.MONTH),
                inicio.get(Calendar.DAY_OF_MONTH));
        dialogo.setTitle(R.string.hasta_fecha);
        dialogo.show();
    }

    /** Lista con casillas de los días que sí tuvieron ventas. */
    private void pedirDiasSueltos() {
        final List<Db.DiaConTotal> dias = db.diasConVentas();
        if (dias.isEmpty()) {
            Toast.makeText(this, R.string.sin_nada_anotado, Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] etiquetas = new String[dias.size()];
        final boolean[] marcados = new boolean[dias.size()];
        for (int i = 0; i < dias.size(); i++) {
            Db.DiaConTotal d = dias.get(i);
            etiquetas[i] = Fechas.nombreDiaCorto(d.fecha) + " " + Fechas.diaMesCorto(d.fecha)
                    + " · " + Dinero.formato(d.total);
            marcados[i] = seleccion.dias.contains(d.fecha);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.elegir_dias)
                .setMultiChoiceItems(etiquetas, marcados,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int cual, boolean marcado) {
                                marcados[cual] = marcado;
                            }
                        })
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.listo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int cual) {
                        TreeSet<String> escogidos = new TreeSet<String>();
                        for (int i = 0; i < marcados.length; i++) {
                            if (marcados[i]) {
                                escogidos.add(dias.get(i).fecha);
                            }
                        }
                        if (escogidos.isEmpty()) {
                            Toast.makeText(ExportarActivity.this, R.string.nada_que_exportar,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        elegir(findViewById(R.id.op_dias_sueltos),
                                Seleccion.deDias(escogidos, ""));
                    }
                })
                .show();
    }

    /** Lo mismo pero por semanas: sólo las que tuvieron movimiento. */
    private void pedirSemanas() {
        List<Db.DiaConTotal> dias = db.diasConVentas();
        if (dias.isEmpty()) {
            Toast.makeText(this, R.string.sin_nada_anotado, Toast.LENGTH_SHORT).show();
            return;
        }

        // Juntar los días por semana, de la más reciente a la más vieja.
        final List<String> lunes = new ArrayList<String>();
        final List<Long> totales = new ArrayList<Long>();
        for (int i = 0; i < dias.size(); i++) {
            String inicio = Fechas.lunesDeLaSemana(dias.get(i).fecha);
            int donde = lunes.indexOf(inicio);
            if (donde < 0) {
                lunes.add(inicio);
                totales.add(dias.get(i).total);
            } else {
                totales.set(donde, totales.get(donde) + dias.get(i).total);
            }
        }

        final String[] etiquetas = new String[lunes.size()];
        final boolean[] marcados = new boolean[lunes.size()];
        for (int i = 0; i < lunes.size(); i++) {
            etiquetas[i] = Fechas.rangoSemana(lunes.get(i)) + " · "
                    + Dinero.formato(totales.get(i));
            marcados[i] = seleccion.dias.contains(lunes.get(i));
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.elegir_semanas)
                .setMultiChoiceItems(etiquetas, marcados,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int cual, boolean marcado) {
                                marcados[cual] = marcado;
                            }
                        })
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.listo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int cual) {
                        List<String> escogidas = new ArrayList<String>();
                        for (int i = 0; i < marcados.length; i++) {
                            if (marcados[i]) {
                                escogidas.add(lunes.get(i));
                            }
                        }
                        if (escogidas.isEmpty()) {
                            Toast.makeText(ExportarActivity.this, R.string.nada_que_exportar,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        elegir(findViewById(R.id.op_semanas),
                                Seleccion.deSemanas(escogidas, ""));
                    }
                })
                .show();
    }

    private void elegirTodo() {
        List<Db.DiaConTotal> dias = db.diasConVentas();
        if (dias.isEmpty()) {
            Toast.makeText(this, R.string.sin_nada_anotado, Toast.LENGTH_SHORT).show();
            return;
        }
        TreeSet<String> todos = new TreeSet<String>();
        for (int i = 0; i < dias.size(); i++) {
            todos.add(dias.get(i).fecha);
        }
        elegir(findViewById(R.id.op_todo),
                Seleccion.deDias(todos, getString(R.string.rango_todo)));
    }

    // ------------------------------------------------------- quién y orden

    private void prepararQuien() {
        quienAmbos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.quien = Seleccion.AMBOS;
                refrescar();
            }
        });
        quienA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.quien = Seleccion.SOLO_A;
                refrescar();
            }
        });
        quienB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.quien = Seleccion.SOLO_B;
                refrescar();
            }
        });
    }

    private void prepararOrden() {
        ordenDia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.agrupacion = Seleccion.POR_DIA;
                refrescar();
            }
        });
        ordenSemana.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.agrupacion = Seleccion.POR_SEMANA;
                refrescar();
            }
        });
        ordenResumen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seleccion.agrupacion = Seleccion.RESUMEN;
                refrescar();
            }
        });
    }

    // ------------------------------------------------------------ refrescado

    private void refrescar() {
        prender(quienAmbos, seleccion.quien == Seleccion.AMBOS);
        prender(quienA, seleccion.quien == Seleccion.SOLO_A);
        prender(quienB, seleccion.quien == Seleccion.SOLO_B);

        prender(ordenDia, seleccion.agrupacion == Seleccion.POR_DIA);
        prender(ordenSemana, seleccion.agrupacion == Seleccion.POR_SEMANA);
        prender(ordenResumen, seleccion.agrupacion == Seleccion.RESUMEN);
        // Con un solo día no hay nada que agrupar.
        tarjetaOrden.setVisibility(seleccion.dias.size() > 1 ? View.VISIBLE : View.GONE);

        txtResumen.setText(seleccion.etiqueta.length() > 0
                ? seleccion.etiqueta : seleccion.descripcionDias());

        Totales t = totalesDeLaSeleccion();
        String dias = seleccion.dias.size() == 1 ? "1 día" : seleccion.dias.size() + " días";
        txtDetalle.setText(dias + " · " + Reportes.piezas(t.piezas()) + " · "
                + seleccion.descripcionQuien(Prefs.nombreA(this), Prefs.nombreB(this)));
        txtTotal.setText(Dinero.formato(t.total()));
    }

    /** Suma de lo que entraría en el reporte con la selección actual. */
    private Totales totalesDeLaSeleccion() {
        Totales t = new Totales();
        if (seleccion.vacia()) {
            return t;
        }
        List<Venta> ventas = db.ventasDelRango(seleccion.primerDia(), seleccion.ultimoDia());
        for (int i = 0; i < ventas.size(); i++) {
            if (seleccion.incluye(ventas.get(i))) {
                t.sumar(ventas.get(i));
            }
        }
        return t;
    }

    private void prender(TextView v, boolean encendido) {
        v.setSelected(encendido);
        v.setTextColor(encendido ? 0xFFFFFFFF : Reportes.COLOR_PRIMARIO);
    }

    // ------------------------------------------------------------- compartir

    private void compartir() {
        if (seleccion.vacia()) {
            Toast.makeText(this, R.string.nada_que_exportar, Toast.LENGTH_SHORT).show();
            return;
        }
        if (totalesDeLaSeleccion().vacio()) {
            Toast.makeText(this, R.string.nada_que_exportar, Toast.LENGTH_SHORT).show();
            return;
        }
        Compartir.menu(this, Reportes.personalizado(this, seleccion));
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
