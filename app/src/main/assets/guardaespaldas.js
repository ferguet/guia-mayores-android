/* =====================================================================
   MODO GUARDAESPALDAS
   =====================================================================
   Se inyecta en cualquier tienda o web normal. Aqui no hay ningun
   tramite que completar, asi que no guia: VIGILA.

   Nace de una observacion de campo: a las personas mayores no les
   arruina el dia no encontrar un boton. Les arruina el mes descubrir
   que llevan seis meses pagando una suscripcion que nunca pidieron, o
   un seguro que venia marcado de fabrica en la pantalla de pagar.

   Lo bueno de las trampas es que casi todas tienen la misma forma en
   todas las tiendas: una casilla ya marcada, un precio con "al mes"
   escondido, un boton gigante de aceptar frente a uno gris de
   rechazar. Por eso esto NO necesita un guion por cada web: reconoce
   la forma de la trampa, no la tienda.

   Lo que NO hace, a proposito:
     - No pulsa nada por la persona. Ni desmarca casillas. Avisa y ya:
       la decision es suya, no mia.
     - No lee ni guarda lo que escribe.
     - No manda nada a ningun sitio.
   ===================================================================== */
(function () {
  if (window.__guardaViva) return;
  window.__guardaViva = true;

  function visible(e) {
    if (!e || !e.offsetParent) return false;
    var r = e.getBoundingClientRect();
    return r.width > 6 && r.height > 6 && r.top < window.innerHeight * 3;
  }
  function limpio(t) { return (t || '').replace(/\s+/g, ' ').trim(); }

  /* Texto que rodea a un elemento: para entender de que va */
  function entorno(e, saltos) {
    var t = '', n = e;
    for (var i = 0; i < (saltos || 3) && n; i++) { n = n.parentElement; if (n) t = limpio(n.textContent); }
    return t.toLowerCase();
  }

  /* ---------------------------------------------------------------
     LAS TRAMPAS
     Ordenadas por lo que cuesta caer en ellas. Una casilla marcada que
     te cobra todos los meses es mucho peor que un aviso de cookies.
     --------------------------------------------------------------- */

  var PAGO = /suscrip|renovaci[oó]n autom|al mes|\/ ?mes|mensual|cada mes|anual|al a[ñn]o|prueba gratis|primer mes|periodo de prueba|seguro|protecci[oó]n|garant[ií]a extendida|cancelaci[oó]n flexible|premium/i;
  var PRECIO = /\d+[.,]\d{2}\s*€|€\s*\d+[.,]\d{2}|\d+\s*euros/i;

  function trampas() {
    var lista = [];

    /* 1. Casillas YA MARCADAS que tienen que ver con dinero.
          Es la trampa mas cara: nadie desmarca lo que ya viene puesto. */
    var cajas = [].slice.call(document.querySelectorAll('input[type=checkbox]'));
    for (var i = 0; i < cajas.length; i++) {
      var c = cajas[i];
      if (!c.checked || !visible(c)) continue;
      var ent = entorno(c, 3);
      if (PAGO.test(ent)) {
        lista.push({
          el: c, gravedad: 3, corto: 'Casilla marcada',
          voz: 'Atencion a esto. Hay una casilla ya marcada que usted no ha tocado, y tiene que ver con un pago. ' +
               'Muchas tiendas las dejan puestas a proposito para colarle un seguro o una suscripcion. ' +
               'Lea lo que pone al lado y, si no lo ha pedido, quitele la marca usted mismo. Yo no se la voy a quitar: es su decision.'
        });
      }
    }

    /* 2. Un precio con "al mes" o "renovacion automatica" cerca.
          Creen que compran una cosa y estan contratando una cuota. */
    var trozos = [].slice.call(document.querySelectorAll('p,span,div,li,label,td,strong,h2,h3'));
    for (var j = 0; j < trozos.length; j++) {
      var z = trozos[j];
      if (z.children.length > 0 || !visible(z)) continue;
      var t = limpio(z.textContent);
      if (t.length < 6 || t.length > 180) continue;
      if (PRECIO.test(t) && /al mes|\/ ?mes|mensual|cada mes|renovaci[oó]n autom|al a[ñn]o/i.test(t)) {
        lista.push({
          el: z, gravedad: 2, corto: 'Le cobran cada mes',
          voz: 'Cuidado, esto no es un pago de una vez. Fijese: pone un precio y al lado dice que es al mes. ' +
               'Si sigue adelante se lo van a cobrar todos los meses hasta que usted lo cancele. ' +
               'Si lo que queria era comprar una cosa suelta, esto no es lo que busca.'
        });
        break;
      }
    }

    /* 3. "Gratis" que luego se cobra solo. */
    for (var k = 0; k < trozos.length; k++) {
      var z2 = trozos[k];
      if (z2.children.length > 0 || !visible(z2)) continue;
      var t2 = limpio(z2.textContent);
      if (t2.length < 8 || t2.length > 200) continue;
      if (/prueba gratis|gratis durante|primer mes gratis|30 d[ií]as gratis|gratis el primer/i.test(t2)) {
        lista.push({
          el: z2, gravedad: 2, corto: 'Gratis solo al principio',
          voz: 'Ojo con la palabra gratis de aqui. Es gratis solo los primeros dias. Cuando se acaben, empiezan a cobrarle solos, sin avisar y sin volver a preguntarle. ' +
               'Si acepta esto, apunte en el calendario cuando termina.'
        });
        break;
      }
    }

    /* 4. Aviso de cookies: se busca el boton de RECHAZAR, que siempre
          esta mas escondido y en gris que el de aceptar. */
    var botones = [].slice.call(document.querySelectorAll('button,a,input[type=button]'));
    var rechazar = null, hayCookies = false;
    for (var m = 0; m < botones.length; m++) {
      var b = botones[m];
      if (!visible(b)) continue;
      var s = limpio(b.textContent || b.value).toLowerCase();
      if (/aceptar todas|acepto|aceptar y continuar|permitir todas/.test(s)) hayCookies = true;
      if (/rechazar|solo (las )?necesarias|solo esenciales|denegar|continuar sin aceptar|no aceptar/.test(s)) rechazar = b;
    }
    if (rechazar && (hayCookies || /cookie/i.test(limpio(document.body.textContent).slice(0, 3000)))) {
      lista.push({
        el: rechazar, gravedad: 1, corto: 'Pulse el gris',
        voz: 'Le esta preguntando si le dejan seguirle por internet para enseñarle anuncios. ' +
             'El boton grande y de color es el que dice que si. El que le señalo, el discreto, es el que dice que no. ' +
             'Puede pulsar el que le señalo tranquilamente: la pagina funciona igual de bien.'
      });
    }

    lista.sort(function (a, b) { return b.gravedad - a.gravedad; });
    return lista;
  }

  /* ---------------- pintura ---------------- */
  var capa, marco, barra, corto, tranquilo, ultimo = null, quieto = 0;

  function crear() {
    capa = document.createElement('div');
    capa.id = '__guardaespaldas';

    var css = document.createElement('style');
    css.textContent = '@keyframes __gPar{0%,100%{opacity:1}50%{opacity:.35}}';

    // Aqui no se usa circulo sino un RECUADRO: no se señala un boton
    // que pulsar, se rodea una cosa de la que hay que desconfiar.
    marco = document.createElement('div');
    marco.setAttribute('style',
      'position:fixed;border:5px solid #e63900;border-radius:10px;' +
      'box-shadow:0 0 0 4px #fff,0 0 26px rgba(230,57,0,.85);' +
      'z-index:2147483646;pointer-events:none;animation:__gPar 1.2s ease-in-out infinite;' +
      'transition:all .3s ease;display:none');

    // Señal discreta de que hay alguien vigilando.
    //
    // Antes, cuando no habia ningun peligro, no se veia absolutamente
    // nada. Eso esta mal por dos motivos: la persona no sabe si la estan
    // cuidando o si la app se ha colgado, y nosotros tampoco podemos
    // distinguir "todo bien" de "esto no funciona". Una banda finita y
    // tranquila lo resuelve sin molestar.
    tranquilo = document.createElement('div');
    tranquilo.setAttribute('style',
      'position:fixed;left:0;right:0;bottom:0;background:#0b7a3b;color:#fff;' +
      'padding:11px 16px calc(11px + env(safe-area-inset-bottom));z-index:2147483645;' +
      'font-family:system-ui,-apple-system,sans-serif;font-size:16px;' +
      'text-align:center;box-shadow:0 -4px 16px rgba(0,0,0,.3)');
    tranquilo.textContent = '\u{1F6E1} Vigilando por usted';

    barra = document.createElement('div');
    barra.setAttribute('style',
      'position:fixed;left:0;right:0;bottom:0;background:#b3261e;color:#fff;' +
      'padding:20px 18px calc(20px + env(safe-area-inset-bottom));z-index:2147483647;' +
      'font-family:system-ui,-apple-system,sans-serif;' +
      'box-shadow:0 -8px 30px rgba(0,0,0,.5);display:none;align-items:center;gap:14px');

    corto = document.createElement('div');
    corto.setAttribute('style', 'flex:1;font-size:28px;font-weight:800;line-height:1.2');

    var rep = document.createElement('button');
    rep.textContent = '\u{1F50A}';
    rep.setAttribute('style',
      'flex-shrink:0;width:62px;height:62px;border-radius:50%;border:4px solid #fff;' +
      'background:transparent;color:#fff;font-size:26px');
    rep.onclick = function (ev) {
      ev.preventDefault(); ev.stopPropagation();
      if (window.__ultimaVoz) decir(window.__ultimaVoz, true);
    };

    barra.appendChild(corto); barra.appendChild(rep);
    capa.appendChild(css); capa.appendChild(marco);
    capa.appendChild(tranquilo); capa.appendChild(barra);
    document.documentElement.appendChild(capa);
  }

  function rodear(el) {
    var r = el.getBoundingClientRect();
    if (r.top < 60 || r.bottom > window.innerHeight - 150) {
      el.scrollIntoView({ block: 'center', behavior: 'smooth' });
      r = el.getBoundingClientRect();
    }
    marco.style.left = (r.left - 7) + 'px';
    marco.style.top = (r.top - 7) + 'px';
    marco.style.width = (r.width + 14) + 'px';
    marco.style.height = (r.height + 14) + 'px';
    marco.style.display = 'block';
  }

  function decir(f, forzar) {
    try { if (window.Android && window.Android.decir) { window.Android.decir(f, !!forzar); return; } } catch (e) {}
    if ('speechSynthesis' in window) {
      speechSynthesis.cancel();
      var u = new SpeechSynthesisUtterance(f);
      u.lang = 'es-ES'; u.rate = 0.86;
      speechSynthesis.speak(u);
    }
  }

  function latido() {
    var t = trampas();
    if (!t.length) {
      // Todo tranquilo: se quita el recuadro y el aviso, pero se deja la
      // banda verde. Callarse del todo hacia imposible saber si estaba
      // vigilando o si la app se habia caido.
      marco.style.display = 'none';
      barra.style.display = 'none';
      tranquilo.style.display = 'block';
      ultimo = null;
      return;
    }
    var p = t[0];
    tranquilo.style.display = 'none';
    barra.style.display = 'flex';
    rodear(p.el);
    if (ultimo !== p.corto) {
      ultimo = p.corto; quieto = 0;
      corto.textContent = p.corto;
      window.__ultimaVoz = p.voz;
      decir(p.voz, false);
    } else {
      quieto++;
      if (quieto === 20) { decir(p.voz, true); quieto = 0; }
    }
  }

  crear();
  setTimeout(latido, 1200);
  setInterval(latido, 1600);
  window.addEventListener('scroll', function () {
    var t = trampas();
    if (t.length) rodear(t[0].el);
  }, true);
})();
