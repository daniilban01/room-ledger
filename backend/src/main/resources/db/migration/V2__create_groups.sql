CREATE TABLE groups (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    join_code  VARCHAR(16)  NOT NULL,
    created_by UUID         NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_groups_join_code UNIQUE (join_code)
);

CREATE TABLE group_members (
    id        UUID PRIMARY KEY,
    group_id  UUID        NOT NULL REFERENCES groups (id),
    user_id   UUID        NOT NULL REFERENCES users (id),
    role      VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_group_members_group_user UNIQUE (group_id, user_id)
);

-- The unique constraint above already indexes (group_id, user_id);
-- this one serves "which groups is this user in?".
CREATE INDEX idx_group_members_user ON group_members (user_id);
