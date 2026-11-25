CREATE TABLE hello_metadata (
                                metadata_key VARCHAR(100) PRIMARY KEY,
                                metadata_value VARCHAR(255) NOT NULL,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO hello_metadata (
    metadata_key,
    metadata_value
)
VALUES (
           'service_name',
           'hello-service'
       );