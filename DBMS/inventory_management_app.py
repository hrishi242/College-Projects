import streamlit as st
import mysql.connector
from mysql.connector import Error
from datetime import datetime, timedelta
import pandas as pd
import time
import plotly

# MySQL database connection function
def create_connection():
    try:
        conn = mysql.connector.connect(
            host="localhost",
            user="root",
            password="Learn123$",
            database="inventorymanagement"
        )
        conn.autocommit = True
        if conn.is_connected():
            return conn
    except Error as e:
        st.error(f"Error: {e}")
    return None

# Login/Signup Page
def login_page():
    st.title("Inventory Management System")

    # User choice: Customer or Employee
    role = st.selectbox("Login as:", ["Select", "Customer", "Employee"])

    if role == "Customer":
        action = st.radio("Choose an option:", ["Login", "Signup"])

        if action == "Login":
            email = st.text_input("Email")
            password = st.text_input("Password", type="password")
            if st.button("Login"):
                if customer_login(email, password):
                    customer_dashboard()
                else:
                    st.error("Invalid login credentials.")
        
        elif action == "Signup":
            # Get customer details
            name = st.text_input("Name")
            email = st.text_input("Email")
            phone = st.text_input("Phone Number")
            address = st.text_input("Address")
            password = st.text_input("Password", type="password")
            
            if st.button("Signup"):
                if customer_signup(name, email, phone, address, password):
                    st.success("Signup successful! You can log in now.")
                else:
                    st.error("Signup failed. Try again.")
    
    elif role == "Employee":
        email = st.text_input("Email")
        password = st.text_input("Password", type="password")
        
        if st.button("Login"):
            employee_role = employee_login(email, password)
            if employee_role:
                employee_dashboard(employee_role)
            else:
                st.error("Invalid login credentials.")

# Customer-related functions
def customer_signup(name, email, phone, address, password):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        try:
            cursor.execute("INSERT INTO customer (name, email, phone, address, password) VALUES (%s, %s, %s, %s, %s)",
                           (name, email, phone, address, password))
            conn.commit()
            print("Customer data committed to database.")
            return True
        except Error as e:
            st.error(f"Error: {e}")
        finally:
            conn.close()
    return False

def customer_login(email, password):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id, name FROM customer WHERE email=%s AND password=%s", (email, password))
        user = cursor.fetchone()
        conn.close()
        if user:
            # Store customer info in session state
            st.session_state['customer_id'] = user[0]
            st.session_state['customer_name'] = user[1]
            return True
    return False

def get_available_stock(product_id):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT SUM(quantity) 
            FROM inventory 
            WHERE product_id = %s
        """, (product_id,))
        result = cursor.fetchone()[0]
        cursor.close()
        conn.close()
        return result or 0
    return 0

def get_inventory_id(product_id):
    """Get the inventory_id with the highest quantity for a product"""
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT inventory_id 
            FROM inventory 
            WHERE product_id = %s 
            ORDER BY quantity DESC 
            LIMIT 1
        """, (product_id,))
        result = cursor.fetchone()
        cursor.close()
        conn.close()
        return result[0] if result else None
    return None

def customer_dashboard():
    # Ensure that the customer dashboard only loads if the customer is logged in
    if 'customer_id' not in st.session_state:
        st.error("Please log in first.")
        return

    st.title("Customer Dashboard")

    # Get customer ID from session state
    customer_id = st.session_state['customer_id']

    # Add logout button in the top right
    if st.button("Logout", key="logout"):
        for key in list(st.session_state.keys()):
            del st.session_state[key]
        st.experimental_set_query_params()  # Refreshes the app by clearing query params
    
    # Shopping cart in session state
    if 'cart' not in st.session_state:
        st.session_state.cart = []
    
    # Display products by category
    category = st.selectbox("Choose a category", get_categories())
    
    if category:
        products = get_products_by_category(category)
        
        # Display products and add to cart
        for product in products:
            col1, col2, col3 = st.columns([3, 2, 1])
            
            with col1:
                st.write(f"Product: {product['name']}")
                st.write(f"Price: ${product['unit_price']:.2f}")
                
            with col2:
                available_stock = get_available_stock(product['id'])
                st.write(f"Available: {available_stock}")
                
            with col3:
                quantity = st.number_input(
                    f"Quantity",
                    min_value=0,
                    max_value=int(available_stock),
                    key=f"qty_{product['id']}"
                )
                
                if quantity > 0:
                    if st.button(f"Add to Cart", key=f"add_{product['id']}"):
                        st.session_state.cart.append({
                            'product_id': product['id'],
                            'inventory_id': get_inventory_id(product['id']),
                            'quantity': quantity,
                            'name': product['name'],
                            'price': product['unit_price']
                        })
                        st.success(f"Added {quantity} {product['name']} to cart")
    
    # Display cart
    if st.session_state.cart:
        st.subheader("Shopping Cart")
        total = 0
        
        for item in st.session_state.cart:
            st.write(f"{item['name']} - Quantity: {item['quantity']} - ${item['price'] * item['quantity']:.2f}")
            total += item['price'] * item['quantity']
        
        st.write(f"Total: ${total:.2f}")
        
        if st.button("Place Order"):
            order_id = place_order(customer_id, st.session_state.cart)
            if order_id:
                st.success(f"Order placed successfully! Order ID: {order_id}")
                st.session_state.cart = []  # Clear cart
            else:
                st.error("Failed to place order")

