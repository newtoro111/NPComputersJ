import { it, expect, vi, beforeEach } from "vitest";
import { api, setToken } from "./api";
beforeEach(() => {
  setToken(null);
  vi.restoreAllMocks();
});
it("sends access tokens in headers without browser storage", async () => {
  setToken("memory-token");
  global.fetch = vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: async () => ({ ok: true }),
  });
  await api("/cart");
  expect(fetch.mock.calls[0][1].headers.Authorization).toBe(
    "Bearer memory-token",
  );
  expect(localStorage.length).toBe(0);
});
it("retains safe conflict details for checkout recovery", async () => {
  global.fetch = vi.fn().mockResolvedValue({
    ok: false,
    status: 409,
    json: async () => ({
      detail: "Quote changed",
      code: "QUOTE_CHANGED",
      correlationId: "abc",
    }),
  });
  await expect(
    api("/orders", {
      method: "POST",
      body: {},
      headers: { "Idempotency-Key": "same-key" },
    }),
  ).rejects.toMatchObject({
    status: 409,
    code: "QUOTE_CHANGED",
    correlationId: "abc",
  });
});
