package es.guiamayores.fase0

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * GUIA HABLADA SOBRE LA SEDE REAL
 *
 * La Fase 0 ya demostro lo unico que habia que demostrar: que una app con
 * su propio navegador puede abrir una sede electronica de verdad y dibujar
 * encima. Esto es el paso siguiente, y cambia el enfoque por completo tras
 * ver como se maneja de verdad una persona mayor:
 *
 *   - NO leen. Ven mal y se cansan. El texto en pantalla es minimo.
 *   - Se les olvida entre una pantalla y otra donde iban.
 *   - Un diccionario de palabras aparte no sirve de nada: para cuando
 *     van a consultarlo, ya se han perdido.
 *
 * Por eso todo va por VOZ, en el momento, referido al boton que tienen
 * delante. Y si se quedan parados, se les repite solo.
 *
 * Lo que esta app NO hace, a proposito: no escribe por ellos, no guarda
 * datos, no pide contraseñas y no toca sus credenciales. Solo señala y
 * habla. Cualquier cosa que se escriba, la escribe la persona.
 */
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var web: WebView
    private var voz: TextToSpeech? = null
    private var vozLista = false
    private var ultimaFrase = ""
    private var ultimaUbicacion = ""

    /**
     * Portada propia, dentro de la app: desde ahi se elige que hacer.
     * Antes se abria directamente la sede del tramite, pero la app ya no
     * sirve para una sola cosa: puede guiar un tramite o vigilar una
     * tienda, y eso hay que poder elegirlo.
     */
    private val urlInicio = "file:///android_asset/inicio.html"

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        voz = TextToSpeech(this, this)

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // LAS COOKIES SON IMPRESCINDIBLES AQUI.
        //
        // El codigo de seguridad (el captcha) no se comprueba solo: el
        // servidor apunta en la sesion que letras te ha enseñado, y esa
        // sesion viaja en una cookie. Si el navegador interno no las
        // guarda, lo que escribe la persona NUNCA le cuadra al servidor,
        // aunque lo haya copiado perfectamente. Sin esto, el tramite es
        // imposible de terminar.
        CookieManager.getInstance().setAcceptCookie(true)

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            // Algunas sedes de la administracion sirven una version
            // distinta (o directamente rota) si detectan que quien entra
            // no es un navegador normal. Nos presentamos como Chrome.
            settings.userAgentString = settings.userAgentString
                .replace("; wv", "")
                .replace(Regex("Version/\\d+\\.\\d+ "), "")
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            settings.setGeolocationEnabled(true)
            addJavascriptInterface(Puente(), "Android")

            // La pantalla de ayuda pide la ubicacion. Solo se concede a
            // nuestras propias paginas (file://): a una web cualquiera de
            // internet no se le da nunca.
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?, callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    val propia = origin?.startsWith("file://") == true
                    callback?.invoke(origin, propia, false)
                }
            }

            webViewClient = object : WebViewClient() {
                /**
                 * Al empezar a cambiar de pagina hay que CALLARSE en el acto.
                 * Si no, la voz sigue explicando la pantalla anterior mientras
                 * la persona ya esta en otra: la confunde mas que ayudarla.
                 */
                /**
                 * Muchas tiendas (AliExpress, Miravia, Temu...) intentan
                 * saltar a su propia aplicacion con enlaces raros del tipo
                 * "intent://" o "aliexpress://". Nuestro navegador no sabe
                 * abrirlos y la pantalla se quedaba en blanco o se salia
                 * sola -que es lo que se veia-. Aqui se ignoran y se sigue
                 * navegando por la web normal, que ademas es donde nuestra
                 * ayuda puede proteger: dentro de su app no llegamos.
                 */
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: android.webkit.WebResourceRequest?
                ): Boolean {
                    val esquema = request?.url?.scheme?.lowercase() ?: return false
                    // El telefono si tiene que poder abrirse: es lo que usa
                    // la pantalla de ayuda si falla el puente con Android.
                    if (esquema == "tel" || esquema == "sms" || esquema == "mailto") {
                        try {
                            startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW, request.url))
                        } catch (e: Exception) { }
                        return true
                    }
                    if (esquema != "http" && esquema != "https" && esquema != "file") {
                        return true      // bloqueado: no salimos de la web
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    voz?.stop()
                    ultimaFrase = ""
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Cada vez que cambia de pantalla hay que volver a meter
                    // la ayuda: al navegar, la pagina nueva llega limpia.
                    view?.postDelayed({ inyectarAyuda(url) }, 800)
                }
            }
        }
        raiz.addView(
            web,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(raiz)
        web.loadUrl(urlInicio)

        // La ubicacion se pide una sola vez, al arrancar, para que cuando
        // de verdad haga falta (pantalla de ayuda) ya este concedida y no
        // haya que pelearse con un permiso en mitad de un susto.
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        }
    }

    /**
     * Decide que ayuda meter en la pagina segun donde este la persona.
     *
     *   - En la web de la cita del DNI -> la GUIA: hay un tramite concreto
     *     que completar, asi que va señalando casilla por casilla.
     *   - En cualquier otra web (una tienda, por ejemplo) -> el
     *     GUARDAESPALDAS: aqui no hay nada que completar, lo que hace
     *     falta es que alguien avise de las casillas ya marcadas, los
     *     cobros mensuales escondidos y los "gratis" que luego se cobran.
     *   - En la pantalla de inicio de la propia app -> nada.
     */
    private var portadaVisible = false

    private fun inyectarAyuda(url: String?) {
        val u = url ?: return
        if (u.startsWith("file://")) {                      // nuestra propia portada
            portadaVisible = true
            saludar()
            return
        }
        portadaVisible = false

        val fichero = if (u.contains("citapreviadnie")) "guia.js" else "guardaespaldas.js"
        val js = try {
            assets.open(fichero).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            hablar("No he podido cargar la ayuda. Cierre la aplicación y vuelva a abrirla.", true)
            return
        }
        // Se limpian las marcas para que la ayuda se reinicie en la pantalla nueva
        web.evaluateJavascript("window.__guiaViva=false;window.__guardaViva=false;") {
            web.evaluateJavascript(js, null)
        }
    }

    /* ---------- Puente entre la guia y la voz de Android ---------- */
    inner class Puente {
        /**
         * @param frase   lo que hay que decir
         * @param forzar  true si la persona ha pedido que se repita: entonces
         *                se dice aunque sea lo mismo que acaba de sonar
         */
        @JavascriptInterface
        fun decir(frase: String, forzar: Boolean) {
            runOnUiThread { hablar(frase, forzar) }
        }

        /**
         * Abre el marcador del telefono con el numero ya puesto.
         *
         * Se usa ACTION_DIAL y no ACTION_CALL a proposito: DIAL solo
         * PREPARA la llamada, y es la persona quien pulsa el boton verde.
         * Una app que marca sola puede llamar al 112 desde un bolsillo, y
         * eso ocupa una linea de emergencias que otro puede necesitar.
         */
        @JavascriptInterface
        fun llamar(numero: String) {
            runOnUiThread {
                try {
                    val i = android.content.Intent(
                        android.content.Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + numero.filter { it.isDigit() || it == '+' })
                    )
                    startActivity(i)
                } catch (e: Exception) {
                    hablar("No he podido abrir el teléfono. Marque usted el número.", true)
                }
            }
        }

        /** La ubicacion se queda solo aqui, para poder decirla en voz alta
         *  si hace falta. No se guarda en disco ni se manda a ningun sitio. */
        @JavascriptInterface
        fun guardarUbicacion(donde: String) {
            ultimaUbicacion = donde
        }

        /**
         * Volver a la portada.
         *
         * Faltaba, y era un agujero gordo: al entrar en una web te
         * quedabas atrapado dentro. La unica salida era el boton de atras
         * de Android, que muchas personas mayores ni usan ni saben que
         * existe. Ahora hay una casa bien visible en la barra de ayuda.
         */
        @JavascriptInterface
        fun inicio() {
            runOnUiThread {
                voz?.stop()
                yaSaludado = false        // que vuelva a saludar al llegar
                web.loadUrl(urlInicio)
            }
        }
    }

    /**
     * Saluda cuando se dan las dos condiciones a la vez: la voz esta lista
     * Y la portada ya esta en pantalla. Cual de las dos llega antes cambia
     * de un movil a otro, asi que en vez de adivinar se llama desde los dos
     * sitios y gana el ultimo. Si la voz todavia se esta preparando, se
     * reintenta unas cuantas veces antes de rendirse.
     */
    private var yaSaludado = false
    private var intentosSaludo = 0

    private fun saludar() {
        if (yaSaludado || !portadaVisible) return
        if (!vozLista) {
            if (intentosSaludo++ < 12) web.postDelayed({ saludar() }, 500)
            return
        }
        yaSaludado = true
        hablar("Hola. Dígame qué quiere hacer, tocando uno de los dos botones grandes.", true)
    }

    private fun hablar(frase: String, forzar: Boolean) {
        if (!vozLista) return
        if (!forzar && frase == ultimaFrase) return   // no repetir sin motivo: agobia
        ultimaFrase = frase
        voz?.speak(frase, TextToSpeech.QUEUE_FLUSH, null, "guia")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val r = voz?.setLanguage(Locale("es", "ES"))
            vozLista = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            // despacio: se entiende mucho mejor, sobre todo con audifonos
            voz?.setSpeechRate(0.86f)
            // El saludo NO se dice aqui. La voz suele estar lista antes de
            // que la pantalla se vea, y entonces se habla al vacio: la
            // persona no oye nada. Se dice desde saludar(), cuando la
            // portada ya esta delante.
            saludar()
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    /**
     * Al salir de la app hay que CALLARSE. Antes solo se paraba la voz al
     * cerrarla del todo (onDestroy), pero cuando alguien sale con el boton
     * de inicio Android no la cierra: la deja en segundo plano. Resultado:
     * la app seguia hablando sola por detras, que es de las cosas mas
     * molestas que puede hacer un movil.
     *
     * Ademas de la voz hay que parar los temporizadores del navegador: si
     * no, la guia sigue latiendo por dentro y vuelve a mandar frases.
     */
    override fun onPause() {
        super.onPause()
        voz?.stop()
        ultimaFrase = ""        // al volver, que pueda repetir el paso actual
        web.onPause()
        web.pauseTimers()
        // Guardar las cookies en disco: si no, al salir y volver se pierde
        // la sesion y el captcha deja de cuadrar.
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        web.resumeTimers()
    }

    override fun onDestroy() {
        voz?.stop(); voz?.shutdown()
        super.onDestroy()
    }
}
