"use strict";

const APP_NAME = "Shelfwise";
const TOKEN_KEY = "shelfwise.token";

const state = {
    token: readToken(),
    user: null,
    organization: null,
    loanFilter: "active",
};

// ---------------------------------------------------------------- utilities

function readToken() {
    try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function saveToken(token) {
    state.token = token;
    try {
        if (token) localStorage.setItem(TOKEN_KEY, token);
        else localStorage.removeItem(TOKEN_KEY);
    } catch { /* storage unavailable; token lives in memory only */ }
}

function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
}

function formatDate(iso) {
    if (!iso) return "—";
    const [y, m, d] = iso.split("-").map(Number);
    return new Date(y, m - 1, d).toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function initials(name) {
    return (name || "?").split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join("");
}

function canManage() {
    return state.user && (state.user.role === "OWNER" || state.user.role === "ADMIN");
}

function toast(message, isError = false) {
    const el = document.createElement("div");
    el.className = "toast" + (isError ? " error" : "");
    el.textContent = message;
    document.getElementById("toast-root").appendChild(el);
    setTimeout(() => el.remove(), 3500);
}

class ApiError extends Error {
    constructor(status, message) { super(message); this.status = status; }
}

async function api(path, { method = "GET", body } = {}) {
    const headers = { "Accept": "application/json" };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (state.token) headers["Authorization"] = "Bearer " + state.token;

    const res = await fetch("/api" + path, {
        method, headers, body: body === undefined ? undefined : JSON.stringify(body),
    });

    if (res.status === 401 && state.token) {
        signOut();
        throw new ApiError(401, "Your session has expired. Please sign in again.");
    }
    if (res.status === 204) return null;

    const data = await res.json().catch(() => null);
    if (!res.ok) throw new ApiError(res.status, data?.error || `Request failed (${res.status})`);
    return data;
}

// ---------------------------------------------------------------- routing

const ROUTES = {
    dashboard: { title: "Dashboard", icon: "◧", render: renderDashboard },
    books: { title: "Books", icon: "▤", render: renderBooks },
    members: { title: "Members", icon: "☺", render: renderMembers },
    loans: { title: "Loans", icon: "⇄", render: renderLoans },
    team: { title: "Team", icon: "◎", render: renderTeam, adminOnly: true },
    settings: { title: "Settings", icon: "⚙", render: renderSettings },
};

function currentRoute() {
    const name = location.hash.replace(/^#\/?/, "") || "dashboard";
    return name;
}

function navigate(name) {
    location.hash = "#/" + name;
}

window.addEventListener("hashchange", render);

async function render() {
    const app = document.getElementById("app");
    const route = currentRoute();

    if (!state.token) {
        if (route !== "signup" && route !== "login") return navigate("login");
        app.innerHTML = renderAuth(route);
        bindAuth(route);
        document.title = `${route === "signup" ? "Create your library" : "Sign in"} — ${APP_NAME}`;
        return;
    }

    if (!state.user) {
        app.innerHTML = `<div class="loading">Loading…</div>`;
        try {
            const me = await api("/auth/me");
            state.user = me.user;
            state.organization = me.organization;
        } catch (e) {
            if (e.status !== 401) app.innerHTML = `<div class="loading">Could not reach the server: ${esc(e.message)}</div>`;
            return;
        }
    }

    const page = ROUTES[route];
    if (!page || (page.adminOnly && !canManage())) return navigate("dashboard");

    document.title = `${page.title} — ${state.organization.name} — ${APP_NAME}`;
    app.innerHTML = renderShell(route);
    bindShell();

    const main = document.getElementById("main");
    main.innerHTML = `<div class="loading">Loading…</div>`;
    try {
        await page.render(main);
    } catch (e) {
        main.innerHTML = `<div class="card empty">${esc(e.message)}</div>`;
    }
}

function signOut() {
    saveToken(null);
    state.user = null;
    state.organization = null;
    navigate("login");
    render();
}

// ---------------------------------------------------------------- auth screens

function renderAuth(mode) {
    const signup = mode === "signup";
    return `
    <div class="auth">
        <section class="auth-hero">
            <div class="brand"><span class="brand-mark">▥</span>${APP_NAME}</div>
            <div>
                <h1>Run your whole library from one place.</h1>
                <p>Catalog books, register members and track every loan. Each library gets its own private workspace for its whole team.</p>
                <ul>
                    <li>Multi-copy inventory with live availability</li>
                    <li>Due dates and overdue tracking</li>
                    <li>Invite librarians and admins to your workspace</li>
                    <li>Your data is isolated from every other library</li>
                </ul>
            </div>
            <div style="opacity:.6;font-size:13px">© ${new Date().getFullYear()} ${APP_NAME}</div>
        </section>
        <section class="auth-panel">
            <form class="auth-card" id="auth-form" novalidate>
                <h2>${signup ? "Create your library" : "Welcome back"}</h2>
                <div class="sub">${signup ? "Set up a workspace for your library in under a minute." : "Sign in to your library workspace."}</div>
                <div id="auth-error"></div>
                ${signup ? `
                <div class="field"><label for="organizationName">Library name</label>
                    <input class="input" id="organizationName" name="organizationName" placeholder="Riverside Public Library" required></div>
                <div class="field"><label for="fullName">Your name</label>
                    <input class="input" id="fullName" name="fullName" autocomplete="name" required></div>` : ""}
                <div class="field"><label for="email">Work email</label>
                    <input class="input" id="email" name="email" type="email" autocomplete="email" required></div>
                <div class="field"><label for="password">Password</label>
                    <input class="input" id="password" name="password" type="password"
                        autocomplete="${signup ? "new-password" : "current-password"}" required>
                    ${signup ? `<span class="hint">At least 8 characters.</span>` : ""}</div>
                <button class="btn btn-primary btn-block" type="submit">${signup ? "Create workspace" : "Sign in"}</button>
                <div class="auth-switch">
                    ${signup
                        ? `Already have an account? <a href="#/login">Sign in</a>`
                        : `New here? <a href="#/signup">Create a library workspace</a>`}
                </div>
            </form>
        </section>
    </div>`;
}

function bindAuth(mode) {
    const form = document.getElementById("auth-form");
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;
        try {
            const session = await api(mode === "signup" ? "/auth/signup" : "/auth/login",
                { method: "POST", body: Object.fromEntries(new FormData(form)) });
            saveToken(session.token);
            state.user = session.user;
            state.organization = session.organization;
            navigate("dashboard");
            if (mode === "signup") toast(`Welcome to ${APP_NAME}! Your workspace is ready.`);
        } catch (e) {
            document.getElementById("auth-error").innerHTML = `<div class="form-error">${esc(e.message)}</div>`;
            button.disabled = false;
        }
    });
}

