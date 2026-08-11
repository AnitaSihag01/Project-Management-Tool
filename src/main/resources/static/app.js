const API = "/api";
const WS_URL = "/ws";

let token = null;
let currentProjectId = null;
let stompClient = null;

function log(msg) {
    const el = document.getElementById("log");
    el.innerHTML += `[${new Date().toLocaleTimeString()}] ${msg}<br>`;
    el.scrollTop = el.scrollHeight;
}

async function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const res = await fetch(`${API}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
    });

    if (!res.ok) {
        log("Login failed: " + (await res.text()));
        return;
    }

    const data = await res.json();
    token = data.token;
    document.getElementById("whoami").textContent = data.fullName;
    document.getElementById("login-section").style.display = "none";
    document.getElementById("board-section").style.display = "block";

    log("Logged in as " + data.fullName);
    connectWebSocket();
}

function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        log("WebSocket connected");

        // Private channel - notifications meant only for this user
        stompClient.subscribe("/user/queue/notifications", (message) => {
            const notif = JSON.parse(message.body);
            log("🔔 NOTIFICATION: " + notif.message);
        });
    });
}

async function loadTasks() {
    currentProjectId = document.getElementById("projectId").value;

    const res = await fetch(`${API}/projects/${currentProjectId}/tasks`, {
        headers: { "Authorization": "Bearer " + token }
    });
    const tasks = await res.json();

    const container = document.getElementById("tasks");
    container.innerHTML = "";
    tasks.forEach(t => {
        container.innerHTML += `<div>#${t.id} ${t.title} — ${t.status} (${t.assigneeName || "unassigned"})</div>`;
    });

    log(`Loaded ${tasks.length} task(s) for project ${currentProjectId}`);

    // Subscribe to this project's shared board channel
    stompClient.subscribe(`/topic/projects/${currentProjectId}`, (message) => {
        log("📋 BOARD EVENT: " + message.body + " — refreshing...");
        loadTasks();
    });
}

async function createTask() {
    const title = document.getElementById("taskTitle").value;

    await fetch(`${API}/projects/${currentProjectId}/tasks`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
        body: JSON.stringify({ title })
    });

    document.getElementById("taskTitle").value = "";
    // no manual reload here - the WebSocket broadcast should trigger it automatically
}