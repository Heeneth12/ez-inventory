-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS inventory;

-- Set search path to inventory schema
SET search_path TO inventory;

-- Create items table
CREATE TABLE inventory.items (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT false,

    -- Item specific fields
    name VARCHAR(255) NOT NULL,
    tenant_id BIGINT NOT NULL,
    item_code VARCHAR(255) NOT NULL UNIQUE,
    sku VARCHAR(255),
    barcode VARCHAR(255),
    item_type inventory.item_type NOT NULL,
    image_url VARCHAR(500),
    category VARCHAR(255),
    unit_of_measure VARCHAR(50),
    brand VARCHAR(255),
    manufacturer VARCHAR(255),
    purchase_price DECIMAL(18, 2),
    selling_price DECIMAL(18, 2),
    mrp DECIMAL(18, 2),
    tax_percentage DECIMAL(5, 2),
    discount_percentage DECIMAL(5, 2),
    hsn_sac_code VARCHAR(50),
    description TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Create indexes for better query performance
CREATE INDEX idx_items_uuid ON inventory.items(uuid);
CREATE INDEX idx_items_tenant_id ON inventory.items(tenant_id);
CREATE INDEX idx_items_item_code ON inventory.items(item_code);
CREATE INDEX idx_items_sku ON inventory.items(sku);
CREATE INDEX idx_items_barcode ON inventory.items(barcode);
CREATE INDEX idx_items_category ON inventory.items(category);
CREATE INDEX idx_items_is_active ON inventory.items(is_active);
CREATE INDEX idx_items_tenant_active ON inventory.items(tenant_id, is_active) WHERE is_deleted = false;
CREATE INDEX idx_items_created_at ON inventory.items(created_at);

-- Add comment to table
COMMENT ON TABLE inventory.items IS 'Stores inventory item master data';
COMMENT ON COLUMN inventory.items.uuid IS 'Unique identifier for external references';
COMMENT ON COLUMN inventory.items.is_deleted IS 'Soft delete flag';


-- Insert 20 Dummy FMCG Items
INSERT INTO inventory.items (
    uuid, created_at, updated_at, is_deleted,
    name, tenant_id, item_code, sku, barcode, item_type,
    unit_of_measure, brand, manufacturer,
    mrp, purchase_price, selling_price,
    tax_percentage, discount_percentage, hsn_sac_code, description, is_active
) VALUES
-- 1. Britannia Good Day Butter Cookies
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Britannia Good Day Butter Cookies', 1, 'BRT-GDB-001', 'SKU-BRT-001', '8901063012345', 'PRODUCT',
    'Pack', 'Britannia', 'Britannia Industries',
    60.00, 45.00, 55.00,
    18.00, 0.00, '19053100', 'Rich butter cookies', true
),
-- 2. Britannia Marie Gold Biscuits
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Britannia Marie Gold Biscuits', 1, 'BRT-MRG-002', 'SKU-BRT-002', '8901063012346', 'PRODUCT',
    'Pack', 'Britannia', 'Britannia Industries',
    35.00, 25.00, 32.00,
    18.00, 0.00, '19053100', 'Crisp and light tea time biscuits', true
),
-- 3. Britannia Bourbon Cream Biscuits
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Britannia Bourbon Cream Biscuits', 1, 'BRT-BRB-003', 'SKU-BRT-003', '8901063012347', 'PRODUCT',
    'Pack', 'Britannia', 'Britannia Industries',
    40.00, 30.00, 38.00,
    18.00, 0.00, '19053100', 'Chocolaty cream biscuits', true
),
-- 4. Maggi 2-Minute Noodles Masala
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Maggi 2-Minute Noodles Masala', 1, 'NES-MAG-004', 'SKU-NES-004', '8901058812345', 'PRODUCT',
    'Pack', 'Maggi', 'Nestle India',
    14.00, 10.50, 13.00,
    12.00, 0.00, '19023010', 'Instant noodles with masala tastemaker', true
),
-- 5. Tata Salt (1kg)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Tata Salt Iodized', 1, 'TAT-SLT-005', 'SKU-TAT-005', '8904043912345', 'PRODUCT',
    'Kg', 'Tata Salt', 'Tata Consumer Products',
    28.00, 20.00, 26.00,
    0.00, 0.00, '25010010', 'Vacuum evaporated iodized salt', true
),
-- 6. Amul Butter (100g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Amul Pasteurized Butter', 1, 'AMU-BUT-006', 'SKU-AMU-006', '8901262010012', 'PRODUCT',
    'Pack', 'Amul', 'GCMMF',
    56.00, 48.00, 54.00,
    12.00, 0.00, '04051000', 'Pasteurized table butter', true
),
-- 7. Surf Excel Easy Wash Detergent Powder (1kg)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Surf Excel Easy Wash', 1, 'HUL-SRF-007', 'SKU-HUL-007', '8901030512345', 'PRODUCT',
    'Kg', 'Surf Excel', 'Hindustan Unilever',
    130.00, 105.00, 125.00,
    18.00, 2.00, '34022010', 'Detergent powder for tough stains', true
),
-- 8. Colgate Strong Teeth Toothpaste (100g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Colgate Strong Teeth', 1, 'COL-TPS-008', 'SKU-COL-008', '8901314010567', 'PRODUCT',
    'Tube', 'Colgate', 'Colgate-Palmolive',
    65.00, 50.00, 62.00,
    18.00, 0.00, '33061020', 'Calcium boost toothpaste', true
),
-- 9. Dettol Antiseptic Liquid (250ml)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Dettol Antiseptic Liquid', 1, 'RB-DET-009', 'SKU-RB-009', '8901396102501', 'PRODUCT',
    'Bottle', 'Dettol', 'Reckitt Benckiser',
    145.00, 115.00, 138.00,
    12.00, 0.00, '30049087', 'Antiseptic liquid for first aid', true
),
-- 10. Aashirvaad Shudh Chakki Atta (5kg)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Aashirvaad Shudh Chakki Atta', 1, 'ITC-ATT-010', 'SKU-ITC-010', '8901725112345', 'PRODUCT',
    'Bag', 'Aashirvaad', 'ITC Limited',
    240.00, 190.00, 230.00,
    0.00, 0.00, '11010000', 'Whole wheat flour 5kg pack', true
),
-- 11. Parle-G Biscuits (130g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Parle-G Original Gluco', 1, 'PAR-GLU-011', 'SKU-PAR-011', '8901719101234', 'PRODUCT',
    'Pack', 'Parle', 'Parle Products',
    10.00, 7.50, 9.50,
    18.00, 0.00, '19053100', 'Glucose biscuits', true
),
-- 12. Cadbury Dairy Milk Silk (60g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Cadbury Dairy Milk Silk', 1, 'CAD-SLK-012', 'SKU-CAD-012', '7622201123456', 'PRODUCT',
    'Bar', 'Cadbury', 'Mondelez India',
    80.00, 60.00, 75.00,
    18.00, 0.00, '18063200', 'Creamy milk chocolate bar', true
),
-- 13. Sunfeast Dark Fantasy Choco Fills
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Sunfeast Dark Fantasy Choco Fills', 1, 'ITC-DFS-013', 'SKU-ITC-013', '8901725189012', 'PRODUCT',
    'Box', 'Sunfeast', 'ITC Limited',
    40.00, 30.00, 38.00,
    18.00, 0.00, '19053100', 'Cookies with molten chocolate centre', true
),
-- 14. Everest Chicken Masala (100g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Everest Chicken Masala', 1, 'EVE-CHI-014', 'SKU-EVE-014', '8901786021005', 'PRODUCT',
    'Box', 'Everest', 'Everest Food Products',
    82.00, 65.00, 78.00,
    5.00, 0.00, '09109100', 'Spices blend for chicken curry', true
),
-- 15. Vim Dishwash Gel (250ml)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Vim Lemon Dishwash Gel', 1, 'HUL-VIM-015', 'SKU-HUL-015', '8901030612345', 'PRODUCT',
    'Bottle', 'Vim', 'Hindustan Unilever',
    55.00, 42.00, 52.00,
    18.00, 0.00, '34022010', 'Lemon dishwashing liquid gel', true
),
-- 16. Lizol Floor Cleaner Floral (500ml)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Lizol Disinfectant Floor Cleaner', 1, 'RB-LIZ-016', 'SKU-RB-016', '8901396345678', 'PRODUCT',
    'Bottle', 'Lizol', 'Reckitt Benckiser',
    109.00, 85.00, 105.00,
    18.00, 0.00, '38089400', 'Floral scented floor cleaner', true
),
-- 17. Kissan Mixed Fruit Jam (500g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Kissan Mixed Fruit Jam', 1, 'HUL-KIS-017', 'SKU-HUL-017', '8901030712345', 'PRODUCT',
    'Jar', 'Kissan', 'Hindustan Unilever',
    160.00, 125.00, 150.00,
    12.00, 0.00, '20079910', 'Mixed fruit jam', true
),
-- 18. Nescafe Classic Coffee (50g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Nescafe Classic Instant Coffee', 1, 'NES-COF-018', 'SKU-NES-018', '8901058865432', 'PRODUCT',
    'Jar', 'Nescafe', 'Nestle India',
    165.00, 130.00, 158.00,
    18.00, 0.00, '21011110', 'Instant coffee powder', true
),
-- 19. Fortune Sunlite Refined Sunflower Oil (1L)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Fortune Refined Sunflower Oil', 1, 'ADA-OIL-019', 'SKU-ADA-019', '8906007281234', 'PRODUCT',
    'Pouch', 'Fortune', 'Adani Wilmar',
    145.00, 120.00, 140.00,
    5.00, 0.00, '15121910', 'Refined sunflower cooking oil', true
),
-- 20. Bournvita Health Drink (500g)
(
    gen_random_uuid(), NOW(), NOW(), false,
    'Cadbury Bournvita', 1, 'CAD-BRN-020', 'SKU-CAD-020', '7622201234567', 'PRODUCT',
    'Jar', 'Bournvita', 'Mondelez India',
    235.00, 190.00, 225.00,
    18.00, 0.00, '19019090', 'Chocolate health drink mix', true
);