// ---------------------------------------------------------------- shell

function renderShell(active) {
    const links = Object.entries(ROUTES)
        .filter(([, r]) => !r.adminOnly || canManage())
        .map(([name, r]) => `<a href="#/${name}" class="${name === active ? "active" : ""}">
            <span class="icon">${r.icon}</span>${r.title}</a>`)
        .join("");

    return `
    <div class="mobile-bar">
        <button class="btn btn-sm" id="menu-toggle" aria-label="Open menu">☰</button>
        <div class="brand"><span class="brand-mark">▥</span>${APP_NAME}</div>
    </div>
    <div class="shell">
        <aside class="sidebar" id="sidebar">
            <div class="brand"><span class="brand-mark">▥</span>${APP_NAME}</div>
            <div class="org-chip"><div class="label">Workspace</div><div class="name">${esc(state.organization.name)}</div></div>
            <nav class="nav">${links}</nav>
            <div class="spacer"></div>
            <div class="user-box">
                <div class="avatar">${esc(initials(state.user.fullName))}</div>
                <div class="meta"><div>${esc(state.user.fullName)}</div><div>${esc(roleLabel(state.user.role))}</div></div>
                <button class="btn btn-sm" id="sign-out" title="Sign out">⎋</button>
            </div>
        </aside>
        <main class="main" id="main"></main>
    </div>`;
}

