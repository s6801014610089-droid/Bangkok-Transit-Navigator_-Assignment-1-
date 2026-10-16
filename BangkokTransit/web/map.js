// Draws the whole network as an SVG map and highlights a route on top of it.
// Station positions are chosen by hand (the CSV has no coordinates).

const LINE_COLOR = {
  'BTS Sukhumvit Line': '#6cc24a',
  'BTS Silom Line': '#00693e',
  'MRT Blue Line': '#1d4ea1',
  'MRT Blue Line Branch': '#1d4ea1',
  'MRT Purple Line': '#7b3f98'
};
const GREY = '#8a8f98';
const ROUTE = '#ff7a00';
const MAP_W = 1760, MAP_H = 800, PAD_TOP = 50;   // extra room so full station names fit
const NS = 'http://www.w3.org/2000/svg';

// Hand-made label positions for crowded spots: [dx, dy, text-anchor] from the station
const LABEL = {
  N9:   { n: [-14, 38, 'end'] },
  N8:   { n: [10, 38, 'start'] },
  CEN:  { n: [0, -34, 'middle'], r: [22, -30, 'start'] },
  S2:   { n: [0, -16, 'middle'] },
  N1:   { r: [-14, -28, 'end'] },
  S12:  { c: [0, -16, 'middle'], n: [0, -32, 'middle'], r: [0, -24, 'middle'] },
  BL33: { c: [14, -6, 'start'], n: [14, -22, 'start'], r: [-20, 6, 'end'] },
  BL32: { c: [14, 12, 'start'] },
  BL09: { c: [0, 22, 'middle'], n: [0, 38, 'middle'] }
};
function anchorFor(nx) { return nx > 0.5 ? 'start' : nx < -0.5 ? 'end' : 'middle'; }

let pos = {};          // code -> {x, y, nx, ny}  (nx, ny = which way the label goes)
let loopCodes = [];    // Blue Line loop stations in order
let loopInfo = null;
let net = null;
let svg = null;
let edgeGeom = {};     // "A|B" (sorted) -> list of points
let edgeEls = {};      // "A|B" -> {line elements}
let stationEls = {};   // code -> group element
let routeLayer = null;

// ---------- layout ----------

function computeLayout(stationCodes) {
  const has = c => stationCodes.has(c);
  pos = {};

  // BTS Sukhumvit: one straight horizontal row
  const sukh = [];
  for (let n = 24; n >= 1; n--) if (has('N' + n)) sukh.push('N' + n);
  sukh.push('CEN');
  for (let e = 1; e <= 23; e++) if (has('E' + e)) sukh.push('E' + e);
  sukh.forEach((c, i) => {
    pos[c] = { x: 40 + i * (1420 / (sukh.length - 1)), y: 110, nx: 0, ny: -1 };
  });

  // MRT Blue Line loop: a rounded rectangle, BL01 near the top-right corner
  loopCodes = [];
  for (let i = 1; i <= 31; i++) {
    const c = 'BL' + String(i).padStart(2, '0');
    if (has(c)) loopCodes.push(c);
    if (c === 'BL05' && has('BL05A')) loopCodes.push('BL05A');
  }
  const r = 100, yTop = 330, xT = 1409, Lx = 886;
  const P = 2 * Lx + 2 * Math.PI * r;
  const d = P / loopCodes.length;
  const s0 = -25 * (d / 75);
  loopInfo = { r, yTop, xT, Lx, P, d, s0 };
  loopCodes.forEach((c, i) => { pos[c] = loopPoint(s0 + i * d); });

  // MRT Blue Line branch (BL32 .. BL37)
  const br = { BL32: [1465, 297], BL33: [1495, 262], BL34: [1560, 262],
    BL35: [1625, 262], BL36: [1625, 330], BL37: [1625, 398] };
  for (const c in br) if (has(c)) pos[c] = { x: br[c][0], y: br[c][1], nx: 0, ny: -1 };
  if (pos.BL36) { pos.BL36.nx = -1; pos.BL36.ny = 0; }
  if (pos.BL37) { pos.BL37.nx = -1; pos.BL37.ny = 0; }

  // BTS Silom: W1 left of Siam, the rest in a second row below
  if (pos.CEN && has('W1')) pos.W1 = { x: pos.CEN.x - 50, y: 175, nx: 0, ny: 1 };
  const sil = ['S1', 'S2', 'S3', 'S5', 'S6', 'S7', 'S8', 'S9', 'S10', 'S11', 'S12'].filter(has);
  const xS2 = pos.BL25 ? pos.BL25.x : 1173;
  const xS12 = 1495;
  const afterS2 = sil.filter(c => c !== 'S1' && c !== 'S2');
  if (has('S1')) pos.S1 = { x: 800 + (xS2 - 800) * 0.45, y: 190, nx: 0, ny: 1 };
  if (has('S2')) pos.S2 = { x: xS2, y: 190, nx: 0, ny: 1 };
  afterS2.forEach((c, i) => {
    pos[c] = { x: xS2 + (i + 1) * (xS12 - xS2) / afterS2.length, y: 190, nx: 0, ny: 1 };
  });

  // MRT Purple Line: one station, above BL09
  if (has('PP16') && pos.BL09) pos.PP16 = { x: pos.BL09.x, y: 262, nx: 0, ny: -1 };
}

