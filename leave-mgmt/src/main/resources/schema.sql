-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP
);


-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255),
    department_id BIGINT,
    manager_id BIGINT,
    role_id BIGINT,
    joined_date DATE NOT NULL,  -- Added for tracking employee start date for prorated accrual
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- Employee status: ACTIVE, ON_LEAVE, SUSPENDED, TERMINATED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Note: Date and status validation moved to application level
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED')),
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id),
    CONSTRAINT fk_user_manager FOREIGN KEY (manager_id) REFERENCES users(id)
);

-- Feature 1.1 & 2: Leave Types and Accrual/Carry-Forward Configuration
CREATE TABLE IF NOT EXISTS leave_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,  -- Indicates if leave is enabled for the company
    max_duration INTEGER,
    paid BOOLEAN NOT NULL DEFAULT TRUE,
    -- Accrual configuration
    accrual_based BOOLEAN NOT NULL DEFAULT FALSE,  -- Indicates if leave is earned over time
    accrual_rate DECIMAL(5,2),  -- Stores rate like 1.50 for 1.5 days/month (max 31.00)
    -- Carry-forward configuration
    is_carry_forward_enabled BOOLEAN NOT NULL DEFAULT FALSE,  -- Indicates if leave can be carried forward
    carry_forward_cap DECIMAL(5,2),  -- Maximum days that can be carried forward
    require_reason BOOLEAN NOT NULL DEFAULT FALSE,
    require_document BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Note: Complex validation moved to application level
    CONSTRAINT chk_accrual_rate CHECK (
        (accrual_based = FALSE AND accrual_rate IS NULL) OR
        (accrual_based = TRUE AND accrual_rate > 0 AND accrual_rate <= 31.00)
    )
);

-- Feature 2 & 3: Employee Leave Balance Tracking
CREATE TABLE IF NOT EXISTS employee_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    current_balance DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    max_balance DECIMAL(5,2),  -- Maximum allowed balance (can be null for unlimited)
    last_accrual_date DATE,  -- Tracks when the last accrual was calculated
    is_eligible_for_accrual BOOLEAN NOT NULL DEFAULT TRUE,  -- Tracks if employee is eligible for accrual
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Foreign key constraints
    CONSTRAINT fk_employee_balance_user FOREIGN KEY (employee_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_balance_leave_type FOREIGN KEY (leave_type_id) 
        REFERENCES leave_types(id) ON DELETE CASCADE,
    -- Unique constraint to ensure one balance record per employee per leave type
    CONSTRAINT uk_employee_leave_type UNIQUE (employee_id, leave_type_id),
    -- Note: Date and balance validation moved to application level
    CONSTRAINT chk_positive_balance CHECK (current_balance >= 0)
);

-- Feature 2: Leave Accrual Tracking
CREATE TABLE IF NOT EXISTS leave_accruals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_balance_id BIGINT NOT NULL,
    accrual_date DATE NOT NULL,
    amount DECIMAL(5,2) NOT NULL,
    accrual_period DATE NOT NULL,  -- Changed from VARCHAR(7) to DATE to store first day of month
    is_prorated TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_accruals_employee_balance FOREIGN KEY (employee_balance_id) 
        REFERENCES employee_balance(id) ON DELETE CASCADE
);

-- Feature 3: Leave Carry Forward Tracking
CREATE TABLE IF NOT EXISTS leave_carry_forwards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_balance_id BIGINT NOT NULL,
    from_year INTEGER NOT NULL,
    to_year INTEGER NOT NULL,
    original_balance DECIMAL(5,2) NOT NULL,
    carried_forward_amount DECIMAL(5,2) NOT NULL,
    forfeited_amount DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    carry_forward_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Foreign key constraint
    CONSTRAINT fk_leave_carry_forwards_employee_balance FOREIGN KEY (employee_balance_id) 
        REFERENCES employee_balance(id) ON DELETE CASCADE
);


CREATE TABLE leave_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    half_day_start BOOLEAN NOT NULL DEFAULT FALSE,
    half_day_end BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    leave_request_reason VARCHAR(1000),
    rejection_reason VARCHAR(1000),
    manager_id BIGINT,
    approved_at TIMESTAMP,
    primary_document_id BIGINT NULL,  -- Just define the column, no FK yet
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_leave_requests_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_types(id) ON DELETE RESTRICT,
    CONSTRAINT fk_leave_requests_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_leave_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_requests_dates CHECK (end_date >= start_date)
);


CREATE TABLE documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    blob_url VARCHAR(500) NOT NULL,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_at DATETIME NOT NULL,
    leave_request_id BIGINT NOT NULL,
    CONSTRAINT fk_document_leave_request
        FOREIGN KEY (leave_request_id)
        REFERENCES leave_requests(id)
        ON DELETE CASCADE
);

ALTER TABLE leave_requests
ADD CONSTRAINT fk_leave_requests_primary_document
FOREIGN KEY (primary_document_id)
REFERENCES documents(id)
ON DELETE SET NULL;

-- Feature 4: Leave Requests

-- Feature 6: Public Holidays
CREATE TABLE IF NOT EXISTS public_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    description VARCHAR(500),
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,  -- TRUE for annual holidays like Christmas
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    -- Unique constraint to prevent duplicate holidays on the same date
    CONSTRAINT uk_holiday_date UNIQUE (date),
    -- Name validation
    CONSTRAINT chk_holiday_name CHECK (LENGTH(TRIM(name)) > 0)
);


-- Create notifications table
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
); 

-- Create indexes for better query performance
CREATE INDEX idx_employee_balance_employee ON employee_balance(employee_id);
CREATE INDEX idx_employee_balance_leave_type ON employee_balance(leave_type_id);
CREATE INDEX idx_leave_accruals_employee_balance ON leave_accruals(employee_balance_id);
CREATE INDEX idx_leave_accruals_period ON leave_accruals(accrual_period);
CREATE INDEX idx_leave_carry_forwards_employee_balance ON leave_carry_forwards(employee_balance_id);
CREATE INDEX idx_leave_carry_forwards_years ON leave_carry_forwards(from_year, to_year);
CREATE INDEX idx_departments_name ON departments(name);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_approved_by ON leave_requests(approved_by_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates ON leave_requests(start_date, end_date);
CREATE INDEX idx_public_holidays_date ON public_holidays(date);
CREATE INDEX idx_public_holidays_recurring ON public_holidays(is_recurring);