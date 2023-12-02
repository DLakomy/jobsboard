CREATE TABLE users (
  email text NOT NULL
, hashedPassword text NOT NULL
, firstName text
, lastName text
, company text
, role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

ALTER TABLE users
ADD CONSTRAINT ck_users_role CHECK (role in ('ADMIN', 'RECRUITER'));

INSERT INTO users (
  email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'dawid@lakomy.github.io'
, 'something'
, 'Dawid'
, 'Hungry'
, 'DL corp.'
, 'ADMIN'
);

INSERT INTO users (
  email
, hashedPassword
, firstName
, lastName
, company
, role
) VALUES (
  'john@lakomy.github.io'
, 'somethingelse'
, 'John'
, 'Hungrytoo'
, 'DL corp.'
, 'RECRUITER'
);
