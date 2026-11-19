// Si abres los HTML desde otro lado (no desde Spring), pon aquí 'http://localhost:8080'
const API = '';
const ETIQ = {PENDIENTE:'Pendiente', EN_PREPARACION:'En preparación', LISTO:'Listo', ENTREGADO:'Entregado', CANCELADO:'Cancelado'};
const $ = s => document.querySelector(s);
const lugar = p => p.mesaNumero ? 'Mesa ' + p.mesaNumero : 'Para llevar';

// ---------- Sesión ----------
// El token se guarda en sessionStorage: dura mientras la pestaña esté abierta, y cada pestaña
// puede tener su propia sesión (por ejemplo, mesero en una y cocina en otra).
const PAGINA = {MESERO: 'mesero.html', COCINA: 'cocina.html', ADMINISTRADOR: 'admin.html'};
function sesion() {
  try { return JSON.parse(sessionStorage.getItem('sesion')); } catch (e) { return null; }
}
function irAlLogin() { sessionStorage.removeItem('sesion'); location.replace('index.html'); }

/** Al cargar cada pantalla: sin sesión vuelve al inicio de sesión; con otro rol, a su propia pantalla. */
function exigirSesion(rol) {
  const s = sesion();
  if (!s) { irAlLogin(); throw new Error('Sin sesión'); }
  if (s.rol !== rol) { location.replace(PAGINA[s.rol]); throw new Error('Otro rol'); }
  const salir = document.getElementById('salir');
  if (salir) {
    salir.textContent = s.nombre + ' · Cerrar sesión';
    salir.onclick = async e => { e.preventDefault(); await api('/auth/logout', 'POST').catch(() => {}); irAlLogin(); };
  }
  return s;
}

async function api(ruta, metodo = 'GET', cuerpo, cabeceras = {}) {
  const s = sesion();
  const r = await fetch(API + '/api' + ruta, {
    method: metodo,
    headers: {'Content-Type': 'application/json', ...(s ? {Authorization: 'Bearer ' + s.token} : {}), ...cabeceras},
    body: cuerpo ? JSON.stringify(cuerpo) : undefined});
  if (r.status === 401 && s) { irAlLogin(); throw new Error('Tu sesión terminó. Inicia sesión de nuevo.'); }
  const d = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(d.error || 'Error ' + r.status);
  return d;
}
const pesos = n => new Intl.NumberFormat('es-CO', {style:'currency', currency:'COP', maximumFractionDigits:0}).format(n);
const hora = f => new Date(f).toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'});
const esc = t => String(t).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
function aviso(msg, error, ms = 3500) {
  const a = $('#aviso'); a.textContent = msg; a.className = error ? 'error' : ''; a.style.display = 'block';
  clearTimeout(aviso.t); aviso.t = setTimeout(() => a.style.display = 'none', ms);
}
