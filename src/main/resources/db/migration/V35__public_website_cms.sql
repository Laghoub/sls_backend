CREATE TABLE website_content (
 id BIGSERIAL PRIMARY KEY, content_key VARCHAR(120) NOT NULL UNIQUE, title_fr TEXT NOT NULL DEFAULT '', title_ar TEXT NOT NULL DEFAULT '', body_fr TEXT NOT NULL DEFAULT '', body_ar TEXT NOT NULL DEFAULT '', image_url TEXT, published BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE website_news (
 id BIGSERIAL PRIMARY KEY, title_fr TEXT NOT NULL, title_ar TEXT NOT NULL, body_fr TEXT NOT NULL DEFAULT '', body_ar TEXT NOT NULL DEFAULT '', image_url TEXT, published BOOLEAN NOT NULL DEFAULT FALSE, published_at TIMESTAMPTZ, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE website_gallery (
 id BIGSERIAL PRIMARY KEY, title_fr TEXT NOT NULL DEFAULT '', title_ar TEXT NOT NULL DEFAULT '', image_url TEXT NOT NULL, category VARCHAR(80) NOT NULL DEFAULT 'general', published BOOLEAN NOT NULL DEFAULT FALSE, sort_order INT NOT NULL DEFAULT 0
);
INSERT INTO website_content(content_key,title_fr,title_ar,body_fr,body_ar,published) VALUES
('home','Bienvenue au Groupe Scolaire Sciences et Lettres','مرحبًا بكم في المجمع المدرسي الخاص علوم وآداب','','',TRUE),
('about','Notre établissement','مؤسستنا','','',TRUE),
('cycles','Nos cycles','أطوارنا التعليمية','','',TRUE),
('pedagogy','Notre pédagogie','منهجنا التربوي','','',TRUE),
('activities','Vie scolaire','الحياة المدرسية','','',TRUE),
('contact','Contact','اتصل بنا','','',TRUE) ON CONFLICT(content_key) DO NOTHING;
INSERT INTO permission(code,name,module,description,active) VALUES
('SITE_WEB_CONSULTER','Consulter la gestion du site','SITE_WEB','Accès aux contenus du site public',TRUE),
('SITE_WEB_MODIFIER','Modifier le site','SITE_WEB','Créer et modifier les contenus, actualités et photos',TRUE) ON CONFLICT(code) DO NOTHING;
INSERT INTO role_permission(role_id,permission_id) SELECT r.id,p.id FROM role r CROSS JOIN permission p WHERE r.code='ADMIN' AND p.code IN ('SITE_WEB_CONSULTER','SITE_WEB_MODIFIER') ON CONFLICT DO NOTHING;
