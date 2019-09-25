create table annotation_status
(
    id            int          not null
        primary key,
    uri           varchar(255) null,
    version       bigint       null,
    name          varchar(255) null,
    defined_at_id int          null,
    constraint FKproject
        foreign key (defined_at_id) references project (id)
);

create table sample_annotation_statuses
(
    sample_id              int null,
    annotation_statuses_id int null,
    constraint FKsample
    foreign key (sample_id) references sample (id),
    constraint FKannot
    foreign key (annotation_statuses_id) references annotation_status (id)
);