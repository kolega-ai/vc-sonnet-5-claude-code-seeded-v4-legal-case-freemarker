alter table client add column national_id varchar(64);

alter table api_key add column callback_url varchar(500);

alter table client_message add column attachment_name varchar(500);
alter table client_message add column attachment_stored_name varchar(500);
