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
  'dawid@dlakomy.github.io'
, '$2a$10$k7SC5Wz54II9QMrB7.FhEeYWApxNQH28tWGKcbtbkXTDE02yYq2Ba'
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
, '$2a$10$yuC4.08NGHHkgAfuSE0ORee1uBQMqn5W5F5srhvWZMy9TnQH39kZS'
, 'John'
, 'Hungrytoo'
, 'DL corp.'
, 'RECRUITER'
);