// s = distance along the loop. Top edge runs right-to-left, then the left arc,
// the bottom edge runs left-to-right, then the right arc back to the top.
function loopPoint(s) {
  const { r, yTop, xT, Lx, P } = loopInfo;
  s = ((s % P) + P) % P;
  const cxL = xT - Lx, cy = yTop + r;
  if (s <= Lx) return { x: xT - s, y: yTop, nx: 0, ny: -1 };
  s -= Lx;
  if (s <= Math.PI * r) {
    const t = s / r;
    return { x: cxL - r * Math.sin(t), y: cy - r * Math.cos(t), nx: -Math.sin(t), ny: -Math.cos(t) };
  }
  s -= Math.PI * r;
  if (s <= Lx) return { x: cxL + s, y: yTop + 2 * r, nx: 0, ny: 1 };
  s -= Lx;
  const t = s / r;
  return { x: xT + r * Math.sin(t), y: cy + r * Math.cos(t), nx: Math.sin(t), ny: Math.cos(t) };
}

function key(a, b) { return a < b ? a + '|' + b : b + '|' + a; }

// The points a line edge is drawn through (the loop follows the curve)
function geometry(e) {
  const ia = loopCodes.indexOf(e.a), ib = loopCodes.indexOf(e.b);
  const n = loopCodes.length;
  if (ia >= 0 && ib >= 0 && e.type === 'Line Track' &&
      (((ia + 1) % n === ib) || ((ib + 1) % n === ia))) {
    const forward = ((ia + 1) % n === ib);
    const from = forward ? ia : ib;
    const pts = [];
    const steps = 12;
    for (let k = 0; k <= steps; k++) {
      const p = loopPoint(loopInfo.s0 + (from + k / steps) * loopInfo.d);
      pts.push([p.x, p.y]);
    }
    return pts;
  }
  const A = pos[e.a], B = pos[e.b];
  if ((e.a === 'CEN' && e.b === 'S1') || (e.a === 'S1' && e.b === 'CEN')) {
    return [[pos.CEN.x, pos.CEN.y], [800, 190], [pos.S1.x, pos.S1.y]];
  }
  return [[A.x, A.y], [B.x, B.y]];
}

// ---------- drawing ----------

function el(name, attrs, parent) {
  const n = document.createElementNS(NS, name);
  for (const k in attrs) n.setAttribute(k, attrs[k]);
  if (parent) parent.appendChild(n);
  return n;
}

function pointsToD(pts) {
  return pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join(' ');
}

