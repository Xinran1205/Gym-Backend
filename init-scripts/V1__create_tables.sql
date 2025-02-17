-- 1.1 users table (basic information of all users)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    address VARCHAR(255) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('Member', 'Trainer', 'Admin') NOT NULL,
    account_status ENUM('Pending', 'Approved', 'Suspended') NOT NULL DEFAULT 'Pending',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 1.2 trainer_profiles table (extended information for personal trainers)
CREATE TABLE IF NOT EXISTS trainer_profiles (
    trainer_profile_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    certifications TEXT,
    specializations TEXT,
    years_of_experience INT,
    biography TEXT,
    availability JSON,  -- If MySQL version does not support JSON, change to TEXT
    rating DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_trainer_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 1.3 admin_profiles table (extended information for administrators)
CREATE TABLE IF NOT EXISTS admin_profiles (
    admin_profile_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    admin_level ENUM('Super', 'Standard'),
    additional_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 1.4 social_media_auth table (social media login authentication information)
CREATE TABLE IF NOT EXISTS social_media_auth (
    auth_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    platform ENUM('Google', 'Facebook') NOT NULL,
    social_media_user_id VARCHAR(255) NOT NULL,
    access_token VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_socialmedia_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 2.1 training_requests table (training requests initiated by members)
CREATE TABLE IF NOT EXISTS training_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    trainer_id INT,  -- Initially can be NULL; to be selected by the member or assigned by the system
    fitness_goal_description TEXT NOT NULL,
    status ENUM('Pending', 'Accepted', 'Rejected', 'AlternativeSuggested') NOT NULL DEFAULT 'Pending',
    alternative_trainer_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_request_member FOREIGN KEY (member_id) REFERENCES users(user_id),
    CONSTRAINT fk_request_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id),
    CONSTRAINT fk_request_alternative FOREIGN KEY (alternative_trainer_id) REFERENCES users(user_id)
);

-- 2.2 training_sessions table (records of scheduled training sessions)
CREATE TABLE IF NOT EXISTS training_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    request_id INT,
    member_id INT NOT NULL,
    trainer_id INT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    duration INT NOT NULL,
    status ENUM('Scheduled', 'Completed', 'Cancelled', 'No-Show') NOT NULL DEFAULT 'Scheduled',
    session_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_request FOREIGN KEY (request_id) REFERENCES training_requests(request_id),
    CONSTRAINT fk_session_member FOREIGN KEY (member_id) REFERENCES users(user_id),
    CONSTRAINT fk_session_trainer FOREIGN KEY (trainer_id) REFERENCES users(user_id)
);

-- 2.3 training_history table (archived records of completed training sessions)
CREATE TABLE IF NOT EXISTS training_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    session_id INT NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration INT NOT NULL,
    session_notes TEXT,
    CONSTRAINT fk_history_member FOREIGN KEY (member_id) REFERENCES users(user_id),
    CONSTRAINT fk_history_session FOREIGN KEY (session_id) REFERENCES training_sessions(session_id)
);

-- 3.1 fitness_centres table (information on chain fitness centers for map display and search)
CREATE TABLE IF NOT EXISTS fitness_centres (
    centre_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(9,6) NOT NULL,
    longitude DECIMAL(9,6) NOT NULL,
    contact_info VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3.2 notifications table (records of notifications sent by the system to users)
CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- 4.1 messages table (internal messaging records between users)
CREATE TABLE IF NOT EXISTS messages (
    message_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
    CONSTRAINT fk_message_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id)
);
