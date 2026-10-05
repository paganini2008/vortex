# syntax=docker/dockerfile:1.7
# Built for each target platform (not $BUILDPLATFORM): the standalone output carries native
# packages such as sharp, compiled for the platform npm ci ran on
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production HOSTNAME=0.0.0.0 PORT=3000 NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public
USER node
EXPOSE 3000
# VORTEX_API_URLS: the TSDB nodes to proxy /tsd/* to, comma separated
CMD ["node", "server.js"]
