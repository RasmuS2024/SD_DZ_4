CREATE TABLE IF NOT EXISTS analysis_reports (
    id BIGSERIAL PRIMARY KEY,
    work_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    file_format VARCHAR(255),
    notes VARCHAR(1000),
    created_at TIMESTAMP
);
