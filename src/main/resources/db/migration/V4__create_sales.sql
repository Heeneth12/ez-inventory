-- Create sales_order_source enum
CREATE TYPE sales_order_source AS ENUM (
    'SALES_TEAM',
    'DIRECT_SALES',
    'MARKETING_CAMPAIGN',
    'ONLINE_CHANNEL',
    'REPEAT_ORDER'
);

-- Create sales_order_status enum
CREATE TYPE sales_order_status AS ENUM (
    'CREATED',
    'PENDING_APPROVAL',
    'REJECTED',
    'CONFIRMED',
    'PARTIALLY_INVOICED',
    'FULLY_INVOICED',
    'CANCELLED'
);

-- Create sales_order table
CREATE TABLE sales_order (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Sales order specific fields
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_number VARCHAR(40) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status sales_order_status NOT NULL,
    source sales_order_source NOT NULL,
    sub_total DECIMAL(18, 2) DEFAULT 0,
    total_discount DECIMAL(18, 2) DEFAULT 0,
    total_tax DECIMAL(18, 2) DEFAULT 0,
    grand_total DECIMAL(18, 2) NOT NULL DEFAULT 0,
    remarks VARCHAR(500),

    -- Foreign key constraint
    CONSTRAINT fk_sales_order_customer FOREIGN KEY (customer_id)
        REFERENCES contact(id) ON DELETE RESTRICT
);

-- Create sales_order_item table
CREATE TABLE sales_order_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Sales order item specific fields
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

    -- Foreign key constraint
    CONSTRAINT fk_so_item_sales_order FOREIGN KEY (sales_order_id)
        REFERENCES sales_order(id) ON DELETE CASCADE
);

-- Create indexes for sales_order
CREATE INDEX idx_sales_order_uuid ON sales_order(uuid);
CREATE INDEX idx_sales_order_tenant_id ON sales_order(tenant_id);
CREATE INDEX idx_sales_order_warehouse_id ON sales_order(warehouse_id);
CREATE INDEX idx_sales_order_order_number ON sales_order(order_number);
CREATE INDEX idx_sales_order_customer_id ON sales_order(customer_id);
CREATE INDEX idx_sales_order_order_date ON sales_order(order_date);
CREATE INDEX idx_sales_order_status ON sales_order(status);
CREATE INDEX idx_sales_order_source ON sales_order(source);
CREATE INDEX idx_sales_order_created_at ON sales_order(created_at);

