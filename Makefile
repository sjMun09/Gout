.PHONY: dev dev-down dev-logs backend frontend

# 로컬 개발 DB 시작
dev:
	docker compose up -d
	@echo "✅ PostgreSQL: localhost:5432"
	@echo "✅ Redis: localhost:6379"

# 로컬 개발 DB 종료
dev-down:
	docker compose down

# 로그 확인
dev-logs:
	docker compose logs -f

# 백엔드 실행 (IDE 없이)
backend:
	cd gout-back && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 프론트엔드 실행
frontend:
	cd gout-front && npm run dev
