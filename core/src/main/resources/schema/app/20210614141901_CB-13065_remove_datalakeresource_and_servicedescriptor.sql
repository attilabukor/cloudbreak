-- // CB-13065 removing datalakeresources and servicedescriptor
-- Migration SQL that makes the change goes here.

ALTER TABLE stack DROP COLUMN datalakeresourceid;

DROP TABLE datalakeresources_rdsconfig;

DROP TABLE servicedescriptor;

DROP TABLE datalakeresources;

-- //@UNDO
-- SQL to undo the change goes here.

CREATE TABLE datalakeresources (
  datalakeclustermanagerurl character varying(255) NULL,
  datalakeclustermanagerfqdn character varying(255) NULL,
  datalakeclustermanagerip character varying(255) NULL,
  environmentcrn character varying(255) NULL,
  datalakeambariurl character varying(255) NULL,
  kerberosconfig_id bigint NULL,
  datalakecomponents text NOT NULL,
  workspace_id bigint NOT NULL,
  datalakeambarifqdn character varying(255) NOT NULL,
  datalakeambariip character varying(255) NOT NULL,
  name character varying(255) NOT NULL,
  datalakestack_id bigint NULL,
  id bigint NOT NULL
);
ALTER TABLE
  public.datalakeresources
ADD
  CONSTRAINT datalakeresources_pkey PRIMARY KEY (id);


CREATE TABLE datalakeresources_rdsconfig (
  rdsconfigs_id bigint NOT NULL,
  datalakeresources_id bigint NOT NULL
);
ALTER TABLE
  datalakeresources_rdsconfig
ADD
  CONSTRAINT datalakeresources_rdsconfig_pkey PRIMARY KEY (datalakeresources_id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_datalakeresources_id FOREIGN KEY (datalakeresources_id) REFERENCES datalakeresources(id);
ALTER TABLE ONLY datalakeresources_rdsconfig ADD CONSTRAINT fk_datalakeresources_rdsconfig_rdsconfig_id FOREIGN KEY (rdsconfigs_id) REFERENCES rdsconfig(id);


CREATE TABLE servicedescriptor (
  componentshosts text NOT NULL,
  blueprintsecretparams text NULL,
  blueprintparams text NOT NULL,
  servicename character varying(255) NOT NULL,
  workspace_id bigint NOT NULL,
  datalakeresources_id bigint NOT NULL,
  id bigint NOT NULL
);
ALTER TABLE
  servicedescriptor
ADD
  CONSTRAINT servicedescriptor_pkey PRIMARY KEY (id);

ALTER TABLE IF EXISTS stack
    ADD COLUMN IF NOT EXISTS datalakeresourceid bigint;

CREATE INDEX idx_datalakeresources_envcrn_name ON datalakeresources USING btree (environmentcrn, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_workspace_id_name ON datalakeresources (workspace_id, name);
CREATE INDEX IF NOT EXISTS idx_datalakeresources_datalakestack_id ON datalakeresources (datalakestack_id);
CREATE INDEX IF NOT EXISTS idx_servicedescriptor_datalakeresources_id ON servicedescriptor (datalakeresources_id);


