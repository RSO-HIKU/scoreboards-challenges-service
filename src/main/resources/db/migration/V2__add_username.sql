ALTER TABLE scoreboards_challenges_service.user_challenge_completions
ADD COLUMN username VARCHAR(255);

-- Optional: Add index for faster queries
CREATE INDEX idx_user_challenge_completions_username ON scoreboards_challenges_service.user_challenge_completions(username);