import { mkdir, rm, cp } from "fs/promises";
import { existsSync } from "fs";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, "..");
const distDir = resolve(projectRoot, "dist");
const targetDir = resolve(projectRoot, "..", "wos-web", "src", "main", "resources", "static");

async function main() {
  if (!existsSync(distDir)) {
    console.error(`[postbuild] Missing dist directory at ${distDir}. Did you run 'bun run build'?`);
    process.exitCode = 1;
    return;
  }

  await rm(targetDir, { recursive: true, force: true });
  await mkdir(targetDir, { recursive: true });
  await cp(distDir, targetDir, { recursive: true });
  console.log(`[postbuild] Copied frontend assets from ${distDir} to ${targetDir}`);
}

main().catch((error) => {
  console.error("[postbuild] Failed to sync frontend assets:", error);
  process.exitCode = 1;
});
