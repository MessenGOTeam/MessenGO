FROM node:20-alpine

WORKDIR /app

# Copy monorepo workspace package files
COPY package*.json ./
COPY shared/package*.json ./shared/
COPY services/auth-service/package*.json ./services/auth-service/

# Install dependencies for shared and auth-service
RUN npm install --omit=dev

# Copy shared code and service code
COPY shared/ ./shared/
COPY services/auth-service/ ./services/auth-service/

WORKDIR /app/services/auth-service

EXPOSE 3001

CMD ["node", "src/server.js"]
