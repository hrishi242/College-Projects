-- Create the InventoryManagement Database
CREATE DATABASE IF NOT EXISTS InventoryManagement;
USE InventoryManagement;

-- Create ENUM type for order status (refactor as a reference table)
CREATE TABLE order_status (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure it's INT, not SERIAL
    status VARCHAR(50) NOT NULL UNIQUE
);

-- Insert order statuses
INSERT INTO order_status (status) VALUES 
    ('Pending'), 
    ('Processed'), 
    ('Shipped'), 
    ('Delivered'), 
    ('Cancelled');

INSERT INTO order_status (id, status) VALUES (4, 'Cancelled - Out of Stock')
ON DUPLICATE KEY UPDATE status = 'Cancelled - Out of Stock';

-- Create Category Table for product categorization
CREATE TABLE category (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Use INT for id
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- Create Supplier Table
CREATE TABLE supplier (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure the ID column is INT for consistency
    name VARCHAR(255) NOT NULL,
    contact_no VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    category_id int,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL
);

-- Create Product Table
CREATE TABLE product (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    unit_price DECIMAL(10, 2) NOT NULL,
    reorder_level INT NOT NULL CHECK (reorder_level >= 0),
    category_id INT,  -- Ensure this is INT, which matches category.id
    supplier_id INT,  -- Ensure this is INT, which matches supplier.id
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    FOREIGN KEY (supplier_id) REFERENCES supplier(id) ON DELETE SET NULL
);

-- Create Warehouse Table
CREATE TABLE warehouse (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure the ID column is INT
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    capacity INT NOT NULL CHECK (capacity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create Inventory Table for tracking stock in warehouses
CREATE TABLE inventory (
    inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    product_id INT,
    warehouse_id INT,
    quantity INT NOT NULL CHECK (quantity >= 0),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    aisle_number VARCHAR(10),  -- New column for aisle location
    shelf_number VARCHAR(10),  -- New column for shelf location
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
);

-- Create Customer Table
CREATE TABLE customer (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure the ID column is INT
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    password VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP
);

-- Create Employee Table
CREATE TABLE employee (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure the ID column is INT
    name VARCHAR(255) NOT NULL,
    position VARCHAR(50),
    phone VARCHAR(50),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(20)
);

-- Create Shipment Table
CREATE TABLE shipment (
    id INT AUTO_INCREMENT PRIMARY KEY,  -- Ensure the ID column is INT
    shipment_date TIMESTAMP,
    status VARCHAR(50),
    tracking_number VARCHAR(100)
);

CREATE TABLE customer_order (
    id INT AUTO_INCREMENT PRIMARY KEY,               -- Ensure the ID column is INT
    customer_id INT NOT NULL,                        -- Ensure this is INT to match customer.id
    employee_id INT,                                 -- Ensure this is INT to match employee.id
    shipment_id INT,                                 -- Ensure this is INT to match shipment.id
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Default the order date to the current timestamp
    total_amount DECIMAL(10, 2) NOT NULL,            -- Total amount for the order
    status_id INT,                                   -- Foreign Key to order_status, ensure it is INT
    FOREIGN KEY (customer_id) REFERENCES customer(id),  -- Foreign Key reference to customer
    FOREIGN KEY (employee_id) REFERENCES employee(id),  -- Foreign Key reference to employee
    FOREIGN KEY (shipment_id) REFERENCES shipment(id),  -- Foreign Key reference to shipment
    FOREIGN KEY (status_id) REFERENCES order_status(id)  -- Foreign Key reference to order_status
);

ALTER TABLE customer_order 
ADD COLUMN notification_shown BOOLEAN DEFAULT NULL,
ADD COLUMN processed_by INT,
ADD COLUMN processed_date TIMESTAMP NULL,
ADD FOREIGN KEY (processed_by) REFERENCES employee(id);

-- Create Order Item Table (removed problematic CHECK constraint)
CREATE TABLE order_item (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    inventory_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES customer_order(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id)
);

-- Create Transaction Log Table (modified)
CREATE TABLE transaction_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT,
    warehouse_id INT NOT NULL,  -- Keep NOT NULL
    type VARCHAR(50) NOT NULL CHECK (type IN ('Restock', 'Dispatch', 'Adjustment')),
    quantity INT NOT NULL CHECK (quantity <> 0),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE CASCADE  -- Changed to CASCADE
);

-- Create Audit Log Table
CREATE TABLE IF NOT EXISTS audit_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    operation_type VARCHAR(50) NOT NULL,
    changed_data TEXT,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE restock_request (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    inventory_id INT,
    requested_quantity INT DEFAULT 100,
    request_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('pending', 'confirmed', 'cancelled') DEFAULT 'pending',
    confirmed_by INT,  -- employee_id who confirmed
    confirmation_date TIMESTAMP NULL,
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id),
    FOREIGN KEY (confirmed_by) REFERENCES employee(id)
);

ALTER TABLE restock_request 
ADD CONSTRAINT unique_pending_request 
UNIQUE (inventory_id, status);

-- Triggers for audit logs on product table
DELIMITER $$

CREATE TRIGGER trg_audit_product_insert
AFTER INSERT ON product
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, changed_data, changed_by)
    VALUES ('product', 'INSERT', CONCAT('ID: ', NEW.id, ', Name: ', NEW.name), USER());
