alter table metadata_field change column name xml_name varchar(255) null;
alter table metadata_field add column name varchar(255) null;
alter table metadata_field add column private_field bit not null default 0;