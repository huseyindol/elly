# Elly CMS - Makefile for Docker Management & Performance Testing
# Kullanım: make [command]
# Örnek: make up, make logs, make restart, make load-test

.PHONY: help build up down restart logs logs-app logs-db shell shell-db clean ps health backup restore load-test stress-test perf-setup

# Default command - yardım göster
help:
	@echo "🚀 Elly CMS Docker Commands"
	@echo ""
	@echo "📦 Setup & Build:"
	@echo "  make setup      - İlk kurulum (env dosyası + build + up)"
	@echo "  make build      - Docker image'ları yeniden build et"
	@echo ""
	@echo "🏃 Start & Stop:"
	@echo "  make up         - Container'ları başlat"
	@echo "  make down       - Container'ları durdur ve kaldır"
	@echo "  make restart    - Container'ları yeniden başlat"
	@echo "  make stop       - Container'ları durdur"
	@echo "  make start      - Durdurulan container'ları başlat"
	@echo ""
	@echo "📊 Monitoring:"
	@echo "  make ps         - Container durumlarını göster"
	@echo "  make logs       - Tüm logları göster"
	@echo "  make logs-app   - Sadece app logları"
	@echo "  make logs-db    - Sadece database logları"
	@echo "  make health     - Health check yap"
	@echo "  make stats      - Resource kullanımını göster"
	@echo ""
	@echo "🔧 Development:"
	@echo "  make shell      - App container'a shell ile bağlan"
	@echo "  make shell-db   - PostgreSQL'e psql ile bağlan"
	@echo "  make rebuild    - Kod değişikliği sonrası rebuild"
	@echo ""
	@echo "🗄️  Database:"
	@echo "  make backup     - Database backup al"
	@echo "  make restore    - Database backup'tan geri yükle"
	@echo "  make db-indexes - Index'leri manuel çalıştır"
	@echo "  make db-perf    - Performance index'lerini çalıştır"
	@echo ""
	@echo "🔥 Performance Testing:"
	@echo "  make perf-setup - Load test araçlarını kur (K6)"
	@echo "  make load-test  - Basic load test çalıştır"
	@echo "  make stress-test - Stress test çalıştır (limit bul)"
	@echo "  make write-test - Write operations test"
	@echo "  make perf-mode  - Performance profili ile başlat"
	@echo "  make monitor    - Real-time monitoring"
	@echo ""
	@echo "🧹 Cleanup:"
	@echo "  make clean      - Container'ları ve volume'ları temizle"
	@echo "  make prune      - Docker sistem temizliği"

# İlk kurulum
setup:
	@echo "📦 Setting up Elly CMS..."
	@if [ ! -f .env ]; then \
		cp env.example .env; \
		echo "✅ .env dosyası oluşturuldu. Lütfen düzenleyin!"; \
	else \
		echo "⚠️  .env dosyası zaten mevcut"; \
	fi
	@make build
	@make up
	@echo "✅ Setup tamamlandı!"
	@echo "🌐 Swagger UI: http://localhost:8080/swagger-ui.html"

# Build
build:
	@echo "🔨 Building Docker images..."
	docker-compose build --no-cache

# Start containers
up:
	@echo "🚀 Starting containers..."
	docker-compose up -d
	@echo "✅ Containers started!"
	@make ps

# Stop and remove containers
down:
	@echo "🛑 Stopping containers..."
	docker-compose down
	@echo "✅ Containers stopped!"

# Restart containers
restart:
	@echo "🔄 Restarting containers..."
	docker-compose restart
	@echo "✅ Containers restarted!"

# Stop containers
stop:
	@echo "⏸️  Stopping containers..."
	docker-compose stop

# Start stopped containers
start:
	@echo "▶️  Starting containers..."
	docker-compose start

# Show logs
logs:
	docker-compose logs -f

# Show app logs only
logs-app:
	docker-compose logs -f app

# Show database logs only
logs-db:
	docker-compose logs -f postgres

# Show container status
ps:
	@echo "📊 Container Status:"
	@docker-compose ps

# Health check
health:
	@echo "🏥 Health Check:"
	@echo ""
	@echo "📱 Application:"
	@curl -s http://localhost:8080/actuator/health | jq '.' || echo "❌ App not responding"
	@echo ""
	@echo "🗄️  Database:"
	@docker-compose exec postgres pg_isready -U postgres || echo "❌ Database not ready"

# Resource stats
stats:
	@echo "📈 Resource Usage:"
	docker stats --no-stream elly-app elly-postgres

# Shell into app container
shell:
	@echo "🐚 Opening shell in app container..."
	docker-compose exec app sh

