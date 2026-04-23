-- Rating tablosu oluşturma
CREATE TABLE IF NOT EXISTS ratings (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (
        rating >= 1
        AND rating <= 5
    ),
    user_identifier VARCHAR(255) NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT uc_rating_user_post UNIQUE (user_identifier, post_id)
);

-- İndeksler
CREATE INDEX IF NOT EXISTS idx_rating_post_id ON ratings (post_id);

CREATE INDEX IF NOT EXISTS idx_rating_user_post ON ratings (user_identifier, post_id);

-- Yorum
COMMENT ON TABLE ratings IS 'Post değerlendirmeleri (1-5 yıldız rating sistemi)';

COMMENT ON COLUMN ratings.rating IS '1-5 arası puan değeri';

COMMENT ON COLUMN ratings.user_identifier IS 'Kullanıcı tanımlayıcı (IP adresi veya user ID)';

COMMENT ON COLUMN ratings.comment IS 'Opsiyonel değerlendirme yorumu';