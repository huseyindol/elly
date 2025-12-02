# Elly CMS - Makefile for Docker Management
# Kullanım: make [command]
# Örnek: make up, make logs, make restart

.PHONY: help build up down restart logs logs-app logs-db shell shell-db clean ps health backup restore

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