END $$

CREATE TRIGGER trg_audit_product_update
AFTER UPDATE ON product
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, changed_data, changed_by)
    VALUES ('product', 'UPDATE', CONCAT('ID: ', NEW.id, ', Name changed from: ', OLD.name, ' to: ', NEW.name), USER());
END $$

CREATE TRIGGER trg_audit_product_delete
AFTER DELETE ON product
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (table_name, operation_type, changed_data, changed_by)
    VALUES ('product', 'DELETE', CONCAT('ID: ', OLD.id, ', Name: ', OLD.name), USER());
END $$

DELIMITER ;

-- Create triggers for order_item and inventory updates
DELIMITER $$

-- Trigger for logging inventory updates
CREATE TRIGGER after_inventory_update
AFTER UPDATE ON inventory
FOR EACH ROW
BEGIN
    -- Log to audit_log
    INSERT INTO audit_log (
        table_name,
        operation_type,
        changed_data,
        changed_by
    )
    VALUES (
        'inventory',
        'UPDATE',
        CONCAT(
            'Product ID: ', NEW.product_id,
            ', Warehouse ID: ', NEW.warehouse_id,
            ', Quantity changed from: ', OLD.quantity,
            ' to: ', NEW.quantity
        ),
        USER()
    );
END$$

-- Trigger for logging customer_order changes
CREATE TRIGGER after_customer_order_update
AFTER UPDATE ON customer_order
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        table_name,
        operation_type,
        changed_data,
        changed_by
    )
    VALUES (
        'customer_order',
        'UPDATE',
        CONCAT(
            'Order ID: ', NEW.id,
            ', Status changed from: ', 
            (SELECT status FROM order_status WHERE id = OLD.status_id),
            ' to: ',
            (SELECT status FROM order_status WHERE id = NEW.status_id)
        ),
        USER()
    );
END$$

-- Trigger for logging new orders
CREATE TRIGGER after_customer_order_insert
AFTER INSERT ON customer_order
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        table_name,
        operation_type,
        changed_data,
        changed_by
    )
    VALUES (
        'customer_order',
        'INSERT',
        CONCAT(
            'New order created - Order ID: ', NEW.id,
            ', Customer ID: ', NEW.customer_id,
            ', Total Amount: ', NEW.total_amount
        ),
        USER()
    );
END$$

DELIMITER ;

-- Create a single trigger to handle inventory updates and logging
DELIMITER $$

CREATE TRIGGER after_order_item_insert
AFTER INSERT ON order_item
FOR EACH ROW
BEGIN
    -- Create a dispatch transaction log entry
    INSERT INTO transaction_log (
        product_id,
        warehouse_id,
        type,
        quantity
    )
    SELECT 
        NEW.product_id,
        i.warehouse_id,
        'Dispatch',
        -NEW.quantity
    FROM inventory i
    WHERE i.inventory_id = NEW.inventory_id;
    
    -- Note: We remove the inventory update from here since it will be handled
    -- by the process_order stored procedure
END$$

