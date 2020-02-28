create table user_info (
  id                serial primary key,
  username          varchar(100)  not null,
  password          varchar(100)  not null,
  token             varchar(63)   not null default '',
  token_create_time int        not null,
  head_img          varchar(256) not null default '',
  create_time       bigint        not null,
  rtmp_token        varchar(256) not null default ''
);

create table meeting_info
(
	id serial primary key,
	name varchar(63) not null,
	time bigint not null,
	info varchar(1023) not null,
	creator int not null,
	constraint meeting_info_fkey
		foreign key (creator) references user_info(id) on update cascade on delete cascade
);

-- 可以查看会议录像的人，这里面的uid不包括创建者
create table user_meeting
(
    id  serial primary key,
    mid int not null,
    uid int not null,
    audience int not null,  -- 是否是参会人，是（1），否（0）
    constraint user_meeting
        foreign key (uid) references user_info(id) on update cascade on delete cascade,
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade
);

create table meeting_record
(
    id serial primary key,
    mid int not null,
    v_path varchar(1023) not null,
    constraint meeting_record
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade
);

create table meeting_comment
(
    id serial primary key,
    mid int not null,
    author int not null,
    comment varchar(1023) not null,
    comment_time bigint not null,
    constraint meeting_comment
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade,
        foreign key (author) references user_info(id) on update cascade on delete cascade
);
