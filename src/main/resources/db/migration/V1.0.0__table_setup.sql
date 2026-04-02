CREATE TABLE invoice (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    file_path    VARCHAR(1024) NOT NULL,
    content_hash VARCHAR(64)   NOT NULL,
    processed_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_invoice_content_hash (content_hash)
);

CREATE TABLE fee_transaction (
    id         BIGINT         NOT NULL AUTO_INCREMENT,
    amount     DECIMAL(8, 2) NOT NULL,
    date       DATE           NOT NULL,
    company    VARCHAR(255)   NOT NULL,
    invoice_id BIGINT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fee_transaction_invoice
        FOREIGN KEY (invoice_id) REFERENCES invoice (id)
);
