import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

const resolveEnv = (env: Record<string, string>, key: string, fallback?: string) => {
  const value = env[key] ?? process.env[key];
  if (value === undefined || value === "") {
    return fallback;
  }
  return value;
};

const buildAllowedHosts = (hosts: string[]): true | string[] => {
  if (hosts.length === 0) {
    return true;
  }

  return hosts.map((raw) => {
    if (raw === "*") {
      return "*";
    }

    const trimmed = raw.trim();
    if (trimmed.length === 0) {
      return "*";
    }

    // Vite handles wildcard patterns natively in strings
    // No need to convert to RegExp objects
    return trimmed;
  });
};

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const isDevServer = command === "serve";
  const backendHost = resolveEnv(env, "BACKEND_HOST", "localhost")!;
  const backendPort = Number.parseInt(resolveEnv(env, "BACKEND_PORT", "8080")!, 10);
  const proxyTarget = `http://${backendHost}:${backendPort}`;
  const disableProxy = resolveEnv(env, "DISABLE_VITE_PROXY", "false") === "true";
  const devServerPort = Number.parseInt(resolveEnv(env, "DEV_SERVER_PORT", "8000")!, 10);
  const shouldProxy = isDevServer && !disableProxy;

  const proxyConfig = shouldProxy
    ? {
        "/api": {
          target: proxyTarget,
          changeOrigin: true,
          secure: false,
        },
        "/logs": {
          target: proxyTarget,
          changeOrigin: true,
          secure: false,
        },
      }
    : undefined;

  const allowedHostsInput = (resolveEnv(env, "ALLOWED_DEV_HOSTS", "") ?? "")
    .split(",")
    .map((value) => value.trim())
    .filter((value) => value.length > 0);

  const allowedHosts = buildAllowedHosts(allowedHostsInput) as true | string[];

  return {
    plugins: [react()],
    server: {
      host: true,
      port: devServerPort,
      strictPort: true,
      allowedHosts,
      proxy: proxyConfig,
    },
  };
});
