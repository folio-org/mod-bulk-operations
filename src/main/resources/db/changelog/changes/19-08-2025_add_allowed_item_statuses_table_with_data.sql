CREATE TABLE IF NOT EXISTS allowed_item_statuses (
  status varchar(255) PRIMARY KEY,
  allowed_statuses TEXT[]
);

insert into allowed_item_statuses(status, allowed_statuses) values ('Available',
  '{"Missing", "Withdrawn", "In process", "In process (non-requestable)", "Intellectual item", "Long missing", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Missing',
  '{"Available", "Withdrawn", "In process (non-requestable)", "Intellectual item", "Long missing", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Withdrawn',
  '{"Available", "Missing", "In process (non-requestable)", "Intellectual item", "Long missing", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('In process (non-requestable)',
  '{"Available", "Missing", "Withdrawn", "Intellectual item", "Long missing", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Intellectual item',
  '{"Available", "Missing", "Withdrawn", "In process (non-requestable)", "Long missing", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Long missing',
  '{"Available", "Missing", "Withdrawn", "In process (non-requestable)", "Intellectual item", "Restricted", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Restricted',
  '{"Available", "Missing", "Withdrawn", "In process (non-requestable)", "Intellectual item", "Long missing", "Unavailable", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Unavailable',
  '{"Available", "Missing", "Withdrawn", "In process (non-requestable)", "Intellectual item", "Long missing", "Restricted", "Unknown"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('Unknown',
  '{"Available", "Missing", "Withdrawn", "In process (non-requestable)", "Intellectual item", "Long missing", "Restricted", "Unavailable"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('In process', '{"Missing", "Withdrawn"}');
