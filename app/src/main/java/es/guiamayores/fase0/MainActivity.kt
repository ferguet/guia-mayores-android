package es.guiamayores.fase0

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * FASE 0 - la prueba que decide si el proyecto es viable.
 *
 * La pregunta a responder es una sola: ¿podemos abrir una sede electronica
 * REAL dentro de nuestra propia app y dibujar encima la ayuda?
 *
 * Ya sabemos que meterla en un iframe dentro de una web es imposible: el
 * servidor de la FNMT responde "X-Frame-Options: SAMEORIGIN" y lo prohibe.
 * Pero un WebView no es un iframe: es un navegador propio haciendo una
 * navegacion normal, asi que esa prohibicion no le afecta. Aqui se
 * comprueba si ademas podemos inyectarle nuestra capa de ayuda encima.
 *
 * Esta app NO rellena formularios, NO guarda datos y NO pide contraseñas.
 * Solo abre la pagina y pinta encima. Es una prueba tecnica, nada mas.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var estado: TextView
    private lateinit var web: WebView

    /** La pagina real de la FNMT donde se solicita el certificado. */
    private val urlReal =
        "https://www.sede.fnmt.gob.es/certificados/persona-fisica/obtener-certificado-software/solicitar-certificado"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Barra de estado propia: para ver de un vistazo si la prueba salio bien
        estado = TextView(this).apply {
            text = "Abriendo la web real de la FNMT…"
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#0a5fa8"))
            setPadding(28, 34, 28, 28)
            gravity = Gravity.CENTER_VERTICAL
        }
        raiz.addView(
            estado,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    estado.text = "Página cargada. Pintando la ayuda encima…"
                    // pequeña espera: algunas sedes reordenan la pagina al terminar
                    view?.postDelayed({ inyectarAyuda() }, 900)
                }

                override fun onReceivedError(
                    view: WebView?, errorCode: Int, description: String?, failingUrl: String?
                ) {
                    estado.setBackgroundColor(Color.parseColor("#b3261e"))
                    estado.text = "No se pudo abrir la web: " + (description ?: "error " + errorCode)
                }
            }
        }
        raiz.addView(
            web,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(raiz)
        web.loadUrl(urlReal)
    }

    /**
     * Inyecta la capa de ayuda sobre la pagina real. Devuelve por consola el
     * texto del elemento señalado, para poder confirmar que de verdad se ha
     * enganchado a un elemento real de la FNMT y no a algo inventado.
     */
    private fun inyectarAyuda() {
        web.evaluateJavascript(JS_AYUDA) { resultado ->
            val limpio = resultado?.trim('"') ?: ""
            if (limpio.isEmpty() || limpio == "null" || limpio.startsWith("ERROR")) {
                estado.setBackgroundColor(Color.parseColor("#b3261e"))
                estado.text = "No se pudo pintar encima. " + limpio
            } else {
                estado.setBackgroundColor(Color.parseColor("#0b7a3b"))
                estado.text = "✅ Funciona. Señalando: " + limpio.replace("\\n", " ").take(48)
            }
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}

/**
 * El codigo que se mete dentro de la pagina de la FNMT.
 *
 * Nota para el futuro: aqui el objetivo se busca a mano con palabras clave.
 * En la version buena esto lo decidiria la IA leyendo la estructura de la
 * pantalla (nunca lo que la persona haya escrito, por proteccion de datos).
 * Escrito sin comillas invertidas ni simbolos de dolar a proposito, para que
 * Kotlin no intente interpretarlo como plantilla.
 */
private const val JS_AYUDA = """
(function(){
  try{
    var viejo = document.getElementById('__ayuda_mayores');
    if(viejo) viejo.remove();

    // Buscar un elemento real y visible de la pagina al que señalar
    var cand = [].slice.call(document.querySelectorAll('a,button'));
    cand = cand.filter(function(e){
      var r = e.getBoundingClientRect();
      return r.width > 40 && r.height > 10 && e.offsetParent !== null;
    });
    var obj = null;
    for(var i=0;i<cand.length;i++){
      if(/solicit|certificad|internet/i.test(cand[i].textContent)){ obj = cand[i]; break; }
    }
    if(!obj) obj = cand[0];
    if(!obj) return 'ERROR: no se encontro ningun elemento';

    obj.scrollIntoView({block:'center'});
    var r = obj.getBoundingClientRect();
    var cx = r.left + Math.min(40, r.width/2);
    var cy = r.top + r.height/2;

    var capa = document.createElement('div');
    capa.id = '__ayuda_mayores';

    var punto = document.createElement('div');
    punto.setAttribute('style',
      'position:fixed;left:'+cx+'px;top:'+cy+'px;width:64px;height:64px;'+
      'margin:-32px 0 0 -32px;border-radius:50%;border:6px solid #ff6b00;'+
      'box-shadow:0 0 0 5px #fff,0 0 26px rgba(255,107,0,.95);'+
      'z-index:2147483647;pointer-events:none;animation:__lat 1.2s ease-in-out infinite');

    var mano = document.createElement('div');
    mano.textContent = '\u{1F446}';
    mano.setAttribute('style',
      'position:fixed;left:'+(cx+12)+'px;top:'+(cy+20)+'px;font-size:34px;'+
      'z-index:2147483647;pointer-events:none');

    var panel = document.createElement('div');
    panel.setAttribute('style',
      'position:fixed;left:0;right:0;bottom:0;background:#0b7a3b;color:#fff;'+
      'padding:20px 22px;z-index:2147483647;font-family:system-ui,sans-serif;'+
      'box-shadow:0 -8px 30px rgba(0,0,0,.45)');
    panel.innerHTML =
      '<div style="font-size:15px;opacity:.9;margin-bottom:6px">Paso 1 de 4</div>'+
      '<div style="font-size:28px;font-weight:800;line-height:1.25;margin-bottom:8px">'+
      'Pulse donde está el círculo naranja.</div>'+
      '<div style="font-size:17px;line-height:1.4;opacity:.95">'+
      'Es la página oficial de verdad. Yo le voy acompañando.</div>';

    var css = document.createElement('style');
    css.textContent = '@keyframes __lat{0%,100%{transform:scale(1);opacity:1}'+
                      '50%{transform:scale(1.3);opacity:.5}}';

    capa.appendChild(css);
    capa.appendChild(punto);
    capa.appendChild(mano);
    capa.appendChild(panel);
    document.documentElement.appendChild(capa);

    return (obj.textContent || 'elemento').trim().substring(0,60);
  }catch(e){
    return 'ERROR: ' + e.message;
  }
})();
"""