-- Composite indexes for common queries
CREATE INDEX idx_sales_order_tenant_status ON sales_order(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_sales_order_tenant_customer ON sales_order(tenant_id, customer_id)
    WHERE is_deleted = false;
CREATE INDEX idx_sales_order_warehouse_status ON sales_order(warehouse_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_sales_order_date_range ON sales_order(tenant_id, order_date)
    WHERE is_deleted = false;

-- Create indexes for sales_order_item
CREATE INDEX idx_so_item_uuid ON sales_order_item(uuid);
CREATE INDEX idx_so_item_sales_order_id ON sales_order_item(sales_order_id);
CREATE INDEX idx_so_item_item_id ON sales_order_item(item_id);
CREATE INDEX idx_so_item_is_deleted ON sales_order_item(is_deleted);
CREATE INDEX idx_so_item_created_at ON sales_order_item(created_at);
CREATE INDEX idx_so_item_updated_at ON sales_order_item(updated_at);

-- Composite index for invoicing tracking
CREATE INDEX idx_so_item_invoicing ON sales_order_item(sales_order_id, invoiced_qty)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE sales_order IS 'Sales orders from customers';
COMMENT ON TABLE sales_order_item IS 'Line items for sales orders';

COMMENT ON COLUMN sales_order.order_number IS 'Unique sales order number';
COMMENT ON COLUMN sales_order.status IS 'Sales order lifecycle status';
COMMENT ON COLUMN sales_order.source IS 'Origin of the sales order';
COMMENT ON COLUMN sales_order.sub_total IS 'Total before discounts and taxes';
COMMENT ON COLUMN sales_order.total_discount IS 'Sum of all discounts';
COMMENT ON COLUMN sales_order.total_tax IS 'Sum of all taxes';
COMMENT ON COLUMN sales_order.grand_total IS 'Final amount including tax and discount';

COMMENT ON COLUMN sales_order_item.ordered_qty IS 'Original quantity requested';
COMMENT ON COLUMN sales_order_item.invoiced_qty IS 'Quantity already invoiced';
COMMENT ON COLUMN sales_order_item.line_total IS 'Calculated: (unit_price * qty) - discount';


-- Create invoice_status enum
CREATE TYPE invoice_status AS ENUM (
    'PENDING',
    'DRAFT',
    'ISSUED',
    'CANCELLED'
);

-- Create invoice_delivery_status enum
CREATE TYPE invoice_delivery_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'DELIVERED',
    'CANCELLED'
);

-- Create invoice_payment_status enum
CREATE TYPE invoice_payment_status AS ENUM (
    'UNPAID',
    'PARTIALLY_PAID',
    'PAID'
);

-- Create invoice_type enum
CREATE TYPE invoice_type AS ENUM (
    'GST_INVOICE',
    'BILL_OF_SUPPLY',
    'RETAIL',
    'WHOLESALE',
    'CASH',
    'CREDIT'
);

-- Create invoice table
CREATE TABLE invoice (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Invoice specific fields
    tenant_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    invoice_number VARCHAR(40) UNIQUE NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    sales_order_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    status invoice_status NOT NULL,
    delivery_status invoice_delivery_status NOT NULL,
    payment_status invoice_payment_status NOT NULL,
    invoice_type invoice_type NOT NULL,
    sub_total DECIMAL(18, 2) NOT NULL,
    total_discount DECIMAL(18, 2) DEFAULT 0,
    total_tax DECIMAL(18, 2) DEFAULT 0,
    grand_total DECIMAL(18, 2) NOT NULL,
    amount_paid DECIMAL(18, 2) NOT NULL,
    balance DECIMAL(18, 2) NOT NULL,
    remarks VARCHAR(500),

    -- Foreign key constraints
    CONSTRAINT fk_invoice_sales_order FOREIGN KEY (sales_order_id)
        REFERENCES sales_order(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id)
        REFERENCES contact(id) ON DELETE RESTRICT
);

-- Create invoice_item table
CREATE TABLE invoice_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Invoice item specific fields
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

    -- Foreign key constraints
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_item_so_item FOREIGN KEY (so_item_id)
        REFERENCES sales_order_item(id) ON DELETE SET NULL
);

-- Create indexes for invoice
CREATE INDEX idx_invoice_uuid ON invoice(uuid);
CREATE INDEX idx_invoice_tenant_id ON invoice(tenant_id);
CREATE INDEX idx_invoice_warehouse_id ON invoice(warehouse_id);
CREATE INDEX idx_invoice_invoice_number ON invoice(invoice_number);
CREATE INDEX idx_invoice_invoice_date ON invoice(invoice_date);
CREATE INDEX idx_invoice_sales_order_id ON invoice(sales_order_id);
CREATE INDEX idx_invoice_customer_id ON invoice(customer_id);
CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_delivery_status ON invoice(delivery_status);
CREATE INDEX idx_invoice_payment_status ON invoice(payment_status);
CREATE INDEX idx_invoice_invoice_type ON invoice(invoice_type);
CREATE INDEX idx_invoice_created_at ON invoice(created_at);

-- Create indexes for invoice_item
CREATE INDEX idx_invoice_item_uuid ON invoice_item(uuid);
CREATE INDEX idx_invoice_item_invoice_id ON invoice_item(invoice_id);
CREATE INDEX idx_invoice_item_so_item_id ON invoice_item(so_item_id);
CREATE INDEX idx_invoice_item_item_id ON invoice_item(item_id);
CREATE INDEX idx_invoice_item_sku ON invoice_item(sku);
CREATE INDEX idx_invoice_item_batch_number ON invoice_item(batch_number);
CREATE INDEX idx_invoice_item_is_deleted ON invoice_item(is_deleted);
CREATE INDEX idx_invoice_item_created_at ON invoice_item(created_at);
CREATE INDEX idx_invoice_item_updated_at ON invoice_item(updated_at);

