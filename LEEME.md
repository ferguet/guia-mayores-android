# Fase 0 — la prueba que decide

## Qué es esto

Una app de Android mínima que responde a **una sola pregunta**:

> ¿Podemos abrir una sede electrónica real dentro de nuestra propia app
> y dibujar la ayuda encima?

Nada más. No rellena formularios, no guarda datos, no pide contraseñas.
Abre la página real de la FNMT y le pinta encima el punto naranja y el
panel verde.

## Por qué hacía falta

Ya comprobamos que **como página web es imposible**: el servidor de la
FNMT responde `X-Frame-Options: SAMEORIGIN`, que prohíbe que otra web la
muestre dentro. Es una protección contra estafas, y está bien que exista.

Pero una app con su propio navegador no es lo mismo que una web dentro de
otra web: es una navegación normal, así que esa prohibición no le afecta.
Esto lo comprueba de verdad, en un móvil de verdad.

## Qué tienes que hacer

### 1. Subirlo a GitHub

Crea un repositorio nuevo (por ejemplo `guia-mayores-android`) y sube esta
carpeta entera:

```
cd C:\Users\fjavi\Desktop\guia-mayores-android
git init
git add .
git commit -m "Fase 0: prueba de navegador propio sobre la FNMT real"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/guia-mayores-android.git
git push -u origin main
```

### 2. Esperar a que GitHub compile

En la página del repositorio, pestaña **Actions**. Tarda unos 3-5 minutos
la primera vez. Cuando termine con un tic verde, entra y abajo del todo
verás **Artifacts → guia-fase0-apk**. Descárgalo.

Es un `.zip`: dentro está el archivo `.apk`.

### 3. Instalarlo en el móvil

Pasa el `.apk` al móvil (por cable, por correo a ti mismo, o por Drive) y
ábrelo. Android te avisará de que viene de fuera de la tienda: hay que
darle permiso a "instalar aplicaciones desconocidas". Es normal para una
app de pruebas propia.

## Qué mirar cuando la abras

La barra de arriba te dice el resultado sin que tengas que interpretar
nada:

- **Barra verde**, "✅ Funciona. Señalando: …" → la prueba ha salido bien.
  El proyecto es viable y podemos seguir.
- **Barra roja** → no ha funcionado. Hazme una foto de la pantalla o
  cópiame el texto y lo miramos.

Y visualmente: tienes que ver la web auténtica de la FNMT con el círculo
naranja encima y la franja verde abajo.

## Qué NO demuestra esta prueba

- No prueba que funcione con la app del banco (esa bloquea a propósito).
- No prueba que la IA sepa leer la pantalla sola: aquí el objetivo se
  busca con palabras clave escritas a mano.
- No prueba que la gente lo entienda. Eso es la Fase 1.
