-- Create sales_order table
CREATE TABLE sales_order (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_number VARCHAR(40) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    source VARCHAR(50) NOT NULL,
    sub_total DECIMAL(18, 2) DEFAULT 0,
    total_discount DECIMAL(18, 2) DEFAULT 0,
    total_tax DECIMAL(18, 2) DEFAULT 0,
    grand_total DECIMAL(18, 2) NOT NULL DEFAULT 0,
    remarks VARCHAR(500)
);

CREATE INDEX idx_sales_order_tenant_id ON sales_order(tenant_id);
CREATE INDEX idx_sales_order_customer_id ON sales_order(customer_id);
CREATE INDEX idx_sales_order_warehouse_id ON sales_order(warehouse_id);

CREATE TABLE sales_order_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    sales_order_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    ordered_qty INTEGER NOT NULL,
    invoiced_qty INTEGER NOT NULL DEFAULT 0,
    unit_price DECIMAL(18, 2) NOT NULL,
    discount DECIMAL(18, 2) DEFAULT 0,
    tax DECIMAL(18, 2) DEFAULT 0,
    line_total DECIMAL(18, 2) NOT NULL,
    CONSTRAINT fk_so_item_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id) ON DELETE CASCADE
);

CREATE INDEX idx_so_item_sales_order_id ON sales_order_item(sales_order_id);
CREATE INDEX idx_so_item_item_id ON sales_order_item(item_id);

-- Create invoice table
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) UNIQUE NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    sales_order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    delivery_status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,
    invoice_type VARCHAR(50) NOT NULL,
    sub_total DECIMAL(18, 2) NOT NULL,
    total_discount DECIMAL(18, 2) DEFAULT 0,
    total_tax DECIMAL(18, 2) DEFAULT 0,
    grand_total DECIMAL(18, 2) NOT NULL,
    amount_paid DECIMAL(18, 2) NOT NULL,
    balance DECIMAL(18, 2) NOT NULL,
    remarks VARCHAR(500),
    CONSTRAINT fk_invoice_sales_order FOREIGN KEY (sales_order_id) REFERENCES sales_order(id) ON DELETE RESTRICT
);

CREATE INDEX idx_invoice_tenant_id ON invoice(tenant_id);
CREATE INDEX idx_invoice_sales_order_id ON invoice(sales_order_id);
CREATE INDEX idx_invoice_customer_id ON invoice(customer_id);

CREATE TABLE invoice_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    invoice_id BIGINT NOT NULL,
    so_item_id BIGINT,
    item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    sku VARCHAR(255),
    batch_number VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(18, 2) NOT NULL,
    discount_amount DECIMAL(18, 2) DEFAULT 0,
    returned_quantity INTEGER DEFAULT 0,
    tax_amount DECIMAL(18, 2) DEFAULT 0,
    line_total DECIMAL(18, 2) NOT NULL,
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_item_so_item FOREIGN KEY (so_item_id) REFERENCES sales_order_item(id) ON DELETE SET NULL
);

CREATE INDEX idx_invoice_item_invoice_id ON invoice_item(invoice_id);
CREATE INDEX idx_invoice_item_item_id ON invoice_item(item_id);
CREATE INDEX idx_invoice_item_so_item_id ON invoice_item(so_item_id);

-- Create payment table
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    payment_number VARCHAR(40) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(100),
    bank_name VARCHAR(255),
    remarks VARCHAR(500),
    allocated_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    unallocated_amount DECIMAL(18, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_tenant_id ON payment(tenant_id);
CREATE INDEX idx_payment_customer_id ON payment(customer_id);

CREATE TABLE payment_allocation (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    allocated_amount DECIMAL(18, 2) NOT NULL,
    allocation_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_payment_allocation_payment FOREIGN KEY (payment_id) REFERENCES payment(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_allocation_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE RESTRICT
);

CREATE INDEX idx_payment_allocation_payment_id ON payment_allocation(payment_id);
CREATE INDEX idx_payment_allocation_invoice_id ON payment_allocation(invoice_id);

-- Create delivery_route table
CREATE TABLE delivery_route (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    route_number VARCHAR(50) UNIQUE NOT NULL,
    area_name VARCHAR(255),
    employee_id BIGINT, -- Storing ID only, external service
    vehicle_number VARCHAR(50),
    status VARCHAR(50),
    start_date TIMESTAMP
    -- REMOVED: CONSTRAINT fk_route_employee reference
);

CREATE INDEX idx_route_tenant_id ON delivery_route(tenant_id);
CREATE INDEX idx_route_employee_id ON delivery_route(employee_id);

CREATE TABLE delivery (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    delivery_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    employee_id BIGINT,
    delivery_person_id BIGINT, -- Storing ID only, external service
    scheduled_date TIMESTAMP,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    delivery_address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    remarks TEXT,
    route_id BIGINT,
    CONSTRAINT fk_delivery_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_route FOREIGN KEY (route_id) REFERENCES delivery_route(id) ON DELETE SET NULL
);

CREATE INDEX idx_delivery_tenant_id ON delivery(tenant_id);
CREATE INDEX idx_delivery_invoice_id ON delivery(invoice_id);
CREATE INDEX idx_delivery_route_id ON delivery(route_id);

CREATE TABLE delivery_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    item_id BIGINT,
    item_name VARCHAR(255),
    invoice_item_id BIGINT,
    batch_number VARCHAR(100),
    delivery_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT fk_delivery_item_delivery FOREIGN KEY (delivery_id) REFERENCES delivery(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_item_invoice_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_item(id) ON DELETE SET NULL
);

CREATE INDEX idx_delivery_item_delivery_id ON delivery_item(delivery_id);
CREATE INDEX idx_delivery_item_item_id ON delivery_item(item_id);

-- Create sales_return table
CREATE TABLE sales_return (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    tenant_id BIGINT NOT NULL,
    return_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_id BIGINT NOT NULL,
    return_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(18, 2) NOT NULL,
    credit_note_payment_id BIGINT,
    CONSTRAINT fk_sales_return_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_credit_note FOREIGN KEY (credit_note_payment_id) REFERENCES payment(id) ON DELETE SET NULL
);

CREATE INDEX idx_sales_return_tenant_id ON sales_return(tenant_id);
CREATE INDEX idx_sales_return_invoice_id ON sales_return(invoice_id);

CREATE TABLE sales_return_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    sales_return_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(18, 2) NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT fk_sales_return_item_return FOREIGN KEY (sales_return_id) REFERENCES sales_return(id) ON DELETE CASCADE
);

CREATE INDEX idx_sales_return_item_return_id ON sales_return_item(sales_return_id);
CREATE INDEX idx_sales_return_item_item_id ON sales_return_item(item_id);