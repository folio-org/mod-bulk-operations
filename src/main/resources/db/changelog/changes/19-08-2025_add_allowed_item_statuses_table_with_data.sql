CREATE TABLE IF NOT EXISTS allowed_item_statuses (
  status varchar(255) PRIMARY KEY,
  allowed_statuses TEXT[]
);

insert into allowed_item_statuses(status, allowed_statuses) values ('AVAILABLE',
  '{"MISSING", "WITHDRAWN", "IN_PROCESS", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('MISSING',
  '{"AVAILABLE", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('WITHDRAWN',
  '{"AVAILABLE", "MISSING", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('IN_PROCESS_NON_REQUESTABLE_',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('INTELLECTUAL_ITEM',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('LONG_MISSING',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "RESTRICTED", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('RESTRICTED',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "UNAVAILABLE", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('UNAVAILABLE',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNKNOWN"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('UNKNOWN',
  '{"AVAILABLE", "MISSING", "WITHDRAWN", "IN_PROCESS_NON_REQUESTABLE_", "INTELLECTUAL_ITEM", "LONG_MISSING", "RESTRICTED", "UNAVAILABLE"}');
insert into allowed_item_statuses(status, allowed_statuses) values ('IN_PROCESS', '{"MISSING", "WITHDRAWN"}');
