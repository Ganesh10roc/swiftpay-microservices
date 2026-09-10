#!/bin/bash
set -e

echo "================================================"
echo "SwiftPay - Build Script"
echo "================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Building parent and common modules...${NC}"
mvn clean install -pl common -DskipTests

echo -e "${BLUE}Step 2: Building Transaction Gateway Service...${NC}"
mvn clean package -pl transaction-gateway -DskipTests

echo -e "${BLUE}Step 3: Building Ledger Service...${NC}"
mvn clean package -pl ledger-service -DskipTests

echo -e "${BLUE}Step 4: Building Analytics Worker...${NC}"
mvn clean package -pl analytics-worker -DskipTests

echo ""
echo -e "${GREEN}================================================"
echo "Build completed successfully!"
echo "================================================${NC}"
echo ""
echo "Next steps:"
echo "1. Build Docker images: docker-compose build"
echo "2. Start services: docker-compose up -d"
echo "3. Access Swagger UI:"
echo "   - Transaction Gateway: http://localhost:8080/swagger-ui.html"
echo "   - Ledger Service: http://localhost:8081/swagger-ui.html"
echo "   - Analytics Worker: http://localhost:8082/swagger-ui.html"
echo ""
