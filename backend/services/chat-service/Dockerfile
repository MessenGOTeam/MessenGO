FROM node:20-alpine

WORKDIR /app

# Copy monorepo workspace package files
COPY package*.json ./
COPY shared/package*.json ./shared/
COPY services/chat-service/package*.json ./services/chat-service/

# Install dependencies for shared and chat-service
RUN npm install --omit=dev

# Copy shared code and service code
COPY shared/ ./shared/
COPY services/chat-service/ ./services/chat-service/

WORKDIR /app/services/chat-service

EXPOSE 3003

CMD ["node", "src/server.js"]