-- Modify the process_order procedure to handle inventory updates
CREATE PROCEDURE process_order(
    IN p_order_id INT,
    IN p_employee_id INT,
    OUT p_result VARCHAR(100)
)
BEGIN
    DECLARE v_can_fulfill BOOLEAN DEFAULT TRUE;
    DECLARE v_done BOOLEAN DEFAULT FALSE;
    DECLARE v_product_id INT;
    DECLARE v_inventory_id INT;
    DECLARE v_quantity INT;
    DECLARE v_available INT;
    
    -- Cursor for order items
    DECLARE order_items_cursor CURSOR FOR
        SELECT product_id, inventory_id, quantity
        FROM order_item
        WHERE order_id = p_order_id;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = TRUE;
    
    -- Start transaction
    START TRANSACTION;
    
    -- Check if order is already processed
    IF EXISTS (SELECT 1 FROM customer_order WHERE id = p_order_id AND status_id != 1) THEN
        SET p_result = 'Order already processed';
        ROLLBACK;
    ELSE
        -- Check stock availability for all items
        OPEN order_items_cursor;
        
        check_quantities: LOOP
            FETCH order_items_cursor INTO v_product_id, v_inventory_id, v_quantity;
            IF v_done THEN
                LEAVE check_quantities;
            END IF;
            
            SELECT quantity INTO v_available
            FROM inventory
            WHERE inventory_id = v_inventory_id
            FOR UPDATE;  -- Lock the row
            
            IF v_available < v_quantity THEN
                SET v_can_fulfill = FALSE;
                LEAVE check_quantities;
            END IF;
        END LOOP;
        
        CLOSE order_items_cursor;
        
        -- Reset for second pass
        SET v_done = FALSE;
        
        IF v_can_fulfill THEN
            -- Update inventory and mark order as processed
            OPEN order_items_cursor;
            
            update_inventory: LOOP
                FETCH order_items_cursor INTO v_product_id, v_inventory_id, v_quantity;
                IF v_done THEN
                    LEAVE update_inventory;
                END IF;
                
                -- This is now the only place where inventory is updated
                UPDATE inventory
                SET quantity = quantity - v_quantity
                WHERE inventory_id = v_inventory_id;
                
                -- Check if restock is needed
                CALL restock_inventory(v_inventory_id);
            END LOOP;
            
            CLOSE order_items_cursor;
            
            -- Update order status to processed
            UPDATE customer_order 
            SET status_id = 2,  -- Processed status
                processed_by = p_employee_id,
                processed_date = CURRENT_TIMESTAMP
            WHERE id = p_order_id;
            
            SET p_result = 'Order processed successfully';
            COMMIT;
        ELSE
            -- Cancel order due to insufficient stock
            UPDATE customer_order
            SET status_id = 4,  -- Cancelled - Out of Stock status
                processed_by = p_employee_id,
                processed_date = CURRENT_TIMESTAMP
            WHERE id = p_order_id;
            
            SET p_result = 'Order cancelled due to insufficient stock';
            COMMIT;
        END IF;
    END IF;
END$$

DELIMITER ;

DELIMITER //

CREATE PROCEDURE restock_inventory(IN inventoryID INT)
BEGIN
    DECLARE current_quantity INT;
    DECLARE existing_request INT;
    
    -- Get current quantity
    SELECT quantity INTO current_quantity 
    FROM inventory 
    WHERE inventory_id = inventoryID;
    
    -- Check if there's already a pending request
    SELECT COUNT(*) INTO existing_request 
    FROM restock_request 
    WHERE inventory_id = inventoryID 
    AND status = 'pending';
    
    -- Check if restocking is needed and no pending request exists
    IF current_quantity < 25 AND existing_request = 0 THEN
        -- Create a restock request
        INSERT INTO restock_request (inventory_id, requested_quantity)
        VALUES (inventoryID, 100)
        ON DUPLICATE KEY UPDATE request_date = CURRENT_TIMESTAMP;
        
        -- Log the restock request
        INSERT INTO audit_log (table_name, operation_type, changed_data, changed_by)
        VALUES ('restock_request', 'CREATE', 
                CONCAT('Inventory ID: ', inventoryID, ', Requested 100 units'), 
                'system');
    END IF;
END //

-- Updated confirm restock procedure
CREATE PROCEDURE confirm_restock(
    IN p_request_id INT,
    IN p_employee_id INT
)
BEGIN
    DECLARE v_inventory_id INT;
    DECLARE v_requested_quantity INT;
    
    START TRANSACTION;
    
    -- Get request details with row lock
    SELECT inventory_id, requested_quantity 
    INTO v_inventory_id, v_requested_quantity
    FROM restock_request 
    WHERE request_id = p_request_id 
    AND status = 'pending'
    FOR UPDATE;
    
    IF v_inventory_id IS NOT NULL THEN
        -- Update inventory quantity
        UPDATE inventory 
        SET quantity = quantity + v_requested_quantity 
        WHERE inventory_id = v_inventory_id;
        
        -- Update request status
        UPDATE restock_request 
        SET status = 'confirmed',
            confirmed_by = p_employee_id,
            confirmation_date = CURRENT_TIMESTAMP
        WHERE request_id = p_request_id;
        
        -- Log the confirmation
        INSERT INTO audit_log (table_name, operation_type, changed_data, changed_by)
        VALUES ('inventory', 'RESTOCK_CONFIRMED', 
                CONCAT('Inventory ID: ', v_inventory_id, ', Added ', v_requested_quantity, ' units'),
                CONCAT('employee:', p_employee_id));
                
        COMMIT;
    ELSE
        ROLLBACK;
    END IF;
