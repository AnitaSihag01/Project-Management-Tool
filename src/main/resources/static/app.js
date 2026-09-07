const API = "/api";
const WS_URL = "/ws";

let token = null;
let currentProjectId = null;
let currentMembers = [];
let stompClient = null;
let boardSubscription = null;
let notifCount = 0;

function log(msg) {
    const el = document.getElementById("log");
    el.innerHTML += `[${new Date().toLocaleTimeString()}] ${msg}<br>`;
    el.scrollTop = el.scrollHeight;
}

function initials(name) {
    if (!name) return "?";
    return name.trim().split(/\s+/).slice(0, 2).map(w => w[0].toUpperCase()).join("");
}

function showTab(tab) {
    document.getElementById("loginTab").style.display = tab === "login" ? "block" : "none";
    document.getElementById("registerTab").style.display = tab === "register" ? "block" : "none";
    document.getElementById("loginTabBtn").classList.toggle("active", tab === "login");
    document.getElementById("registerTabBtn").classList.toggle("active", tab === "register");
}

function authHeaders(json) {
    const h = { "Authorization": "Bearer " + token };
    if (json) h["Content-Type"] = "application/json";
    return h;
}

function enterApp(data) {
    token = data.token;
    document.getElementById("whoami").textContent = data.fullName;
    document.getElementById("login-section").style.display = "none";
    document.getElementById("board-section").classList.add("show");
    document.getElementById("navRight").style.display = "flex";
    connectWebSocket();
    loadProjects();
}

async function login() {
    const btn = event.target;
    btn.disabled = true; btn.textContent = "Logging in…";
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    try {
        const res = await fetch(`${API}/auth/login`, {
            method: "POST", headers: authHeaders(true),
            body: JSON.stringify({ email, password })
        });
        if (!res.ok) { log("Login failed: " + (await res.text())); return; }
        enterApp(await res.json());
    } finally {
        btn.disabled = false; btn.textContent = "Log in";
    }
}

async function register() {
    const btn = event.target;
    if (btn.disabled) return;
    btn.disabled = true; btn.textContent = "Creating account…";
    try {
        const fullName = document.getElementById("regFullName").value;
        const email = document.getElementById("regEmail").value;
        const password = document.getElementById("regPassword").value;
        const res = await fetch(`${API}/auth/register`, {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ fullName, email, password })
        });
        if (!res.ok) { log("Registration failed: " + (await res.text())); return; }
        enterApp(await res.json());
    } finally {
        btn.disabled = false; btn.textContent = "Create account";
    }
}

function logout() {
    location.reload();
}

function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({ Authorization: "Bearer " + token }, () => {
        log("WebSocket connected");
        stompClient.subscribe("/user/queue/notifications", (message) => {
            const notif = JSON.parse(message.body);
            log("🔔 " + notif.message);
            bumpNotifBadge();
        });
    });
}

// ---------- Projects ----------

async function loadProjects() {
    const res = await fetch(`${API}/projects`, { headers: authHeaders() });
    const projects = await res.json();
    const select = document.getElementById("projectSelect");
    select.innerHTML = "";
    projects.forEach(p => {
        const opt = document.createElement("option");
        opt.value = p.id; opt.textContent = p.name;
        select.appendChild(opt);
    });
    if (projects.length > 0) {
        currentProjectId = projects[0].id;
        await onProjectChange();
    } else {
        log("No projects yet — create one on the left.");
    }
}

async function createProject() {
    const name = document.getElementById("newProjectName").value;
    if (!name.trim()) return;
    await fetch(`${API}/projects`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ name, description: "" })
    });
    document.getElementById("newProjectName").value = "";
    await loadProjects();
}

async function onProjectChange() {
    currentProjectId = document.getElementById("projectSelect").value;
    await loadMembers();
    await loadTasks();
}

// ---------- Members ----------

async function loadMembers() {
    const res = await fetch(`${API}/projects/${currentProjectId}/members`, { headers: authHeaders() });
    currentMembers = res.ok ? await res.json() : [];
    const list = document.getElementById("membersList");
    list.innerHTML = currentMembers.length
        ? currentMembers.map(m => `
            <div class="member-row">
              <span class="avatar">${initials(m.fullName)}</span>
              <span>${m.fullName} <span style="color:var(--ink-soft)">· ${m.role}</span></span>
            </div>`).join("")
        : `<div class="empty-note">No members yet</div>`;
}

async function addMember() {
    const email = document.getElementById("newMemberEmail").value;
    if (!email.trim()) return;
    const res = await fetch(`${API}/projects/${currentProjectId}/members`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ email })
    });
    if (!res.ok) { log("Add member failed: " + (await res.text())); return; }
    document.getElementById("newMemberEmail").value = "";
    await loadMembers();
}

// ---------- Tasks ----------

