ALTER TABLE usuario ADD column cognito_sub VARCHAR(255);
ALTER TABLE usuario ADD CONSTRAINT uq_usuario_cognito_sub UNIQUE (cognito_sub);