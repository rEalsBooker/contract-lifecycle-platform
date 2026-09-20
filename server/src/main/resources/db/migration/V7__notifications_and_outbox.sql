CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    recipient_membership_id BIGINT NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT NOT NULL,
    route VARCHAR(255) NOT NULL,
    dedup_key VARCHAR(191) NOT NULL,
    read_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_notification_dedup (tenant_id, recipient_membership_id, dedup_key),
    KEY idx_notification_inbox (tenant_id, recipient_membership_id, read_at, created_at),
    CONSTRAINT fk_notification_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    event_key VARCHAR(191) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(32) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload_json JSON NOT NULL,
    event_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_error VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at DATETIME(3) NULL,
    UNIQUE KEY uk_outbox_event_key (tenant_id, event_key),
    KEY idx_outbox_dispatch (event_status, available_at, id),
    CONSTRAINT fk_outbox_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
