-- Flow 6: optimistic locking. Every UPDATE of an alert runs "... SET version = version + 1 WHERE id = ? AND version = ?".
-- If someone else changed the row in between, the WHERE matches nothing and the update is refused (409).
ALTER TABLE alert ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
