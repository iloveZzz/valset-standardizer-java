package com.yss.valset.transfer.domain.model.config;

/**
 * 文件收发分拣模块的配置键常量。
 */
public final class TransferConfigKeys {

    public static final String FILE_TYPE = "fileType";

    private TransferConfigKeys() {
    }

    /** 本地目录来源的目录路径。 */
    public static final String DIRECTORY = "directory";
    /** 是否递归扫描子目录。 */
    public static final String RECURSIVE = "recursive";
    /** 扫描或处理数量上限。 */
    public static final String LIMIT = "limit";
    /** 是否包含隐藏文件。 */
    public static final String INCLUDE_HIDDEN = "includeHidden";

    /** 邮件或服务连接协议。 */
    public static final String PROTOCOL = "protocol";
    /** 连接主机地址。 */
    public static final String HOST = "host";
    /** 连接端口。 */
    public static final String PORT = "port";
    /** 登录用户名。 */
    public static final String USERNAME = "username";
    /** 登录密码。 */
    public static final String PASSWORD = "password";
    /** 邮件文件夹名称。 */
    public static final String FOLDER = "folder";
    /** 是否启用 SSL。 */
    public static final String SSL = "ssl";
    /** 是否启用 STARTTLS。 */
    public static final String START_TLS = "startTls";

    /** S3 桶名称。 */
    public static final String BUCKET = "bucket";
    /** S3 区域。 */
    public static final String REGION = "region";
    /** S3 服务端点地址。 */
    public static final String ENDPOINT_URL = "endpointUrl";
    /** S3 访问密钥标识。 */
    public static final String ACCESS_KEY = "accessKey";
    /** S3 访问密钥。 */
    public static final String SECRET_KEY = "secretKey";
    /** 是否使用路径样式访问 S3。 */
    public static final String USE_PATH_STYLE = "usePathStyle";
    /** S3 对象前缀。 */
    public static final String PREFIX = "prefix";
    /** S3 对象前缀别名。 */
    public static final String KEY_PREFIX = "keyPrefix";

    /** SFTP 私钥文件路径。 */
    public static final String PRIVATE_KEY_PATH = "privateKeyPath";
    /** SFTP 私钥口令。 */
    public static final String PASSPHRASE = "passphrase";
    /** SFTP 远程目录。 */
    public static final String REMOTE_DIR = "remoteDir";
    /** 是否严格校验主机密钥。 */
    public static final String STRICT_HOST_KEY_CHECKING = "strictHostKeyChecking";
    /** 连接超时时间，单位毫秒。 */
    public static final String CONNECT_TIMEOUT_MILLIS = "connectTimeoutMillis";
    /** 通道超时时间，单位毫秒。 */
    public static final String CHANNEL_TIMEOUT_MILLIS = "channelTimeoutMillis";

    /** 最大重试次数。 */
    public static final String MAX_RETRY_COUNT = "maxRetryCount";
    /** 重试间隔秒数。 */
    public static final String RETRY_DELAY_SECONDS = "retryDelaySeconds";
    /** 目标路径。 */
    public static final String TARGET_PATH = "targetPath";
    /** 规则执行结果说明。 */
    public static final String RULE_MESSAGE = "ruleMessage";
    /** 探测得到的文件类型。 */
    public static final String PROBE_DETECTED_TYPE = "probeDetectedType";
    /** 探测插件附加属性。 */
    public static final String PROBE_ATTRIBUTES = "probeAttributes";
    /** 文件是否已经完成探测。 */
    public static final String PROBE_DETECTED = "probeDetected";
    /** 路由分组策略。 */
    public static final String GROUP_STRATEGY = "groupStrategy";
    /** 路由分组字段。 */
    public static final String GROUP_FIELD = "groupField";
    /** 路由分组表达式。 */
    public static final String GROUP_EXPRESSION = "groupExpression";
    /** 路由分组键。 */
    public static final String GROUP_KEY = "groupKey";
    /** 路由分组名称。 */
    public static final String GROUP_NAME = "groupName";
    /** 路由分组目标映射。 */
    public static final String GROUP_TARGET_MAPPING = "groupTargetMapping";
    /** 分组路由默认目标编码。 */
    public static final String DEFAULT_TARGET_CODE = "defaultTargetCode";
    /** 目标标识。 */
    public static final String TARGET_ID = "targetId";
    /** 目标编码。 */
    public static final String TARGET_CODE = "targetCode";
    /** 目标名称。 */
    public static final String TARGET_NAME = "targetName";
    /** 目标类型。 */
    public static final String TARGET_TYPE = "targetType";
    /** 目标路径模板。 */
    public static final String TARGET_PATH_TEMPLATE = "targetPathTemplate";
    /** 重命名规则模板。 */
    public static final String RENAME_PATTERN = "renamePattern";
    /** 邮件发件人。 */
    public static final String FROM = "from";
    /** 邮件收件人。 */
    public static final String TO = "to";
    /** 邮件抄送人。 */
    public static final String CC = "cc";
    /** 邮件密送人。 */
    public static final String BCC = "bcc";
    /** 邮件主题模板。 */
    public static final String SUBJECT_TEMPLATE = "subjectTemplate";
    /** 邮件正文模板。 */
    public static final String BODY_TEMPLATE = "bodyTemplate";
    /** 是否转发原始邮件正文。 */
    public static final String FORWARD_MAIL_CONTENT = "forwardMailContent";
    /** 是否保留原始发件人。 */
    public static final String FORWARD_ORIGINAL_SENDER = "forwardOriginalSender";
    /** 是否需要认证。 */
    public static final String AUTH = "auth";
    /** 请求超时时间，单位毫秒。 */
    public static final String TIMEOUT_MILLIS = "timeoutMillis";

