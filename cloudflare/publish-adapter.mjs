/**
 * 教务适配脚本 Cloudflare R2 发布工具
 *
 * 用法:
 *   node cloudflare/publish-adapter.mjs \
 *     --adapter-name LNTU_WEBVPN_EAMS \
 *     --school-id LNTU \
 *     --folder LNTU \
 *     --adapter-id LNTU_01 \
 *     --js-path lntu_eams_webvpn.js \
 *     --version 1.0.0 \
 *     --input app/src/main/assets/adapters/LNTU/lntu_eams_webvpn.js \
 *     --notes "修复学期自动选择逻辑"
 */

import { readFileSync, existsSync } from "fs";
import { createHash } from "crypto";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = resolve(__dirname, "..");

// ---- 配置 ----
const CF_ACCOUNT_ID = process.env.CF_ACCOUNT_ID || "";
const CF_API_TOKEN = process.env.CF_API_TOKEN || "";
const R2_BUCKET = process.env.R2_BUCKET || "";
const PUBLIC_BASE = process.env.PUBLIC_BASE_URL || "";
const CF_API = `https://api.cloudflare.com/client/v4/accounts/${CF_ACCOUNT_ID}/r2/buckets/${R2_BUCKET}/objects`;

// ---- 参数解析 ----
function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {};
  for (let i = 0; i < args.length; i += 2) {
    const key = args[i].replace(/^--/, "").replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    opts[key] = args[i + 1];
  }
  return opts;
}

// ---- SHA256 ----
function sha256(buffer) {
  return createHash("sha256").update(buffer).digest("hex");
}

// ---- Cloudflare R2 API ----
async function r2Put(objectKey, body, contentType = "application/octet-stream") {
  const url = `${CF_API}/${encodeURIComponent(objectKey)}`;
  const resp = await fetch(url, {
    method: "PUT",
    headers: {
      Authorization: `Bearer ${CF_API_TOKEN}`,
      "Content-Type": contentType,
    },
    body,
  });

  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`R2 PUT ${objectKey} failed (${resp.status}): ${text}`);
  }

  const json = await resp.json();
  if (!json.success) {
    throw new Error(`R2 PUT ${objectKey} API error: ${JSON.stringify(json.errors)}`);
  }
  return json;
}

async function r2Get(objectKey) {
  const url = `${CF_API}/${encodeURIComponent(objectKey)}`;
  const resp = await fetch(url, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${CF_API_TOKEN}`,
    },
  });

  if (resp.status === 404) return null;
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`R2 GET ${objectKey} failed (${resp.status}): ${text}`);
  }

  return await resp.text();
}

// ---- 主流程 ----
async function main() {
  const opts = parseArgs();

  if (!CF_API_TOKEN) {
    console.error("错误: 请设置环境变量 CF_API_TOKEN");
    process.exit(1);
  }
  if (!CF_ACCOUNT_ID) {
    console.error("错误: 请设置环境变量 CF_ACCOUNT_ID");
    process.exit(1);
  }
  if (!R2_BUCKET) {
    console.error("错误: 请设置环境变量 R2_BUCKET");
    process.exit(1);
  }
  if (!PUBLIC_BASE) {
    console.error("错误: 请设置环境变量 PUBLIC_BASE_URL");
    process.exit(1);
  }

  const adapterName = opts.adapterName;
  const schoolId = opts.schoolId;
  const folder = opts.folder;
  const adapterId = opts.adapterId;
  const jsPath = opts.jsPath;
  const version = opts.version || "1.0.0";
  const inputPath = opts.input;
  const notes = opts.notes || "";

  if (!adapterName || !inputPath) {
    console.error("用法: node publish-adapter.mjs --adapter-name NAME --input PATH [options]");
    console.error("必需: --adapter-name, --input");
    console.error("可选: --school-id, --folder, --adapter-id, --js-path, --version, --notes");
    process.exit(1);
  }

  const fullPath = resolve(PROJECT_ROOT, inputPath);
  if (!existsSync(fullPath)) {
    console.error(`文件不存在: ${fullPath}`);
    process.exit(1);
  }

  const jsContent = readFileSync(fullPath);
  const hash = sha256(jsContent);
  const r2Key = `adapters/${folder || schoolId}/${adapterName}-v${version}.js`;
  const downloadUrl = `${PUBLIC_BASE}/${r2Key}`;

  console.log(`\n📦 发布适配器`);
  console.log(`   名称: ${adapterName}`);
  console.log(`   版本: ${version}`);
  console.log(`   文件: ${fullPath} (${jsContent.length} bytes)`);
  console.log(`   SHA256: ${hash}`);
  console.log(`   R2 Key: ${r2Key}`);
  console.log(`   URL: ${downloadUrl}\n`);

  // 1. 上传脚本文件
  console.log("⬆️  上传脚本到 R2...");
  await r2Put(r2Key, jsContent, "application/javascript");
  console.log("   ✅ 脚本已上传\n");

  // 2. 获取或创建 manifest.json
  console.log("📋 更新 manifest.json...");
  let manifest = { adapters: [], updatedAt: "" };
  const existingManifest = await r2Get("manifest.json");
  if (existingManifest) {
    try {
      manifest = JSON.parse(existingManifest);
    } catch {
      console.warn("   ⚠️ 现有 manifest.json 解析失败，将创建新的");
    }
  }

  // 移除同名旧条目
  manifest.adapters = (manifest.adapters || []).filter(
    (a) => a.adapterName !== adapterName
  );

  const entry = {
    adapterName,
    schoolId: schoolId || folder || adapterName,
    folder: folder || schoolId || adapterName,
    adapterId: adapterId || adapterName,
    jsPath: jsPath || `${adapterName}-v${version}.js`,
    version,
    downloadUrl,
    sha256: hash,
    enabled: true,
    notes,
  };

  manifest.adapters.push(entry);
  manifest.updatedAt = new Date().toISOString();

  const manifestJson = JSON.stringify(manifest, null, 2);
  await r2Put("manifest.json", manifestJson, "application/json");
  console.log(`   ✅ manifest.json 已更新 (${manifest.adapters.length} 个适配器)\n`);

  // 3. 上传 school_index.json (使用项目本地的)
  console.log("📚 更新 school_index.json...");
  const localIndex = resolve(PROJECT_ROOT, "app/src/main/assets/school_index.json");
  if (existsSync(localIndex)) {
    const indexContent = readFileSync(localIndex, "utf-8");
    try {
      JSON.parse(indexContent);
    } catch {
      console.error("   ❌ 本地 school_index.json 格式错误!");
      process.exit(1);
    }
    await r2Put("school_index.json", indexContent, "application/json");
    console.log("   ✅ school_index.json 已上传\n");
  } else {
    console.warn("   ⚠️ 本地 school_index.json 不存在，跳过\n");
  }

  // 4. 结果摘要
  console.log("═══════════════════════════════════");
  console.log("🎉 发布完成!");
  console.log(`   脚本: ${downloadUrl}`);
  console.log(`   Manifest: ${PUBLIC_BASE}/manifest.json`);
  console.log(`   School Index: ${PUBLIC_BASE}/school_index.json`);
  console.log("═══════════════════════════════════\n");
}

main().catch((err) => {
  console.error("❌ 发布失败:", err.message);
  process.exit(1);
});