function bindShell() {
    document.getElementById("sign-out").addEventListener("click", signOut);
    document.getElementById("menu-toggle").addEventListener("click", () =>
        document.getElementById("sidebar").classList.toggle("open"));
}

function roleLabel(role) {
    return { OWNER: "Owner", ADMIN: "Admin", LIBRARIAN: "Librarian" }[role] || role;
}

function pageHead(title, subtitle, actionsHtml = "") {
    return `<div class="page-head"><div><h1>${esc(title)}</h1>${subtitle ? `<p>${esc(subtitle)}</p>` : ""}</div>
        <div>${actionsHtml}</div></div>`;
}

// ---------------------------------------------------------------- modal forms

/**
 * Opens a modal form. `fields` is a list of {name, label, type, value, options, hint, min, max}.
 * `onSubmit(values)` may throw; its message is shown inside the modal.
 */
function openForm({ title, fields, submitLabel = "Save", onSubmit }) {
    const backdrop = document.createElement("div");
    backdrop.className = "modal-backdrop";
    backdrop.innerHTML = `
        <form class="card modal" novalidate>
            <h2>${esc(title)}</h2>
            <div class="modal-error"></div>
            ${fields.map(fieldHtml).join("")}
            <div class="modal-actions">
                <button type="button" class="btn" data-cancel>Cancel</button>
                <button type="submit" class="btn btn-primary">${esc(submitLabel)}</button>
            </div>
        </form>`;
    document.body.appendChild(backdrop);

    const form = backdrop.querySelector("form");
    const close = () => { backdrop.remove(); document.removeEventListener("keydown", onKey); };
    const onKey = (e) => { if (e.key === "Escape") close(); };
    document.addEventListener("keydown", onKey);
    backdrop.addEventListener("mousedown", (e) => { if (e.target === backdrop) close(); });
    backdrop.querySelector("[data-cancel]").addEventListener("click", close);
    form.querySelector("input, select")?.focus();

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = {};
        for (const f of fields) {
            const raw = form.elements[f.name].value;
            values[f.name] = f.type === "number" || f.type === "select-id" ? (raw === "" ? null : Number(raw)) : raw;
        }
        const button = form.querySelector("button[type=submit]");
        button.disabled = true;
        try {
            await onSubmit(values);
            close();
        } catch (e) {
            form.querySelector(".modal-error").innerHTML = `<div class="form-error">${esc(e.message)}</div>`;
            button.disabled = false;
        }
    });
}

function fieldHtml(f) {
    const id = "f-" + f.name;
    let control;
    if (f.type === "select" || f.type === "select-id") {
        control = `<select class="input" id="${id}" name="${f.name}">
            ${f.placeholder ? `<option value="">${esc(f.placeholder)}</option>` : ""}
            ${f.options.map(o => `<option value="${esc(o.value)}" ${String(o.value) === String(f.value) ? "selected" : ""}
                ${o.disabled ? "disabled" : ""}>${esc(o.label)}</option>`).join("")}
        </select>`;
    } else {
        control = `<input class="input" id="${id}" name="${f.name}" type="${f.type || "text"}"
            value="${esc(f.value ?? "")}" ${f.min != null ? `min="${f.min}"` : ""} ${f.max != null ? `max="${f.max}"` : ""}
            ${f.autocomplete ? `autocomplete="${f.autocomplete}"` : ""}>`;
    }
    return `<div class="field"><label for="${id}">${esc(f.label)}</label>${control}
        ${f.hint ? `<span class="hint">${esc(f.hint)}</span>` : ""}</div>`;
}

function confirmAction(message, onConfirm) {
    openForm({ title: message, fields: [], submitLabel: "Delete", onSubmit: onConfirm });
}

