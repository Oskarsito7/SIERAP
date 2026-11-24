/*
 * SIERAP wireframes: builds the browser bar and the app shell of every frame.
 *
 * A frame with data-role gets the sidebar with that role's menu (approved by the lead
 * on 2026-09-27, docs/06; Reports and Audit log were added later, as in roleMenus.ts),
 * the top bar with the user and its content in the main area.
 * A frame without data-role (login, email links) gets the public header and footer.
 * Colors, logos and fonts follow the UPC identity of the frontend (ADR-019).
 *
 *   <div class="frame" data-role="teacher" data-active="groups"
 *        data-title="Mis grupos" data-crumb="Momento 1 · 2026-2" data-path="/grupos">
 *     ...screen content...
 *   </div>
 */
(function () {
  "use strict";

  var MENUS = {
    super_admin: [
      ["faculties", "Facultades y programas"],
      ["periods", "Periodos académicos"],
      ["parameters", "Parámetros globales"],
      ["types", "Tipos de evaluador"],
      ["admins", "Administradores"],
      ["reports", "Reportes"],
      ["audit", "Bitácora"]
    ],
    admin: [
      ["moments", "Momentos"],
      ["teachers", "Docentes"],
      ["codes", "Códigos de registro"],
      ["reports", "Reportes"],
      ["audit", "Bitácora"]
    ],
    teacher: [
      ["groups", "Mis grupos"],
      ["students", "Mis estudiantes"],
      ["evaluators", "Evaluadores externos"],
      ["codes", "Códigos de registro"],
      ["reports", "Reportes"]
    ],
    student: [
      ["moment", "Grupos del momento"],
      ["mygroup", "Mi grupo y entregas"],
      ["grades", "Mis notas"]
    ],
    external_evaluator: [
      ["assigned", "Grupos asignados"]
    ]
  };

  var USERS = {
    super_admin: ["AS", "Administrador SIERAP", "Super administrador"],
    admin: ["LM", "Laura Mendoza", "Admin · Fac. de Ingenierías y Tecnologías"],
    teacher: ["MD", "Marta Díaz", "Docente · Fac. de Ingenierías y Tecnologías"],
    student: ["JR", "Juan Rojas", "Estudiante · Ing. de Sistemas · 2.º semestre"],
    external_evaluator: ["SV", "Sofía Vega", "Evaluadora externa · Empresario"]
  };

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (text) node.textContent = text;
    return node;
  }

  function browserBar(path) {
    var bar = el("div", "w-browser");
    var dots = el("div", "w-dots");
    for (var i = 0; i < 3; i++) dots.appendChild(el("i"));
    bar.appendChild(dots);
    bar.appendChild(el("div", "w-url", "sierap.unicesar.edu.co" + (path || "/")));
    return bar;
  }

  function sidebar(role, active) {
    var side = el("aside", "w-side");
    var brand = el("div", "w-brand");
    var logo = el("img");
    logo.src = "img/upc-logo-horizontal-blanco.svg";
    logo.alt = "Universidad Popular del Cesar";
    brand.appendChild(logo);
    brand.appendChild(el("b", null, "SIERAP"));
    brand.appendChild(el("span", null, "Universidad Popular del Cesar"));
    side.appendChild(brand);

    var nav = el("nav", "w-nav");
    (MENUS[role] || []).forEach(function (item) {
      nav.appendChild(el("span", item[0] === active ? "is-active" : null, item[1]));
    });
    side.appendChild(nav);

    var foot = el("nav", "w-nav w-side-foot");
    foot.appendChild(el("span", active === "account" ? "is-active" : null, "Mi cuenta"));
    foot.appendChild(el("span", null, "Cerrar sesión"));
    side.appendChild(foot);
    return side;
  }

  function topBar(role, title, crumb) {
    var top = el("header", "w-top");
    var left = el("div");
    if (crumb) left.appendChild(el("div", "w-crumb", crumb));
    left.appendChild(el("div", "w-h2", title || ""));
    top.appendChild(left);

    var info = USERS[role] || ["?", "", ""];
    var user = el("div", "w-user");
    user.appendChild(el("div", "w-avatar", info[0]));
    var names = el("div", null, info[1]);
    names.appendChild(el("small", null, info[2]));
    user.appendChild(names);
    top.appendChild(user);
    return top;
  }

  document.querySelectorAll(".frame").forEach(function (frame) {
    var data = frame.dataset;
    var content = Array.prototype.slice.call(frame.childNodes);

    if (data.role) {
      var shell = el("div", "w-shell");
      shell.appendChild(sidebar(data.role, data.active));
      var main = el("div", "w-main");
      main.appendChild(topBar(data.role, data.title, data.crumb));
      var body = el("main", "w-content");
      content.forEach(function (node) { body.appendChild(node); });
      main.appendChild(body);
      shell.appendChild(main);
      frame.appendChild(shell);
    } else {
      var head = el("header", "w-public-head");
      var mark = el("img");
      mark.src = "img/upc-logo-horizontal.svg";
      mark.alt = "Universidad Popular del Cesar";
      head.appendChild(mark);
      var name = el("div");
      name.appendChild(el("b", null, "SIERAP"));
      name.appendChild(el("span", null, "Sistema Integral Evaluador de Resultados de Aprendizajes por Programa"));
      head.appendChild(name);
      frame.insertBefore(head, frame.firstChild);
      var foot = el("footer", "w-public-foot");
      foot.appendChild(el("b", null, "Universidad Popular del Cesar · Educación y Futuro"));
      foot.appendChild(el("span", null, "Programa de Ingeniería de Sistemas · Valledupar, Cesar"));
      frame.appendChild(foot);
    }
    frame.insertBefore(browserBar(data.path), frame.firstChild);
  });
})();
