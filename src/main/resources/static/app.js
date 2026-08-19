'use strict';

const state = {
  workspaces: [],
  sessions: [],
  selectedSessionId: null,
  execution: null,
  gitMode: 'status',
  pollTimer: null,
  sessionRefreshTick: 0
};

const $ = (id) => document.getElementById(id);

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  });
  const text = await response.text();
  let body = null;
  if (text) {
    try { body = JSON.parse(text); } catch { body = text; }
  }
  if (!response.ok) {
    const detail = body && typeof body === 'object' ? (body.detail || body.title) : body;
    throw new Error(detail || `Request failed: ${response.status}`);
  }
  return body;
}

function toast(message) {
  const el = $('toast');
  el.textContent = message;
  el.hidden = false;
  clearTimeout(el._timer);
  el._timer = setTimeout(() => { el.hidden = true; }, 5000);
}

function shortId(value) {
  return value ? value.slice(0, 8) : '—';
}

function formatTime(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

async function checkHealth() {
  try {
    const health = await api('/actuator/health');
    $('healthBadge').textContent = health.status === 'UP' ? 'online' : String(health.status || 'unknown').toLowerCase();
  } catch (error) {
    $('healthBadge').textContent = 'offline';
  }
}

async function loadWorkspaces() {
  try {
    state.workspaces = await api('/api/v1/workspaces');
  } catch (error) {
    state.workspaces = [];
    toast(`Workspace API unavailable: ${error.message}`);
  }
  renderWorkspaces();
}

function renderWorkspaces() {
  const select = $('workspaceSelect');
  const previous = select.value;
  select.replaceChildren();

  if (!state.workspaces.length) {
    const option = document.createElement('option');
    option.value = '';
    option.textContent = 'No workspace available';
    select.append(option);
    $('newSessionButton').disabled = true;
    return;
  }

  state.workspaces.forEach((workspace) => {
    const option = document.createElement('option');
    option.value = workspace.id;
    option.textContent = workspace.gitRepository ? workspace.id : `${workspace.id} (non-git)`;
    select.append(option);
  });

  if (previous && state.workspaces.some((item) => item.id === previous)) {
    select.value = previous;
  }
  $('newSessionButton').disabled = false;
}

async function loadSessions({ preserveSelection = true } = {}) {
  state.sessions = await api('/api/v1/sessions');
  if (!preserveSelection || !state.sessions.some((s) => s.id === state.selectedSessionId)) {
    state.selectedSessionId = state.sessions[0]?.id || null;
  }
  renderSessions();
  if (state.selectedSessionId) {
    await refreshSelectedSession();
  } else {
    renderNoSession();
  }
}

function renderSessions() {
  const list = $('sessionList');
  list.replaceChildren();

  if (!state.sessions.length) {
    const empty = document.createElement('div');
    empty.className = 'muted';
    empty.textContent = 'No sessions yet.';
    list.append(empty);
    return;
  }

  state.sessions.forEach((session) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `session-item${session.id === state.selectedSessionId ? ' active' : ''}`;

    const title = document.createElement('strong');
    title.textContent = session.workspaceId;
    const id = document.createElement('span');
    id.className = 'session-id';
    id.textContent = `${shortId(session.id)} · ${String(session.status).toLowerCase()}`;

    button.append(title, id);
    button.addEventListener('click', async () => {
      state.selectedSessionId = session.id;
      renderSessions();
      await refreshSelectedSession();
    });
    list.append(button);
  });
}

async function createSession() {
  const workspaceId = $('workspaceSelect').value;
  if (!workspaceId) return;
  try {
    const session = await api('/api/v1/sessions', {
      method: 'POST',
      body: JSON.stringify({ workspaceId })
    });
    state.selectedSessionId = session.id;
    await loadSessions();
  } catch (error) {
    toast(error.message);
  }
}

async function refreshSelectedSession() {
  if (!state.selectedSessionId) {
    renderNoSession();
    return;
  }
  try {
    const [session, execution] = await Promise.all([
      api(`/api/v1/sessions/${encodeURIComponent(state.selectedSessionId)}`),
      api(`/api/v1/sessions/${encodeURIComponent(state.selectedSessionId)}/execution`)
    ]);
    state.execution = execution;
    const index = state.sessions.findIndex((item) => item.id === session.id);
    if (index >= 0) state.sessions[index] = session;
    renderSession(session, execution);
  } catch (error) {
    toast(error.message);
  }
}

function renderNoSession() {
  state.execution = null;
  $('sessionMeta').textContent = 'Select or create a session';
  $('timeline').className = 'timeline empty-state';
  $('timeline').textContent = 'No session selected.';
  $('promptInput').disabled = true;
  $('sendButton').disabled = true;
  $('cancelButton').disabled = true;
  $('executionBadge').className = 'badge neutral';
  $('executionBadge').textContent = 'idle';
  $('promptHint').textContent = 'Select a session first.';
}