def get_categories():
    conn = create_connection()
    cursor = conn.cursor()
    # Query to get distinct category names from the category table via a join with the product table
    cursor.execute("""
        SELECT DISTINCT c.name 
        FROM product p
        JOIN category c ON p.category_id = c.id
    """)
    categories = [row[0] for row in cursor.fetchall()]
    cursor.close()
    return categories

def get_products_by_category(category_name):
    conn = create_connection()
    cursor = conn.cursor(dictionary=True)
    # Joining product with category to filter by category name
    cursor.execute("""
        SELECT p.* 
        FROM product p
        JOIN category c ON p.category_id = c.id
        WHERE c.name = %s
    """, (category_name,))
    products = cursor.fetchall()
    cursor.close()
    return products

def place_order(customer_id, items):
    conn = create_connection()
    if conn:
        try:
            cursor = conn.cursor()
            # First validate all quantities
            for item in items:
                cursor.execute("""
                    SELECT quantity 
                    FROM inventory 
                    WHERE inventory_id = %s
                    FOR UPDATE
                """, (item['inventory_id'],))
                available_quantity = cursor.fetchone()[0]
                
                if available_quantity < item['quantity']:
                    st.error(f"Not enough stock for {item['name']}. Available: {available_quantity}")
                    return None
            
            # Start transaction
            conn.start_transaction()

            # Calculate total amount
            total_amount = sum(item['price'] * item['quantity'] for item in items)

            # Create customer_order
            cursor.execute("""
                INSERT INTO customer_order 
                (customer_id, total_amount, status_id, order_date) 
                VALUES (%s, %s, %s, CURRENT_TIMESTAMP)
            """, (customer_id, total_amount, 1))
            
            order_id = cursor.lastrowid

            # Create order_items and update inventory
            for item in items:
                # Insert order item
                cursor.execute("""
                    INSERT INTO order_item 
                    (order_id, product_id, inventory_id, quantity, unit_price) 
                    VALUES (%s, %s, %s, %s, %s)
                """, (order_id, item['product_id'], item['inventory_id'], 
                      item['quantity'], item['price']))
                
                # Update inventory quantity - do this only once per item
                cursor.execute("""
                    UPDATE inventory 
                    SET quantity = quantity - %s 
                    WHERE inventory_id = %s
                """, (item['quantity'], item['inventory_id']))

                # Check if restock is needed - use a SELECT instead of direct procedure call
                cursor.execute("""
                    SELECT quantity 
                    FROM inventory 
                    WHERE inventory_id = %s
                """, (item['inventory_id'],))
                new_quantity = cursor.fetchone()[0]
                
                if new_quantity < 25:
                    # Insert a restock request instead of direct procedure call
                    cursor.execute("""
                        INSERT INTO restock_request 
                        (inventory_id, requested_quantity, request_date, status)
                        SELECT %s, 100, CURRENT_TIMESTAMP, 'pending'
                        WHERE NOT EXISTS (
                            SELECT 1 
                            FROM restock_request 
                            WHERE inventory_id = %s 
                            AND status = 'pending'
                        )
                    """, (item['inventory_id'], item['inventory_id']))

            conn.commit()
            return order_id

        except Error as e:
            conn.rollback()
            st.error(f"Error placing order: {e}")
            return None
        finally:
            cursor.close()
            conn.close()
    return None

