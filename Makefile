# ============================================================
# KOBE backend — Docker 中心の開発タスク
# ローカルに JDK / Gradle は不要 (Docker さえあれば動く)。
# ============================================================
COMPOSE := docker compose

# Gradle を dev サービスのイメージ上で実行する (DB 不要なタスク用)。
# build/ や .gradle/ はマウントしたソース配下に書かれる。
GRADLE := $(COMPOSE) --profile dev run --rm --no-deps dev ./gradlew --no-daemon

.DEFAULT_GOAL := help

.PHONY: help up dev down clean logs ps test lint format build

help: ## このヘルプを表示
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-8s\033[0m %s\n", $$1, $$2}'

up: ## 本番相当: DB + アプリ(jar) を起動 (localhost:8080)
	$(COMPOSE) --profile prod up --build -d

dev: ## 開発: DB + アプリ(ソースから bootRun) を起動
	$(COMPOSE) --profile dev up --build

down: ## すべて停止
	$(COMPOSE) down

clean: ## 停止してボリューム (DB データ) も削除
	$(COMPOSE) down -v

logs: ## ログを追従
	$(COMPOSE) logs -f

ps: ## 稼働状況
	$(COMPOSE) ps

test: ## テスト (Docker 上の Gradle / H2)
	$(GRADLE) test

lint: ## ktlint チェック
	$(GRADLE) ktlintCheck

format: ## ktlint 自動整形
	$(GRADLE) ktlintFormat

build: ## ビルド (jar 生成)
	$(GRADLE) build
