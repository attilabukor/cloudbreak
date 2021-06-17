-- // CB-12965 new entries in flowlog tables: operation type field
-- Migration SQL that makes the change goes here.

ALTER TABLE IF EXISTS flowlog add COLUMN operationtype varchar(255);

-- //@UNDO
-- SQL to undo the change goes here.

ALTER TABLE IF EXISTS flowlog DROP COLUMN operationtype;

