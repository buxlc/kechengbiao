export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/manifest.json") {
      const object = await env.ADAPTER_BUCKET.get("manifest.json");
      if (!object) {
        return new Response(JSON.stringify({
          schemaVersion: 1,
          generatedAt: new Date().toISOString(),
          adapters: []
        }, null, 2), {
          headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
        });
      }
      return new Response(object.body, {
        headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
      });
    }

    if (request.method === "GET" && url.pathname.startsWith("/adapters/")) {
      const key = url.pathname.replace(/^\/+/, "");
      const object = await env.ADAPTER_BUCKET.get(key);
      if (!object) return new Response("not found", { status: 404 });
      return new Response(object.body, {
        headers: {
          "content-type": "application/javascript; charset=utf-8",
          "cache-control": "public, max-age=60"
        }
      });
    }

    if (request.method === "POST" && url.pathname === "/upload") {
      const payload = await request.json();
      const { path, content, contentType, sha256 } = payload || {};
      if (!path || typeof content !== "string") {
        return new Response(JSON.stringify({ ok: false, error: "missing path/content" }), { status: 400 });
      }
      await env.ADAPTER_BUCKET.put(path, content, {
        httpMetadata: { contentType: contentType || "application/octet-stream" },
        customMetadata: { sha256: sha256 || "" }
      });
      return new Response(JSON.stringify({ ok: true, path }), {
        headers: { "content-type": "application/json; charset=utf-8" }
      });
    }

    return new Response("ok", { status: 200 });
  }
};