-- Composite index for batch and return tracking
CREATE INDEX idx_invoice_item_batch_tracking ON invoice_item(item_id, batch_number)
    WHERE is_deleted = false;
CREATE INDEX idx_invoice_item_returns ON invoice_item(invoice_id, returned_quantity)
    WHERE is_deleted = false AND returned_quantity > 0;

-- Add table and column comments
COMMENT ON TABLE invoice IS 'Sales invoices for customer billing';
COMMENT ON TABLE invoice_item IS 'Line items for invoices with stock and pricing details';

COMMENT ON COLUMN invoice.invoice_number IS 'Unique invoice number (e.g., INV-2025-0001)';
COMMENT ON COLUMN invoice.sales_order_id IS 'Link to originating sales order';
COMMENT ON COLUMN invoice.status IS 'Invoice processing status';
COMMENT ON COLUMN invoice.delivery_status IS 'Delivery/fulfillment status';
COMMENT ON COLUMN invoice.payment_status IS 'Payment collection status';
COMMENT ON COLUMN invoice.invoice_type IS 'Type/category of invoice';
COMMENT ON COLUMN invoice.amount_paid IS 'Amount already paid by customer';
COMMENT ON COLUMN invoice.balance IS 'Outstanding amount (grand_total - amount_paid)';

COMMENT ON COLUMN invoice_item.so_item_id IS 'Link to sales order item (for partial invoicing)';
COMMENT ON COLUMN invoice_item.batch_number IS 'Batch/lot for stock tracking and COGS calculation';
COMMENT ON COLUMN invoice_item.returned_quantity IS 'Quantity returned by customer';
COMMENT ON COLUMN invoice_item.line_total IS 'Calculated: qty × price - discount + tax';


-- Create payment_method enum
CREATE TYPE payment_method AS ENUM (
    'CASH',
    'BANK_TRANSFER',
    'CHEQUE',
    'CREDIT_CARD',
    'DEBIT_CARD',
    'UPI',
    'MOBILE_WALLET',
    'NET_BANKING',
    'OTHER',
    'CREDIT_NOTE'
);

-- Create payment_status enum
CREATE TYPE payment_status AS ENUM (
    'DRAFT',
    'PENDING',
    'CLEARED',
    'ALLOCATED',
    'PARTIALLY_ALLOCATED',
    'CANCELLED',
    'REFUNDED',
    'COMPLETED',
    'RECEIVED'
);

-- Create payment table
CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Payment specific fields
    tenant_id BIGINT NOT NULL,
    payment_number VARCHAR(40) UNIQUE NOT NULL,
    customer_id BIGINT NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    status payment_status NOT NULL,
    payment_method payment_method NOT NULL,
    reference_number VARCHAR(100),
    bank_name VARCHAR(255),
    remarks VARCHAR(500),
    allocated_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    unallocated_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,

    -- Foreign key constraint
    CONSTRAINT fk_payment_customer FOREIGN KEY (customer_id)
        REFERENCES contact(id) ON DELETE RESTRICT
);

