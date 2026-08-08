# Cuentas Tianguis

Aplicación de Android para llevar la cuenta diaria de un puesto de artesanías
en el tianguis. La vendedora anota lo que se va vendiendo separando las piezas
del **artesano A (Angel)** y las del **artesano B (Bryan)**, y en todo momento
tiene a la vista cuánto lleva cada uno y cuánto lleva el día. Al cierre de la
semana la app arma el corte semanal con el mismo detalle.

Todo se guarda en el teléfono. No pide permisos, no necesita internet y no
tiene cuentas de usuario.

## Cómo instalarla

El APK ya compilado está en **`apk/cuentas-tianguis-1.1.apk`**.

1. Copia ese archivo al teléfono (por cable, WhatsApp o Drive).
2. Ábrelo desde el teléfono. Android va a pedir permiso para *instalar
   aplicaciones desconocidas*: acéptalo para el archivo.
3. Listo. Aparece como **Cuentas Tianguis**.

Funciona en Android 5.0 (Lollipop) en adelante.

## Cómo se usa

### El día

La pantalla principal es la cuenta de un día:

- **Arriba** se elige el día. Las flechas `◀ ▶` pasan al día anterior o al
  siguiente, y tocando la fecha se abre el calendario. El botón **HOY**
  aparece sólo cuando estás viendo otro día.
- **Para anotar una venta**: se toca `A` o `B` según de quién sea la pieza, se
  escribe qué se vendió, cuántas piezas y a cuánto cada una, y se le da a
  **Agregar venta**.
  - El campo de la descripción va aprendiendo: al escribir sugiere lo que ya
    se ha vendido antes, y al elegir una sugerencia propone el último precio
    con el que se vendió esa pieza.
  - La cantidad viene en `1`, que es lo más común.
- **Para corregir o borrar**: se toca la venta en la lista. Se abre una
  ventana para cambiar cualquier dato o eliminarla.
- **Abajo** siempre están los tres totales: lo de Angel, lo de Bryan y el
  **total del día**.

### La semana

El botón **Ver semana** abre el corte semanal (de lunes a domingo):

- Cada día con ventas aparece con lo de A, lo de B y su total.
- Tocando un día se abre la cuenta de ese día.
- Abajo están los totales de la semana por artesano y el **total de la
  semana**.
- Las flechas `◀ ▶` cambian de semana.

### Exportar reportes a la medida

El botón **Exportar** abre una pantalla para armar el reporte que quieras, sin
quedarte con sólo "el día" o "la semana". Se arma en tres pasos:

**1. ¿De qué días?**

| Opción | Qué hace |
|---|---|
| Hoy · Ayer | Un solo día. |
| Esta semana · Semana pasada | La semana completa, de lunes a domingo. |
| Este mes · Mes pasado | El mes entero. |
| Entre dos fechas… | Elige el día de inicio y el de fin en el calendario. Sirve para quincenas, puentes, ferias de varios días, lo que sea. |
| Elegir días sueltos… | Una lista con casillas de los días que sí tuvieron ventas. Marca los que quieras aunque no sean seguidos: por ejemplo, sólo los sábados del mes. |
| Elegir semanas… | Lo mismo pero por semanas completas, para juntar varias semanas que no van seguidas. |
| Todo lo anotado | Desde la primera venta hasta la última. |

**2. ¿De quién?** — **Los dos**, **sólo A (Angel)** o **sólo B (Bryan)**.
Cuando eliges a uno solo, el reporte deja de mostrar la columna del otro y los
totales quedan únicamente con lo de esa persona.

**3. ¿Cómo se ordena?** — **Día por día**, **por semana** o **sólo resumen**
(nada más el detalle de las piezas y los totales, sin desglose de fechas).

Abajo, mientras eliges, se va viendo cuántos días entraron, cuántas piezas y
cuánto suma. De ahí se comparte igual que todo lo demás: texto, imagen o PDF.

### Compartir

El botón **Compartir** (en el día o en la semana) ofrece tres formatos, y los
tres dicen exactamente lo mismo porque salen del mismo corte:

| Formato | Para qué sirve |
|---|---|
| **Texto** | Se manda como mensaje de WhatsApp, se copia y se pega. Es el texto en bruto, sin archivos. |
| **Imagen (PNG)** | Una foto de la cuenta, bien formada, para mandar por WhatsApp sin que se pierda el formato. |
| **PDF** | Documento tamaño A4, paginado, con encabezado en cada hoja y numeración al pie. Es el que se imprime o se archiva. |

Los archivos se generan dentro de la app y se entregan a la app con la que
compartes; no se guardan en la galería ni ocupan espacio permanente.

### Los nombres de los artesanos

Vienen como *Angel* y *Bryan*. Se pueden cambiar desde el menú de los tres
puntos → **Nombres de artesanos**. Las letras A y B no cambian, así que las
cuentas viejas se siguen entendiendo.

