# Database

Both the Android and iOS platforms use a SQLite database for storing data.

Minimum SQLite version:
- Android 24 - V3.9.2
- iOS 14.0 - V3.32.3

## Schema

```
CREATE TABLE Measurement(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_name TEXT,
    start_time INTEGER,
    runtime REAL,
    is_done INTEGER,
    is_uploaded INTEGER,
    is_failed INTEGER,
    failure_msg TEXT,
    is_upload_failed INTEGER,
    upload_failure_msg TEXT,
    is_rerun INTEGER,
    report_id TEXT,
    is_anomaly INTEGER,
    test_keys TEXT,
    url_id INTEGER,
    result_id INTEGER,
    rerun_network TEXT,
    FOREIGN KEY(`url_id`) REFERENCES Url(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
    FOREIGN KEY(`result_id`) REFERENCES Result(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE Result(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    test_group_name TEXT,
    start_time INTEGER,
    is_viewed INTEGER,
    is_done INTEGER,
    data_usage_up INTEGER,
    data_usage_down INTEGER,
    failure_msg TEXT,
    network_id INTEGER,
    descriptor_runId INTEGER REFERENCES TestDescriptor (`runId`),
    task_origin TEXT DEFAULT 'ooni-run',
    FOREIGN KEY(`network_id`) REFERENCES Network(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE Network(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    network_name TEXT,
    ip TEXT,
    asn TEXT,
    country_code TEXT,
    network_type TEXT
);

CREATE TABLE Url(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT,
    category_code TEXT,
    country_code TEXT
);

CREATE TABLE TestDescriptor (
  runId INTEGER,
  name TEXT,
  short_description TEXT,
  description TEXT,
  author TEXT,
  nettests TEXT,
  name_intl TEXT,
  short_description_intl TEXT,
  description_intl TEXT,
  icon TEXT,
  color TEXT,
  animation TEXT,
  expiration_date INTEGER,
  date_created INTEGER,
  date_updated INTEGER,
  revision TEXT,
  previous_revision TEXT,
  is_expired INTEGER,
  auto_update INTEGER,
  PRIMARY KEY(`runId`)
);
```
