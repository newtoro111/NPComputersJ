let accessToken = null,
  csrf = null,
  refreshPromise = null;
export function setToken(token) {
  accessToken = token;
}
async function csrfToken() {
  if (!csrf) {
    const r = await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
    if (!r.ok) throw new Error("Could not initialize authentication.");
    csrf = (await r.json()).token;
  }
  return csrf;
}
export async function api(
  path,
  { method = "GET", body, headers = {}, retry = true } = {},
) {
  const authPath = path.startsWith("/auth/");
  const h = { ...headers };
  if (body !== undefined) h["Content-Type"] = "application/json";
  if (accessToken) h.Authorization = `Bearer ${accessToken}`;
  if (authPath && method !== "GET") h["X-XSRF-TOKEN"] = await csrfToken();
  const r = await fetch("/api/v1" + path, {
    method,
    headers: h,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (r.status === 401 && retry && !authPath) {
    try {
      await refresh();
      return api(path, { method, body, headers, retry: false });
    } catch {
      accessToken = null;
    }
  }
  if (r.status === 204) return null;
  const data = await r.json().catch(() => ({ detail: "Request failed." }));
  if (!r.ok) {
    const e = new Error(
      data.detail || data.message || `Request failed (${r.status}).`,
    );
    e.status = r.status;
    e.code = data.code;
    e.correlationId = data.correlationId;
    throw e;
  }
  return data;
}
export async function refresh() {
  if (!refreshPromise) {
    const action = async () => {
      const r = await api("/auth/refresh", { method: "POST", retry: false });
      setToken(r.accessToken);
      return r;
    };
    refreshPromise = (
      navigator.locks ? navigator.locks.request("np-refresh", action) : action()
    ).finally(() => (refreshPromise = null));
  }
  return refreshPromise;
}
export async function signOut() {
  try {
    await api("/auth/logout", { method: "POST" });
  } finally {
    setToken(null);
  }
}
