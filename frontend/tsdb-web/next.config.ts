import type { NextConfig } from "next";

// Settings come from .env in this directory (see .env.example), which Next.js loads itself.
const nextConfig: NextConfig = {
  // A self-contained server.js for the Docker image
  output: "standalone",
};

export default nextConfig;