# Shell into database
shell-db:
	@echo "🗄️  Connecting to PostgreSQL..."
	docker-compose exec postgres psql -U postgres -d postgres

# Rebuild after code changes
rebuild:
	@echo "🔄 Rebuilding after code changes..."
	docker-compose up -d --build app
	@echo "✅ Rebuild complete!"
	@make logs-app

# Database backup
backup:
	@echo "💾 Creating database backup..."
	@mkdir -p backups
	@docker-compose exec postgres pg_dump -U postgres -d postgres --schema=elly > backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "✅ Backup created in backups/ directory"

# Database restore (Usage: make restore FILE=backups/backup_20231201_120000.sql)
restore:
	@if [ -z "$(FILE)" ]; then \
		echo "❌ Error: FILE parameter required"; \
		echo "Usage: make restore FILE=backups/backup_20231201_120000.sql"; \
		exit 1; \
	fi
	@echo "📥 Restoring database from $(FILE)..."
	@docker-compose exec -T postgres psql -U postgres -d postgres < $(FILE)
	@echo "✅ Database restored!"

# Manually run database indexes
db-indexes:
	@echo "🔧 Running database indexes..."
	docker-compose exec postgres psql -U postgres -d postgres -c "SET search_path TO elly;" -f /docker-entrypoint-initdb.d/01-indexes.sql
	@echo "✅ Indexes created!"

# Clean everything (including volumes!)
clean:
	@echo "⚠️  WARNING: This will delete all data!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		docker-compose down -v; \
		docker system prune -f; \
		echo "✅ Cleanup complete!"; \
	else \
		echo "❌ Cancelled"; \
	fi

# Docker system prune
prune:
	@echo "🧹 Cleaning Docker system..."
	docker system prune -f
	@echo "✅ Prune complete!"

# Production deployment
prod-up:
	@echo "🚀 Starting production containers..."
	docker-compose -f docker-compose.prod.yml up -d
	@echo "✅ Production containers started!"

prod-down:
	@echo "🛑 Stopping production containers..."
	docker-compose -f docker-compose.prod.yml down

prod-logs:
	docker-compose -f docker-compose.prod.yml logs -f

# ========================================
# PERFORMANCE TESTING COMMANDS
# ========================================

# Install load testing tools
perf-setup:
	@echo "🔧 Installing performance testing tools..."
	@if command -v k6 >/dev/null 2>&1; then \
		echo "✅ K6 already installed: $$(k6 version)"; \
	else \
		echo "📦 Installing K6..."; \
		brew install k6 || echo "❌ Failed to install K6. Please install manually: https://k6.io/docs/get-started/installation/"; \
	fi
	@if command -v ab >/dev/null 2>&1; then \
		echo "✅ Apache Bench already installed"; \
	else \
		echo "⚠️  Apache Bench not found (usually pre-installed on macOS)"; \
	fi
	@echo ""
	@echo "📚 Load test dosyaları:"
	@ls -lh load-tests/ 2>/dev/null || echo "❌ load-tests/ klasörü bulunamadı!"
	@echo ""
	@echo "✅ Setup complete! Run 'make load-test' to start testing."

# Database performance indexes
db-perf:
	@echo "🔧 Installing performance indexes..."
	@if [ -f src/main/resources/db-performance-indexes.sql ]; then \
		psql "postgresql://xxx:yyy@zzz" \
			-f src/main/resources/db-performance-indexes.sql && \
		echo "✅ Performance indexes installed!" || \
		echo "❌ Failed to install indexes. Check connection and file."; \
	else \
		echo "❌ db-performance-indexes.sql not found!"; \
	fi

# Start application in performance mode
perf-mode:
	@echo "🚀 Starting application in performance mode..."
	./mvnw spring-boot:run -Dspring-boot.run.profiles=performance

# Run basic load test with K6
load-test:
	@echo "🔥 Running basic load test..."
	@if [ ! -d load-tests ]; then \
		echo "❌ load-tests/ directory not found!"; \
		exit 1; \
	fi
	@if ! command -v k6 >/dev/null 2>&1; then \
		echo "❌ K6 not installed! Run 'make perf-setup' first."; \
		exit 1; \
	fi
	@echo "⏳ Testing: 10→50→100 concurrent users for ~5 minutes..."
	@echo "📊 Target: http://localhost:8080"
	@echo ""
	k6 run load-tests/k6-basic-test.js

