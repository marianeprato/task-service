CREATE TABLE tasks (
    task_id             UUID PRIMARY KEY,
    task_title          VARCHAR(100) NOT NULL,
    task_description    VARCHAR(500) NOT NULL,
    task_creation_date  DATE NOT NULL,
    task_due_date       DATE NOT NULL,
    priority            VARCHAR(20) NOT NULL
);
