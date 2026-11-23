.PHONY: up down logs backend-test frontend-check e2e promote-admin

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f backend

backend-test:
	cd backend && mvn -B verify

frontend-check:
	cd frontend && npm ci && npm run typecheck && npm run lint && npm test && npm run build

e2e:
	cd frontend && npx playwright test

promote-admin:
	@test -n "$(EMAIL)" || (echo "Usage: make promote-admin EMAIL=organizer@example.com" && exit 1)
	docker compose run --rm --no-deps backend \
	  --spring.main.web-application-type=none \
	  --ksm.scheduling.enabled=false \
	  --ksm.tasks.promote-admin=$(EMAIL)
