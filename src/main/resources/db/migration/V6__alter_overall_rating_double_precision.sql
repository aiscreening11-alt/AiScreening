-- Migration V6: Alter overall_rating to DOUBLE PRECISION to match JPA Double type
ALTER TABLE interviews ALTER COLUMN overall_rating TYPE DOUBLE PRECISION;
