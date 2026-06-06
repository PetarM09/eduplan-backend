ALTER TABLE godisnji_planovi
    ADD COLUMN razlog_vracanja TEXT,
    ADD COLUMN odobren_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN odobrio_id UUID REFERENCES korisnici(id),
    ADD COLUMN vracen_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN vratio_id UUID REFERENCES korisnici(id);

ALTER TABLE operativni_planovi
    ADD COLUMN razlog_vracanja TEXT,
    ADD COLUMN odobren_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN odobrio_id UUID REFERENCES korisnici(id),
    ADD COLUMN vracen_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN vratio_id UUID REFERENCES korisnici(id);
