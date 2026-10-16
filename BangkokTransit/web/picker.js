/* Grouped station picker: enhances every <input list="stationList">.
   Groups by line (colored header), ordered by station code. Input value stays = station name. */
(function () {
    var GROUPS = [
        { id: 'sukhumvit', title: 'BTS สายสุขุมวิท (Sukhumvit) N24 → E23', color: '#7ac143', test: /^(N|E|CEN)/ },
        { id: 'silom',     title: 'BTS สายสีลม (Silom) W1 → S12',          color: '#1b7a4d', test: /^(W|S\d|CEN)/ },
        { id: 'blue',      title: 'MRT สายสีน้ำเงิน (Blue) BL01 → BL37',    color: '#1f4fa3', test: /^BL/ },
        { id: 'purple',    title: 'MRT สายสีม่วง (Purple) PP16',            color: '#7b3fa0', test: /^PP/ },
        { id: 'other',     title: 'อื่น ๆ',                                  color: '#888',    test: /./ }
    ];
    function lineOf(code, gid) {
        if (code === 'CEN') return gid === 'sukhumvit' || gid === 'silom';
        for (var i = 0; i < GROUPS.length; i++) if (GROUPS[i].id === gid) return GROUPS[i].test.test(code);
        return false;
    }
    function primaryGroup(code) {
        if (code === 'CEN') return ['sukhumvit', 'silom'];
        if (/^(N|E)\d/.test(code)) return ['sukhumvit'];
        if (/^(W|S)\d/.test(code)) return ['silom'];
        if (/^BL/.test(code)) return ['blue'];
        if (/^PP/.test(code)) return ['purple'];
        return ['other'];
    }
    function sortKey(code) {           // N24..N1, CEN, E1..E23 ; W1, CEN, S1.. ; BL01..
        if (code === 'CEN') return 0;
        var m = /^([A-Z]+)(\d+)([A-Z]?)$/.exec(code); if (!m) return 9999;
        var n = parseInt(m[2], 10) + (m[3] ? 0.5 : 0);
        if (m[1] === 'N' || m[1] === 'W') return -n;   // before CEN, descending
        return n;
    }
    function readOptions() {
        var out = [];
        var opts = document.querySelectorAll('#stationList option');
        opts.forEach(function (o) {
            var name = o.value, text = (o.label || o.textContent || '');
            var codes = text.match(/CEN|(?:BL|PP|N|E|W|S)\d+[A-Z]?/g) || [];
            if (name) out.push({ name: name, codes: codes });
        });
        return out;
    }
    function build(items) {
        var map = {};
        GROUPS.forEach(function (g) { map[g.id] = []; });
        items.forEach(function (it) {
            var codes = it.codes.length ? it.codes : ['?'];
            codes.forEach(function (c) {
                primaryGroup(c).forEach(function (gid) { map[gid].push({ code: c, name: it.name }); });
            });
        });
        GROUPS.forEach(function (g) { map[g.id].sort(function (a, b) { return sortKey(a.code) - sortKey(b.code); }); });
        return map;
    }
    var css = document.createElement('style');
    css.textContent =
        '.sp-wrap{position:relative}' +
        '.sp-panel{position:absolute;z-index:50;left:0;right:0;top:100%;max-height:340px;overflow:auto;background:#fff;border:1px solid #d9d9d9;border-radius:10px;box-shadow:0 8px 24px rgba(0,0,0,.15);margin-top:4px;display:none;min-width:260px}' +
        '.sp-head{position:sticky;top:0;color:#fff;font-size:12px;font-weight:700;padding:5px 10px}' +
        '.sp-item{display:flex;align-items:center;gap:8px;padding:5px 10px;cursor:pointer;font-size:14px;color:#222}' +
        '.sp-item.act,.sp-item:hover{background:#fff4dc}' +
        '.sp-badge{min-width:44px;text-align:center;color:#fff;border-radius:999px;font-size:11px;font-weight:700;padding:2px 6px}';
    document.head.appendChild(css);

    function enhance(input) {
        if (input._sp) return; input._sp = true;
        input.removeAttribute('list');
        input.setAttribute('autocomplete', 'off');
        var wrap = document.createElement('div'); wrap.className = 'sp-wrap';
        wrap.style.display = 'block';
        input.parentNode.insertBefore(wrap, input); wrap.appendChild(input);
        var panel = document.createElement('div'); panel.className = 'sp-panel'; wrap.appendChild(panel);
        var rows = [], active = -1;

        function render() {
            var q = input.value.trim().toLowerCase();
            var data = build(readOptions());
            panel.innerHTML = ''; rows = [];
            GROUPS.forEach(function (g) {
                var list = data[g.id].filter(function (x) {
                    return !q || x.name.toLowerCase().indexOf(q) >= 0 || x.code.toLowerCase().indexOf(q) >= 0;
                });
                if (!list.length) return;
                var h = document.createElement('div'); h.className = 'sp-head'; h.style.background = g.color; h.textContent = g.title;
                panel.appendChild(h);
                list.forEach(function (x) {
                    var d = document.createElement('div'); d.className = 'sp-item';
                    d.innerHTML = '<span class="sp-badge" style="background:' + g.color + '"></span><span></span>';
                    d.children[0].textContent = x.code; d.children[1].textContent = x.name;
                    d.addEventListener('mousedown', function (e) { e.preventDefault(); pick(x.name); });
                    panel.appendChild(d); rows.push(d); d._name = x.name;
                });
            });
            active = -1;
        }
        function open() { render(); panel.style.display = 'block'; }
        function close() { panel.style.display = 'none'; }
        function pick(name) {
            input.value = name; close();
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        }
        function mark(i) {
            rows.forEach(function (r) { r.classList.remove('act'); });
            if (i >= 0 && i < rows.length) { active = i; rows[i].classList.add('act'); rows[i].scrollIntoView({ block: 'nearest' }); }
        }
        input.addEventListener('focus', open);
        input.addEventListener('click', open);
        input.addEventListener('input', function () { if (panel.style.display !== 'block') panel.style.display = 'block'; render(); });
        input.addEventListener('blur', close);
        input.addEventListener('keydown', function (e) {
            if (e.key === 'ArrowDown') { e.preventDefault(); if (panel.style.display !== 'block') open(); mark(Math.min(active + 1, rows.length - 1)); }
            else if (e.key === 'ArrowUp') { e.preventDefault(); mark(Math.max(active - 1, 0)); }
            else if (e.key === 'Enter' && active >= 0) { e.preventDefault(); pick(rows[active]._name); }
            else if (e.key === 'Escape') close();
        });
    }
    function init() {
        var dl = document.getElementById('stationList');
        if (!dl) return;
        function go() {
            if (!dl.children.length) return false;
            document.querySelectorAll('input[list="stationList"]').forEach(enhance);
            return true;
        }
        if (!go()) new MutationObserver(function (m, ob) { if (go()) ob.disconnect(); }).observe(dl, { childList: true });
    }
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();