## Cómo se compila

El proyecto **no usa ninguna librería externa**: base de datos, PDF, imágenes
y el compartir archivos se resuelven con el framework de Android. Por eso se
puede compilar sin Android Studio y sin Gradle.

### Con el script (lo que se usó aquí)

En Debian o Ubuntu:

```bash
sudo apt-get install aapt apksigner zipalign dalvik-exchange \
                     android-sdk-build-tools android-sdk-platform-23
./build.sh
```

El APK firmado queda en `build/cuentas-tianguis.apk`.

El script hace, en orden: compila y enlaza los recursos con `aapt2`, compila
el Java contra `android.jar`, convierte las clases a `classes.dex`, arma el
APK, lo alinea con `zipalign` y lo firma con `apksigner`.

Variables útiles:

| Variable | Para qué |
|---|---|
| `VERSION_CODIGO`, `VERSION_NOMBRE` | Número de versión del APK. |
| `ANDROID_JAR` | Ruta a un `android.jar` distinto. |
| `CUENTAS_LLAVE` | Ruta del archivo de firma. |

> **Cuida la llave de firma.** La primera compilación crea
> `build/llave-firma.jks`. Android sólo deja instalar una actualización encima
> si viene firmada con la misma llave; si se pierde, hay que desinstalar la
> app (y con ella las cuentas guardadas) antes de instalar la nueva versión.
> El archivo está en `.gitignore` a propósito: no debe subirse al repositorio.
> Guárdalo aparte, en un respaldo.
>
> El APK de `apk/` se firmó con la llave que se generó al compilarlo. Si vas a
> sacar versiones nuevas, conserva esa misma llave (se entrega aparte del
> repositorio) o desinstala la app antes de instalar la siguiente versión.

Para borrar lo generado (conservando la llave):

```bash
./build.sh limpiar
```

### Con Android Studio

El proyecto también tiene la estructura estándar de Gradle. Basta abrir esta
carpeta en Android Studio y compilar normalmente; ahí sí se descarga el SDK
completo desde internet.

## Pruebas

La lógica que no depende de Android —el manejo del dinero, las fechas de la
semana y el armado de los cortes— se prueba en la computadora, sin emulador:

```bash
herramientas/probar.sh
```

Son 121 comprobaciones. Cubren el redondeo del dinero en centavos (para que
sumar muchos precios nunca pierda un centavo), los límites de la semana
lunes–domingo, los cambios de mes y de año, el agrupado de piezas iguales, la
salida en texto de los cortes y —lo más delicado— la selección de días de los
reportes a la medida: que un rango al revés se acomode solo, que los días
sueltos no arrastren días vecinos, que filtrar por un artesano deje fuera
todo lo del otro y que las sumas cuadren en cada combinación.

## Cómo está hecho por dentro

```
.
├── build.sh                    compila el APK sin Android Studio
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/mx/tianguis/cuentas/
│   │   ├── MainActivity.java       la cuenta del día
│   │   ├── SemanaActivity.java     el corte de la semana
│   │   ├── ExportarActivity.java   armar un reporte a la medida
│   │   ├── DialogoVenta.java       corregir o borrar una venta
│   │   ├── Db.java                 base de datos SQLite
│   │   ├── Venta.java / Totales.java
│   │   ├── Seleccion.java          qué días, de quién y cómo se agrupa
│   │   ├── Dinero.java             pesos y centavos
│   │   ├── Fechas.java             fechas en español, semana lunes–domingo
│   │   ├── Reporte.java            el corte, en abstracto
│   │   ├── Reportes.java           arma el corte del día y el de la semana
│   │   ├── Lienzo.java             motor de dibujo del corte
│   │   ├── ExportTexto.java        el corte en texto
│   │   ├── ExportImagen.java       el corte en PNG
│   │   ├── ExportPdf.java          el corte en PDF paginado
│   │   ├── ArchivosProvider.java   entrega los archivos al compartir
│   │   └── Compartir.java          el menú de compartir
│   └── res/                        pantallas, colores e iconos
└── herramientas/
    ├── GenerarIcono.java       genera los iconos de la app
    ├── PruebaLogica.java       las pruebas
    └── probar.sh
```

Dos decisiones que vale la pena conocer:

**El dinero se guarda en centavos**, como número entero, nunca con decimales
de punto flotante. Así, sumar mil veces `$0.10` da exactamente `$100.00` y las
cuentas del día cuadran siempre.

**Los tres formatos salen del mismo corte.** `Reportes` arma un objeto
`Reporte` y de ahí salen el texto, la imagen y el PDF. La imagen y el PDF
comparten además el mismo motor de dibujo (`Lienzo`), sólo cambia la escala y
quién pagina, así que no pueden desincronizarse ni decir cosas distintas.
