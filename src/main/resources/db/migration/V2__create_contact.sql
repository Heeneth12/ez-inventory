-- Create contact table
CREATE TABLE contact (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    contact_code VARCHAR(255),
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    gst_number VARCHAR(50),
    contact_type VARCHAR(50),
    credit_days INTEGER,
    connected_tenant_id BIGINT,
    network_request_id BIGINT,
    active BOOLEAN DEFAULT true
    CONSTRAINT fk_contact_network_request
            FOREIGN KEY (network_request_id)
            REFERENCES network_requests(id)
            ON DELETE SET NULL
);

-- Create address table
CREATE TABLE address (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    route VARCHAR(255),
    area VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    pin_code VARCHAR(20),
    address_type VARCHAR(50),
    contact_id BIGINT,
    CONSTRAINT fk_address_contact FOREIGN KEY (contact_id) REFERENCES contact(id) ON DELETE CASCADE
);

-- Create indexes for contact table
CREATE INDEX idx_contact_uuid ON contact(uuid);
CREATE INDEX idx_contact_tenant_id ON contact(tenant_id);
CREATE INDEX idx_contact_code ON contact(contact_code);
CREATE INDEX idx_contact_email ON contact(email);
CREATE INDEX idx_contact_phone ON contact(phone);
CREATE INDEX idx_contact_gst_number ON contact(gst_number);

-- Create indexes for address table
CREATE INDEX idx_address_uuid ON address(uuid);
CREATE INDEX idx_address_contact_id ON address(contact_id);
CREATE INDEX idx_address_type ON address(address_type);
CREATE INDEX idx_address_city ON address(city);
CREATE INDEX idx_address_state ON address(state);
CREATE INDEX idx_address_pin_code ON address(pin_code);

CREATE INDEX idx_contact_connected_tenant ON contact(connected_tenant_id);
CREATE INDEX idx_contact_tenant_connected_lookup ON contact(tenant_id, connected_tenant_id);

-- Add comments
COMMENT ON TABLE contact IS 'Stores contact information for customers, suppliers, etc.';
COMMENT ON TABLE address IS 'Stores addresses associated with contacts';
COMMENT ON COLUMN contact.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN address.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN contact.is_deleted IS 'Soft delete flag';
COMMENT ON COLUMN address.is_deleted IS 'Soft delete flag';


SET search_path TO inventory;

INSERT INTO contact (
    uuid,
    contact_code,
    tenant_id,
    name,
    email,
    phone,
    gst_number,
    contact_type,
    credit_days,
    active
) VALUES

-- SUPPLIERS
(gen_random_uuid()::text, 'SUP-001', 1, 'Britannia Industries Ltd', 'sales@britannia.com', '9000000001', '29AACCB4321A1ZP', 'SUPPLIER', 30, true),
(gen_random_uuid()::text, 'SUP-002', 1, 'Hindustan Unilever Ltd', 'trade@hul.com', '9000000002', '27AAACH1234A1ZQ', 'SUPPLIER', 21, true),
(gen_random_uuid()::text, 'SUP-003', 1, 'ITC Limited', 'supply@itc.com', '9000000003', '33AAACI5555A1ZR', 'SUPPLIER', 30, true),
(gen_random_uuid()::text, 'SUP-004', 1, 'Nestle India Ltd', 'orders@nestle.com', '9000000004', '19AAACN4444A1ZS', 'SUPPLIER', 15, true),
(gen_random_uuid()::text, 'SUP-005', 1, 'Tata Consumer Products', 'b2b@tataconsumer.com', '9000000005', '27AAACT8888A1ZT', 'SUPPLIER', 30, true),
(gen_random_uuid()::text, 'SUP-006', 1, 'Parle Products Pvt Ltd', 'supply@parle.com', '9000000006', '27AAACP7777A1ZU', 'SUPPLIER', 20, true),
(gen_random_uuid()::text, 'SUP-007', 1, 'Amul Dairy', 'distribution@amul.com', '9000000007', '24AAACA6666A1ZV', 'SUPPLIER', 15, true),
(gen_random_uuid()::text, 'SUP-008', 1, 'Dabur India Ltd', 'trade@dabur.com', '9000000008', '09AAACD9999A1ZW', 'SUPPLIER', 30, true),

-- CUSTOMERS
(gen_random_uuid()::text, 'CUS-001', 1, 'Ramesh Kirana Store', 'ramesh.store@gmail.com', '9100000001', NULL, 'CUSTOMER', 7, true),
(gen_random_uuid()::text, 'CUS-002', 1, 'Sri Lakshmi Super Market', 'lakshmi.market@gmail.com', '9100000002', NULL, 'CUSTOMER', 10, true),
(gen_random_uuid()::text, 'CUS-003', 1, 'Balaji Wholesale Mart', 'balaji.wholesale@gmail.com', '9100000003', NULL, 'CUSTOMER', 15, true),
(gen_random_uuid()::text, 'CUS-004', 1, 'Sai Ganesh Stores', 'saiganesh@gmail.com', '9100000004', NULL, 'CUSTOMER', 7, true),
(gen_random_uuid()::text, 'CUS-005', 1, 'City Fresh Mart', 'cityfresh@gmail.com', '9100000005', NULL, 'CUSTOMER', 10, true),
(gen_random_uuid()::text, 'CUS-006', 1, 'Venkateswara Traders', 'venkateswara@gmail.com', '9100000006', NULL, 'CUSTOMER', 15, true),
(gen_random_uuid()::text, 'CUS-007', 1, 'Daily Needs Store', 'dailyneeds@gmail.com', '9100000007', NULL, 'CUSTOMER', 5, true),
(gen_random_uuid()::text, 'CUS-008', 1, 'Om Sai Retailers', 'omsai@gmail.com', '9100000008', NULL, 'CUSTOMER', 7, true),