# Run stress test
stress-test:
	@echo "🔥 Running stress test..."
	@if ! command -v k6 >/dev/null 2>&1; then \
		echo "❌ K6 not installed! Run 'make perf-setup' first."; \
		exit 1; \
	fi
	@echo "⏳ Testing: 50→100→200→300→400→500 users (~13 minutes)"
	@echo "⚠️  WARNING: This will push your system to its limits!"
	@echo "📊 Target: http://localhost:8080"
	@echo ""
	@read -p "Continue? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		k6 run load-tests/k6-stress-test.js; \
	else \
		echo "❌ Cancelled"; \
	fi

# Run write operations test
write-test:
	@echo "🔥 Running write operations test..."
	@if ! command -v k6 >/dev/null 2>&1; then \
		echo "❌ K6 not installed! Run 'make perf-setup' first."; \
		exit 1; \
	fi
	@echo "⚠️  WARNING: This will create test data in your database!"
	@echo "📊 Target: http://localhost:8080"
	@echo ""
	@read -p "Continue? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		k6 run load-tests/k6-write-test.js; \
	else \
		echo "❌ Cancelled"; \
	fi

# Quick Apache Bench test
ab-test:
	@echo "🔥 Quick Apache Bench test..."
	@if ! command -v ab >/dev/null 2>&1; then \
		echo "❌ Apache Bench not installed!"; \
		exit 1; \
	fi
	@echo "⏳ Testing: 1000 requests, 100 concurrent"
	@echo "📊 Target: http://localhost:8080/api/pages"
	@echo ""
	ab -n 1000 -c 100 http://localhost:8080/api/pages

# Real-time monitoring during tests
monitor:
	@echo "📊 Real-time Monitoring (Press Ctrl+C to stop)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@while true; do \
		clear; \
		echo "📊 ELLY CMS - Real-time Monitoring"; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		echo ""; \
		echo "🏥 Health:"; \
		curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "❌ Offline"; \
		echo ""; \
		echo "🔌 Hikari Connection Pool:"; \
		curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | jq -r '.measurements[0].value' 2>/dev/null | xargs -I {} echo "  Active: {}"; \
		curl -s http://localhost:8080/actuator/metrics/hikaricp.connections | jq -r '.measurements[0].value' 2>/dev/null | xargs -I {} echo "  Total: {}"; \
		echo ""; \
		echo "💾 JVM Memory:"; \
		curl -s http://localhost:8080/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value' 2>/dev/null | awk '{printf "  Used: %.2f MB\n", $$1/1024/1024}'; \
		curl -s http://localhost:8080/actuator/metrics/jvm.memory.max | jq -r '.measurements[0].value' 2>/dev/null | awk '{printf "  Max: %.2f MB\n", $$1/1024/1024}'; \
		echo ""; \
		echo "🖥️  CPU:"; \
		curl -s http://localhost:8080/actuator/metrics/system.cpu.usage | jq -r '.measurements[0].value' 2>/dev/null | awk '{printf "  Usage: %.2f%%\n", $$1*100}'; \
		echo ""; \
		echo "Updated: $$(date '+%H:%M:%S')"; \
		echo "Press Ctrl+C to stop..."; \
		sleep 3; \
	done

# Performance report
perf-report:
	@echo "📊 Performance Test Report"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@if [ -f load-tests/summary.json ]; then \
		echo "📈 Last Test Results:"; \
		cat load-tests/summary.json | jq '.metrics'; \
	else \
		echo "❌ No test results found. Run 'make load-test' first."; \
	fi

# Help for performance testing
perf-help:
	@echo "🔥 ELLY CMS - Performance Testing Guide"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "📚 Documentation:"
	@echo "  - LOAD_TEST_GUIDE.md       - Başlangıç rehberi"
	@echo "  - PERFORMANCE_ANALYSIS.md  - Olası sorunlar ve çözümler"
	@echo "  - OPTIMIZATION_EXAMPLES.md - Kod örnekleri"
	@echo ""
	@echo "🎯 Quick Start:"
	@echo "  1. make perf-setup    # Araçları kur"
	@echo "  2. make perf-mode     # Uygulamayı başlat (performance mode)"
	@echo "  3. make db-perf       # Index'leri yükle"
	@echo "  4. make load-test     # Test çalıştır"
	@echo ""
	@echo "📊 Monitoring:"
	@echo "  - Terminal 1: make perf-mode      (Uygulamayı çalıştır)"
	@echo "  - Terminal 2: make monitor        (Metrics'leri izle)"
	@echo "  - Terminal 3: make load-test      (Test çalıştır)"
	@echo ""
	@echo "📖 Full guide: open LOAD_TEST_GUIDE.md"

