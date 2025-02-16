FROM node:alpine AS builder
WORKDIR /app
COPY . .
FROM nginx:alpine
COPY --from=builder /app /usr/share/nginx/html
COPY ./nginx.conf /etc/nginx/nginx.conf