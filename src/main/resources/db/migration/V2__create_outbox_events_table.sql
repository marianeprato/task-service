CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         TEXT NOT NULL,
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL,
    published_at    TIMESTAMP
);

-- Speeds up the relay's poll query (findTop50ByPublishedAtIsNullOrderByCreatedAtAsc).
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
