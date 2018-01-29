# --- !Ups

CREATE TABLE USER (
  ID                     BIGINT NOT NULL AUTO_INCREMENT,
  GROUP_ID               BIGINT NOT NULL,
  ROLE_ID                INT UNSIGNED NOT NULL,
  STATUS_ID              INT UNSIGNED NOT NULL,
  USER_ID                CHAR(36) NOT NULL,
  AVATAR_URL             VARCHAR(254),
  EMAIL                  VARCHAR(127) NOT NULL,
  FIRST_NAME             VARCHAR(64),
  LAST_NAME              VARCHAR(64),
  USERNAME               VARCHAR(127) NOT NULL,
  USERNAME_SUFFIX        INT UNSIGNED,
  CREATED_TIME           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  MODIFIED_TIME          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE              CURRENT_TIMESTAMP,
  CONSTRAINT             USER_USER_ID_UC
    UNIQUE                 (USER_ID),
  CONSTRAINT             USER_EMAIL_UC
    UNIQUE                 (EMAIL),
  CONSTRAINT             USER_USERNAME_SUFFIX_UC
    UNIQUE                 (USERNAME, USERNAME_SUFFIX),
  CONSTRAINT             USER_GROUP_FK
    FOREIGN KEY            (GROUP_ID)
    REFERENCES             USER_GROUP (ID),
  CONSTRAINT             USER_ROLE_FK
    FOREIGN KEY            (ROLE_ID)
    REFERENCES             USER_ROLE(ID),
  CONSTRAINT             USER_STATUS_FK
    FOREIGN KEY            (STATUS_ID)
    REFERENCES             CURRENT_STATUS (ID),
  PRIMARY KEY            (ID)
);

# --- !Downs

SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS USER;
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;