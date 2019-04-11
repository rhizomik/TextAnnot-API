create table hibernate_sequence
(
    next_val bigint null
)
    collate = utf8mb4_unicode_ci;

create table metadata_template
(
    id         int          not null
        primary key,
    uri        varchar(255) null,
    version    bigint       null,
    name       varchar(255) null,
    start_date datetime     null,
    constraint UK_8w42mbwddut6vuhqrea0ur1pl
        unique (name)
)
    collate = utf8mb4_unicode_ci;

create table metadata_field
(
    id            int auto_increment
        primary key,
    uri           varchar(255) null,
    version       bigint       null,
    category      varchar(255) null,
    name          varchar(255) null,
    type          varchar(255) null,
    defined_at_id int          null,
    constraint FKf8pgpk9p1pulcwoyntrhmosx
        foreign key (defined_at_id) references metadata_template (id)
)
    collate = utf8mb4_unicode_ci;

create table tag_hierarchy
(
    id      int          not null
        primary key,
    uri     varchar(255) null,
    version bigint       null,
    name    varchar(255) null,
    constraint UK_r9u8kxt68ujmfg08n9ie6146f
        unique (name)
)
    collate = utf8mb4_unicode_ci;

create table tag
(
    id               int          not null
        primary key,
    uri              varchar(255) null,
    version          bigint       null,
    name             varchar(255) null,
    parent_id        int          null,
    tag_hierarchy_id int          null,
    constraint FK7qmvvbjehhppyl7shle3qeli1
        foreign key (parent_id) references tag (id),
    constraint FKm5ihvfiu0gy0n56d4hkj7wqtb
        foreign key (tag_hierarchy_id) references tag_hierarchy (id)
)
    collate = utf8mb4_unicode_ci;

create table text_annot_sample
(
    dtype           varchar(31)  not null,
    id              int          not null
        primary key,
    uri             varchar(255) null,
    version         bigint       null,
    text            longtext     not null,
    content         longtext     null,
    described_by_id int          null,
    tagged_by_id    int          null,
    constraint FK6tpct746fbokryxn50i0y6r0j
        foreign key (described_by_id) references metadata_template (id),
    constraint FKqtiblcr8xi0lbn7kuh2yxslc0
        foreign key (tagged_by_id) references tag_hierarchy (id)
)
    collate = utf8mb4_unicode_ci;

create table metadata_value
(
    id        int          not null
        primary key,
    uri       varchar(255) null,
    version   bigint       null,
    value     longtext     null,
    fora_id   int          null,
    values_id int          null,
    constraint FKrc40t3puayqblv6l7s3yn91hn
        foreign key (fora_id) references text_annot_sample (id),
    constraint FKre39664hdf93rimc0hrqmg6dl
        foreign key (values_id) references metadata_field (id)
)
    collate = utf8mb4_unicode_ci;

create table text_annot_user
(
    dtype    varchar(31)  not null,
    username varchar(255) not null
        primary key,
    uri      varchar(255) null,
    version  bigint       null,
    email    varchar(255) null,
    password varchar(256) null
)
    collate = utf8mb4_unicode_ci;

create table annotation
(
    id                int          not null
        primary key,
    uri               varchar(255) null,
    version           bigint       null,
    end               int          null,
    reviewed          bit          null,
    start             int          null,
    linguist_username varchar(255) null,
    sample_id         int          null,
    tag_id            int          null,
    constraint FK7y2bynabc4k7melr8bygtksl
        foreign key (tag_id) references tag (id),
    constraint FKhqfk4e5pu7kjuk7nuyb6cd0rv
        foreign key (linguist_username) references text_annot_user (username),
    constraint FKmc3of2kpftpog3livp77a0hsk
        foreign key (sample_id) references text_annot_sample (id)
)
    collate = utf8mb4_unicode_ci;

