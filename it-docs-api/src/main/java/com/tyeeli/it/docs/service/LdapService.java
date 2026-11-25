package com.tyeeli.it.docs.service;

import java.util.List;

/**
 * LDAP 域账号认证（javax.naming，JDK 自带零依赖）。
 *
 * <p>配置来源：管理界面（it_config，见 {@link SysConfigService}）优先，.env 兜底。
 * 只做 bind 校验，不做复杂查询；LDAP 不可用时抛 IllegalStateException，
 * 由 {@link AuthService} 决定降级策略（默认 fail-open=false，只允许本地账号）。</p>
 *
 * <p>实现类：{@link com.tyeeli.it.docs.service.impl.LdapServiceImpl}</p>
 */
public interface LdapService {

    /**
     * LDAP 是否已配置（未配置时登录只走本地账号）。
     */
    boolean configured();

    /**
     * 校验域账号密码。
     *
     * @return 绑定成功返回用户信息
     * @throws com.tyeeli.it.docs.common.BizException 密码错误（401）
     * @throws IllegalStateException                  LDAP 服务不可用（由调用方决定降级）
     */
    LdapUser bind(String username, String password);

    /**
     * 管理界面「测试连接」：用给定配置尝试 bind，返回结果描述。
     */
    String test(SysConfigService.LdapConfig cfg, String testUser, String testPwd);

    /**
     * 搜索域用户（「从 AD 导入」，分页）：用配置里的查询账号 bind 后 SUBTREE 搜索 baseDn，
     * 按 sAMAccountName / displayName / cn / userPrincipalName 模糊匹配，排除禁用账号。
     * 关键词为空 = 列全部可用域用户。实现侧带短缓存，翻页不重复拉 AD。
     *
     * @param page     页码，从 1 开始
     * @param pageSize 每页条数（1~100）
     * @throws IllegalStateException 未配置 url/baseDn 或未配置查询账号（bindUser/bindPassword）
     */
    LdapPage searchUsers(String keyword, int page, int pageSize);

    /**
     * 查询用户所属 AD 组（RESTRICTED 空间 ACL 按域组授权用）。
     * 用配置里的查询账号 bind 后按 sAMAccountName 搜 memberOf，同时返回
     * 组 CN（如 IT-Admins）与完整 DN（如 CN=IT-Admins,OU=Groups,DC=corp,DC=com），
     * ACL 里两种写法都能命中。只含直接归属，不含嵌套组。
     * 实现带 5 分钟缓存；LDAP 不可用 / 未配查询账号时返回空列表（域组授权静默失效，
     * USER 直授不受影响），绝不抛异常拖垮权限判定链。
     *
     * @param username 登录名（sAMAccountName）
     */
    List<String> fetchGroups(String username);

    /**
     * 分页结果：items 当前页数据（按登录名排序）/ total 匹配总数。
     */
    class LdapPage {
        public final List<LdapUser> items;
        public final long total;

        public LdapPage(List<LdapUser> items, long total) {
            this.items = items;
            this.total = total;
        }
    }

    /**
     * LDAP 用户基础信息（首登自动建档 / 域用户导入用）。
     */
    class LdapUser {
        public final String username;
        public final String displayName;
        public final String email;

        public LdapUser(String username, String displayName, String email) {
            this.username = username;
            this.displayName = displayName;
            this.email = email;
        }
    }
}
