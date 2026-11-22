-- Soft delete: a deleted group disappears for its members, but its rows stay. A hard DELETE is not
-- possible anyway (the append-only activity_events reference the group) and would erase a financial
-- history the members may still need. NULL means the group is active.
ALTER TABLE groups ADD COLUMN deleted_at TIMESTAMPTZ;