function drawMap(network, container) {
  net = network;
  const codes = new Set(net.stations.map(s => s.code));
  computeLayout(codes);

  container.innerHTML = '';
  svg = el('svg', { viewBox: '0 ' + (-PAD_TOP) + ' ' + MAP_W + ' ' + (MAP_H + PAD_TOP), id: 'mapSvg', role: 'img',
    'aria-label': 'Map of the Bangkok rail network' });
  container.appendChild(svg);

  const gEdges = el('g', { class: 'base' }, svg);
  const gLabels = el('g', { class: 'base' }, svg);
  const gStations = el('g', { class: 'base' }, svg);
  routeLayer = el('g', {}, svg);
  edgeGeom = {}; edgeEls = {}; stationEls = {};

  // which stations are interchanges
  const interchange = new Set(net.crossPlatform);
  for (const e of net.edges) if (e.type !== 'Line Track') { interchange.add(e.a); interchange.add(e.b); }

  // edges: walking links first (dashed grey), then the coloured lines
  const order = net.edges.slice().sort((x, y) => (x.type === 'Line Track') - (y.type === 'Line Track'));
  for (const e of order) {
    if (!pos[e.a] || !pos[e.b]) continue;
    const pts = geometry(e);
    const k = key(e.a, e.b);
    edgeGeom[k] = pts;
    const attrs = e.type === 'Line Track'
        ? { d: pointsToD(pts), fill: 'none', stroke: LINE_COLOR[e.line] || '#444',
          'stroke-width': 6, 'stroke-linecap': 'round', 'stroke-linejoin': 'round' }
        : { d: pointsToD(pts), fill: 'none', stroke: GREY, 'stroke-width': 2.5,
          'stroke-dasharray': '7 5' };
    (edgeEls[k] = edgeEls[k] || []).push(el('path', attrs, gEdges));
  }

  // Siam cross-platform self-loop (dashed grey circle next to the station)
  for (const c of net.crossPlatform) {
    if (!pos[c]) continue;
    const p = pos[c];
    const loop = el('circle', { cx: p.x + 16, cy: p.y - 16, r: 13, fill: 'none', stroke: GREY,
      'stroke-width': 2.5, 'stroke-dasharray': '5 4' }, gEdges);
    edgeEls['SELF|' + c] = [loop];
    edgeGeom['SELF|' + c] = [[p.x + 16, p.y - 16]];
  }

  // stations
  const names = {};
  net.stations.forEach(s => { names[s.code] = s.name; });
  const lineOf = {};
  for (const e of net.edges) if (e.type === 'Line Track') { lineOf[e.a] = lineOf[e.a] || e.line; lineOf[e.b] = lineOf[e.b] || e.line; }
  lineOf.PP16 = 'MRT Purple Line';

  for (const s of net.stations) {
    const p = pos[s.code];
    if (!p) continue;
    const g = el('g', {}, gStations);
    const color = LINE_COLOR[lineOf[s.code]] || '#444';
    const isX = interchange.has(s.code);
    el('circle', { cx: p.x, cy: p.y, r: isX ? 8 : 5.5, fill: '#fff',
      stroke: isX ? '#111' : color, 'stroke-width': isX ? 3 : 2.5 }, g);
    const tip = el('title', {}, g);
    tip.textContent = s.code + ' - ' + s.name;
    stationEls[s.code] = g;

    const off = isX ? 20 : 15;
    const ov = LABEL[s.code] || {};
    if (isX) {
      // interchange stations: code + bold name (hand-placed where crowded)
      const c = ov.c || [p.nx * off + (p.nx > 0.5 ? 2 : p.nx < -0.5 ? -2 : 0), p.ny * off + 4, anchorFor(p.nx)];
      const label = el('text', { x: p.x + c[0], y: p.y + c[1], 'font-size': 12,
        'text-anchor': c[2], fill: '#222' }, gLabels);
      label.textContent = s.code;
      const n = ov.n || [p.nx * (off + 15), p.ny * (off + 15) + 4, anchorFor(p.nx)];
      const nm = el('text', { x: p.x + n[0], y: p.y + n[1], 'font-size': 12, 'font-weight': 700,
        'text-anchor': n[2], fill: '#111' }, gLabels);
      nm.textContent = s.name;
    } else {
      // normal stations: the NAME is the label (the code stays in the hover tooltip)
      const short = s.name;   // full name
      let t;
      const rightSide = ['BL35', 'BL36', 'BL37'].includes(s.code);   // short branch: names on the right
      if (rightSide) {
        t = el('text', { x: p.x + 11, y: p.y + 3.5, 'font-size': 10.5,
          'text-anchor': 'start', fill: '#333' }, gLabels);
      } else if (Math.abs(p.nx) > 0.5) {     // left / right side: horizontal text
        t = el('text', { x: p.x + p.nx * 11, y: p.y + 3.5, 'font-size': 10.5,
          'text-anchor': anchorFor(p.nx), fill: '#333' }, gLabels);
      } else {                               // top / bottom: text turned sideways
        const up = p.ny < 0 && p.y < 200;   // blue top row: names go down, inside the loop
        const x = p.x + 3.5, y = p.y + (up ? -11 : 11);
        t = el('text', { x: x, y: y, 'font-size': 10.5, 'text-anchor': 'start', fill: '#333',
          transform: 'rotate(' + (up ? -90 : 90) + ' ' + x + ' ' + y + ')' }, gLabels);
      }
      t.textContent = short;
    }
  }

  // line names
  const tag = (x, y, text, color) => {
    const t = el('text', { x, y, 'font-size': 15, 'font-weight': 700, fill: color }, gLabels);
    t.textContent = text;
  };
  // line names at the free end of each line (same colour as the line)
  if (pos.E23) tag(pos.E23.x + 18, pos.E23.y + 5, 'BTS Sukhumvit Line', '#4a9a2c');
  if (pos.S12) tag((pos.S9 || pos.S12).x - 40, pos.S12.y - 16, 'BTS Silom Line', LINE_COLOR['BTS Silom Line']);
  tag(620, 450, 'MRT Blue Line (loop + branch)', LINE_COLOR['MRT Blue Line']);
  if (pos.PP16) tag(pos.PP16.x + 16, pos.PP16.y + 5, 'MRT Purple Line', LINE_COLOR['MRT Purple Line']);

  drawLegend(gLabels);
}