// ---------------------------------------------------------------- dashboard

async function renderDashboard(main) {
    const d = await api("/dashboard");
    const firstName = state.user.fullName.split(" ")[0];

    main.innerHTML = `
        ${pageHead(`Hello, ${firstName}`, `Here's what's happening at ${state.organization.name}.`,
            `<button class="btn btn-primary" id="quick-loan">+ New loan</button>`)}
        <div class="stats">
            ${stat("Titles", d.titles)}
            ${stat("Copies", d.totalCopies)}
            ${stat("Members", d.members)}
            ${stat("On loan", d.activeLoans)}
            ${stat("Overdue", d.overdueLoans, d.overdueLoans > 0)}
        </div>
        <div class="card">
            <div class="card-pad" style="padding-bottom:0"><h3>Due next</h3></div>
            ${d.dueSoon.length ? loanTable(d.dueSoon, false) :
                `<div class="empty">No books are out right now. ${d.titles === 0
                    ? `Start by <a href="#/books">adding your first book</a>.` : ""}</div>`}
        </div>`;

    main.querySelector("#quick-loan").addEventListener("click", () => newLoan(() => render()));
    bindReturnButtons(main, () => render());
}

function stat(label, value, warn = false) {
    return `<div class="card stat ${warn ? "warn" : ""}"><div class="label">${esc(label)}</div>
        <div class="value">${Number(value).toLocaleString()}</div></div>`;
}

// ---------------------------------------------------------------- books

async function renderBooks(main, query = "") {
    main.innerHTML = `
        ${pageHead("Books", "Your catalog and what's on the shelf right now.",
            `<button class="btn btn-primary" id="add-book">+ Add book</button>`)}
        <div class="toolbar"><input class="input" id="book-search" type="search"
            placeholder="Search title, author or ISBN" value="${esc(query)}"></div>
        <div class="card" id="book-list"><div class="loading">Loading…</div></div>`;

    const list = main.querySelector("#book-list");
    const load = async () => {
        const q = main.querySelector("#book-search").value;
        const books = await api("/books" + (q ? "?q=" + encodeURIComponent(q) : ""));
        list.innerHTML = books.length ? `
            <div class="table-wrap"><table>
                <thead><tr><th>Title</th><th>Author</th><th>ISBN</th><th>Available</th><th></th></tr></thead>
                <tbody>${books.map(b => `<tr>
                    <td><strong>${esc(b.title)}</strong></td>
                    <td>${esc(b.author)}</td>
                    <td class="muted">${esc(b.isbn)}</td>
                    <td><span class="badge ${b.availableCopies === 0 ? "badge-red" : "badge-green"}">
                        ${b.availableCopies} / ${b.copies}</span></td>
                    <td class="actions">
                        <button class="btn btn-sm" data-edit="${b.id}">Edit</button>
                        <button class="btn btn-sm btn-danger" data-delete="${b.id}">Delete</button>
                    </td></tr>`).join("")}
                </tbody></table></div>`
            : `<div class="empty">${q ? "No books match your search." : "No books yet. Add your first title to get started."}</div>`;

        list.querySelectorAll("[data-edit]").forEach(btn => btn.addEventListener("click", () =>
            bookForm(books.find(b => b.id === Number(btn.dataset.edit)), load)));
        list.querySelectorAll("[data-delete]").forEach(btn => btn.addEventListener("click", () => {
            const book = books.find(b => b.id === Number(btn.dataset.delete));
            confirmAction(`Delete “${book.title}”?`, async () => {
                await api("/books/" + book.id, { method: "DELETE" });
                toast("Book deleted");
                load();
            });
        }));
    };

    let timer;
    main.querySelector("#book-search").addEventListener("input", () => {
        clearTimeout(timer);
        timer = setTimeout(load, 250);
    });
    main.querySelector("#add-book").addEventListener("click", () => bookForm(null, load));
    await load();
}

