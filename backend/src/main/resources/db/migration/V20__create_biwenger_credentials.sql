CREATE TABLE biwenger_credentials (
    id BIGSERIAL PRIMARY KEY,

    assistant_user_id BIGINT NOT NULL UNIQUE,

    biwenger_user_id BIGINT NOT NULL,

    encrypted_token TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    last_validated_at TIMESTAMP,

    CONSTRAINT fk_biwenger_credentials_assistant_user
        FOREIGN KEY (assistant_user_id)
        REFERENCES assistant_users(id)
        ON DELETE CASCADE
);