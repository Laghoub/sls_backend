-- Coordonnees geographiques des arrets pour la vue cartographique du module Transport.
-- Migration additive : ne modifie pas les migrations Transport precedentes.
ALTER TABLE transport_stop
    ADD COLUMN IF NOT EXISTS latitude NUMERIC(10,7),
    ADD COLUMN IF NOT EXISTS longitude NUMERIC(10,7);

ALTER TABLE transport_stop
    DROP CONSTRAINT IF EXISTS chk_transport_stop_latitude,
    DROP CONSTRAINT IF EXISTS chk_transport_stop_longitude;

ALTER TABLE transport_stop
    ADD CONSTRAINT chk_transport_stop_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    ADD CONSTRAINT chk_transport_stop_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
