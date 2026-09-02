-- 6. SEED DEMO DATA

-- Demo Accounts
INSERT INTO auth_ctx.accounts (account_id, phone_number, email, password_hash, is_verified)
VALUES
('00000000-0000-0000-0000-000000000001', '+15551234567', 'alex@example.com', '$2a$10$eO0V4hO2U2Q6gU9gM6eZ..0m8yF0qM9k1q5PqW3Vn7R4F2sX1O5a', TRUE),
('00000000-0000-0000-0000-000000000002', '+15552345678', 'yuki@example.com', '$2a$10$eO0V4hO2U2Q6gU9gM6eZ..0m8yF0qM9k1q5PqW3Vn7R4F2sX1O5a', TRUE),
('00000000-0000-0000-0000-000000000003', '+15553456789', 'elena@example.com', '$2a$10$eO0V4hO2U2Q6gU9gM6eZ..0m8yF0qM9k1q5PqW3Vn7R4F2sX1O5a', TRUE),
('00000000-0000-0000-0000-000000000004', '+15554567890', 'chloe@example.com', '$2a$10$eO0V4hO2U2Q6gU9gM6eZ..0m8yF0qM9k1q5PqW3Vn7R4F2sX1O5a', TRUE),
('00000000-0000-0000-0000-000000000005', '+15555678901', 'marcus@example.com', '$2a$10$eO0V4hO2U2Q6gU9gM6eZ..0m8yF0qM9k1q5PqW3Vn7R4F2sX1O5a', TRUE)
ON CONFLICT (account_id) DO NOTHING;

-- Demo Profiles (Coordinates: Longitude first, Latitude second in ST_MakePoint)
-- Alex: Tokyo (139.6503, 35.6762)
INSERT INTO people_ctx.user_profiles (
    user_id, first_name, birthdate, gender, interested_in, bio,
    location, max_distance_km, age_min_pref, age_max_pref,
    interest_tags, job_title, company, school, is_active
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Alex',
    '1998-04-12',
    'MAN',
    'WOMEN',
    'Product architect & electronic music enthusiast. Always looking for the next hidden coffee spot or weekend mountain trail.',
    ST_SetSRID(ST_MakePoint(139.6503, 35.6762), 4326)::geography,
    50,
    21,
    34,
    ARRAY['Travel', 'EDM', 'Coffee', 'Hiking', 'AI', 'Architecture', 'Photography'],
    'Product Architect',
    'Apex Labs',
    'Stanford University',
    TRUE
) ON CONFLICT (user_id) DO NOTHING;

-- Yuki: Shibuya, Tokyo (139.7016, 35.6580)
INSERT INTO people_ctx.user_profiles (
    user_id, first_name, birthdate, gender, interested_in, bio,
    location, max_distance_km, age_min_pref, age_max_pref,
    interest_tags, job_title, company, school, is_active
) VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Yuki',
    '2000-06-15',
    'WOMAN',
    'MEN',
    'Art director by day, vinyl hunter by night. Obsessed with Shibuya jazz bars, film photography, and matcha lattes.',
    ST_SetSRID(ST_MakePoint(139.7016, 35.6580), 4326)::geography,
    50,
    22,
    34,
    ARRAY['Travel', 'Photography', 'EDM', 'Matcha', 'Art Galleries', 'Vinyl Records'],
    'Senior Art Director',
    'Monolith Studio',
    'Tokyo University of the Arts',
    TRUE
) ON CONFLICT (user_id) DO NOTHING;