function bookForm(book, onDone) {
    openForm({
        title: book ? "Edit book" : "Add a book",
        submitLabel: book ? "Save changes" : "Add book",
        fields: [
            { name: "title", label: "Title", value: book?.title },
            { name: "author", label: "Author", value: book?.author },
            { name: "isbn", label: "ISBN", value: book?.isbn },
            { name: "copies", label: "Copies", type: "number", min: 1, value: book?.copies ?? 1,
                hint: "How many physical copies your library owns." },
        ],
        onSubmit: async (values) => {
            await api(book ? "/books/" + book.id : "/books", { method: book ? "PUT" : "POST", body: values });
            toast(book ? "Book updated" : "Book added");
            onDone();
        },
    });
}

// ---------------------------------------------------------------- members

async function renderMembers(main) {
    main.innerHTML = `
        ${pageHead("Members", "People who can borrow from your library.",
            `<button class="btn btn-primary" id="add-member">+ Add member</button>`)}
        <div class="card" id="member-list"><div class="loading">Loading…</div></div>`;

    const list = main.querySelector("#member-list");
    const load = async () => {
        const members = await api("/members");
        list.innerHTML = members.length ? `
            <div class="table-wrap"><table>
                <thead><tr><th>Name</th><th>Email</th><th></th></tr></thead>
                <tbody>${members.map(m => `<tr>
                    <td><strong>${esc(m.name)}</strong></td>
                    <td class="muted">${esc(m.email)}</td>
                    <td class="actions">
                        <button class="btn btn-sm" data-edit="${m.id}">Edit</button>
                        <button class="btn btn-sm btn-danger" data-delete="${m.id}">Delete</button>
                    </td></tr>`).join("")}
                </tbody></table></div>`
            : `<div class="empty">No members yet.</div>`;

        list.querySelectorAll("[data-edit]").forEach(btn => btn.addEventListener("click", () =>
            memberForm(members.find(m => m.id === Number(btn.dataset.edit)), load)));
        list.querySelectorAll("[data-delete]").forEach(btn => btn.addEventListener("click", () => {
            const member = members.find(m => m.id === Number(btn.dataset.delete));
            confirmAction(`Delete ${member.name}?`, async () => {
                await api("/members/" + member.id, { method: "DELETE" });
                toast("Member deleted");
                load();
            });
        }));
    };

    main.querySelector("#add-member").addEventListener("click", () => memberForm(null, load));
    await load();
}

function memberForm(member, onDone) {
    openForm({
        title: member ? "Edit member" : "Add a member",
        submitLabel: member ? "Save changes" : "Add member",
        fields: [
            { name: "name", label: "Full name", value: member?.name },
            { name: "email", label: "Email", type: "email", value: member?.email },
        ],
        onSubmit: async (values) => {
            await api(member ? "/members/" + member.id : "/members", { method: member ? "PUT" : "POST", body: values });
            toast(member ? "Member updated" : "Member added");
            onDone();
        },
    });
}

// ---------------------------------------------------------------- loans

async function renderLoans(main) {
    const filters = { active: "On loan", overdue: "Overdue", returned: "Returned", all: "All" };
    main.innerHTML = `
        ${pageHead("Loans", `Loans last ${state.organization.loanDurationDays} days by default.`,
            `<button class="btn btn-primary" id="new-loan">+ New loan</button>`)}
        <div class="toolbar"><div class="tabs">${Object.entries(filters).map(([key, label]) =>
            `<button data-filter="${key}" class="${key === state.loanFilter ? "active" : ""}">${label}</button>`).join("")}
        </div></div>
        <div class="card" id="loan-list"><div class="loading">Loading…</div></div>`;

    const list = main.querySelector("#loan-list");
    const load = async () => {
        main.querySelectorAll("[data-filter]").forEach(b => b.classList.toggle("active", b.dataset.filter === state.loanFilter));
        const loans = await api("/loans?status=" + state.loanFilter);
        list.innerHTML = loans.length ? loanTable(loans, true) : `<div class="empty">Nothing here.</div>`;
        bindReturnButtons(list, load);
    };

    main.querySelectorAll("[data-filter]").forEach(btn => btn.addEventListener("click", () => {
        state.loanFilter = btn.dataset.filter;
        load();
    }));
    main.querySelector("#new-loan").addEventListener("click", () => newLoan(load));
    await load();
}

