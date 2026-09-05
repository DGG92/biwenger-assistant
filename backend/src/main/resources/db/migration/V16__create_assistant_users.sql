CREATE TABLE assistant_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    manager_id BIGINT,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_assistant_users_username UNIQUE (username),
    CONSTRAINT uk_assistant_users_manager UNIQUE (manager_id),
    CONSTRAINT fk_assistant_users_manager
        FOREIGN KEY (manager_id)
        REFERENCES managers(id)
);