async function loadTasks() {
    const res = await fetch(`${API}/projects/${currentProjectId}/tasks`, { headers: authHeaders() });
    const tasks = await res.json();
    ["TODO", "IN_PROGRESS", "DONE"].forEach(s => {
        document.getElementById(`col-${s}`).innerHTML = "";
        document.getElementById(`count-${s}`).textContent = tasks.filter(t => t.status === s).length;
    });
    tasks.forEach(renderTaskCard);
    log(`Loaded ${tasks.length} task(s)`);

    if (boardSubscription) boardSubscription.unsubscribe();
    boardSubscription = stompClient.subscribe(`/topic/projects/${currentProjectId}`, () => {
        log("📋 Board updated — refreshing...");
        loadTasks();
    });
}

function renderTaskCard(t) {
    const col = document.getElementById(`col-${t.status}`);
    const card = document.createElement("div");
    card.className = "card";

    const nextStatus = { TODO: "IN_PROGRESS", IN_PROGRESS: "DONE", DONE: null }[t.status];
    const moveBtn = nextStatus
        ? `<button class="move" onclick="moveTask(${t.id}, '${nextStatus}')">Move to ${nextStatus.replace("_", " ")} →</button>`
        : "";

    const assigneeOptions = [`<option value="">Unassigned</option>`]
        .concat(currentMembers.map(m =>
            `<option value="${m.email}" ${t.assigneeName === m.fullName ? "selected" : ""}>${m.fullName}</option>`
        )).join("");

    card.innerHTML = `
        <div class="title">#${t.id} ${t.title}</div>
        <select onchange="assignTask(${t.id}, this.value)">${assigneeOptions}</select>
        ${moveBtn}
        <div>
            <button class="comments-toggle" onclick="toggleComments(${t.id})">Comments</button>
            <div id="comments-${t.id}" style="display:none;"></div>
        </div>
    `;
    col.appendChild(card);
}

async function moveTask(taskId, newStatus) {
    const res = await fetch(`${API}/tasks/${taskId}/status`, {
        method: "PUT", headers: authHeaders(true),
        body: JSON.stringify({ status: newStatus })
    });
    if (!res.ok) { log("Move task failed: " + (await res.text())); return; }
    await loadTasks();
}

async function assignTask(taskId, email) {
    if (!email) return;
    const res = await fetch(`${API}/tasks/${taskId}/assignee`, {
        method: "PUT", headers: authHeaders(true),
        body: JSON.stringify({ email })
    });
    if (!res.ok) { log("Assign failed: " + (await res.text())); return; }
    await loadTasks();
}

async function createTask() {
    const title = document.getElementById("taskTitle").value;
    if (!title.trim()) return;
    const res = await fetch(`${API}/projects/${currentProjectId}/tasks`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ title })
    });
    if (!res.ok) { log("Create task failed: " + (await res.text())); return; }
    document.getElementById("taskTitle").value = "";
    await loadTasks();
}

// ---------- Comments ----------

async function toggleComments(taskId) {
    const box = document.getElementById(`comments-${taskId}`);
    if (box.style.display === "none") {
        box.style.display = "block";
        await loadComments(taskId);
    } else {
        box.style.display = "none";
    }
}

async function loadComments(taskId) {
    const res = await fetch(`${API}/tasks/${taskId}/comments`, { headers: authHeaders() });
    const comments = res.ok ? await res.json() : [];
    const box = document.getElementById(`comments-${taskId}`);
    box.innerHTML = (comments.length
        ? comments.map(c => `<div class="comment-box"><b>${c.authorName}:</b> ${c.content}</div>`).join("")
        : `<div class="comment-box" style="color:var(--ink-soft);">No comments yet</div>`)
      + `<div class="comment-row">
            <input id="newComment-${taskId}" placeholder="Write a comment" />
            <button class="btn btn-outline btn-sm" onclick="addComment(${taskId})">Send</button>
         </div>`;
}

async function addComment(taskId) {
    const input = document.getElementById(`newComment-${taskId}`);
    const content = input.value;
    if (!content.trim()) return;
    await fetch(`${API}/tasks/${taskId}/comments`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ content })
    });
    input.value = "";
    await loadComments(taskId);
}

// ---------- Notifications ----------

function bumpNotifBadge() {
    notifCount++;
    const badge = document.getElementById("notifCount");
    badge.textContent = notifCount;
    badge.style.display = "flex";
}

function toggleNotifs() {
    const panel = document.getElementById("notifDropdown");
    const opening = panel.style.display !== "block";
    panel.style.display = opening ? "block" : "none";
    if (opening) loadNotifications();
}

async function loadNotifications() {
    const res = await fetch(`${API}/notifications`, { headers: authHeaders() });
    const notifs = res.ok ? await res.json() : [];
    const dropdown = document.getElementById("notifDropdown");
    dropdown.innerHTML = notifs.length
        ? notifs.map(n => `<div class="notif-item">${n.message}</div>`).join("")
        : `<div class="notif-empty">No notifications yet</div>`;
    notifCount = 0;
    document.getElementById("notifCount").style.display = "none";
}