function renderSession(session, execution) {
  $('sessionMeta').textContent = `${session.workspaceId} · ${shortId(session.id)}${session.providerThreadId ? ` · thread ${shortId(session.providerThreadId)}` : ''}`;

  const timeline = $('timeline');
  timeline.className = 'timeline';
  timeline.replaceChildren();
  session.events.forEach((event) => timeline.append(renderEvent(event)));
  if (!session.events.length) {
    timeline.className = 'timeline empty-state';
    timeline.textContent = 'No events yet.';
  } else {
    timeline.scrollTop = timeline.scrollHeight;
  }

  const running = Boolean(execution?.running);
  $('executionBadge').className = `badge${running ? '' : ' neutral'}`;
  $('executionBadge').textContent = String(execution?.status || 'idle').toLowerCase();
  $('promptInput').disabled = session.status !== 'ACTIVE' || running;
  $('sendButton').disabled = session.status !== 'ACTIVE' || running;
  $('cancelButton').disabled = session.status !== 'ACTIVE';
  $('promptHint').textContent = running
    ? `Codex turn running since ${formatTime(execution.startedAt)}`
    : execution?.error
      ? `Last turn failed: ${execution.error}`
      : session.status === 'ACTIVE'
        ? 'Ctrl+Enter to send.'
        : `Session is ${String(session.status).toLowerCase()}.`;

  if ($('workspaceSelect').value !== session.workspaceId && state.workspaces.some((item) => item.id === session.workspaceId)) {
    $('workspaceSelect').value = session.workspaceId;
  }
}

function renderEvent(event) {
  const card = document.createElement('article');
  card.className = `event ${event.actor || 'SYSTEM'}`;

  const meta = document.createElement('div');
  meta.className = 'event-meta';
  const actor = document.createElement('span');
  actor.textContent = `${event.actor || 'SYSTEM'} · ${event.type || 'EVENT'}`;
  const time = document.createElement('span');
  time.textContent = formatTime(event.occurredAt);
  meta.append(actor, time);

  const message = document.createElement('div');
  message.className = 'event-message';
  message.textContent = event.message || '';

  card.append(meta, message);
  return card;
}

async function sendPrompt(event) {
  event?.preventDefault();
  if (!state.selectedSessionId) return;
  const input = $('promptInput').value.trim();
  if (!input) return;

  $('sendButton').disabled = true;
  $('promptInput').disabled = true;
  try {
    state.execution = await api(`/api/v1/sessions/${encodeURIComponent(state.selectedSessionId)}/messages`, {
      method: 'POST',
      body: JSON.stringify({ input })
    });
    $('promptInput').value = '';
    await refreshSelectedSession();
  } catch (error) {
    toast(error.message);
    await refreshSelectedSession();
  }
}

async function cancelSession() {
  if (!state.selectedSessionId) return;
  try {
    state.execution = await api(`/api/v1/sessions/${encodeURIComponent(state.selectedSessionId)}/cancel`, { method: 'POST' });
    await refreshSelectedSession();
    await loadSessions();
  } catch (error) {
    toast(error.message);
  }
}

async function refreshGit() {
  const workspace = $('workspaceSelect').value;
  const info = state.workspaces.find((item) => item.id === workspace);
  if (!workspace || !info?.gitRepository) {
    $('gitOutput').textContent = 'Select a Git workspace.';
    return;
  }

  $('gitOutput').textContent = 'Loading...';
  try {
    let path;
    if (state.gitMode === 'status') {
      path = `/api/v1/workspaces/${encodeURIComponent(workspace)}/git/status`;
    } else {
      path = `/api/v1/workspaces/${encodeURIComponent(workspace)}/git/diff?staged=${state.gitMode === 'staged'}`;
    }
    const result = await api(path);
    $('gitOutput').textContent = result.output || '(clean / no output)';
  } catch (error) {
    $('gitOutput').textContent = `Git inspection failed: ${error.message}`;
  }
}

function selectGitMode(mode) {
  state.gitMode = mode;
  ['status', 'diff', 'staged'].forEach((name) => {
    $(`${name}Tab`).classList.toggle('active', name === mode);
  });
  refreshGit();
}

async function poll() {
  if (state.selectedSessionId) {
    await refreshSelectedSession();
  }
  state.sessionRefreshTick += 1;
  if (state.sessionRefreshTick >= 5) {
    state.sessionRefreshTick = 0;
    try {
      state.sessions = await api('/api/v1/sessions');
      renderSessions();
    } catch (error) {
      // The selected-session refresh already surfaces connectivity errors.
    }
  }
}

function startPolling() {
  clearInterval(state.pollTimer);
  state.pollTimer = setInterval(() => poll().catch(() => {}), 1000);
}

$('newSessionButton').addEventListener('click', createSession);
$('refreshSessionsButton').addEventListener('click', () => loadSessions().catch((error) => toast(error.message)));
$('promptForm').addEventListener('submit', sendPrompt);
$('cancelButton').addEventListener('click', cancelSession);
$('refreshGitButton').addEventListener('click', refreshGit);
$('workspaceSelect').addEventListener('change', refreshGit);
$('statusTab').addEventListener('click', () => selectGitMode('status'));
$('diffTab').addEventListener('click', () => selectGitMode('diff'));
$('stagedTab').addEventListener('click', () => selectGitMode('staged'));
$('promptInput').addEventListener('keydown', (event) => {
  if (event.key === 'Enter' && event.ctrlKey) {
    event.preventDefault();
    sendPrompt();
  }
});

(async function boot() {
  await checkHealth();
  await loadWorkspaces();
  try {
    await loadSessions();
  } catch (error) {
    toast(error.message);
    renderNoSession();
  }
  await refreshGit();
  startPolling();
})();
