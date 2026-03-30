-- Create Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    profile_picture VARCHAR(500),
    bio TEXT,
    email_verified BOOLEAN DEFAULT FALSE,
    oauth_provider VARCHAR(50),
    oauth_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(email)
);

-- Create Refresh Tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Roadmaps table
CREATE TABLE roadmaps (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    current_role VARCHAR(255),
    target_role VARCHAR(255) NOT NULL,
    experience_level VARCHAR(50),
    timeframe VARCHAR(100),
    is_published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Roadmap Steps table
CREATE TABLE roadmap_steps (
    id BIGSERIAL PRIMARY KEY,
    roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
    phase_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Skills table
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Roadmap Step Skills (many-to-many)
CREATE TABLE roadmap_step_skills (
    id BIGSERIAL PRIMARY KEY,
    step_id BIGINT NOT NULL REFERENCES roadmap_steps(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE(step_id, skill_id)
);

-- Create User Skills table
CREATE TABLE user_skills (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency_level VARCHAR(50),
    years_of_experience INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, skill_id)
);

-- Create User Roadmap Progress table
CREATE TABLE user_roadmap_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roadmap_id BIGINT NOT NULL REFERENCES roadmaps(id) ON DELETE CASCADE,
    step_id BIGINT REFERENCES roadmap_steps(id) ON DELETE SET NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, roadmap_id, step_id)
);

-- Create Chat Messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    response TEXT,
    message_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Jobs table
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    salary_min DECIMAL(10,2),
    salary_max DECIMAL(10,2),
    location VARCHAR(255),
    job_type VARCHAR(50),
    experience_level VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Job Skills (many-to-many)
CREATE TABLE job_skills (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    UNIQUE(job_id, skill_id)
);

-- Create Job Applications table
CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) DEFAULT 'PENDING',
    match_score DECIMAL(5,2),
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    UNIQUE(job_id, student_id)
);

-- Create Resumes table
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_url VARCHAR(500),
    content TEXT,
    ats_score INT,
    analysis JSONB,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analyzed_at TIMESTAMP
);

-- Create Mentor Profiles table
CREATE TABLE mentor_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    expertise TEXT,
    hourly_rate DECIMAL(10,2),
    bio TEXT,
    rating DECIMAL(3,2) DEFAULT 0,
    is_approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Mentor Sessions table
CREATE TABLE mentor_sessions (
    id BIGSERIAL PRIMARY KEY,
    mentor_id BIGINT NOT NULL REFERENCES mentor_profiles(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INT,
    status VARCHAR(50) DEFAULT 'SCHEDULED',
    meeting_link VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES mentor_sessions(id) ON DELETE SET NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    message TEXT,
    data JSONB,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Quiz Questions table
CREATE TABLE quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(500),
    option_b VARCHAR(500),
    option_c VARCHAR(500),
    option_d VARCHAR(500),
    correct_answer VARCHAR(50),
    explanation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Quiz Attempts table
CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    selected_answer VARCHAR(50),
    is_correct BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_roadmaps_user_id ON roadmaps(user_id);
CREATE INDEX idx_chat_messages_user_id ON chat_messages(user_id);
CREATE INDEX idx_jobs_company_id ON jobs(company_id);
CREATE INDEX idx_job_applications_job_id ON job_applications(job_id);
CREATE INDEX idx_job_applications_student_id ON job_applications(student_id);
CREATE INDEX idx_mentor_profiles_user_id ON mentor_profiles(user_id);
CREATE INDEX idx_mentor_sessions_mentor_id ON mentor_sessions(mentor_id);
CREATE INDEX idx_mentor_sessions_student_id ON mentor_sessions(student_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_quiz_attempts_user_id ON quiz_attempts(user_id);
