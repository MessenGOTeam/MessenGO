FROM node:20-alpine

WORKDIR /app

# Copy monorepo workspace package files
COPY package*.json ./
COPY shared/package*.json ./shared/
COPY services/message-service/package*.json ./services/message-service/

# Install dependencies for shared and message-service
RUN npm install --omit=dev

# Copy shared code and service code
COPY shared/ ./shared/
COPY services/message-service/ ./services/message-service/

WORKDIR /app/services/message-service

EXPOSE 3004

CMD ["node", "src/server.js"]
