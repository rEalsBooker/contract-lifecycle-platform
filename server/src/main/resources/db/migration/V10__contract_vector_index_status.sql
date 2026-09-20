CREATE TABLE contract_vector_indexes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    contract_id BIGINT NOT NULL,
    contract_version_id BIGINT NOT NULL,
    index_status VARCHAR(32) NOT NULL,
    chunk_count INT NOT NULL DEFAULT 0,
    embedding_model VARCHAR(128) NOT NULL,
    last_error VARCHAR(1000) NULL,
    indexed_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_vector_index_version (tenant_id, contract_id, contract_version_id),
    KEY idx_vector_index_current (tenant_id, contract_id, index_status),
    CONSTRAINT fk_vector_index_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_vector_index_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    CONSTRAINT fk_vector_index_version FOREIGN KEY (contract_version_id) REFERENCES contract_versions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