-- Create payment_allocation table
CREATE TABLE payment_allocation (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Payment allocation specific fields
    tenant_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    allocated_amount DECIMAL(18, 2) NOT NULL,
    allocation_date TIMESTAMP NOT NULL,

    -- Foreign key constraints
    CONSTRAINT fk_payment_allocation_payment FOREIGN KEY (payment_id)
        REFERENCES payment(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_allocation_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice(id) ON DELETE RESTRICT
);

-- Create indexes for payment
CREATE INDEX idx_payment_uuid ON payment(uuid);
CREATE INDEX idx_payment_tenant_id ON payment(tenant_id);
CREATE INDEX idx_payment_payment_number ON payment(payment_number);
CREATE INDEX idx_payment_customer_id ON payment(customer_id);
CREATE INDEX idx_payment_payment_date ON payment(payment_date);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_payment_method ON payment(payment_method);
CREATE INDEX idx_payment_reference_number ON payment(reference_number);
CREATE INDEX idx_payment_created_at ON payment(created_at);

-- Create indexes for payment_allocation
CREATE INDEX idx_payment_allocation_uuid ON payment_allocation(uuid);
CREATE INDEX idx_payment_allocation_tenant_id ON payment_allocation(tenant_id);
CREATE INDEX idx_payment_allocation_payment_id ON payment_allocation(payment_id);
CREATE INDEX idx_payment_allocation_invoice_id ON payment_allocation(invoice_id);
CREATE INDEX idx_payment_allocation_date ON payment_allocation(allocation_date);
CREATE INDEX idx_payment_allocation_is_deleted ON payment_allocation(is_deleted);
CREATE INDEX idx_payment_allocation_created_at ON payment_allocation(created_at);
CREATE INDEX idx_payment_allocation_updated_at ON payment_allocation(updated_at);

-- Composite indexes
CREATE INDEX idx_payment_allocation_payment_invoice ON payment_allocation(payment_id, invoice_id)
    WHERE is_deleted = false;

-- Add table and column comments
COMMENT ON TABLE payment IS 'Customer payment receipts and tracking';
COMMENT ON TABLE payment_allocation IS 'Payment allocation to invoices';

COMMENT ON COLUMN payment.payment_number IS 'Unique payment receipt number (e.g., PAY-2025-0001)';
COMMENT ON COLUMN payment.amount IS 'Total payment amount received';
COMMENT ON COLUMN payment.allocated_amount IS 'Amount already allocated to invoices';
COMMENT ON COLUMN payment.unallocated_amount IS 'Amount not yet allocated (advance payment)';
COMMENT ON COLUMN payment.reference_number IS 'Cheque number, transaction ID, or reference';
COMMENT ON COLUMN payment.status IS 'Payment processing and allocation status';

COMMENT ON COLUMN payment_allocation.allocated_amount IS 'Amount from payment applied to this invoice';
COMMENT ON COLUMN payment_allocation.allocation_date IS 'When this allocation was made';


-- Create shipment_type enum
CREATE TYPE shipment_type AS ENUM (
    'CUSTOMER_PICKUP',
    'THIRD_PARTY_COURIER',
    'IN_HOUSE_DELIVERY'
);

-- Create shipment_status enum
CREATE TYPE shipment_status AS ENUM (
    'PENDING',
    'SCHEDULED',
    'SHIPPED',
    'DELIVERED',
    'CANCELLED'
);

-- Create route_status enum
CREATE TYPE route_status AS ENUM (
    'CREATED',
    'IN_TRANSIT',
    'COMPLETED'
);

-- Create employee table (minimal structure for delivery person tracking)
CREATE TABLE employee (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Basic employee fields
    tenant_id BIGINT NOT NULL,
    employee_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    designation VARCHAR(100),
    is_active BOOLEAN DEFAULT true
);

-- Create delivery_route table
CREATE TABLE delivery_route (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Route specific fields
    tenant_id BIGINT NOT NULL,
    route_number VARCHAR(50) UNIQUE NOT NULL,
    area_name VARCHAR(255),
    employee_id BIGINT,
    vehicle_number VARCHAR(50),
    status route_status,
    start_date TIMESTAMP,

    -- Foreign key constraint
    CONSTRAINT fk_route_employee FOREIGN KEY (employee_id)
        REFERENCES employee(id) ON DELETE SET NULL
);

-- Create delivery table
CREATE TABLE delivery (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Delivery specific fields
    tenant_id BIGINT NOT NULL,
    delivery_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    type shipment_type NOT NULL,
    status shipment_status NOT NULL,
    delivery_person_id BIGINT,
    scheduled_date TIMESTAMP,
    shipped_date TIMESTAMP,
    delivered_date TIMESTAMP,
    delivery_address VARCHAR(500),
    contact_person VARCHAR(100),
    contact_phone VARCHAR(20),
    remarks TEXT,
    route_id BIGINT,

    -- Foreign key constraints
    CONSTRAINT fk_delivery_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_customer FOREIGN KEY (customer_id)
        REFERENCES contact(id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_person FOREIGN KEY (delivery_person_id)
        REFERENCES employee(id) ON DELETE SET NULL,
    CONSTRAINT fk_delivery_route FOREIGN KEY (route_id)
        REFERENCES delivery_route(id) ON DELETE SET NULL
);

-- Create delivery_item table
CREATE TABLE delivery_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Delivery item specific fields
    item_id BIGINT,
    item_name VARCHAR(255),
    invoice_item_id BIGINT,
    batch_number VARCHAR(100),
    delivery_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,

    -- Foreign key constraints
    CONSTRAINT fk_delivery_item_delivery FOREIGN KEY (delivery_id)
        REFERENCES delivery(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_item_invoice_item FOREIGN KEY (invoice_item_id)
        REFERENCES invoice_item(id) ON DELETE SET NULL
);

-- Create indexes for employee
CREATE INDEX idx_employee_uuid ON employee(uuid);
CREATE INDEX idx_employee_tenant_id ON employee(tenant_id);
CREATE INDEX idx_employee_employee_code ON employee(employee_code);
CREATE INDEX idx_employee_name ON employee(name);
CREATE INDEX idx_employee_is_active ON employee(is_active);
CREATE INDEX idx_employee_is_deleted ON employee(is_deleted);
CREATE INDEX idx_employee_created_at ON employee(created_at);
CREATE INDEX idx_employee_updated_at ON employee(updated_at);

-- Create indexes for delivery_route
CREATE INDEX idx_route_uuid ON delivery_route(uuid);
CREATE INDEX idx_route_tenant_id ON delivery_route(tenant_id);
CREATE INDEX idx_route_route_number ON delivery_route(route_number);
CREATE INDEX idx_route_area_name ON delivery_route(area_name);
CREATE INDEX idx_route_employee_id ON delivery_route(employee_id);
CREATE INDEX idx_route_status ON delivery_route(status);
CREATE INDEX idx_route_start_date ON delivery_route(start_date);
CREATE INDEX idx_route_is_deleted ON delivery_route(is_deleted);
CREATE INDEX idx_route_created_at ON delivery_route(created_at);
CREATE INDEX idx_route_updated_at ON delivery_route(updated_at);

-- Composite indexes for routes
CREATE INDEX idx_route_tenant_status ON delivery_route(tenant_id, status)
    WHERE is_deleted = false;
CREATE INDEX idx_route_employee_status ON delivery_route(employee_id, status)
    WHERE is_deleted = false;

-- Create indexes for delivery
CREATE INDEX idx_delivery_uuid ON delivery(uuid);
CREATE INDEX idx_delivery_tenant_id ON delivery(tenant_id);
CREATE INDEX idx_delivery_delivery_number ON delivery(delivery_number);
CREATE INDEX idx_delivery_invoice_id ON delivery(invoice_id);
CREATE INDEX idx_delivery_customer_id ON delivery(customer_id);
CREATE INDEX idx_delivery_type ON delivery(type);
CREATE INDEX idx_delivery_status ON delivery(status);
CREATE INDEX idx_delivery_person_id ON delivery(delivery_person_id);
CREATE INDEX idx_delivery_route_id ON delivery(route_id);
CREATE INDEX idx_delivery_scheduled_date ON delivery(scheduled_date);
CREATE INDEX idx_delivery_shipped_date ON delivery(shipped_date);
CREATE INDEX idx_delivery_delivered_date ON delivery(delivered_date);
CREATE INDEX idx_delivery_created_at ON delivery(created_at);

-- Create indexes for delivery_item
CREATE INDEX idx_delivery_item_uuid ON delivery_item(uuid);
CREATE INDEX idx_delivery_item_delivery_id ON delivery_item(delivery_id);
CREATE INDEX idx_delivery_item_item_id ON delivery_item(item_id);
CREATE INDEX idx_delivery_item_invoice_item_id ON delivery_item(invoice_item_id);
CREATE INDEX idx_delivery_item_batch_number ON delivery_item(batch_number);
CREATE INDEX idx_delivery_item_created_at ON delivery_item(created_at);

-- Add table and column comments
COMMENT ON TABLE employee IS 'Employee master for delivery personnel and staff';
COMMENT ON TABLE delivery_route IS 'Delivery routes for organizing shipments';
COMMENT ON TABLE delivery IS 'Delivery/shipment tracking for invoices';
COMMENT ON TABLE delivery_item IS 'Line items for deliveries';

COMMENT ON COLUMN delivery.delivery_number IS 'Unique delivery number (e.g., DEV-2025-001)';
COMMENT ON COLUMN delivery.type IS 'Delivery method/type';
COMMENT ON COLUMN delivery.status IS 'Current delivery status';
COMMENT ON COLUMN delivery.scheduled_date IS 'When delivery is scheduled';
COMMENT ON COLUMN delivery.shipped_date IS 'When shipment was dispatched';
COMMENT ON COLUMN delivery.delivered_date IS 'When delivery was completed';

COMMENT ON COLUMN delivery_route.route_number IS 'Unique route identifier (e.g., ROUTE-001)';
COMMENT ON COLUMN delivery_route.status IS 'Route progress status';

COMMENT ON COLUMN delivery_item.batch_number IS 'Batch number for tracking';
COMMENT ON COLUMN delivery_item.invoice_item_id IS 'Link to specific invoice line item';

-- Create sales_return table
CREATE TABLE sales_return (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Sales return specific fields
    tenant_id BIGINT NOT NULL,
    return_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_id BIGINT NOT NULL,
    return_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(18, 2) NOT NULL,
    credit_note_payment_id BIGINT,

    -- Foreign key constraints
    CONSTRAINT fk_sales_return_invoice FOREIGN KEY (invoice_id)
        REFERENCES invoice(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sales_return_credit_note FOREIGN KEY (credit_note_payment_id)
        REFERENCES payment(id) ON DELETE SET NULL
);

-- Create sales_return_item table
CREATE TABLE sales_return_item (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Sales return item specific fields
    sales_return_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(18, 2) NOT NULL,
    reason VARCHAR(500),

    -- Foreign key constraint
    CONSTRAINT fk_sales_return_item_return FOREIGN KEY (sales_return_id)
        REFERENCES sales_return(id) ON DELETE CASCADE
);

-- Create indexes for sales_return
CREATE INDEX idx_sales_return_uuid ON sales_return(uuid);
CREATE INDEX idx_sales_return_tenant_id ON sales_return(tenant_id);
CREATE INDEX idx_sales_return_return_number ON sales_return(return_number);
CREATE INDEX idx_sales_return_invoice_id ON sales_return(invoice_id);
CREATE INDEX idx_sales_return_return_date ON sales_return(return_date);
CREATE INDEX idx_sales_return_credit_note_payment_id ON sales_return(credit_note_payment_id);
CREATE INDEX idx_sales_return_created_at ON sales_return(created_at);

-- Create indexes for sales_return_item
CREATE INDEX idx_sales_return_item_uuid ON sales_return_item(uuid);
CREATE INDEX idx_sales_return_item_return_id ON sales_return_item(sales_return_id);
CREATE INDEX idx_sales_return_item_item_id ON sales_return_item(item_id);
CREATE INDEX idx_sales_return_item_created_at ON sales_return_item(created_at);

-- Add table and column comments
COMMENT ON TABLE sales_return IS 'Sales returns from customers';
COMMENT ON TABLE sales_return_item IS 'Line items for sales returns';

COMMENT ON COLUMN sales_return.return_number IS 'Unique sales return number (e.g., SR-2025-001)';
COMMENT ON COLUMN sales_return.invoice_id IS 'Original invoice being returned against';
COMMENT ON COLUMN sales_return.return_date IS 'Date when return was processed';
COMMENT ON COLUMN sales_return.total_amount IS 'Total value of returned items';
COMMENT ON COLUMN sales_return.credit_note_payment_id IS 'Link to credit note payment record';

COMMENT ON COLUMN sales_return_item.quantity IS 'Quantity being returned';
COMMENT ON COLUMN sales_return_item.unit_price IS 'Original unit price from invoice';
COMMENT ON COLUMN sales_return_item.reason IS 'Reason for return (e.g., Damaged, Wrong Item, Defective)';
