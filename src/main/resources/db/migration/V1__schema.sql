create table app_user (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    full_name varchar(255) not null,
    role varchar(30) not null,
    enabled boolean not null default true,
    created_at timestamp not null default now()
);

create table client (
    id bigserial primary key,
    name varchar(255) not null,
    type varchar(30) not null,
    email varchar(255),
    phone varchar(50),
    address varchar(500),
    conflict_check_notes varchar(1000),
    status varchar(30) not null default 'ACTIVE',
    portal_user_id bigint references app_user(id),
    created_at timestamp not null default now()
);

create table practice_area (
    id bigserial primary key,
    name varchar(150) not null unique,
    description varchar(500),
    active boolean not null default true
);

create table matter (
    id bigserial primary key,
    title varchar(255) not null,
    description varchar(2000),
    client_id bigint not null references client(id),
    practice_area_id bigint references practice_area(id),
    assigned_attorney_id bigint references app_user(id),
    status varchar(30) not null default 'INTAKE',
    sensitivity_level varchar(20) not null default 'LOW',
    opened_date date,
    closed_date date,
    created_at timestamp not null default now()
);

create table document (
    id bigserial primary key,
    matter_id bigint not null references matter(id),
    title varchar(255) not null,
    category varchar(30) not null,
    privilege_level varchar(30) not null default 'NONE',
    visibility varchar(30) not null default 'INTERNAL_ONLY',
    uploaded_by_id bigint references app_user(id),
    created_at timestamp not null default now()
);

create table document_version (
    id bigserial primary key,
    document_id bigint not null references document(id),
    version_number int not null,
    original_filename varchar(500) not null,
    stored_filename varchar(500) not null,
    file_size bigint not null default 0,
    uploaded_by_id bigint references app_user(id),
    uploaded_at timestamp not null default now(),
    notes varchar(1000)
);

create table task (
    id bigserial primary key,
    matter_id bigint not null references matter(id),
    title varchar(255) not null,
    description varchar(2000),
    assignee_id bigint references app_user(id),
    priority varchar(20) not null default 'MEDIUM',
    status varchar(20) not null default 'OPEN',
    due_date date,
    created_at timestamp not null default now()
);

create table note (
    id bigserial primary key,
    matter_id bigint not null references matter(id),
    author_id bigint not null references app_user(id),
    body varchar(4000) not null,
    created_at timestamp not null default now()
);

create table client_message (
    id bigserial primary key,
    matter_id bigint not null references matter(id),
    sender_id bigint not null references app_user(id),
    body varchar(4000) not null,
    status varchar(20) not null default 'DRAFT',
    approved_by_id bigint references app_user(id),
    created_at timestamp not null default now()
);

create table deadline (
    id bigserial primary key,
    matter_id bigint not null references matter(id),
    title varchar(255) not null,
    due_date date not null,
    responsible_id bigint references app_user(id),
    completed boolean not null default false,
    reminder_sent_at timestamp,
    created_at timestamp not null default now()
);

create table audit_log (
    id bigserial primary key,
    actor_id bigint references app_user(id),
    action varchar(100) not null,
    entity_type varchar(100) not null,
    entity_id bigint,
    before_state varchar(2000),
    after_state varchar(2000),
    timestamp timestamp not null default now()
);

create table notification (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    message varchar(500) not null,
    link varchar(500),
    read boolean not null default false,
    created_at timestamp not null default now()
);

create table outbox (
    id bigserial primary key,
    to_email varchar(255) not null,
    subject varchar(255) not null,
    body varchar(4000) not null,
    type varchar(50) not null,
    created_at timestamp not null default now()
);

create table api_key (
    id bigserial primary key,
    name varchar(150) not null,
    key_value varchar(255) not null unique,
    created_by_id bigint references app_user(id),
    active boolean not null default true,
    created_at timestamp not null default now()
);

create table retention_setting (
    id bigint primary key,
    document_retention_days int not null default 2555,
    matter_retention_days int not null default 3650,
    deadline_reminder_days int not null default 3
);

create table password_reset_token (
    id bigserial primary key,
    user_id bigint not null references app_user(id),
    token varchar(255) not null unique,
    expires_at timestamp not null,
    used boolean not null default false,
    created_at timestamp not null default now()
);

create index idx_matter_status on matter(status);
create index idx_matter_client on matter(client_id);
create index idx_matter_attorney on matter(assigned_attorney_id);
create index idx_document_matter on document(matter_id);
create index idx_task_matter on task(matter_id);
create index idx_deadline_matter on deadline(matter_id);
create index idx_audit_entity on audit_log(entity_type, entity_id);
create index idx_notification_user on notification(user_id);