END //

DELIMITER ;

-- Trigger for shipment tracking
DELIMITER $$

CREATE TRIGGER before_shipment_insert 
BEFORE INSERT ON shipment
FOR EACH ROW
BEGIN
    -- Generate tracking number: format SH-YYYYMMDD-XXXX
    SET NEW.tracking_number = CONCAT(
        'SH-',
        DATE_FORMAT(NOW(), '%Y%m%d'),
        '-',
        LPAD((SELECT COUNT(*) + 1 FROM shipment), 4, '0')
    );
    
    -- Set initial shipment status
    SET NEW.status = 'Pending';
    SET NEW.shipment_date = CURRENT_TIMESTAMP;
END$$

-- Trigger for logging new shipments
CREATE TRIGGER after_shipment_insert
AFTER INSERT ON shipment
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        table_name,
        operation_type,
        changed_data,
        changed_by
    )
    VALUES (
        'shipment',
        'INSERT',
        CONCAT(
            'New shipment created - Shipment ID: ', NEW.id,
            ', Tracking Number: ', NEW.tracking_number,
            ', Status: ', NEW.status
        ),
        USER()
    );
END$$

-- Trigger for when a shipment status changes
CREATE TRIGGER after_shipment_update
AFTER UPDATE ON shipment
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO audit_log (
            table_name,
            operation_type,
            changed_data,
            changed_by
        )
        VALUES (
            'shipment',
            'UPDATE',
            CONCAT(
                'Shipment ID: ', NEW.id,
                ', Status changed from: ', OLD.status,
                ' to: ', NEW.status,
                ', Tracking Number: ', NEW.tracking_number
            ),
            USER()
        );
    END IF;
END$$

-- Trigger before customer order insert to validate and set initial values
CREATE TRIGGER before_customer_order_insert
BEFORE INSERT ON customer_order
FOR EACH ROW
BEGIN
    -- Create new shipment
    INSERT INTO shipment (shipment_date, status)
    VALUES (CURRENT_TIMESTAMP, 'Pending');
    
    -- Set the shipment_id to the newly created shipment
    SET NEW.shipment_id = LAST_INSERT_ID();
    
    -- Set initial status to Pending (assuming 1 is the ID for 'Pending' status)
    SET NEW.status_id = (SELECT id FROM order_status WHERE status = 'Pending');
    
    -- Set order date if not provided
    IF NEW.order_date IS NULL THEN
        SET NEW.order_date = CURRENT_TIMESTAMP;
    END IF;
END$$

-- Trigger after customer order insert for logging
CREATE TRIGGER after_customer_order_insert_audit
AFTER INSERT ON customer_order
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        table_name,
        operation_type,
        changed_data,
        changed_by
    )
    VALUES (
        'customer_order',
        'INSERT',
        CONCAT(
            'New order created - Order ID: ', NEW.id,
            ', Customer ID: ', NEW.customer_id,
            ', Total Amount: ', NEW.total_amount,
            ', Shipment ID: ', NEW.shipment_id
        ),
        USER()
    );
END$$

DELIMITER ;

-- Create indexes for optimized querying
CREATE INDEX idx_customer_order_customer ON customer_order(customer_id);
CREATE INDEX idx_order_item_order ON order_item(order_id);
CREATE INDEX idx_order_item_product ON order_item(product_id);

-- Create the 'report' table
CREATE TABLE report (
    id INT AUTO_INCREMENT PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    generation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    generated_by VARCHAR(255) NOT NULL
);

-- Trigger to handle report generation before inserting a record
DELIMITER $$

CREATE TRIGGER generate_report_trigger
BEFORE INSERT ON report
FOR EACH ROW
BEGIN
    SET NEW.generated_by = USER();
END $$

DELIMITER ;

-- Example insertion into the 'report' table
INSERT INTO report (report_type) VALUES ('Inventory Report');

-- Populate category table
INSERT INTO category (name, description) VALUES
    ('Electronics', 'Devices and gadgets'),
    ('Furniture', 'Household and office furniture'),
    ('Clothing', 'Apparel for all genders and ages'),
    ('Books', 'Educational and recreational books'),
    ('Toys', 'Toys for children of all ages'),
    ('Groceries', 'Daily essentials and food items'),
    ('Beauty', 'Beauty and personal care products'),
    ('Automotive', 'Car and bike accessories'),
    ('Sports', 'Sports and outdoor equipment'),
    ('Kitchen', 'Kitchen appliances and utensils');

