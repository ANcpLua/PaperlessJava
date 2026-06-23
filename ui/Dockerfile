FROM node:alpine@sha256:a2dc166a387cc6ca1e62d0c8e265e49ca985d6e60abc9fe6e6c3d6ce8e63f606 AS builder
WORKDIR /app
COPY . .
FROM nginx:alpine@sha256:1a8724a52d432501548a8d8681bb1554c2d09778f8b9ed0882fc3442549980b7
COPY --from=builder /app /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/nginx.conf