# Employee-related functions
def employee_login(email, password):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, name, position 
            FROM employee 
            WHERE email=%s AND password=%s
        """, (email, password))
        result = cursor.fetchone()
        conn.close()
        if result:
            # Store all employee information in session state
            st.session_state.employee_id = result[0]
            st.session_state.employee_name = result[1]
            st.session_state.employee_position = result[2]
            return result[2]  # Return position
    return None

def get_sales_by_category(start_date, end_date):
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT 
                c.name as category_name,
                COUNT(DISTINCT co.id) as total_orders,
                SUM(oi.quantity) as total_items_sold,
                SUM(oi.quantity * oi.unit_price) as total_revenue,
                AVG(oi.unit_price) as average_price
            FROM customer_order co
            JOIN order_item oi ON co.id = oi.order_id
            JOIN product p ON oi.product_id = p.id
            JOIN category c ON p.category_id = c.id
            WHERE co.order_date BETWEEN %s AND %s
                AND co.status_id >= 1  -- Include all valid orders
            GROUP BY c.name
            ORDER BY total_revenue DESC
        """, (start_date, end_date))
        results = cursor.fetchall()
        cursor.close()
        conn.close()
        return results
    return []


def get_top_products_by_period(start_date, end_date, category=None):
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        query = """
            WITH product_data AS (
                SELECT 
                    p.id as product_id,
                    p.name as product_name,
                    c.name as category_name,
                    SUM(oi.quantity) as total_sold,
                    SUM(oi.quantity * oi.unit_price) as total_revenue,
                    AVG(oi.unit_price) as average_price
                FROM customer_order co
                JOIN order_item oi ON co.id = oi.order_id
                JOIN product p ON oi.product_id = p.id
                JOIN category c ON p.category_id = c.id
                WHERE co.order_date BETWEEN %s AND %s
                """
        
        params = [start_date, end_date]
        
        if category:
            query += " AND c.name = %s"
            params.append(category)
            
        query += """
                GROUP BY p.id, p.name, c.name
            )
            SELECT 
                pd.*,
                (
                    SELECT AVG(oi2.unit_price)
                    FROM order_item oi2
                    JOIN customer_order co2 ON oi2.order_id = co2.id
                    WHERE oi2.product_id = pd.product_id
                    AND co2.order_date BETWEEN %s AND %s
                ) as average_historical_price
            FROM product_data pd
            ORDER BY total_revenue DESC 
            LIMIT 10
        """
        
        # Add the date parameters again for the nested query
        params.extend([start_date, end_date])
        
        cursor.execute(query, tuple(params))
        results = cursor.fetchall()
        cursor.close()
        conn.close()
        return results
    return []

def get_top_selling_products_overall(category=None):
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        query = """
            WITH product_data AS (
                SELECT 
                    p.id as product_id,
                    p.name as product_name,
                    c.name as category_name,
                    SUM(oi.quantity) as total_sold,
                    SUM(oi.quantity * oi.unit_price) as total_revenue,
                    AVG(oi.unit_price) as average_price
                FROM customer_order co
                JOIN order_item oi ON co.id = oi.order_id
                JOIN product p ON oi.product_id = p.id
                JOIN category c ON p.category_id = c.id
                """
        
        params = []
        
        if category:
            query += " WHERE c.name = %s"
            params.append(category)
        
        query += """
                GROUP BY p.id, p.name, c.name
            )
            SELECT 
                pd.*,
                (
                    SELECT AVG(oi2.unit_price)
                    FROM order_item oi2
                    JOIN customer_order co2 ON oi2.order_id = co2.id
                    WHERE oi2.product_id = pd.product_id
                ) as average_historical_price
            FROM product_data pd
            ORDER BY total_revenue DESC 
            LIMIT 10
        """
        
        cursor.execute(query, tuple(params))
        results = cursor.fetchall()
        cursor.close()
        conn.close()
        return results
    return []


