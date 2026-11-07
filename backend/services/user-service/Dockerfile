FROM node:20-alpine

WORKDIR /app

# Copy monorepo workspace package files
COPY package*.json ./
COPY shared/package*.json ./shared/
COPY services/user-service/package*.json ./services/user-service/

# Install dependencies for shared and user-service
RUN npm install --omit=dev

# Copy shared code and service code
COPY shared/ ./shared/
COPY services/user-service/ ./services/user-service/

WORKDIR /app/services/user-service

EXPOSE 3002

CMD ["node", "src/server.js"]
