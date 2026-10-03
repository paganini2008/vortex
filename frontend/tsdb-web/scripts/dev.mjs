// `npm run dev`: reads VORTEX_WEB_PORT from this app's .env (see .env.example) before the dev
// server starts, since the port has to be known first; Next.js loads the rest of .env itself.
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import nextEnv from "@next/env";

nextEnv.loadEnvConfig(fileURLToPath(new URL("..", import.meta.url)), true);
const port = process.env.VORTEX_WEB_PORT || "3000";
const child = spawn("next", ["dev", "-p", port], { stdio: "inherit", shell: true, env: process.env });
child.on("exit", (code) => process.exit(code ?? 0));