def get_inventory_status():
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT 
                p.id as product_id,
                p.name as product_name,
                i.quantity as current_stock,
                p.reorder_level,
                s.name as supplier_name,
                s.id as supplier_id,
                w.name as warehouse_name,
                i.aisle_number,
                i.shelf_number
            FROM inventory i
            JOIN product p ON i.product_id = p.id
            JOIN supplier s ON p.supplier_id = s.id
            JOIN warehouse w ON i.warehouse_id = w.id
        """)
        results = cursor.fetchall()
        cursor.close()
        conn.close()
        return results
    return []

def update_supplier(product_id, new_supplier_id):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        try:
            cursor.execute("""
                UPDATE product 
                SET supplier_id = %s 
                WHERE id = %s
            """, (new_supplier_id, product_id))
            conn.commit()
            return True
        except Error as e:
            st.error(f"Error updating supplier: {e}")
            return False
        finally:
            cursor.close()
            conn.close()
    return False

def get_all_suppliers():
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT id, name, contact_no, email FROM supplier")
        suppliers = cursor.fetchall()
        cursor.close()
        conn.close()
        return suppliers
    return []

def process_order(order_id, employee_id):
    conn = create_connection()
    if conn:
        try:
            cursor = conn.cursor()
            
            # Start transaction
            conn.start_transaction()
            
            # Update order status
            cursor.execute("""
                UPDATE customer_order 
                SET status_id = 2,  -- Set to 'Processed'
                    processed_by = %s,
                    processed_date = CURRENT_TIMESTAMP
                WHERE id = %s AND status_id = 1
            """, (employee_id, order_id))
            
            if cursor.rowcount == 0:
                conn.rollback()
                return False, "Order not found or already processed"
            
            # Commit the transaction
            conn.commit()
            return True, "Order processed successfully"
            
        except Error as e:
            conn.rollback()
            return False, str(e)
        finally:
            cursor.close()
            conn.close()
    return False, "Database connection error"

def get_customer_orders():
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT 
                co.id as order_id,
                c.name as customer_name,
                co.order_date,
                co.total_amount,
                os.status as order_status,
                GROUP_CONCAT(CONCAT(p.name, ' (', oi.quantity, ')')) as products
            FROM customer_order co
            JOIN customer c ON co.customer_id = c.id
            JOIN order_status os ON co.status_id = os.id
            JOIN order_item oi ON co.id = oi.order_id
            JOIN product p ON oi.product_id = p.id
            WHERE co.status_id = 1  -- Only show pending orders
            GROUP BY co.id
            ORDER BY co.order_date DESC
        """)
        orders = cursor.fetchall()
        cursor.close()
        conn.close()
        return orders
    return []

def get_pending_restocks():
    conn = create_connection()
    if conn:
        cursor = conn.cursor(dictionary=True)
        cursor.execute("""
            SELECT 
                r.request_id,
                r.inventory_id,
                p.name as product_name,
                i.quantity as current_quantity,
                r.requested_quantity,
                r.request_date,
                w.name as warehouse_name,
                i.aisle_number,
                i.shelf_number
            FROM restock_request r
            JOIN inventory i ON r.inventory_id = i.inventory_id
            JOIN product p ON i.product_id = p.id
            JOIN warehouse w ON i.warehouse_id = w.id
            WHERE r.status = 'pending'
            ORDER BY r.request_date
        """)
        restocks = cursor.fetchall()
        cursor.close()
        conn.close()
        return restocks
    return []

def confirm_restock(request_id, employee_id):
    conn = create_connection()
    if conn:
        try:
            cursor = conn.cursor()
            
            # Start transaction
            conn.start_transaction()
            
            # Get inventory_id and requested quantity
            cursor.execute("""
                SELECT inventory_id, requested_quantity 
                FROM restock_request 
                WHERE request_id = %s AND status = 'pending'
                FOR UPDATE
            """, (request_id,))
            
            result = cursor.fetchone()
            if not result:
                conn.rollback()
                return False, "Restock request not found or already processed"
                
            inventory_id, requested_quantity = result
            
            # Update inventory
            cursor.execute("""
                UPDATE inventory 
                SET quantity = quantity + %s 
                WHERE inventory_id = %s
            """, (requested_quantity, inventory_id))
            
            # Update restock request status
            cursor.execute("""
                UPDATE restock_request 
                SET status = 'confirmed',
                    confirmed_by = %s,
                    confirmation_date = CURRENT_TIMESTAMP
                WHERE request_id = %s
            """, (employee_id, request_id))
            
            # Commit the transaction
            conn.commit()
            return True, "Restock confirmed successfully"
            
        except Error as e:
            conn.rollback()
            return False, str(e)
        finally:
            cursor.close()
            conn.close()
    return False, "Database connection error"

