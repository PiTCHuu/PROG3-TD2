create type PlayerPositionEnum as enum('GK', 'DEF', 'MIDF', 'STR');

create type ContinentEnum as enum('AFRICA', 'EUROPA', 'ASIA', 'AMERICA');

create table Player(
    id serial primary key,
    name varchar(255) not null,
    age int not null,
    position PlayerPositionEnum not null,
    id_team int not null,
    CONSTRAINT fk_product FOREIGN KEY (id_team) REFERENCES Team(id)
        ON DELETE CASCADE
);
create table Team(
    id serial primary key,
    name varchar(255) not null,
    continent ContinentEnum not null
);
