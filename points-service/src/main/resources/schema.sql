-- Баланс очков пользователей
CREATE TABLE points_accounts
(
    uuid             UUID        NOT NULL PRIMARY KEY,
    user_id          VARCHAR(50) NOT NULL UNIQUE,
    total_points     BIGINT      NOT NULL DEFAULT 0,
    available_points BIGINT      NOT NULL DEFAULT 0,
    frozen_points    BIGINT      NOT NULL DEFAULT 0,
    level            INTEGER     NOT NULL DEFAULT 1,
    version          BIGINT,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,

    CONSTRAINT chk_points_nonnegative CHECK (available_points >= 0 AND frozen_points >= 0 AND total_points >= 0),
    CONSTRAINT chk_positive_level CHECK (level > 0)
);

-- Индексы для таблицы points_accounts
CREATE INDEX idx_user_id ON points_accounts (user_id);
CREATE INDEX idx_total_points ON points_accounts (total_points DESC);
CREATE INDEX idx_level ON points_accounts (level DESC);

-- Транзакции по баллам
CREATE TABLE points_transactions
(
    transaction_id   UUID         NOT NULL PRIMARY KEY,
    user_id          VARCHAR(255) NOT NULL,
    points_delta     BIGINT       NOT NULL,
    type             VARCHAR(50)  NOT NULL,
    status           VARCHAR(50)  NOT NULL,
    event_id         VARCHAR(255) NOT NULL,
    rule_id          VARCHAR(255) NOT NULL,
    description      TEXT,
    source           VARCHAR(255) NOT NULL,
    transaction_date TIMESTAMP    NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,

    CONSTRAINT chk_points_delta_not_zero CHECK (points_delta != 0)
);

-- Индексы для таблицы points_transactions
CREATE INDEX idx_transactions_user_id ON points_transactions (user_id);
CREATE INDEX idx_transactions_date ON points_transactions (transaction_date);
CREATE INDEX idx_transactions_event_id ON points_transactions (event_id);
CREATE INDEX idx_transactions_rule_id ON points_transactions (rule_id);
CREATE INDEX idx_transactions_status ON points_transactions (status);

-- Правила начисления очков
CREATE TABLE points_rules
(
    rule_id      VARCHAR(100) NOT NULL PRIMARY KEY,
    event_type   VARCHAR(50)  NOT NULL,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    points_value INTEGER      NOT NULL,
    formula      TEXT,
    max_daily    INTEGER,
    max_total    INTEGER,
    priority     INTEGER      NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT true,
    valid_from   TIMESTAMP    NOT NULL,
    valid_to     TIMESTAMP,

    CONSTRAINT chk_positive_priority CHECK (priority >= 0),
    CONSTRAINT chk_max_daily_positive CHECK (max_daily IS NULL OR max_daily > 0),
    CONSTRAINT chk_max_total_positive CHECK (max_total IS NULL OR max_total > 0),
    CONSTRAINT chk_valid_dates CHECK (valid_to IS NULL OR valid_to > valid_from)
);

-- Индексы для таблицы points_rules
CREATE INDEX idx_rules_event_type ON points_rules (event_type);
CREATE INDEX idx_rules_active ON points_rules (is_active);
CREATE INDEX idx_rules_validity ON points_rules (valid_from, valid_to);
CREATE INDEX idx_rules_priority ON points_rules (priority);

-- Условия правил (дополнительные таблицы)
CREATE TABLE rule_conditions
(
    rule_id         VARCHAR(100) NOT NULL,
    condition_key   VARCHAR(100) NOT NULL,
    condition_value VARCHAR(200) NOT NULL,

    PRIMARY KEY (rule_id, condition_key),
    CONSTRAINT fk_rule_conditions_rule FOREIGN KEY (rule_id)
        REFERENCES points_rules (rule_id) ON DELETE CASCADE
);

-- Индексы для таблицы rule_conditions
CREATE INDEX idx_rule_conditions_rule ON rule_conditions (rule_id);

