update metadata_field set type = 0;
alter table metadata_field change column type type int default 0;