-- Elena: Roppongi, Tokyo (139.7314, 35.6628)
INSERT INTO people_ctx.user_profiles (
    user_id, first_name, birthdate, gender, interested_in, bio,
    location, max_distance_km, age_min_pref, age_max_pref,
    interest_tags, job_title, company, school, is_active
) VALUES (
    '00000000-0000-0000-0000-000000000003',
    'Elena',
    '1999-11-20',
    'WOMAN',
    'MEN',
    'Sommelier & contemporary ceramicist. Searching for someone to exchange curated Spotify playlists and cook spicy midnight pasta with.',
    ST_SetSRID(ST_MakePoint(139.7314, 35.6628), 4326)::geography,
    50,
    23,
    35,
    ARRAY['Wine Tasting', 'Ceramics', 'Indie Rock', 'Italian Cuisine', 'Modern Art'],
    'Lead Sommelier',
    'L''Aura Tokyo',
    'Culinary Institute of Milan',
    TRUE
) ON CONFLICT (user_id) DO NOTHING;

-- Chloe: Daikanyama, Tokyo (139.7038, 35.6491)
INSERT INTO people_ctx.user_profiles (
    user_id, first_name, birthdate, gender, interested_in, bio,
    location, max_distance_km, age_min_pref, age_max_pref,
    interest_tags, job_title, company, school, is_active
) VALUES (
    '00000000-0000-0000-0000-000000000004',
    'Chloe',
    '2001-02-08',
    'WOMAN',
    'MEN',
    'Fashion stylist & editorial photographer. Living between Tokyo and Paris.',
    ST_SetSRID(ST_MakePoint(139.7038, 35.6491), 4326)::geography,
    50,
    21,
    30,
    ARRAY['Fashion', 'Editorial', 'Vintage', 'French Cinema', 'House Music'],
    'Fashion Stylist',
    'Vogue Japan',
    'Bunka Fashion College',
    TRUE
) ON CONFLICT (user_id) DO NOTHING;

-- Profile Photos
INSERT INTO people_ctx.profile_media (media_id, user_id, r2_object_key, media_url, display_order)
VALUES
('00000000-0000-0000-0001-000000000001', '00000000-0000-0000-0000-000000000001', 'users/alex/1.jpg', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80', 0),
('00000000-0000-0000-0001-000000000002', '00000000-0000-0000-0000-000000000001', 'users/alex/2.jpg', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800&q=80', 1),
('00000000-0000-0000-0001-000000000003', '00000000-0000-0000-0000-000000000002', 'users/yuki/1.jpg', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=800&q=80', 0),
('00000000-0000-0000-0001-000000000004', '00000000-0000-0000-0000-000000000002', 'users/yuki/2.jpg', 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800&q=80', 1),
('00000000-0000-0000-0001-000000000005', '00000000-0000-0000-0000-000000000003', 'users/elena/1.jpg', 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=800&q=80', 0),
('00000000-0000-0000-0001-000000000006', '00000000-0000-0000-0000-000000000004', 'users/chloe/1.jpg', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800&q=80', 0)
ON CONFLICT (media_id) DO NOTHING;

-- Initial Mutual Matches
-- Match between Alex (001) and Yuki (002)
INSERT INTO interaction_ctx.matches (match_id, user_one_id, user_two_id, is_active)
VALUES ('00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', TRUE)
ON CONFLICT (match_id) DO NOTHING;

-- Match between Alex (001) and Elena (003)
INSERT INTO interaction_ctx.matches (match_id, user_one_id, user_two_id, is_active)
VALUES ('00000000-0000-0000-0002-000000000002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', TRUE)
ON CONFLICT (match_id) DO NOTHING;

-- Demo Messages in Alex & Yuki match
INSERT INTO chat_ctx.messages (message_id, match_id, sender_id, recipient_id, content, is_read, created_at)
VALUES
('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Hey Alex! Loved your taste in music.', TRUE, NOW() - INTERVAL '2 hours'),
('00000000-0000-0000-0003-000000000002', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Thanks Yuki! Shibuya jazz bars are incredible. Have you been to Bar Trench?', TRUE, NOW() - INTERVAL '1 hour 45 minutes'),
('00000000-0000-0000-0003-000000000003', '00000000-0000-0000-0002-000000000001', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Yes! Let''s grab coffee or a drink there this Friday?', FALSE, NOW() - INTERVAL '10 minutes')
ON CONFLICT (message_id) DO NOTHING;
