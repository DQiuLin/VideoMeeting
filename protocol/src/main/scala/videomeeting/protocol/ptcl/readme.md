# 接口说明

meetingManager接口：/videomeeting/meetingManager

## webClient
### 1 账号注册
- 协议：Http post
- url：user/signUp
- 接收：SignUp(username: String, password: String)
- 返回：SignUpRsp
- 备注：此处的密码应是加密后的密码

### 2 上传头像与个人资料管理
个人资料管理：待添加/修改

上传头像：
- 协议：Http post
- url：file/uploadFile
- 用法未知

### 3 参会者有权查看会议录像以及会议信息
我创建的：
- 协议：Http get
- url：meeting/initiate
- 参数：uid：登录用户id
- 返回：InitiateRsp(Option[List[InitiateMeetingInfo]])

我参加的：
- 协议：Http get
- url：meeting/attend
- 参数：uid：登录用户id
- 返回：InitiateRsp(Option[List[AttendMeetingInfo]])

### 4 会议发起者可以邀请其他未参会用户查看会议录像
- 协议：Http post
- url：meeting/invite
- 接收：AddInvite(invite: Int, meetingId: Int, invited: Int)
- 返回：AddInviteRsp
- 参数说明：
    - invite：邀请者id，即登陆者，用于权限验证
    - meetingId：会议id
    - invited：被邀请者id
    
### 5 能查看会议录像的用户能在会议录像下进行评论
创建与参会同3

邀请我的：
- 协议：Http get
- url：meeting/invited
- 参数：uid：登录用户id
- 返回：InitiateRsp(Option[List[InviteMeetingInfo]])

评论：
- 协议：Http post
- url：meeting/comment
- 接收：Comment(meetingId: Int, userId: Int, comment: String)
- 返回：CommentRsp
- 参数说明：
    - meetingId：会议id
    - userId：发布评论者id
    - comment：评论
- 备注：这部分后端暂未鉴权

### 6 会议发起者可以对邀请来的用户和评论进行管理
移除被邀请者：
- 协议：Http post
- url：meeting/remove
- 接收：Remove(meetingId: Int, userId: Int)
- 返回：RemoveRsp

删除评论：
- 协议：Http post
- url：meeting/delete
- 接收：Delete(id: Int)
- 返回：DeleteRsp
- 参数说明：
    - id：评论id
