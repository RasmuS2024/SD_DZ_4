CREATE TABLE IF NOT EXISTS works (
    id BIGSERIAL PRIMARY KEY,
    student_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    s3_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_works_student_name ON works (student_name);