-- История изменений уровней (дополнительная)
CREATE TABLE level_history
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      VARCHAR(50) NOT NULL,
    old_level    INTEGER     NOT NULL,
    new_level    INTEGER     NOT NULL,
    changed_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_points BIGINT      NOT NULL,
    reason       VARCHAR(255)
);

-- Индексы для таблицы level_history
CREATE INDEX idx_level_history_user_id ON level_history (user_id);
CREATE INDEX idx_level_history_changed_at ON level_history (changed_at DESC);

-- Дневные лимиты по правилам (дополнительная)
CREATE TABLE daily_rule_limits
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(50)  NOT NULL,
    rule_id            VARCHAR(100) NOT NULL,
    date               DATE         NOT NULL,
    points_accumulated INTEGER      NOT NULL DEFAULT 0,

    UNIQUE (user_id, rule_id, date),
    CONSTRAINT fk_daily_limits_rule FOREIGN KEY (rule_id)
        REFERENCES points_rules (rule_id) ON DELETE CASCADE,
    CONSTRAINT chk_positive_accumulated CHECK (points_accumulated >= 0)
);

-- Индексы для таблицы daily_rule_limits
CREATE INDEX idx_daily_limits_user_date ON daily_rule_limits (user_id, date);
CREATE INDEX idx_daily_limits_rule_date ON daily_rule_limits (rule_id, date);
CREATE INDEX idx_daily_limits_composite ON daily_rule_limits (user_id, rule_id, date);

-- Общие лимиты по правилам
CREATE TABLE total_rule_limits
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(50)  NOT NULL,
    rule_id            VARCHAR(100) NOT NULL,
    points_accumulated INTEGER      NOT NULL DEFAULT 0,

    UNIQUE (user_id, rule_id),
    CONSTRAINT fk_total_limits_rule FOREIGN KEY (rule_id)
        REFERENCES points_rules (rule_id) ON DELETE CASCADE,
    CONSTRAINT chk_total_positive_accumulated CHECK (points_accumulated >= 0)
);

-- Индексы для таблицы total_rule_limits
CREATE INDEX idx_total_limits_user ON total_rule_limits (user_id);
CREATE INDEX idx_total_limits_rule ON total_rule_limits (rule_id);

-- Триггеры и функции

-- Автоматическое обновление updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_points_accounts_updated_at
    BEFORE UPDATE
    ON points_accounts
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Автоматическая вставка UUID для точек аккаунтов
CREATE OR REPLACE FUNCTION generate_points_account_uuid()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.uuid IS NULL THEN
        NEW.uuid = gen_random_uuid();
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER set_points_account_uuid
    BEFORE INSERT
    ON points_accounts
    FOR EACH ROW
EXECUTE FUNCTION generate_points_account_uuid();

-- Представление для сводной информации по пользователям
CREATE VIEW user_points_summary AS
SELECT pa.user_id,
       pa.total_points,
       pa.available_points,
       pa.frozen_points,
       pa.level,
       pa.created_at,
       pa.updated_at,
       COUNT(pt.transaction_id)                                           as total_transactions,
       SUM(CASE WHEN pt.points_delta > 0 THEN pt.points_delta ELSE 0 END) as total_earned,
       SUM(CASE WHEN pt.points_delta < 0 THEN pt.points_delta ELSE 0 END) as total_spent
FROM points_accounts pa
         LEFT JOIN points_transactions pt ON pa.user_id = pt.user_id
GROUP BY pa.user_id,
         pa.total_points,
         pa.available_points,
         pa.frozen_points,
         pa.level,
         pa.created_at,
         pa.updated_at;

-- Представление для активных правил
CREATE VIEW active_rules AS
SELECT rule_id,
       event_type,
       name,
       points_value,
       formula,
       max_daily,
       max_total,
       priority
FROM points_rules
WHERE is_active = true
  AND valid_from <= CURRENT_TIMESTAMP
  AND (valid_to IS NULL OR valid_to > CURRENT_TIMESTAMP)
ORDER BY priority ASC;