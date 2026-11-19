# --- Build stage: compile the static bundle ---
FROM node:24-alpine AS build
WORKDIR /app

# Install dependencies first so this layer is cached until package*.json changes.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime stage: only nginx + the built files ---
FROM nginx:1.28-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