-- Populate supplier table
INSERT INTO supplier (name, contact_no, email, address, category_id) VALUES
    ('Tech Supplies Ltd.', '123-456-7890', 'contact@techsupplies.com', '123 Tech Avenue', 1),
    ('Furniture World', '987-654-3210', 'info@furnitureworld.com', '456 Furniture Street', 2),
    ('Fashion House', '555-666-7777', 'sales@fashionhouse.com', '789 Fashion Boulevard', 3),
    ('Book Haven', '444-333-2222', 'service@bookhaven.com', '234 Book Rd', 4),
    ('Toyland', '111-222-3333', 'contact@toyland.com', '567 Fun St', 5),
    ('Grocery Market', '666-777-8888', 'info@grocerymarket.com', '891 Grocery Ln', 6),
    ('Beauty Store', '222-111-5555', 'support@beautystore.com', '321 Beauty Blvd', 7),
    ('Auto Hub', '333-444-5555', 'sales@autohub.com', '654 Auto Ave', 8),
    ('Sports Center', '777-888-9999', 'service@sportscenter.com', '135 Sport St', 9),
    ('Kitchenware Ltd.', '888-999-0000', 'info@kitchenware.com', '246 Kitchen Rd', 10);

-- Populate product table with 10 products per category
INSERT INTO product (name, description, unit_price, reorder_level, category_id, supplier_id) VALUES
    -- Electronics
    ('Laptop', 'High-end gaming laptop', 1500.00, 10, 1, 1),
    ('Smartphone', 'Latest model smartphone', 700.00, 15, 1, 1),
    ('Headphones', 'Noise-cancelling over-ear headphones', 120.00, 20, 1, 1),
    ('Smartwatch', 'Water-resistant smartwatch', 250.00, 8, 1, 1),
    ('Tablet', 'High-resolution display tablet', 450.00, 10, 1, 1),
    ('Wireless Charger', 'Qi wireless charger', 30.00, 25, 1, 1),
    ('Bluetooth Speaker', 'Portable Bluetooth speaker', 80.00, 12, 1, 1),
    ('Laptop Stand', 'Adjustable laptop stand', 50.00, 5, 1, 1),
    ('Camera', 'Digital camera with 4K video', 550.00, 8, 1, 1),
    ('Keyboard', 'Mechanical keyboard with RGB lighting', 100.00, 30, 1, 1),
    
    -- Furniture
    ('Office Chair', 'Ergonomic office chair', 200.00, 5, 2, 2),
    ('Desk', 'Wooden office desk', 300.00, 10, 2, 2),
    ('Bookshelf', '5-shelf wooden bookshelf', 150.00, 20, 2, 2),
    ('Sofa', '3-seater sofa', 700.00, 12, 2, 2),
    ('Dining Table', 'Round wooden dining table', 500.00, 8, 2, 2),
    ('Armchair', 'Leather armchair', 350.00, 5, 2, 2),
    ('Cabinet', 'Wooden storage cabinet', 250.00, 15, 2, 2),
    ('Coffee Table', 'Glass top coffee table', 100.00, 18, 2, 2),
    ('Side Table', 'Small side table', 50.00, 30, 2, 2),
    ('Mirror', 'Wall-mounted mirror', 120.00, 10, 2, 2),
    
    -- Clothing
    ('T-shirt', '100% cotton T-shirt', 15.00, 20, 3, 3),
    ('Jeans', 'Denim jeans for men', 40.00, 15, 3, 3),
    ('Sweater', 'Woolen sweater', 25.00, 25, 3, 3),
    ('Jacket', 'Leather jacket', 100.00, 5, 3, 3),
    ('Dress', 'Summer dress for women', 45.00, 18, 3, 3),
    ('Shoes', 'Running shoes', 60.00, 10, 3, 3),
    ('Hat', 'Baseball cap', 15.00, 30, 3, 3),
    ('Socks', 'Cotton socks pack of 3', 10.00, 40, 3, 3),
    ('Gloves', 'Winter gloves', 20.00, 15, 3, 3),
    ('Scarf', 'Woolen scarf', 25.00, 12, 3, 3),
    
    -- Books
    ('Novel', 'Bestselling novel', 12.99, 10, 4, 4),
    ('Science Book', 'Educational science book', 25.00, 15, 4, 4),
    ('Textbook', 'Mathematics textbook', 50.00, 20, 4, 4),
    ('Biography', 'Biography of a famous personality', 18.00, 5, 4, 4),
    ('Fiction', 'Fantasy fiction novel', 10.00, 25, 4, 4),
    ('Cookbook', 'Healthy recipes cookbook', 15.00, 12, 4, 4),
    ('History Book', 'History of ancient civilizations', 30.00, 10, 4, 4),
    ('Poetry', 'Collection of modern poems', 8.00, 18, 4, 4),
    ('Guide', 'Travel guide to Europe', 20.00, 30, 4, 4),
    ('Mystery Novel', 'Thriller mystery novel', 22.00, 8, 4, 4),
    
    -- Toys
    ('Toy Car', 'Remote-controlled toy car', 25.00, 8, 5, 5),
    ('Doll', 'Soft stuffed doll', 15.00, 12, 5, 5),
    ('Puzzle', '1000-piece jigsaw puzzle', 10.00, 30, 5, 5),
    ('Building Blocks', 'Building blocks set', 20.00, 20, 5, 5),
    ('Action Figure', 'Superhero action figure', 25.00, 10, 5, 5),
    ('Board Game', 'Family board game', 30.00, 15, 5, 5),
    ('Robot Toy', 'Interactive robot toy', 50.00, 5, 5, 5),
    ('Train Set', 'Electric toy train set', 60.00, 8, 5, 5),
    ('Bicycle', 'Kids bicycle with training wheels', 80.00, 5, 5, 5),
    ('Toy Gun', 'Water toy gun', 15.00, 25, 5, 5),
    
    -- Groceries
    ('Milk', 'Organic whole milk', 3.50, 50, 6, 6),
    ('Bread', 'Whole wheat bread', 2.00, 100, 6, 6),
    ('Eggs', 'Dozen organic eggs', 4.00, 50, 6, 6),
    ('Butter', 'Unsalted butter', 3.00, 75, 6, 6),
    ('Cheese', 'Cheddar cheese', 5.00, 60, 6, 6),
    ('Juice', 'Fresh orange juice', 3.50, 100, 6, 6),
    ('Rice', 'Basmati rice', 10.00, 200, 6, 6),
    ('Pasta', 'Whole wheat pasta', 3.00, 80, 6, 6),
    ('Cereal', 'Organic cereal', 5.00, 150, 6, 6),
    ('Sugar', 'Refined sugar', 2.50, 100, 6, 6),
    
    -- Beauty
    ('Shampoo', 'Herbal shampoo', 8.99, 10, 7, 7),
    ('Conditioner', 'Moisturizing conditioner', 8.00, 15, 7, 7),
    ('Lipstick', 'Matte lipstick', 15.00, 20, 7, 7),
    ('Face Cream', 'Anti-aging face cream', 25.00, 10, 7, 7),
    ('Perfume', 'Floral fragrance perfume', 50.00, 8, 7, 7),
    ('Nail Polish', 'Set of 3 nail polishes', 10.00, 25, 7, 7),
    ('Mascara', 'Lengthening mascara', 12.00, 18, 7, 7),
    ('Face Mask', 'Hydrating face mask', 7.00, 30, 7, 7),
    ('Hair Oil', 'Nourishing hair oil', 10.00, 40, 7, 7),
    ('Body Lotion', 'Moisturizing body lotion', 12.00, 35, 7, 7),

    -- Automotive
    ('Car Wax', 'High gloss car wax', 15.00, 5, 8, 8),
    ('Tire Cleaner', 'Tire cleaning solution', 8.00, 10, 8, 8),
    ('Car Battery', '12V Car battery', 100.00, 15, 8, 8),
    ('Car Air Freshener', 'Car air freshener spray', 5.00, 20, 8, 8),
    ('Oil Filter', 'Automotive oil filter', 12.00, 25, 8, 8),
    ('Windshield Wiper', 'Replacement windshield wipers', 20.00, 30, 8, 8),
    ('Seat Cover', 'Leather seat cover', 40.00, 10, 8, 8),
    ('Car Polish', 'High-quality car polish', 30.00, 8, 8, 8),
    ('Car Jack', 'Heavy-duty car jack', 60.00, 5, 8, 8),
    ('Rim Cleaner', 'Wheel rim cleaning solution', 10.00, 40, 8, 8);

