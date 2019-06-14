create table hibernate_sequence
(
    next_val bigint null
);

insert into hibernate_sequence values (0);
insert into hibernate_sequence values (0);
insert into hibernate_sequence values (0);
insert into hibernate_sequence values (0);
insert into hibernate_sequence values (0);
insert into hibernate_sequence values (0);

create table project
(
    id                     int          not null
        primary key,
    uri                    varchar(255) null,
    version                bigint       null,
    name                   varchar(255) null,
    precalculated_tag_tree longtext     null,
    constraint UK_3k75vvu7mevyvvb5may5lj8k7
        unique (name)
);

create table metadata_field
(
    id                 int auto_increment
        primary key,
    uri                varchar(255) null,
    version            bigint       null,
    category           varchar(255) null,
    include_statistics bit          null,
    name               varchar(255) null,
    type               varchar(255) null,
    defined_at_id      int          null,
    constraint FKfywhpwaxrj6htah1r0hfvuqu2
        foreign key (defined_at_id) references project (id)
);

create table sample
(
    dtype      varchar(31)  not null,
    id         int          not null
        primary key,
    uri        varchar(255) null,
    version    bigint       null,
    text       longtext     not null,
    content    longtext     null,
    project_id int          null,
    constraint FKeytsctv5b8stbijub6yeil5ie
        foreign key (project_id) references project (id)
);

create table metadata_value
(
    id        int          not null
        primary key,
    uri       varchar(255) null,
    version   bigint       null,
    value     longtext     null,
    fora_id   int          null,
    values_id int          null,
    constraint FK5p2dlkp54vhchey6l1spk2c0a
        foreign key (fora_id) references sample (id),
    constraint FKre39664hdf93rimc0hrqmg6dl
        foreign key (values_id) references metadata_field (id)
);

create table tag
(
    id         int          not null
        primary key,
    uri        varchar(255) null,
    version    bigint       null,
    name       varchar(255) null,
    parent_id  int          null,
    project_id int          null,
    constraint FK7qmvvbjehhppyl7shle3qeli1
        foreign key (parent_id) references tag (id),
    constraint FKbyy56vice9njgl86752up8120
        foreign key (project_id) references project (id)
);

create table text_annot_user
(
    dtype    varchar(31)  not null,
    username varchar(255) not null
        primary key,
    uri      varchar(255) null,
    version  bigint       null,
    email    varchar(255) null,
    password varchar(256) null
);

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
    constraint FKoh4hdr7ypj9lv9kulj5bf7d3e
        foreign key (sample_id) references sample (id)
);

