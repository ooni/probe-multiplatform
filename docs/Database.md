# Database

Both the Android and iOS platforms use a SQLite database for storing data.

Minimum SQLite version:
- Android 24 - V3.9.2
- iOS 14.0 - V3.32.3

## Schema

### TestDescriptor

Stores installed test descriptors: Run V2 descriptors and NewsMediaScan default descriptors.
It does not store OONI default descriptors, but it will in the future.

```sql
CREATE TABLE TestDescriptor (
    runId TEXT NOT NULL,
    revision INTEGER NOT NULL,
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
    auto_update INTEGER,
    revisions TEXT,
    PRIMARY KEY(`runId`, `revision`)
);
```

#### Notes
- We store old revisions in the database, so old results can still show the old descriptor
  information. But we clear `nettests field of old revisions, to save space, since we no longer
  need them.

### Result

A Result is an aggregation of measurements from a single TestDescriptor ran in a point in time.

```sql
CREATE TABLE Result(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    descriptor_name TEXT,
    descriptor_runId TEXT,
    descriptor_revision INTEGER,
    start_time INTEGER,
    is_viewed INTEGER,
    is_done INTEGER,
    data_usage_up INTEGER,
    data_usage_down INTEGER,
    failure_msg TEXT,
    network_id INTEGER,
    task_origin TEXT DEFAULT 'ooni-run',
    FOREIGN KEY(`network_id`) REFERENCES Network(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION,
    FOREIGN KEY(`descriptor_runId`, `descriptor_revision`) REFERENCES TestDescriptor(`runId`, `revision`) ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE INDEX idx_result_start_time ON Result (start_time);
CREATE INDEX idx_result_descriptor_name ON Result (descriptor_name);
CREATE INDEX idx_result_task_origin ON Result (task_origin);
```

#### Notes

* `descriptor_runId` and `descriptor_revision` are only present when the Result is related with an
  installed TestDescriptor. If its a Result from a default OONI descriptor, it's linked using the
  `descriptor_name` instead.
* `task_origin` possible values are:
  * `ooni-run` for manually user-started tests;
  * `autorun` for automatically started background tests.

### Network

Network details of one or more Results.

```sql
CREATE TABLE Network(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    network_name TEXT,
    asn TEXT,
    country_code TEXT,
    network_type TEXT
);
```

### Measurement

Stores the outcome of a single net-test from a TestDescriptor. If the net-test has more than
one input (`web_connectivity` tests usually), a Measurement is created for each input.

```sql
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

CREATE INDEX idx_measure_result_id ON Measurement (result_id);
CREATE INDEX idx_measure_start_time ON Measurement (start_time);
```

#### Notes

* We are no longer storing the full `test_keys` in the database. We only store the keys for
Performance net-tests is they are present.

### Url

Stores the Url details for a `web_connectivity` net-test Measurement.

```sql
CREATE TABLE Url(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT,
    category_code TEXT,
    country_code TEXT
);
```
