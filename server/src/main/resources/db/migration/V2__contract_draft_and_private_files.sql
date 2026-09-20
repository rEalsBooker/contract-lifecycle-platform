CREATE TABLE file_objects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    uploader_membership_id BIGINT NOT NULL,
    storage_key VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    object_status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_file_objects_storage_key (storage_key),
    KEY idx_file_objects_tenant (tenant_id),
    CONSTRAINT fk_file_objects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_file_objects_uploader FOREIGN KEY (uploader_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE contracts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    owner_membership_id BIGINT NOT NULL,
    contract_no VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    counterparty_name VARCHAR(255) NOT NULL,
    total_amount DECIMAL(18, 2) NOT NULL,
    business_status VARCHAR(16) NOT NULL,
    archive_status VARCHAR(16) NOT NULL,
    current_version_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_contracts_tenant_no (tenant_id, contract_no),
    KEY idx_contracts_tenant_status (tenant_id, business_status, updated_at),
    CONSTRAINT fk_contracts_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_contracts_owner FOREIGN KEY (owner_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE contract_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    source_file_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_status VARCHAR(16) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by_membership_id BIGINT NOT NULL,
    UNIQUE KEY uk_contract_versions_no (contract_id, version_no),
    CONSTRAINT fk_contract_versions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_contract_versions_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_contract_versions_file FOREIGN KEY (source_file_id) REFERENCES file_objects(id),
    CONSTRAINT fk_contract_versions_creator FOREIGN KEY (created_by_membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_current_version
    FOREIGN KEY (current_version_id) REFERENCES contract_versions(id);

CREATE TABLE contract_grants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    membership_id BIGINT NOT NULL,
    permission_code VARCHAR(32) NOT NULL,
    valid_until DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_contract_grants_scope (contract_id, membership_id, permission_code),
    KEY idx_contract_grants_membership (tenant_id, membership_id),
    CONSTRAINT fk_contract_grants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_contract_grants_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_contract_grants_membership FOREIGN KEY (membership_id) REFERENCES memberships(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