function loanStatus(loan) {
    if (loan.returnDate) return `<span class="badge badge-gray">Returned</span>`;
    if (loan.overdue) return `<span class="badge badge-red">Overdue</span>`;
    const days = Math.round((new Date(loan.dueDate) - new Date(new Date().toISOString().slice(0, 10))) / 86400000);
    if (days <= 2) return `<span class="badge badge-amber">Due ${days === 0 ? "today" : `in ${days}d`}</span>`;
    return `<span class="badge badge-blue">On loan</span>`;
}

function loanTable(loans, showReturned) {
    return `<div class="table-wrap"><table>
        <thead><tr><th>Book</th><th>Member</th><th>Issued</th><th>Due</th>
            ${showReturned ? "<th>Returned</th>" : ""}<th>Status</th><th></th></tr></thead>
        <tbody>${loans.map(l => `<tr>
            <td><strong>${esc(l.book.title)}</strong><div class="muted">${esc(l.book.author)}</div></td>
            <td>${esc(l.member.name)}<div class="muted">${esc(l.member.email)}</div></td>
            <td>${formatDate(l.issueDate)}</td>
            <td>${formatDate(l.dueDate)}</td>
            ${showReturned ? `<td>${formatDate(l.returnDate)}</td>` : ""}
            <td>${loanStatus(l)}</td>
            <td class="actions">${l.returnDate ? "" : `<button class="btn btn-sm" data-return="${l.id}">Mark returned</button>`}</td>
        </tr>`).join("")}</tbody></table></div>`;
}

function bindReturnButtons(container, onDone) {
    container.querySelectorAll("[data-return]").forEach(btn => btn.addEventListener("click", async () => {
        btn.disabled = true;
        try {
            await api(`/loans/${btn.dataset.return}/return`, { method: "POST" });
            toast("Book returned");
            onDone();
        } catch (e) {
            toast(e.message, true);
            btn.disabled = false;
        }
    }));
}

async function newLoan(onDone) {
    let books, members;
    try {
        [books, members] = await Promise.all([api("/books"), api("/members")]);
    } catch (e) {
        return toast(e.message, true);
    }
    if (!books.length || !members.length) {
        return toast(!books.length ? "Add a book before creating a loan." : "Add a member before creating a loan.", true);
    }

    openForm({
        title: "New loan",
        submitLabel: "Issue book",
        fields: [
            { name: "bookId", label: "Book", type: "select-id", placeholder: "Choose a book…",
                options: books.map(b => ({ value: b.id, label: `${b.title} — ${b.availableCopies} of ${b.copies} available`,
                    disabled: b.availableCopies === 0 })) },
            { name: "memberId", label: "Member", type: "select-id", placeholder: "Choose a member…",
                options: members.map(m => ({ value: m.id, label: `${m.name} (${m.email})` })),
                hint: `Due back in ${state.organization.loanDurationDays} days.` },
        ],
        onSubmit: async (values) => {
            if (!values.bookId || !values.memberId) throw new Error("Choose a book and a member.");
            await api("/loans", { method: "POST", body: values });
            toast("Book issued");
            onDone();
        },
    });
}

// ---------------------------------------------------------------- team

