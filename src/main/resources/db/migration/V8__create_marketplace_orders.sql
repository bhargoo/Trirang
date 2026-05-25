CREATE TABLE marketplace_orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    buyer_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    razorpay_order_id VARCHAR(255),
    delivery_address TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_marketplace_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_orders_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings(id) ON DELETE CASCADE
);

CREATE INDEX idx_marketplace_orders_buyer ON marketplace_orders(buyer_id);
CREATE INDEX idx_marketplace_orders_listing ON marketplace_orders(listing_id);
CREATE INDEX idx_marketplace_orders_status ON marketplace_orders(status);
