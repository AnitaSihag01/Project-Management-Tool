const API = "/api";
const WS_URL = "/ws";

let token = null;
let currentProjectId = null;
let currentMembers = [];
let stompClient = null;
let boardSubscription = null;

function log(msg) {
    const el = document.getElementById("log");
    el.innerHTML += `[${new Date().toLocaleTimeString()}] ${msg}<br>`;
    el.scrollTop = el.scrollHeight;
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
    document.getElementById("board-section").style.display = "block";
    document.getElementById("navRight").style.display = "flex";
    connectWebSocket();
    loadProjects();
}

async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const res = await fetch(`${API}/auth/login`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ email, password })
    });
    if (!res.ok) { log("Login failed: " + (await res.text())); return; }
    enterApp(await res.json());
}

async function register() {
    async function register() {
        const btn = event.target;
        if (btn.disabled) return;
        btn.disabled = true;
    const fullName = document.getElementById("regFullName").value;
    const email = document.getElementById("regEmail").value;
    const password = document.getElementById("regPassword").value;
    const res = await fetch(`${API}/auth/register`, {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ fullName, email, password })
    });
    if (!res.ok) { log("Registration failed: " + (await res.text())); return; }
    enterApp(await res.json());
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
    list.innerHTML = currentMembers.map(m =>
        `<div>${m.fullName} <span class="text-muted">(${m.role})</span></div>`
    ).join("") || `<div class="text-muted">No members</div>`;
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
    ["TODO", "IN_PROGRESS", "DONE"].forEach(s => document.getElementById(`col-${s}`).innerHTML = "");
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
    card.className = "task-card";

    const nextStatus = { TODO: "IN_PROGRESS", IN_PROGRESS: "DONE", DONE: null }[t.status];
    const moveBtn = nextStatus
        ? `<button class="btn btn-sm btn-outline-secondary mt-2" onclick="moveTask(${t.id}, '${nextStatus}')">Move to ${nextStatus.replace("_", " ")} →</button>`
        : "";

    const assigneeOptions = [`<option value="">Unassigned</option>`]
        .concat(currentMembers.map(m =>
            `<option value="${m.email}" ${m.email === (t.assigneeName && m.fullName === t.assigneeName ? m.email : "") ? "selected" : ""}>${m.fullName}</option>`
        )).join("");

    card.innerHTML = `
        <div class="fw-semibold">#${t.id} ${t.title}</div>
        <select class="form-select form-select-sm mt-1" onchange="assignTask(${t.id}, this.value)">
            ${assigneeOptions}
        </select>
        ${moveBtn}
        <div>
            <button class="btn btn-sm btn-link p-0 mt-1" onclick="toggleComments(${t.id}, this)">💬 Comments</button>
            <div id="comments-${t.id}" style="display:none;"></div>
        </div>
    `;
    col.appendChild(card);
}

async function moveTask(taskId, newStatus) {
    await fetch(`${API}/tasks/${taskId}/status`, {
        method: "PUT", headers: authHeaders(true),
        body: JSON.stringify({ status: newStatus })
    });
}

async function assignTask(taskId, email) {
    if (!email) return;
    await fetch(`${API}/tasks/${taskId}/assignee`, {
        method: "PUT", headers: authHeaders(true),
        body: JSON.stringify({ email })
    });
}

async function createTask() {
    const title = document.getElementById("taskTitle").value;
    if (!title.trim()) return;
    await fetch(`${API}/projects/${currentProjectId}/tasks`, {
        method: "POST", headers: authHeaders(true),
        body: JSON.stringify({ title })
    });
    document.getElementById("taskTitle").value = "";
}

// ---------- Comments ----------

async function toggleComments(taskId, btn) {
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
    const comments = await res.json();
    const box = document.getElementById(`comments-${taskId}`);
    box.innerHTML = comments.map(c =>
        `<div class="comment-box"><b>${c.authorName}:</b> ${c.content}</div>`
    ).join("") + `
        <div class="input-group input-group-sm mt-1">
            <input id="newComment-${taskId}" class="form-control" placeholder="Write a comment" />
            <button class="btn btn-outline-primary" onclick="addComment(${taskId})">Send</button>
        </div>
    `;
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

let notifCount = 0;

function bumpNotifBadge() {
    notifCount++;
    const badge = document.getElementById("notifCount");
    badge.textContent = notifCount;
    badge.style.display = "inline-block";
}

async function loadNotifications() {
    const res = await fetch(`${API}/notifications`, { headers: authHeaders() });
    const notifs = await res.json();
    const dropdown = document.getElementById("notifDropdown");
    dropdown.innerHTML = notifs.length
        ? notifs.map(n => `<div class="notif-item">${n.message}</div>`).join("")
        : `<div class="p-2 text-muted small">No notifications yet</div>`;
    notifCount = 0;
    document.getElementById("notifCount").style.display = "none";
}