async function renderTeam(main) {
    main.innerHTML = `
        ${pageHead("Team", "Staff who can sign in to this workspace.",
            `<button class="btn btn-primary" id="add-staff">+ Add staff</button>`)}
        <div class="card" id="team-list"><div class="loading">Loading…</div></div>`;

    const list = main.querySelector("#team-list");
    const load = async () => {
        const staff = await api("/team");
        list.innerHTML = `<div class="table-wrap"><table>
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th></th></tr></thead>
            <tbody>${staff.map(u => {
                const removable = u.id !== state.user.id && u.role !== "OWNER"
                    && (u.role !== "ADMIN" || state.user.role === "OWNER");
                return `<tr>
                    <td><strong>${esc(u.fullName)}</strong>${u.id === state.user.id ? ` <span class="muted">(you)</span>` : ""}</td>
                    <td class="muted">${esc(u.email)}</td>
                    <td><span class="badge ${u.role === "LIBRARIAN" ? "badge-gray" : "badge-blue"}">${roleLabel(u.role)}</span></td>
                    <td class="actions">${removable ? `<button class="btn btn-sm btn-danger" data-remove="${u.id}">Remove</button>` : ""}</td>
                </tr>`;
            }).join("")}</tbody></table></div>`;

        list.querySelectorAll("[data-remove]").forEach(btn => btn.addEventListener("click", () => {
            const user = staff.find(u => u.id === Number(btn.dataset.remove));
            confirmAction(`Remove ${user.fullName} from the team?`, async () => {
                await api("/team/" + user.id, { method: "DELETE" });
                toast("Staff member removed");
                load();
            });
        }));
    };

    main.querySelector("#add-staff").addEventListener("click", () => openForm({
        title: "Add staff member",
        submitLabel: "Add to team",
        fields: [
            { name: "fullName", label: "Full name" },
            { name: "email", label: "Email", type: "email" },
            { name: "password", label: "Temporary password", type: "password", autocomplete: "new-password",
                hint: "Share this with them so they can sign in." },
            { name: "role", label: "Role", type: "select", value: "LIBRARIAN", options: [
                { value: "LIBRARIAN", label: "Librarian — books, members and loans" },
                { value: "ADMIN", label: "Admin — also manages team and settings" },
            ] },
        ],
        onSubmit: async (values) => {
            await api("/team", { method: "POST", body: values });
            toast("Staff member added");
            load();
        },
    }));
    await load();
}

// ---------------------------------------------------------------- settings

async function renderSettings(main) {
    const org = await api("/organization");
    state.organization = org;
    const editable = canManage();

    main.innerHTML = `
        ${pageHead("Settings", editable ? "Workspace-wide settings for your library." : "Only owners and admins can change these settings.")}
        <form class="card card-pad" id="settings-form" style="max-width:560px" novalidate>
            <div id="settings-error"></div>
            <div class="field"><label for="s-name">Library name</label>
                <input class="input" id="s-name" name="name" value="${esc(org.name)}" ${editable ? "" : "disabled"}></div>
            <div class="row">
                <div class="field"><label for="s-days">Loan duration (days)</label>
                    <input class="input" id="s-days" name="loanDurationDays" type="number" min="1" max="365"
                        value="${org.loanDurationDays}" ${editable ? "" : "disabled"}></div>
                <div class="field"><label for="s-max">Max active loans per member</label>
                    <input class="input" id="s-max" name="maxActiveLoansPerMember" type="number" min="1" max="100"
                        value="${org.maxActiveLoansPerMember}" ${editable ? "" : "disabled"}></div>
            </div>
            <p class="muted" style="margin-top:0">Workspace created ${new Date(org.createdAt).toLocaleDateString()}.</p>
            ${editable ? `<button class="btn btn-primary" type="submit">Save settings</button>` : ""}
        </form>`;

    if (!editable) return;
    const form = main.querySelector("#settings-form");
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const values = Object.fromEntries(new FormData(form));
        values.loanDurationDays = Number(values.loanDurationDays);
        values.maxActiveLoansPerMember = Number(values.maxActiveLoansPerMember);
        try {
            state.organization = await api("/organization", { method: "PUT", body: values });
            toast("Settings saved");
            render();
        } catch (e) {
            main.querySelector("#settings-error").innerHTML = `<div class="form-error">${esc(e.message)}</div>`;
        }
    });
}

render();
