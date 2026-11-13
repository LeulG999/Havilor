-- Flow F27: the full last AI assessment of an alert (verdict, confidence, summary, evidence, next steps, references),
-- stored as JSON text so the detail page can show it again without a new (paid) AI call. Null = never assessed.
ALTER TABLE alert ADD COLUMN last_assessment TEXT;
