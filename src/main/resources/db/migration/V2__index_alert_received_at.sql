-- Flow 2: the alert list is ordered newest first (received_at DESC, id DESC as tie-breaker).
-- This index matches that order, so PostgreSQL can read a page straight from the index
-- instead of sorting the whole table on every request.
CREATE INDEX idx_alert_received_at_id ON alert (received_at DESC, id DESC);