function drawLegend(g) {
  const x0 = 40, y0 = 775;
  const items = [['BTS Sukhumvit Line', LINE_COLOR['BTS Sukhumvit Line'], false],
    ['BTS Silom Line', LINE_COLOR['BTS Silom Line'], false],
    ['MRT Blue Line', LINE_COLOR['MRT Blue Line'], false],
    ['MRT Purple Line', LINE_COLOR['MRT Purple Line'], false],
    ['Interchange (walk, or cross-platform at Siam)', GREY, true],
    ['Your route', ROUTE, false]];
  items.forEach((it, i) => {
    const y = y0 + i * 0;
    const x = x0 + [0, 190, 360, 520, 690, 1090][i];
    el('line', { x1: x, y1: y, x2: x + 36, y2: y, stroke: it[1], 'stroke-width': it[2] ? 2.5 : 6,
      'stroke-dasharray': it[2] ? '7 5' : '' }, g);
    const t = el('text', { x: x + 44, y: y + 5, 'font-size': 13, fill: '#333' }, g);
    t.textContent = it[0];
  });
}

// ---------- route highlight ----------

function clearRoute() {
  if (!svg) return;
  svg.classList.remove('dim');
  routeLayer.innerHTML = '';
  for (const k in edgeEls) edgeEls[k].forEach(n => n.classList.remove('onroute'));
  for (const c in stationEls) stationEls[c].classList.remove('onroute');
}

