CREATE ROLE np_app LOGIN PASSWORD :'app_password';
CREATE ROLE np_migrate LOGIN PASSWORD :'migration_password';
ALTER DATABASE npcomputers OWNER TO np_migrate;
GRANT ALL ON SCHEMA public TO np_migrate;