-- Populate warehouse table
INSERT INTO warehouse (name, location, capacity) VALUES
    ('Main Warehouse', 'New York', 500),
    ('Secondary Warehouse', 'Los Angeles', 300),
    ('Central Warehouse', 'Chicago', 400),
    ('West Warehouse', 'San Francisco', 200),
    ('East Warehouse', 'Boston', 150),
    ('South Warehouse', 'Miami', 250),
    ('North Warehouse', 'Seattle', 180),
    ('Midwest Warehouse', 'Denver', 320),
    ('Northeast Warehouse', 'Philadelphia', 220),
    ('Southwest Warehouse', 'Phoenix', 275);

-- Populate inventory table to match each product in the product table
INSERT INTO inventory (product_id, warehouse_id, quantity, aisle_number, shelf_number) VALUES
    -- Electronics
    (1, 1, 30, 'A1', 'S1'),
    (2, 1, 20, 'A1', 'S2'),
    (3, 2, 45, 'A2', 'S1'),
    (4, 2, 25, 'A2', 'S2'),
    (5, 3, 35, 'A3', 'S1'),
    (6, 3, 80, 'A3', 'S2'),
    (7, 4, 15, 'A4', 'S1'),
    (8, 4, 20, 'A4', 'S2'),
    (9, 5, 10, 'A5', 'S1'),
    (10, 5, 25, 'A5', 'S2'),

    -- Furniture
    (11, 1, 15, 'B1', 'S1'),
    (12, 1, 10, 'B1', 'S2'),
    (13, 2, 20, 'B2', 'S1'),
    (14, 2, 12, 'B2', 'S2'),
    (15, 3, 10, 'B3', 'S1'),
    (16, 3, 30, 'B3', 'S2'),
    (17, 4, 18, 'B4', 'S1'),
    (18, 4, 40, 'B4', 'S2'),
    (19, 5, 30, 'B5', 'S1'),
    (20, 5, 35, 'B5', 'S2'),

    -- Clothing
    (21, 1, 100, 'C1', 'S1'),
    (22, 1, 60, 'C1', 'S2'),
    (23, 2, 150, 'C2', 'S1'),
    (24, 2, 80, 'C2', 'S2'),
    (25, 3, 120, 'C3', 'S1'),
    (26, 3, 140, 'C3', 'S2'),
    (27, 4, 50, 'C4', 'S1'),
    (28, 4, 90, 'C4', 'S2'),
    (29, 5, 130, 'C5', 'S1'),
    (30, 5, 75, 'C5', 'S2'),

    -- Books
    (31, 1, 200, 'D1', 'S1'),
    (32, 1, 120, 'D1', 'S2'),
    (33, 2, 100, 'D2', 'S1'),
    (34, 2, 95, 'D2', 'S2'),
    (35, 3, 80, 'D3', 'S1'),
    (36, 3, 60, 'D3', 'S2'),
    (37, 4, 140, 'D4', 'S1'),
    (38, 4, 110, 'D4', 'S2'),
    (39, 5, 60, 'D5', 'S1'),
    (40, 5, 100, 'D5', 'S2'),

    -- Toys
    (41, 1, 50, 'E1', 'S1'),
    (42, 1, 30, 'E1', 'S2'),
    (43, 2, 70, 'E2', 'S1'),
    (44, 2, 40, 'E2', 'S2'),
    (45, 3, 55, 'E3', 'S1'),
    (46, 3, 20, 'E3', 'S2'),
    (47, 4, 35, 'E4', 'S1'),
    (48, 4, 45, 'E4', 'S2'),
    (49, 5, 65, 'E5', 'S1'),
    (50, 5, 25, 'E5', 'S2'),

    -- Groceries
    (51, 1, 300, 'F1', 'S1'),
    (52, 1, 250, 'F1', 'S2'),
    (53, 2, 500, 'F2', 'S1'),
    (54, 2, 350, 'F2', 'S2'),
    (55, 3, 400, 'F3', 'S1'),
    (56, 3, 280, 'F3', 'S2'),
    (57, 4, 450, 'F4', 'S1'),
    (58, 4, 200, 'F4', 'S2'),
    (59, 5, 300, 'F5', 'S1'),
    (60, 5, 320, 'F5', 'S2'),

    -- Beauty
    (61, 1, 50, 'G1', 'S1'),
    (62, 1, 40, 'G1', 'S2'),
    (63, 2, 60, 'G2', 'S1'),
    (64, 2, 30, 'G2', 'S2'),
    (65, 3, 90, 'G3', 'S1'),
    (66, 3, 50, 'G3', 'S2'),
    (67, 4, 20, 'G4', 'S1'),
    (68, 4, 55, 'G4', 'S2'),
    (69, 5, 40, 'G5', 'S1'),
    (70, 5, 35, 'G5', 'S2'),

    -- Automotive
    (71, 1, 25, 'H1', 'S1'),
    (72, 1, 40, 'H1', 'S2'),
    (73, 2, 15, 'H2', 'S1'),
    (74, 2, 10, 'H2', 'S2'),
    (75, 3, 12, 'H3', 'S1'),
    (76, 3, 20, 'H3', 'S2'),
    (77, 4, 30, 'H4', 'S1'),
    (78, 4, 18, 'H4', 'S2'),
    (79, 5, 50, 'H5', 'S1'),
    (80, 5, 45, 'H5', 'S2');

