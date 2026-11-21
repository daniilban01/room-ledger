CREATE TABLE settlements (
    id          UUID PRIMARY KEY,
    group_id    UUID           NOT NULL REFERENCES groups (id),
    from_user   UUID           NOT NULL REFERENCES users (id),
    to_user     UUID           NOT NULL REFERENCES users (id),
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    status      VARCHAR(10)    NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED')),
    created_by  UUID           NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ    NOT NULL,
    resolved_at TIMESTAMPTZ,
    version     BIGINT         NOT NULL,
    CONSTRAINT chk_settlements_different_users CHECK (from_user <> to_user),
    -- A settlement has a resolution time exactly when it is no longer pending.
    CONSTRAINT chk_settlements_resolved_at CHECK ((status = 'PENDING') = (resolved_at IS NULL))
);

CREATE INDEX idx_settlements_group_created ON settlements (group_id, created_at DESC);

CREATE TABLE activity_events (
    id         UUID PRIMARY KEY,
    group_id   UUID        NOT NULL REFERENCES groups (id),
    actor_id   UUID        NOT NULL REFERENCES users (id),
    type       VARCHAR(40) NOT NULL,
    payload    JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_activity_events_group_created ON activity_events (group_id, created_at DESC);

-- The activity log is append-only. The application never updates or deletes events, and these
-- triggers make the database itself refuse it, so not even a bug or a manual SQL session can
-- rewrite the history.
CREATE FUNCTION reject_activity_event_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'activity_events is append-only: % is not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER activity_events_no_update_or_delete
    BEFORE UPDATE OR DELETE ON activity_events
    FOR EACH ROW EXECUTE FUNCTION reject_activity_event_change();

CREATE TRIGGER activity_events_no_truncate
    BEFORE TRUNCATE ON activity_events
    FOR EACH STATEMENT EXECUTE FUNCTION reject_activity_event_change();
