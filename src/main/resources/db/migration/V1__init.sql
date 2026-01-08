-- Challenges table - stores monthly challenges
CREATE TABLE IF NOT EXISTS scoreboards_challenges_service.challenges (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(title, month, year)
);

-- User challenge completions - tracks which users completed which challenges
CREATE TABLE IF NOT EXISTS scoreboards_challenges_service.user_challenge_completions (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    challenge_id INTEGER NOT NULL REFERENCES scoreboards_challenges_service.challenges(id) ON DELETE CASCADE,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, challenge_id)
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_user_challenge_completions_user_id ON scoreboards_challenges_service.user_challenge_completions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_challenge_completions_challenge_id ON scoreboards_challenges_service.user_challenge_completions(challenge_id);
CREATE INDEX IF NOT EXISTS idx_challenges_month_year ON scoreboards_challenges_service.challenges(month, year);

-- Insert some default challenges for current month
INSERT INTO scoreboards_challenges_service.challenges (title, description, month, year) VALUES 
    ('Peak Collector', 'Climb 5 different peaks this month', EXTRACT(MONTH FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE)),
    ('Trail Blazer', 'Complete 3 different trails', EXTRACT(MONTH FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE)),
    ('Early Bird', 'Start a hike before 7 AM', EXTRACT(MONTH FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE)),
    ('Weekend Warrior', 'Hike every weekend this month', EXTRACT(MONTH FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE)),
    ('Social Butterfly', 'Post 5 pictures from your hikes', EXTRACT(MONTH FROM CURRENT_DATE), EXTRACT(YEAR FROM CURRENT_DATE));