-- Populate customer table
INSERT INTO customer (name, email, phone, address, password) VALUES
    ('John Doe', 'johndoe@example.com', '555-1234', '123 Main St', 'johndoe'),
    ('Jane Smith', 'janesmith@example.com', '555-5678', '456 Oak St', 'janesmith'),
    ('Michael Brown', 'michaelb@example.com', '555-8765', '789 Pine St', 'michaelb'),
    ('Sarah Johnson', 'sarahj@example.com', '555-3456', '321 Maple St', 'sarahj'),
    ('Chris Lee', 'chrisl@example.com', '555-9876', '654 Cedar St', 'chrisl'),
    ('Linda White', 'lindaw@example.com', '555-4321', '321 Elm St', 'lindaw'),
    ('James Black', 'jamesb@example.com', '555-1122', '213 Ash St', 'jamesb'),
    ('Emily Green', 'emilyg@example.com', '555-3344', '789 Willow St', 'emilyg'),
    ('David Young', 'davidy@example.com', '555-5566', '432 Poplar St', 'davidy'),
    ('Olivia Hill', 'oliviah@example.com', '555-7788', '987 Fir St', 'oliviah');

-- Populate employee table
INSERT INTO employee (name, position, phone, email, password) VALUES
    ('Alice Johnson', 'Sales Representative', '555-9876', 'alice@example.com', 'alicej'),
    ('Bob Williams', 'Warehouse Staff', '555-4321', 'bob@example.com', 'bobwill'),
    ('Carol Anderson', 'System Administrator', '555-6789', 'carol@example.com', 'carola'),
    ('Daniel Evans', 'Sales Representative', '555-1111', 'daniel@example.com', 'danieleva'),
    ('Eve Foster', 'Purchasing Manager', '555-2222', 'eve@example.com', 'evefors'),
    ('Frank Harris', 'Business Analyst', '555-3333', 'frank@example.com', 'frankhare'),
    ('Grace Lewis', 'Warehouse Staff', '555-4444', 'grace@example.com', 'graceyl'),
    ('Henry Martin', 'Business Analyst', '555-5555', 'henry@example.com', 'henrymartini'),
    ('Irene Nelson', 'Warehouse Staff', '555-6666', 'irene@example.com', 'irenel'),
    ('Jack O\'Connor', 'System Administrator', '555-7777', 'jack@example.com', 'JackOLantern');

