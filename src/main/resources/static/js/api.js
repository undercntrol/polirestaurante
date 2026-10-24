// Si abres los HTML desde otro lado (no desde Spring), pon aquí 'http://localhost:8080'
const API = '';
const ETIQ = {PENDIENTE:'Pendiente', EN_PREPARACION:'En preparación', LISTO:'Listo', ENTREGADO:'Entregado', CANCELADO:'Cancelado'};
const $ = s => document.querySelector(s);

async function api(ruta, metodo = 'GET', cuerpo) {
  const r = await fetch(API + '/api' + ruta, {
    method: metodo, headers: {'Content-Type': 'application/json'},
    body: cuerpo ? JSON.stringify(cuerpo) : undefined});
  const d = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(d.error || 'Error ' + r.status);
  return d;
}
const pesos = n => new Intl.NumberFormat('es-CO', {style:'currency', currency:'COP', maximumFractionDigits:0}).format(n);
const hora = f => new Date(f).toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit'});
const esc = t => String(t).replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
function aviso(msg, error) {
  const a = $('#aviso'); a.textContent = msg; a.className = error ? 'error' : ''; a.style.display = 'block';
  clearTimeout(aviso.t); aviso.t = setTimeout(() => a.style.display = 'none', 3500);
}