-- BOTH
(gen_random_uuid()::text, 'BTH-001', 1, 'Metro Cash & Carry', 'metro@metro.co.in', '9200000001', '27AAACM1111A1ZX', 'BOTH', 15, true),
(gen_random_uuid()::text, 'BTH-002', 1, 'Reliance Smart', 'trade@reliance.com', '9200000002', '27AAACR2222A1ZY', 'BOTH', 30, true),
(gen_random_uuid()::text, 'BTH-003', 1, 'More Supermarket', 'supply@more.com', '9200000003', '29AAACM3333A1ZZ', 'BOTH', 15, true),
(gen_random_uuid()::text, 'BTH-004', 1, 'Spencer’s Retail', 'orders@spencers.com', '9200000004', '19AAACS4444A1ZA', 'BOTH', 20, true);


-- ========================================================
-- 2. INSERT DUMMY DATA INTO ADDRESSES
-- ========================================================
INSERT INTO address (
    uuid,
    address_line1,
    address_line2,
    route,
    area,
    city,
    state,
    country,
    pin_code,
    address_type,
    contact_id
)
SELECT
    gen_random_uuid()::text,
    a.address_line1,
    a.address_line2,
    a.route,
    a.area,
    a.city,
    a.state,
    a.country,
    a.pin_code,
    a.addr_type,
    c.id
FROM (
    VALUES
    -- SUPPLIER ADDRESSES
    ('SUP-001', 'Plot 22, Industrial Area', 'Phase 2', 'NH-44', 'Peenya', 'Bengaluru', 'Karnataka', 'India', '560058', 'OFFICE'),
    ('SUP-002', 'Unilever House', 'Andheri East', 'WE Highway', 'Chakala', 'Mumbai', 'Maharashtra', 'India', '400093', 'OFFICE'),
    ('SUP-003', 'ITC Green Centre', 'Sardar Patel Marg', 'Central Road', 'Chanakyapuri', 'New Delhi', 'Delhi', 'India', '110021', 'OFFICE'),
    ('SUP-004', 'Nestle Campus', 'DLF Phase 3', 'Cyber City Road', 'Udyog Vihar', 'Gurgaon', 'Haryana', 'India', '122002', 'OFFICE'),
    ('SUP-005', 'Tata Consumer HQ', 'Lower Parel', 'Dr Annie Besant Rd', 'Worli', 'Mumbai', 'Maharashtra', 'India', '400018', 'OFFICE'),
    ('SUP-006', 'Parle Agro Plant', 'MIDC', 'Industrial Rd', 'Chakan', 'Pune', 'Maharashtra', 'India', '410501', 'OFFICE'),
    ('SUP-007', 'Amul Dairy Plant', 'State Highway', 'Anand Rd', 'Anand', 'Anand', 'Gujarat', 'India', '388001', 'OFFICE'),
    ('SUP-008', 'Dabur Factory', 'Industrial Area', 'GT Road', 'Sahibabad', 'Ghaziabad', 'Uttar Pradesh', 'India', '201010', 'OFFICE'),

    -- CUSTOMER ADDRESSES
    ('CUS-001', 'Shop No 12', 'Main Road', 'MG Road', 'Begumpet', 'Hyderabad', 'Telangana', 'India', '500016', 'SHIPPING'),
    ('CUS-002', 'Door No 45-2', 'Near Temple', 'Station Rd', 'Kukatpally', 'Hyderabad', 'Telangana', 'India', '500072', 'SHIPPING'),
    ('CUS-003', 'Warehouse 3', 'Wholesale Market', 'Market Rd', 'Kothapet', 'Hyderabad', 'Telangana', 'India', '500035', 'SHIPPING'),
    ('CUS-004', 'Shop No 9', 'Opp Bus Stand', 'Main Rd', 'Madhapur', 'Hyderabad', 'Telangana', 'India', '500081', 'SHIPPING'),
    ('CUS-005', 'Unit 18', 'City Complex', 'Ring Rd', 'LB Nagar', 'Hyderabad', 'Telangana', 'India', '500074', 'SHIPPING'),
    ('CUS-006', 'Plot 11', 'Wholesale Yard', 'Market Rd', 'Jeedimetla', 'Hyderabad', 'Telangana', 'India', '500055', 'SHIPPING'),
    ('CUS-007', 'Shop 4', 'Daily Market', 'Local Rd', 'Uppal', 'Hyderabad', 'Telangana', 'India', '500039', 'SHIPPING'),
    ('CUS-008', 'Shop No 22', 'Retail Zone', 'High St', 'Ameerpet', 'Hyderabad', 'Telangana', 'India', '500038', 'SHIPPING'),

    -- BOTH ADDRESSES
    ('BTH-001', 'Metro Warehouse', 'Sector 18', 'Link Rd', 'Vashi', 'Navi Mumbai', 'Maharashtra', 'India', '400703', 'OFFICE'),
    ('BTH-002', 'Reliance Distribution', 'Business Park', 'Outer Ring Rd', 'Whitefield', 'Bengaluru', 'Karnataka', 'India', '560066', 'OFFICE'),
    ('BTH-003', 'More Logistics Hub', 'Sector 5', 'Service Rd', 'Salt Lake', 'Kolkata', 'West Bengal', 'India', '700091', 'OFFICE'),
    ('BTH-004', 'Spencers Central DC', 'Industrial Estate', 'GST Rd', 'Tambaram', 'Chennai', 'Tamil Nadu', 'India', '600045', 'OFFICE')
) AS a(
    contact_code,
    address_line1,
    address_line2,
    route,
    area,
    city,
    state,
    country,
    pin_code,
    addr_type
)
JOIN contact c ON c.contact_code = a.contact_code;