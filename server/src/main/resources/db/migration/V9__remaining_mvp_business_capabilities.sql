ALTER TABLE ai_findings
    ADD COLUMN reviewed_by_membership_id BIGINT NULL AFTER review_status,
    ADD COLUMN review_note VARCHAR(1000) NULL AFTER reviewed_by_membership_id,
    ADD COLUMN reviewed_at DATETIME(3) NULL AFTER review_note,
    ADD COLUMN updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) AFTER created_at,
    ADD CONSTRAINT fk_ai_finding_reviewer FOREIGN KEY (reviewed_by_membership_id) REFERENCES memberships(id);

CREATE TABLE ai_question_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, contract_id BIGINT NOT NULL,
    contract_version_id BIGINT NOT NULL, membership_id BIGINT NOT NULL, question VARCHAR(1000) NOT NULL,
    answer_text TEXT NOT NULL, source_json JSON NOT NULL, tool_context_json JSON NULL,
    model_name VARCHAR(128) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_ai_question_contract (tenant_id, contract_id, created_at),
    CONSTRAINT fk_ai_question_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_ai_question_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_ai_question_version FOREIGN KEY (contract_version_id) REFERENCES contract_versions(id),
    CONSTRAINT fk_ai_question_member FOREIGN KEY (membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE fulfillment_tasks
    ADD COLUMN contractual_due_date DATE NULL AFTER due_date,
    ADD COLUMN internal_plan_date DATE NULL AFTER contractual_due_date;
UPDATE fulfillment_tasks SET contractual_due_date=due_date, internal_plan_date=due_date WHERE due_date IS NOT NULL;

CREATE TABLE task_extension_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, task_id BIGINT NOT NULL,
    applicant_membership_id BIGINT NOT NULL, approver_membership_id BIGINT NOT NULL,
    original_internal_plan_date DATE NOT NULL, requested_internal_plan_date DATE NOT NULL,
    reason VARCHAR(1000) NOT NULL, approval_status VARCHAR(32) NOT NULL, approval_note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), reviewed_at DATETIME(3) NULL,
    KEY idx_extension_approver (tenant_id, approver_membership_id, approval_status),
    KEY idx_extension_task (tenant_id, task_id, approval_status),
    CONSTRAINT fk_extension_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_extension_task FOREIGN KEY (task_id) REFERENCES fulfillment_tasks(id),
    CONSTRAINT fk_extension_applicant FOREIGN KEY (applicant_membership_id) REFERENCES memberships(id),
    CONSTRAINT fk_extension_approver FOREIGN KEY (approver_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE contracts ADD COLUMN termination_reason VARCHAR(1000) NULL AFTER archive_status;
UPDATE contract_versions v JOIN contracts c ON c.current_version_id=v.id SET v.version_status='ACTIVE';

CREATE TABLE contract_version_change_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, contract_id BIGINT NOT NULL,
    candidate_version_id BIGINT NOT NULL, requester_membership_id BIGINT NOT NULL,
    reviewer_membership_id BIGINT NOT NULL, change_summary VARCHAR(1000) NOT NULL,
    request_status VARCHAR(32) NOT NULL, review_note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), reviewed_at DATETIME(3) NULL,
    KEY idx_version_change_review (tenant_id, reviewer_membership_id, request_status),
    CONSTRAINT fk_version_change_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_version_change_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_version_change_version FOREIGN KEY (candidate_version_id) REFERENCES contract_versions(id),
    CONSTRAINT fk_version_change_requester FOREIGN KEY (requester_membership_id) REFERENCES memberships(id),
    CONSTRAINT fk_version_change_reviewer FOREIGN KEY (reviewer_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE contract_archive_snapshots (
    id BIGINT PRIMARY KEY AUTO_INCREMENT, tenant_id BIGINT NOT NULL, contract_id BIGINT NOT NULL,
    contract_version_id BIGINT NOT NULL, snapshot_json JSON NOT NULL,
    archived_by_membership_id BIGINT NOT NULL, archive_reason VARCHAR(1000) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_archive_contract (tenant_id, contract_id, created_at),
    CONSTRAINT fk_archive_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_archive_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_archive_version FOREIGN KEY (contract_version_id) REFERENCES contract_versions(id),
    CONSTRAINT fk_archive_member FOREIGN KEY (archived_by_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
