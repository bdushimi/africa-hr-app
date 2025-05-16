-- Insert roles if they don't exist
INSERT INTO roles (name, description)
SELECT 'ROLE_ADMIN', 'Administrator role'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

INSERT INTO roles (name, description)
SELECT 'ROLE_MANAGER', 'Manager role'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MANAGER');

INSERT INTO roles (name, description)
SELECT 'ROLE_STAFF', 'Regular staff role'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_STAFF');


-- Insert departments if they don't exist
INSERT INTO departments (name, description)
SELECT 'Human Resources', 'Manages employee relations, recruitment, training, and HR policies'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Human Resources');

INSERT INTO departments (name, description)
SELECT 'Information Technology', 'Handles IT infrastructure, software development, and technical support'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Information Technology');

INSERT INTO departments (name, description)
SELECT 'Operations', 'Oversees day-to-day business operations and process improvement'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Operations');


-- Insert admin user if not exists
INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+hradmin@gmail.com', 
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Admin', 'HR', 
       (SELECT id FROM departments WHERE name = 'Human Resources'),
       (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'),
       '2025-01-01', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+hradmin@gmail.com');


-- Insert manager user if not exists
INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+opsmanager@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Manager', 'OPs',
       (SELECT id FROM departments WHERE name = 'Operations'),
       (SELECT id FROM roles WHERE name = 'ROLE_MANAGER'),
       '2025-01-15', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+opsmanager@gmail.com');

-- Default user
INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+itmanager@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Manager', 'IT',
       (SELECT id FROM departments WHERE name = 'Information Technology'),
       (SELECT id FROM roles WHERE name = 'ROLE_MANAGER'),
       '2025-02-01', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+itmanager@gmail.com');

INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+staffuser3@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Jane', 'Smith',
       (SELECT id FROM departments WHERE name = 'Information Technology'),
       (SELECT id FROM roles WHERE name = 'ROLE_STAFF'),
       '2025-03-01', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+staffuser3@gmail.com');


INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+staffuser4@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Mike', 'Johnson',
       (SELECT id FROM departments WHERE name = 'Operations'),
       (SELECT id FROM roles WHERE name = 'ROLE_STAFF'),
       '2025-01-15', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+staffuser4@gmail.com');


INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+staffuser5@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'Sarah', 'Williams',
       (SELECT id FROM departments WHERE name = 'Operations'),
       (SELECT id FROM roles WHERE name = 'ROLE_STAFF'),
       '2025-04-15', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+staffuser5@gmail.com');

INSERT INTO users (email, password, first_name, last_name, department_id, role_id, joined_date, status)
SELECT 'africahrapp+staffuser6@gmail.com',
       '$2a$10$uSHOq9pUUjOP4MVDlpClGeVmjAgDw.vHc9kFE215zsrBGi/EumIV6', -- Encoded 'Enter@123'
       'David', 'Brown',
       (SELECT id FROM departments WHERE name = 'Information Technology'),
       (SELECT id FROM roles WHERE name = 'ROLE_STAFF'),
       '2025-04-30', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'africahrapp+staffuser6@gmail.com');


-- Update manager relationships
UPDATE users u
JOIN (
    SELECT department_id, id AS manager_id
    FROM users
    WHERE role_id = (SELECT id FROM roles WHERE name = 'ROLE_MANAGER')
) dept_mgrs
ON u.department_id = dept_mgrs.department_id
SET u.manager_id = dept_mgrs.manager_id
WHERE u.id != dept_mgrs.manager_id;

UPDATE users u
JOIN users admin_hr ON admin_hr.first_name = 'Admin' AND admin_hr.last_name = 'HR'
SET u.manager_id = admin_hr.id
WHERE u.role_id = (SELECT id FROM roles WHERE name = 'ROLE_MANAGER');



