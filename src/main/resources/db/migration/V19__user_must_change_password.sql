-- Flow F30: a person created by a manager gets a temporary password and must set their own before using Havilor.
-- Existing users keep working as before (false).
ALTER TABLE app_user ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
