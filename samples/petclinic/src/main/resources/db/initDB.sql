-- DDL script for creation of the database, assuming it's initially empty.

CREATE TABLE vet (
   id INTEGER IDENTITY PRIMARY KEY,
   firstName VARCHAR(30) NOT NULL,
   lastName VARCHAR(30) NOT NULL
);
CREATE INDEX vet_lastName ON vet (lastName);

CREATE TABLE specialty (
   id INTEGER IDENTITY PRIMARY KEY,
   name VARCHAR(80) NOT NULL,
   UNIQUE (name)
);

CREATE TABLE vet_specialty (
   vetId INTEGER NOT NULL,
   specialtyId INTEGER NOT NULL,
   FOREIGN KEY (vetId) REFERENCES vet,
   FOREIGN KEY (specialtyId) REFERENCES specialty
);

CREATE TABLE petType (
   id INTEGER IDENTITY PRIMARY KEY,
   name VARCHAR(80) NOT NULL,
   UNIQUE (name)
);

CREATE TABLE owner (
   id INTEGER IDENTITY PRIMARY KEY,
   firstName VARCHAR(30) NOT NULL,
   lastName VARCHAR(30) NOT NULL,
   address VARCHAR(255),
   city VARCHAR(80),
   telephone VARCHAR(20) NOT NULL
);
CREATE INDEX owner_lastName ON owner (lastName);

CREATE TABLE pet (
   id INTEGER IDENTITY PRIMARY KEY,
   name VARCHAR(30) NOT NULL,
   birthDate DATE,
   typeId INTEGER NOT NULL,
   ownerId INTEGER NOT NULL,
   FOREIGN KEY (typeId) REFERENCES petType,
   FOREIGN KEY (ownerId) REFERENCES owner
);
CREATE INDEX pet_name ON pet (name);

CREATE TABLE visit (
   id INTEGER IDENTITY PRIMARY KEY,
   petId INTEGER NOT NULL,
   date DATETIME NOT NULL,
   description VARCHAR(255),
   FOREIGN KEY (petId) REFERENCES pet
);
CREATE INDEX visit_petId ON visit (petId);
