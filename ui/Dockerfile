FROM node:alpine@sha256:a2dc166a387cc6ca1e62d0c8e265e49ca985d6e60abc9fe6e6c3d6ce8e63f606 AS builder
WORKDIR /app
COPY . .
FROM nginx:alpine@sha256:20316569d8f81a160065d7d2a5eeffc7ca97d79022462ee255fd23fa103a6b5c
COPY --from=builder /app /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/nginx.conf