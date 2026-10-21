-- Nguồn: docs/db/C5-community-moderation-ai-notification.sql (Cluster 5, Version v0.1 · 19/10/2026)
-- Bảng: (không có bảng) seed system_parameters của cụm 5, trừ EMBEDDING_MODEL (tách sang migration pgvector, xem docs/db/C5)

-- =====================================================================================
-- 10. SEED: system parameters of this cluster (UC54). CHATBOT_DAILY_QUESTION_QUOTA is in cluster 1.
-- =====================================================================================

INSERT INTO system_parameters (param_key, param_value, value_type, description) VALUES
    ('LAB_REVIEW_EDIT_DAYS',                '7',    'INTEGER', 'A Lab Review can be edited for N days after posting (UC37)'),
    ('CONTENT_REPORT_DAILY_LIMIT',          '20',   'INTEGER', 'Content Reports per user per day (UC49, UH-29)'),
    ('COMPLAINT_WINDOW_DAYS',               '30',   'INTEGER', 'Complaint allowed until N days after the order is closed (UC51)'),
    ('COMPLAINT_INFO_RESPONSE_DAYS',        '7',    'INTEGER', 'Days for a party to send requested evidence (UC52)'),
    ('CHATBOT_CONTEXT_TURNS',               '10',   'INTEGER', 'Conversation turns sent as context (UC19)'),
    ('CHATBOT_MAX_CITATIONS',               '3',    'INTEGER', 'Citations per answer (UC19)'),
    ('RECOMMENDATION_MAX_RESULTS',          '10',   'INTEGER', 'Labs per recommendation (UC18)'),
    ('RECOMMENDATION_CACHE_MINUTES',        '15',   'INTEGER', 'Recommendation cache per user (UC18)'),
    ('IMAGE_ANALYSIS_CONFIDENCE_THRESHOLD', '0.80', 'DECIMAL', '[TBC] value not given by the SRS. Auto-tag above, suggestion below (UC40)');