    /** 文件系统空间标识。 */
    public static final String PARENT_ID = "parentId";
    /** 文件存储配置标识。 */
    public static final String STORAGE_SETTING_ID = "storageSettingId";
    /** 分片大小，单位字节。 */
    public static final String CHUNK_SIZE = "chunkSize";

    /** 来源类型。 */
    public static final String SOURCE_TYPE = "sourceType";
    /** 来源编码。 */
    public static final String SOURCE_CODE = "sourceCode";
    /** 来源引用标识。 */
    public static final String SOURCE_REF = "sourceRef";
    /** 触发类型。 */
    public static final String TRIGGER_TYPE = "triggerType";
    /** 文件内容指纹。 */
    public static final String CONTENT_FINGERPRINT = "contentFingerprint";

    /** 邮件ID。 */
    public static final String MAIL_ID = "mailId";
    /** 邮件发件人地址。 */
    public static final String MAIL_FROM = "mailFrom";
    /** 邮件收件人地址。 */
    public static final String MAIL_TO = "mailTo";
    /** 邮件抄送地址。 */
    public static final String MAIL_CC = "mailCc";
    /** 邮件密送地址。 */
    public static final String MAIL_BCC = "mailBcc";
    /** 邮件主题。 */
    public static final String MAIL_SUBJECT = "mailSubject";
    /** 邮件正文。 */
    public static final String MAIL_BODY = "mailBody";
    /** 邮件协议。 */
    public static final String MAIL_PROTOCOL = "mailProtocol";
    /** 邮件文件夹。 */
    public static final String MAIL_FOLDER = "mailFolder";
    /** 邮件附件索引。 */
    public static final String ATTACHMENT_INDEX = "attachmentIndex";
    /** 邮件附件名称。 */
    public static final String ATTACHMENT_NAME = "attachmentName";
    /** 邮件附件内容类型。 */
    public static final String ATTACHMENT_CONTENT_TYPE = "attachmentContentType";
    /** 邮件附件大小。 */
    public static final String ATTACHMENT_SIZE = "attachmentSize";
    /** 邮件附件总数。 */
    public static final String ATTACHMENT_COUNT = "attachmentCount";
    /** 邮件附件名称列表。 */
    public static final String ATTACHMENT_NAMES = "attachmentNames";
    /** 来源处理去重键。 */
    public static final String CHECKPOINT_KEY = "checkpointKey";
    /** 来源处理引用标识。 */
    public static final String CHECKPOINT_REF = "checkpointRef";
    /** 来源处理名称。 */
    public static final String CHECKPOINT_NAME = "checkpointName";
    /** 来源处理指纹。 */
    public static final String CHECKPOINT_FINGERPRINT = "checkpointFingerprint";
    /** 来源扫描游标。 */
    public static final String CHECKPOINT_SCAN_CURSOR = "scanCursor";

    /** 远程路径。 */
    public static final String REMOTE_PATH = "remotePath";
    /** 对象键。 */
    public static final String OBJECT_KEY = "objectKey";
    /** ETag。 */
    public static final String E_TAG = "etag";

    /** 最大重试次数默认值。 */
    public static final String MAX_RETRY_COUNT_DEFAULT = "3";
    /** 重试间隔默认值，单位秒。 */
    public static final String RETRY_DELAY_SECONDS_DEFAULT = "60";
}
