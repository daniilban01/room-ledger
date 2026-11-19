CREATE TABLE expenses (
    id           UUID PRIMARY KEY,
    group_id     UUID          NOT NULL REFERENCES groups (id),
    description  VARCHAR(200)  NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount > 0),
    paid_by      UUID          NOT NULL REFERENCES users (id),
    expense_date DATE          NOT NULL,
    split_type   VARCHAR(10)   NOT NULL CHECK (split_type IN ('EQUAL', 'EXACT', 'ITEMS')),
    source       VARCHAR(10)   NOT NULL CHECK (source IN ('MANUAL', 'RECEIPT')),
    created_by   UUID          NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL,
    deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    version      BIGINT        NOT NULL
);

-- Serves the paginated list "newest expenses of a group" and skips deleted rows entirely.
CREATE INDEX idx_expenses_group_date ON expenses (group_id, expense_date DESC, created_at DESC) WHERE NOT deleted;

CREATE TABLE expense_shares (
    id         UUID PRIMARY KEY,
    expense_id UUID           NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id    UUID           NOT NULL REFERENCES users (id),
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    CONSTRAINT uq_expense_shares_expense_user UNIQUE (expense_id, user_id)
);

CREATE INDEX idx_expense_shares_user ON expense_shares (user_id);

CREATE TABLE expense_items (
    id         UUID PRIMARY KEY,
    expense_id UUID           NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    name       VARCHAR(200)   NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    position   INT            NOT NULL
);

CREATE INDEX idx_expense_items_expense ON expense_items (expense_id);

CREATE TABLE expense_item_assignees (
    item_id UUID NOT NULL REFERENCES expense_items (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    PRIMARY KEY (item_id, user_id)
);