-- Populate shipment table
INSERT INTO shipment (shipment_date, status, tracking_number) VALUES
    (CURRENT_TIMESTAMP, 'Shipped', 'TRK123456'),
    (CURRENT_TIMESTAMP, 'Delivered', 'TRK789012'),
    (CURRENT_TIMESTAMP, 'Pending', 'TRK345678'),
    (CURRENT_TIMESTAMP, 'Processed', 'TRK901234'),
    (CURRENT_TIMESTAMP, 'Cancelled', 'TRK567890'),
    (CURRENT_TIMESTAMP, 'Delivered', 'TRK678901'),
    (CURRENT_TIMESTAMP, 'Shipped', 'TRK234567'),
    (CURRENT_TIMESTAMP, 'Processed', 'TRK890123'),
    (CURRENT_TIMESTAMP, 'Pending', 'TRK345678'),
    (CURRENT_TIMESTAMP, 'Delivered', 'TRK123789');

-- Populate report table
INSERT INTO report (report_type, generated_by) VALUES
    ('Inventory Report', 'admin1'),
    ('Sales Report', 'admin2'),
    ('Order Summary', 'admin3'),
    ('Customer Feedback', 'admin4'),
    ('Supply Chain Analysis', 'admin5'),
    ('Year-End Review', 'admin6'),
    ('Quarterly Report', 'admin7'),
    ('Monthly Summary', 'admin8'),
    ('Product Performance', 'admin9'),
    ('Employee Performance', 'admin10');