def employee_dashboard(position):
    st.title(f"{position} Dashboard")

        # Add logout button in the top right
    if st.button("Logout", key="logout"):
        for key in list(st.session_state.keys()):
            del st.session_state[key]
        st.experimental_set_query_params()  # Refreshes the app by clearing query params
    
    # Check if employee is logged in
    if 'employee_id' not in st.session_state:
        st.error("You are logged out")
        return

    if position == "Warehouse Staff":
        # Create tabs for different functions
        tab1, tab2 = st.tabs(["Order Processing", "Restock Management"])
        
        with tab1:
            st.header("Order Processing")
            orders = get_customer_orders()
            
            if not orders:
                st.info("No pending orders.")
            else:
                for order in orders:
                    with st.expander(f"Order #{order['order_id']} - {order['customer_name']}"):
                        st.write(f"Order Date: {order['order_date']}")
                        st.write(f"Status: {order['order_status']}")
                        st.write(f"Products: {order['products']}")
                        st.write(f"Total Amount: ${order['total_amount']:.2f}")
                        
                        # Generate unique key for each button
                        button_key = f"process_order_{order['order_id']}"
                        if st.button(f"Process Order #{order['order_id']}", key=button_key):
                            success, message = process_order(order['order_id'], st.session_state.employee_id)
                            if success:
                                st.success(f"Order #{order['order_id']} has been processed successfully")
                                time.sleep(1)  # Give time for the success message to be seen
                                st.rerun()  # Refresh the page to update the orders list
                            else:
                                st.error(f"Failed to process order: {message}")
            
        with tab2:
            st.header("Pending Restock Requests")
            pending_restocks = get_pending_restocks()
            
            if not pending_restocks:
                st.info("No pending restock requests.")
            else:
                for restock in pending_restocks:
                    with st.expander(f"Restock Request - {restock['product_name']}"):
                        col1, col2 = st.columns(2)
                        
                        with col1:
                            st.write(f"Warehouse: {restock['warehouse_name']}")
                            st.write(f"Location: Aisle {restock['aisle_number']}, Shelf {restock['shelf_number']}")
                            
                        with col2:
                            st.write(f"Current Quantity: {restock['current_quantity']}")
                            st.write(f"Requested Quantity: {restock['requested_quantity']}")
                            st.write(f"Request Date: {restock['request_date']}")
                        
                        # Generate unique key for each confirm button
                        confirm_key = f"confirm_restock_{restock['request_id']}"
                        if st.button("Confirm Stock Received", key=confirm_key):
                            success, message = confirm_restock(restock['request_id'], st.session_state.employee_id)
                            if success:
                                st.success(f"Restock confirmed for {restock['product_name']}")
                                time.sleep(1)  # Give time for the success message to be seen
                                st.rerun()  # Refresh the page to update the list
                            else:
                                st.error(f"Failed to confirm restock: {message}")

    elif position == "Sales Representative":
        st.header("Sales Analytics")
        
        # Get categories for filtering
        categories = get_categories()
        selected_category = st.selectbox("Select Category", ["All"] + categories)
        
        # Display top selling products
        st.subheader("Top Selling Products")
        if selected_category == "All":
            top_products = get_top_selling_products_overall()
        else:
            #category_id = get_category_id(selected_category)
            top_products = get_top_selling_products_overall(selected_category)
            
        if top_products:
            df = pd.DataFrame(top_products)
            st.dataframe(df)
            
            # Visualize data
            st.bar_chart(df.set_index('product_name')['total_sold'])

    elif position == "Purchasing Manager":
        st.header("Inventory Management")
        
        # Display current inventory status
        inventory = get_inventory_status()
        suppliers = get_all_suppliers()
        
        for item in inventory:
            with st.expander(f"{item['product_name']} - Current Stock: {item['current_stock']}"):
                st.write(f"Warehouse: {item['warehouse_name']}")
                st.write(f"Location: Aisle {item['aisle_number']}, Shelf {item['shelf_number']}")
                st.write(f"Current Supplier: {item['supplier_name']}")
                st.write(f"Reorder Level: {item['reorder_level']}")
                
                # Supplier selection and update
                new_supplier = st.selectbox(
                    f"Change supplier for {item['product_name']}",
                    options=[s['name'] for s in suppliers],
                    index=[i for i, s in enumerate(suppliers) if s['name'] == item['supplier_name']][0]
                )
                
                if st.button(f"Update Supplier for {item['product_name']}"):
                    new_supplier_id = next(s['id'] for s in suppliers if s['name'] == new_supplier)
                    if update_supplier(item['product_id'], new_supplier_id):
                        st.success(f"Successfully updated supplier for {item['product_name']}")

    elif position == "Business Analyst":
        st.header("Sales Analysis")
    
        # Date range selection
        col1, col2 = st.columns(2)
        with col1:
            start_date = st.date_input("Start Date", datetime.now() - timedelta(days=30))
        with col2:
            end_date = st.date_input("End Date", datetime.now())
    
        if start_date <= end_date:
            # Get sales data by category
            sales_data = get_sales_by_category(start_date, end_date)
        
            if sales_data:
                # Convert to DataFrame for easier manipulation
                df = pd.DataFrame(sales_data)
            
                # Overall metrics
                st.subheader("Overall Performance")
                total_revenue = df['total_revenue'].sum()
                total_orders = df['total_orders'].sum()
                total_items = df['total_items_sold'].sum()
            
                col1, col2, col3 = st.columns(3)
                with col1:
                    st.metric("Total Revenue", f"${total_revenue:,.2f}")
                with col2:
                    st.metric("Total Orders", f"{total_orders:,}")
                with col3:
                    st.metric("Items Sold", f"{total_items:,}")
            
                # Category breakdown
                st.subheader("Sales by Category")
            
                # Create a bar chart for category revenue
                fig_revenue = {
                    'data': [{
                        'x': df['category_name'],
                        'y': df['total_revenue'],
                        'type': 'bar',
                        'name': 'Revenue'
                    }],
                    'layout': {
                        'title': 'Revenue by Category',
                        'yaxis': {'title': 'Revenue ($)'}
                    }
                }
                st.plotly_chart(fig_revenue)
            
                # Detailed category metrics
                for idx, row in df.iterrows():
                    with st.expander(f"{row['category_name']} Details"):
                        col1, col2, col3 = st.columns(3)
                        with col1:
                            st.metric("Revenue", f"${row['total_revenue']:,.2f}")
                        with col2:
                            st.metric("Orders", f"{row['total_orders']:,}")
                        with col3:
                            st.metric("Items Sold", f"{row['total_items_sold']:,}")
                    
                        # Show top products in this category
                        st.write("Top Products in Category")
                        top_products = get_top_products_by_period(start_date, end_date, row['category_name'])
                        if top_products:
                            products_df = pd.DataFrame(top_products)
                            st.dataframe(
                                products_df[['product_name', 'total_sold', 'total_revenue', 'average_price']],
                                column_config={
                                    'total_revenue': st.column_config.NumberColumn(
                                        format="$%.2f"
                                    ),
                                    'average_price': st.column_config.NumberColumn(
                                        format="$%.2f"
                                    )
                                }
                            )
            
                # Overall top products
                st.subheader("Top Products Overall")
                top_products_overall = get_top_products_by_period(start_date, end_date)
                if top_products_overall:
                    top_df = pd.DataFrame(top_products_overall)
                    st.dataframe(
                        top_df,
                        column_config={
                            'total_revenue': st.column_config.NumberColumn(
                                format="$%.2f"
                            ),
                            'average_price': st.column_config.NumberColumn(
                                format="$%.2f"
                            )
                        }
                    )
        else:
            st.error("End date must be after start date")

    elif position == "System Administrator":
        st.header("System Administration")
        
        # Create tabs for different management sections
        tab1, tab2, tab3, tab4 = st.tabs(["Users", "Inventory", "Orders", "System Logs"])
        
        with tab1:
            st.subheader("User Management")
            # Display customers and employees
            customers = pd.read_sql("SELECT id, name, email, phone FROM customer", create_connection())
            employees = pd.read_sql("SELECT id, name, position, email FROM employee", create_connection())
            
            st.write("Customers")
            st.dataframe(customers)
            
            st.write("Employees")
            st.dataframe(employees)
        
        with tab2:
            st.subheader("Inventory Management")
            inventory_df = pd.DataFrame(get_inventory_status())
            st.dataframe(inventory_df)
        
        with tab3:
            st.subheader("Order Management")
            orders_df = pd.DataFrame(get_customer_orders())
            st.dataframe(orders_df)
        
        with tab4:
            st.subheader("System Logs")
            logs = pd.read_sql("""
                SELECT * FROM audit_log 
                ORDER BY changed_at DESC 
                LIMIT 100
            """, create_connection())
            st.dataframe(logs)

def get_category_id(category_name):
    conn = create_connection()
    if conn:
        cursor = conn.cursor()
        cursor.execute("SELECT id FROM category WHERE name = %s", (category_name,))
        result = cursor.fetchone()
        cursor.close()
        conn.close()
        return result[0] if result else None
    return None
    
# Main app
if __name__ == "__main__":
    if 'employee_id' in st.session_state and st.session_state.get('employee_position'):
        # If employee is logged in, show their dashboard
        employee_dashboard(st.session_state.employee_position)
    elif 'customer_id' in st.session_state:
        # If customer is logged in, show customer dashboard
        customer_dashboard()
    else:
        # Show login page
        login_page()
