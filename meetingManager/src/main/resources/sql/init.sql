create table user_info
(
	id int auto_increment,
	username varchar(63) not null,
	password varchar(63) not null,
	head_img varchar(256) not null default '',
	created long not null,
	constraint user_info
		primary key (id)
);

create table meeting_info
(
	id int auto_increment,
	name varchar(63) not null,
	time long not null,
	info varchar(1023) not null,
	creator int not null,
	constraint meeting_info
		primary key (id),
		foreign key (creator) references user_info(id) on update cascade on delete cascade
);

-- 可以查看会议录像的人，这里面的uid不包括创建者
create table user_meeting
(
    id  int auto_increment,
    mid int not null,
    uid int not null,
    audience int not null,  -- 是否是参会人，是（1），否（0）
    constraint user_msg
        primary key (id),
        foreign key (uid) references user_info(id) on update cascade on delete cascade,
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade
);

create table meeting_record
(
    id int auto_increment,
    mid int not null,
    v_path varchar(1023) not null,
    r_path varchar(1023),   -- 会议记录文件路径（可选）
    constraint meeting_record
        primary key (id),
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade
);

create table meeting_comment
(
    id int auto_increment,
    mid int not null,
    author int not null,
    comment varchar(1023) not null,
    comment_time long not null,
    constraint meeting_comment
        primary key (id),
        foreign key (mid) references meeting_info(id) on update cascade on delete cascade,
        foreign key (author) references user_info(id) on update cascade on delete cascade
);
