# --- !Ups

CREATE TABLE USER_X_GROUP (
    ID                            BIGINT NOT NULL AUTO_INCREMENT,
    GROUP_ID                      BIGINT NOT NULL,
    USER_ID                       BIGINT NOT NULL,
    USER_PERMISSION_ID            INT UNSIGNED NOT NULL,
    CREATED_TIME                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_TIME                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT                    UXGROUP_USER_GROUP_UC
      UNIQUE                        (USER_ID, GROUP_ID),
    CONSTRAINT                    UXGROUP_GROUP_FK
      FOREIGN KEY                   (GROUP_ID)
      REFERENCES                    USER_GROUP (ID),
    CONSTRAINT                    UXGROUP_USER_FK
      FOREIGN KEY                   (USER_ID)
      REFERENCES                    USER (ID),
    CONSTRAINT                    UXGROUP_USER_PERMISSION_FK
      FOREIGN KEY                   (USER_PERMISSION_ID)
      REFERENCES                    USER_PERMISSION (ID),
    PRIMARY KEY                   (ID)
);

# --- !Downs

SET UNIQUE_CHECKS = 0;
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS USER_X_GROUP;
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
