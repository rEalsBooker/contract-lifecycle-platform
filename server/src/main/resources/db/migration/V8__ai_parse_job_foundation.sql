ALTER TABLE contract_parse_jobs
    ADD COLUMN request_key VARCHAR(64) NULL AFTER execution_mode,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER request_key,
    ADD COLUMN provider_name VARCHAR(64) NULL AFTER attempt_count,
    ADD COLUMN model_name VARCHAR(128) NULL AFTER provider_name,
    ADD COLUMN prompt_version VARCHAR(32) NULL AFTER model_name,
    ADD COLUMN started_at DATETIME(3) NULL AFTER failure_message,
    ADD COLUMN finished_at DATETIME(3) NULL AFTER started_at,
    ADD UNIQUE KEY uk_parse_job_request (tenant_id, request_key);

CREATE TABLE ai_findings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    contract_version_id BIGINT NOT NULL,
    parse_job_id BIGINT NOT NULL,
    finding_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    source_page_no INT NULL,
    source_excerpt VARCHAR(1000) NULL,
    confidence DECIMAL(5,2) NULL,
    raw_json JSON NOT NULL,
    review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_ai_findings_job (tenant_id, parse_job_id, review_status),
    CONSTRAINT fk_ai_finding_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ai_finding_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_ai_finding_version FOREIGN KEY (contract_version_id) REFERENCES contract_versions(id),
    CONSTRAINT fk_ai_finding_job FOREIGN KEY (parse_job_id) REFERENCES contract_parse_jobs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