// path = list of {code, name, line} from /api/route
function highlightRoute(path) {
  clearRoute();
  if (!svg || !path || path.length < 2) return;
  svg.classList.add('dim');

  const onRoute = new Set();
  const changeAt = new Set();
  for (let i = 0; i < path.length; i++) {
    onRoute.add(path[i].code);
    if (i + 1 < path.length) {
      const a = path[i], b = path[i + 1];
      let k = key(a.code, b.code);
      if (a.code === b.code) k = 'SELF|' + a.code;   // cross-platform at Siam
      if (a.code === b.code || a.line !== b.line) { changeAt.add(a.code); changeAt.add(b.code); }
      const pts = edgeGeom[k];
      if (!pts) continue;
      if (a.code === b.code) {
        el('circle', { cx: pts[0][0], cy: pts[0][1], r: 13, fill: 'none', stroke: ROUTE,
          'stroke-width': 5, 'stroke-dasharray': '5 4' }, routeLayer);
      } else {
        const isWalk = a.line !== b.line;
        // white halo under the orange stroke so it stands out on any line colour
        el('path', { d: pointsToD(pts), fill: 'none', stroke: '#fff', 'stroke-width': 15,
          'stroke-linecap': 'round', 'stroke-linejoin': 'round' }, routeLayer);
        el('path', { d: pointsToD(pts), fill: 'none', stroke: ROUTE, 'stroke-width': isWalk ? 5 : 10,
          'stroke-linecap': 'round', 'stroke-linejoin': 'round',
          'stroke-dasharray': isWalk ? '7 5' : '' }, routeLayer);
      }
    }
  }
  // draw the lines again on top of the orange so the line colour stays visible
  for (let i = 0; i + 1 < path.length; i++) {
    const a = path[i], b = path[i + 1];
    if (a.code === b.code || a.line !== b.line) continue;
    const pts = edgeGeom[key(a.code, b.code)];
    if (pts) el('path', { d: pointsToD(pts), fill: 'none', stroke: LINE_COLOR[a.line] || '#444',
      'stroke-width': 4, 'stroke-linecap': 'round' }, routeLayer);
  }

  // markers: every station on the route gets a name; start, end and changes get a big marker
  const first = path[0].code, last = path[path.length - 1].code;
  const seen = new Set();
  for (const s of path) {
    if (seen.has(s.code)) continue;
    seen.add(s.code);
    const p = pos[s.code];
    if (!p) continue;
    const isEnd = s.code === first || s.code === last;
    const isChange = changeAt.has(s.code);
    if (isEnd || isChange) {
      el('circle', { cx: p.x, cy: p.y, r: 12, fill: isEnd ? ROUTE : '#fff', stroke: '#111',
        'stroke-width': 3 }, routeLayer);
    } else {
      el('circle', { cx: p.x, cy: p.y, r: 6, fill: '#fff', stroke: ROUTE, 'stroke-width': 3 }, routeLayer);
    }
  }
  // name labels (only for the big markers, to avoid clutter), with a white outline
  const label = (code, text, dy) => {
    const p = pos[code];
    if (!p) return;
    const ov = (LABEL[code] || {}).r;
    const x = p.x + (ov ? ov[0] : p.nx * 26), y = p.y + (ov ? ov[1] : p.ny * 26 + 5) + (dy || 0);
    const t = el('text', { x, y, 'font-size': 14, 'font-weight': 800, 'text-anchor': ov ? ov[2] : anchorFor(p.nx),
      fill: '#111', stroke: '#fff', 'stroke-width': 4, 'paint-order': 'stroke' }, routeLayer);
    t.textContent = text;
  };
  const shown = new Set();
  for (const s of path) {
    if (shown.has(s.code)) continue;
    const isEnd = s.code === first || s.code === last;
    if (!(isEnd || changeAt.has(s.code))) continue;
    shown.add(s.code);
    const prefix = s.code === first ? 'Start: ' : s.code === last ? 'End: ' : 'Change: ';
    label(s.code, prefix + s.name, 0);
  }
}