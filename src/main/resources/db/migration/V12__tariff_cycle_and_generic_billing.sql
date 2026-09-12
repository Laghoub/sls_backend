-- V12 - Tarification par cycle et moteur de facturation générique
-- Ne modifie aucune migration précédente.

ALTER TABLE tariff
    ADD COLUMN cycle_id BIGINT REFERENCES cycle(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_tariff_cycle_id ON tariff (cycle_id);
