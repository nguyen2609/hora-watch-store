-- Fictional catalog only. No user accounts, passwords or customer records.
-- Repeating this script never overwrites existing categories or products.
BEGIN;

INSERT INTO app.categories (name, description) VALUES
    ('Automatic', 'Mechanical timepieces with self-winding movements.'),
    ('Smartwatch', 'Connected watches for everyday activity.')
ON CONFLICT (name) DO NOTHING;

INSERT INTO app.watches
    (category_id, name, brand, model_number, price, stock_quantity, watch_type,
     movement_type, battery_life_hours, description, image_url, available)
SELECT category.id, product.name, 'HORA', product.model_number, product.price,
       product.stock_quantity, product.watch_type, product.movement_type,
       product.battery_life_hours, product.description, NULL, true
FROM (VALUES
    ('Automatic', 'Heritage Automatic', 'HORA-HERITAGE-A01', 6500000, 12,
     'AUTOMATIC', 'Self-winding mechanical', NULL::integer,
     'A timeless automatic watch with a refined everyday profile.'),
    ('Automatic', 'Atelier Classic', 'HORA-ATELIER-A02', 8200000, 8,
     'AUTOMATIC', 'Self-winding mechanical', NULL::integer,
     'A classic mechanical timepiece for formal occasions.'),
    ('Automatic', 'Voyager Automatic', 'HORA-VOYAGER-A03', 10500000, 6,
     'AUTOMATIC', 'Self-winding mechanical', NULL::integer,
     'A distinctive automatic design for the modern traveler.'),
    ('Smartwatch', 'Everyday Smart', 'HORA-EVERYDAY-S01', 4500000, 20,
     'SMART', NULL::varchar, 48,
     'A versatile smartwatch designed for daily routines.'),
    ('Smartwatch', 'Active Smart', 'HORA-ACTIVE-S02', 5600000, 15,
     'SMART', NULL::varchar, 72,
     'A connected companion with extended battery life.'),
    ('Smartwatch', 'Urban Smart', 'HORA-URBAN-S03', 7200000, 10,
     'SMART', NULL::varchar, 96,
     'A sleek smartwatch with a contemporary urban aesthetic.')
) AS product(category_name, name, model_number, price, stock_quantity, watch_type,
             movement_type, battery_life_hours, description)
JOIN app.categories AS category ON category.name = product.category_name
ON CONFLICT (model_number) DO NOTHING;

COMMIT;