-- Feature 1.1 & 2: Default Leave Types with Accrual and Carry-Forward Settings
INSERT INTO leave_types (
    name, 
    description, 
    is_default, 
    is_enabled,
    max_duration, 
    paid, 
    accrual_based,
    accrual_rate,
    is_carry_forward_enabled,
    carry_forward_cap,
    require_reason,
    require_document,
    created_at
)
VALUES 
    ('Annual Leave', 
     '1.5 working days/month (18 days/year) + seniority bonus', 
     TRUE,
     TRUE,  -- Enabled by default
     18, 
     TRUE, 
     TRUE,   -- Accrual-based
     1.50,   -- 1.5 days per month
     TRUE,   -- Carry-forward enabled
     5.00,   -- Can carry forward up to 5 days
     FALSE,  -- Require reason
     FALSE,  -- Require document
     CURRENT_TIMESTAMP),
    
    ('Maternity Leave', 
     '12 weeks (84 days) fully paid, additional in case of complications', 
     TRUE,
     TRUE,  -- Enabled by default
     84, 
     TRUE, 
     FALSE,  -- Non-accrual based
     NULL,   -- No accrual rate
     FALSE,  -- No carry-forward
     NULL,   -- No carry-forward cap
     TRUE,  -- Require reason
     TRUE,  -- Require document
     CURRENT_TIMESTAMP),
    
    ('Sick Leave', 
     '15 days/year (short-term), 6.5 months (long-term) with pay rules', 
     TRUE,
     TRUE,  -- Enabled by default
     15, 
     TRUE, 
     FALSE,  -- Non-accrual based
     NULL,   -- No accrual rate
     FALSE,  -- No carry-forward
     NULL,   -- No carry-forward cap
     TRUE,  -- Require reason
     TRUE,  -- Require document
     CURRENT_TIMESTAMP),
    
    ('Circumstantial Leave', 
     'Specific personal events (e.g., marriage, death)', 
     TRUE,
     TRUE,  -- Enabled by default
     3, 
     TRUE, 
     FALSE,  -- Non-accrual based
     NULL,   -- No accrual rate
     FALSE,  -- No carry-forward
     NULL,   -- No carry-forward cap
     TRUE,  -- Require reason
     FALSE,  -- Require document
     CURRENT_TIMESTAMP),
    
    ('Authorized Absence', 
     'For official missions, training, or public holidays', 
     TRUE,
     TRUE,  -- Enabled by default
     5, 
     TRUE, 
     FALSE,  -- Non-accrual based
     NULL,   -- No accrual rate
     FALSE,  -- No carry-forward
     NULL,   -- No carry-forward cap
     TRUE,  -- Require reason
     FALSE,  -- Require document
     CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    is_default = VALUES(is_default),
    is_enabled = VALUES(is_enabled),
    max_duration = VALUES(max_duration),
    paid = VALUES(paid),
    accrual_based = VALUES(accrual_based),
    accrual_rate = VALUES(accrual_rate),
    require_reason = VALUES(require_reason),
    require_document = VALUES(require_document),
    is_carry_forward_enabled = VALUES(is_carry_forward_enabled),
    carry_forward_cap = VALUES(carry_forward_cap),
    updated_at = CURRENT_TIMESTAMP;

-- Feature 2 & 3: Initialize Employee Leave Balances
-- Create empty balance records for all users and accrual-based leave types
-- Only for eligible employees (active status)
INSERT INTO employee_balance (
    employee_id,
    leave_type_id,
    current_balance,
    max_balance,
    last_accrual_date,
    is_eligible_for_accrual,
    created_at
)
SELECT 
    u.id as employee_id,
    lt.id as leave_type_id,
    0.00 as current_balance,  -- Start with zero balance
    CASE 
        WHEN lt.max_duration IS NOT NULL THEN lt.max_duration
        ELSE NULL
    END as max_balance,
    u.joined_date as last_accrual_date,  -- Set to joined date for initial setup
    CASE 
        WHEN u.status = 'ACTIVE' AND lt.accrual_based = TRUE THEN TRUE
        ELSE FALSE
    END as is_eligible_for_accrual,
    CURRENT_TIMESTAMP as created_at
FROM users u
CROSS JOIN leave_types lt
WHERE NOT EXISTS (
    SELECT 1 FROM employee_balance eb 
    WHERE eb.employee_id = u.id 
    AND eb.leave_type_id = lt.id
)
-- Only create balances for accrual-based leave types
AND lt.accrual_based = TRUE;

-- Insert Rwanda Public Holidays for 2025
INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'New Year''s Day', '2025-01-01', 'Celebration of the new year', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-01-01');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Heroes Day', '2025-02-01', 'National Heroes Day', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-02-01');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Genocide against the Tutsi Memorial Day', '2025-04-07', 'Commemoration of the 1994 Genocide against the Tutsi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-04-07');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Labor Day', '2025-05-01', 'International Workers'' Day', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-05-01');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Independence Day', '2025-07-01', 'Celebration of Rwanda''s independence', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-07-01');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Liberation Day', '2025-07-04', 'Celebration of the end of the 1994 Genocide against the Tutsi', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-07-04');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Umuganura Day', '2025-08-01', 'First Fruits Day - Traditional harvest celebration', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-08-01');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Assumption Day', '2025-08-15', 'Religious holiday', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-08-15');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Christmas Day', '2025-12-25', 'Celebration of Christmas', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-12-25');

INSERT INTO public_holidays (name, date, description, is_recurring)
SELECT 'Boxing Day', '2025-12-26', 'Day after Christmas, traditional gift-giving day', TRUE
WHERE NOT EXISTS (SELECT 1 FROM public_holidays WHERE date = '2025-12-26');

