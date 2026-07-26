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

    /**
     * Cita previa del DNI (Policia Nacional).
     *
     * Antes apuntaba al certificado digital de la FNMT, pero al mirar esa
     * web de verdad se vio que obliga a instalar un programa de escritorio
     * -en un movil el tramite no se puede terminar-. Este si se completa
     * entero desde el telefono, y ademas tiene pantallas mucho mas simples.
     */
    private val urlInicio = "https://www.citapreviadnie.es/"

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
            addJavascriptInterface(Puente(), "Android")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Cada vez que cambia de pantalla hay que volver a meter
                    // la guia: al navegar, la pagina nueva llega limpia.
                    view?.postDelayed({ inyectarGuia() }, 800)
                }
            }
        }
        raiz.addView(
            web,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(raiz)
        web.loadUrl(urlInicio)
    }

    /** Mete guia.js (que vive dentro de la app) en la pagina de la FNMT. */
    private fun inyectarGuia() {
        val js = try {
            assets.open("guia.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            hablar("No he podido cargar la ayuda. Cierre la aplicación y vuelva a abrirla.", true)
            return
        }
        // Se limpia la marca para que la guia se reinicie en la pantalla nueva
        web.evaluateJavascript("window.__guiaViva = false;") {
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
            if (vozLista) {
                hablar("Le voy a ayudar a sacar el certificado digital. " +
                       "Toque siempre donde vea el círculo naranja.", true)
            }
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
