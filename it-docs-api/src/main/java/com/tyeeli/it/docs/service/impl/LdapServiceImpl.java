package com.tyeeli.it.docs.service.impl;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyeeli.it.docs.service.LdapService;
import com.tyeeli.it.docs.service.SysConfigService;
import com.tyeeli.it.docs.service.SysConfigService.LdapConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LDAP 域账号认证（javax.naming，JDK 自带零依赖）。
 * 配置来源：管理界面（it_config 表，SysConfigService）优先，.env 兜底。
 * 只做 bind 校验，不做复杂查询；LDAP 不可用时抛异常，由 AuthService 决定降级策略。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LdapServiceImpl implements LdapService {

    private final SysConfigService sysConfigService;

    /**
     * LDAP 是否已配置（未配置时登录只走本地账号）
     */
    @Override
    public boolean configured() {
        return sysConfigService.getLdapConfig().configured;
    }

    /**
     * 用户登录 bind：依次尝试
     * userDnPattern（uid={0},ou=people,DC=corp,DC=com）
     * upnPattern   （{0}@corp.com，Active Directory 常用）
     * 两种模板都失败 → 返回 null（密码错误）；服务连不上 → 抛 IllegalStateException。
     */
    @Override
    public LdapUser bind(String username, String password) {
        LdapConfig cfg = sysConfigService.getLdapConfig();
        return doBind(cfg, username, password);
    }

    LdapUser doBind(LdapConfig cfg, String username, String password) {
        if (!cfg.configured) {
            throw new IllegalStateException("LDAP 未配置（url / baseDn 为空）");
        }
        if (StrUtil.isNotBlank(cfg.userDnPattern)) {
            String userDn = cfg.userDnPattern.replace("{0}", username);
            if (tryBind(cfg.url, userDn, password)) {
                return new LdapUser(username, username, null);
            }
        }
        if (StrUtil.isNotBlank(cfg.upnPattern)) {
            String upn = cfg.upnPattern.replace("{0}", username);
            if (tryBind(cfg.url, upn, password)) {
                return new LdapUser(username, username, null);
            }
        }
        return null;
    }

    /**
     * 管理界面「测试连接」：
     * - 填了测试账号密码 → 真实 bind，成功即验证配置+账号全链路；
     * - 没填 → 用假 DN 探测连通性：能收到「认证失败」说明服务器可达、配置格式可解析，
     * 连不上/超时才是失败。
     */
    @Override
    public String test(LdapConfig cfg, String testUser, String testPwd) {
        if (StrUtil.isNotBlank(testUser)
                && testPwd != null && !testPwd.isEmpty()) {
            LdapUser u = doBind(cfg, testUser.trim(), testPwd);
            if (u != null) {
                return "连接成功：账号 " + testUser.trim() + " 域认证通过，配置可用";
            }
            throw new IllegalStateException("服务器可达，但测试账号认证失败——请核对 DN/UPN 模板与账号密码");
        }
        // 仅连通性探测
        if (StrUtil.isBlank(cfg.userDnPattern) && StrUtil.isBlank(cfg.upnPattern)) {
            throw new IllegalStateException("LDAP 未配置认证模板：userDnPattern 与 upnPattern 至少填一个");
        }
        String probeDn = StrUtil.isNotBlank(cfg.userDnPattern)
                ? cfg.userDnPattern.replace("{0}", "__itdocs_probe__")
                : cfg.upnPattern.replace("{0}", "__itdocs_probe__");
        tryBind(cfg.url, probeDn, "__itdocs_probe_wrong_pwd__");
        // 到这里说明收到了 AuthenticationException（账号不存在/密码错）→ 服务器可达
        return "连接成功：LDAP 服务器可达（仅连通性探测；填测试账号可进一步验证认证）";
    }

    private boolean tryBind(String url, String dn, String password) {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, password);
        // 超时，LDAP 挂了别拖死登录线程
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");
        // AD 跨域/跨分区时跟随引用，避免 Unprocessed Continuation Reference
        env.put(Context.REFERRAL, "follow");
        DirContext ctx = null;
        try {
            ctx = new InitialDirContext(env);
            return true;
        } catch (javax.naming.AuthenticationException e) {
            return false;
        } catch (Exception e) {
            // 连不上 / 超时：LDAP 服务异常，不是密码错
            log.error("LDAP bind 异常（服务不可用？）dn={}, err={}", dn, e.getMessage());
            throw new IllegalStateException("LDAP 服务不可用: " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * 「从 AD 导入」（分页）：查询账号 bind 后 SUBTREE 搜索，按登录名/姓名/UPN 模糊匹配。
     * 过滤：objectCategory=person（排除计算机账号）、未禁用（userAccountControl 第 2 位）。
     * 结果全量拉取后按登录名排序，内存切片分页；60 秒缓存，翻页不重复请求 AD。
     */
    @Override
    public LdapPage searchUsers(String keyword, int page, int pageSize) {
        LdapConfig cfg = sysConfigService.getLdapConfig();
        if (!cfg.configured) {
            throw new IllegalStateException("LDAP 未配置（url / baseDn 为空），请先在系统设置保存");
        }
        if (StrUtil.isBlank(cfg.bindUser) || StrUtil.isBlank(cfg.bindPassword)) {
            throw new IllegalStateException("未配置 LDAP 查询账号（系统设置 → 查询账号），无法搜索域用户");
        }
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1 || pageSize > 100) {
            pageSize = 20;
        }
        String k = keyword == null ? "" : keyword.trim();
        List<LdapUser> all = cachedList(cfg, k);
        int total = all.size();
        int from = (page - 1) * pageSize;
        List<LdapUser> items = from >= total
                ? Collections.<LdapUser>emptyList()
                : new ArrayList<LdapUser>(all.subList(from, Math.min(from + pageSize, total)));
        return new LdapPage(items, total);
    }

    /** 全量列表缓存（同一关键词 60 秒内翻页直接命中） */
    private volatile List<LdapUser> cacheList;
    private volatile String cacheKeyword = "";
    private volatile long cacheAt = 0L;
    private static final long CACHE_TTL_MS = 60_000L;

    private List<LdapUser> cachedList(LdapConfig cfg, String k) {
        synchronized (this) {
            if (cacheAt > 0 && System.currentTimeMillis() - cacheAt < CACHE_TTL_MS
                    && k.equals(cacheKeyword) && cacheList != null) {
                return cacheList;
            }
        }
        List<LdapUser> fresh = fetchAll(cfg, k);
        synchronized (this) {
            cacheList = fresh;
            cacheKeyword = k;
            cacheAt = System.currentTimeMillis();
        }
        return fresh;
    }

    /**
     * 实际拉取：AD 服务器单次搜索有 1000 条上限（MaxPageSize），
     * 必须用 Paged Results Control 分批把匹配结果全部拉回来。
     */
    private List<LdapUser> fetchAll(LdapConfig cfg, String k) {
        // AD：userAccountControl bit 1 (0x2) = 禁用，1.2.840.113556.1.4.803 是按位与的 OID
        String filter;
        if (k.isEmpty()) {
            filter = "(&(objectCategory=person)(objectClass=user)"
                    + "(!(userAccountControl:1.2.840.113556.1.4.803:=2)))";
        } else {
            String esc = ldapFilterEscape(k);
            filter = "(&(objectCategory=person)(objectClass=user)"
                    + "(!(userAccountControl:1.2.840.113556.1.4.803:=2))"
                    + "(|(sAMAccountName=*" + esc + "*)(displayName=*" + esc + "*)"
                    + "(cn=*" + esc + "*)(userPrincipalName=*" + esc + "*)))";
        }

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, cfg.url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, cfg.bindUser);
        env.put(Context.SECURITY_CREDENTIALS, cfg.bindPassword);
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "15000");
        // AD 跨分区/跨域会返回 referral（续传引用），默认 ignore 会在搜索时抛
        // "Unprocessed Continuation Reference(s)"，改成 follow 自动跟随引用
        env.put(Context.REFERRAL, "follow");

        final int maxEnum = 10000;
        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setTimeLimit(15000);
            sc.setReturningAttributes(new String[]{"sAMAccountName", "displayName", "cn", "mail", "userPrincipalName"});

            Map<String, LdapUser> uniq = new LinkedHashMap<String, LdapUser>();
            byte[] cookie = null;
            do {
                ctx.setRequestControls(new Control[]{new PagedResultsControl(500, cookie, Control.CRITICAL)});
                NamingEnumeration<SearchResult> rs = ctx.search(cfg.baseDn, filter, sc);
                try {
                    while (rs.hasMore()) {
                        SearchResult r = rs.next();
                        String uname = attr(r, "sAMAccountName");
                        if (StrUtil.isBlank(uname)) {
                            continue;
                        }
                        String displayName = StrUtil.blankToDefault(attr(r, "displayName"), attr(r, "cn"));
                        String email = StrUtil.blankToDefault(attr(r, "mail"), attr(r, "userPrincipalName"));
                        uniq.put(uname.toLowerCase(), new LdapUser(uname, displayName, email));
                        if (uniq.size() >= maxEnum) {
                            break;
                        }
                    }
                } catch (javax.naming.PartialResultException pre) {
                    // 仍有引用没被 follow（比如引用指向不可达的域控制器）：
                    // 用已搜到的部分结果，不整体报错
                    log.warn("LDAP 搜索遇到未处理的续传引用，返回部分结果（{} 条）：{}",
                            uniq.size(), pre.getMessage());
                }
                if (uniq.size() >= maxEnum) {
                    break;
                }
                cookie = null;
                Control[] resp = ctx.getResponseControls();
                if (resp != null) {
                    for (Control c : resp) {
                        if (c instanceof PagedResultsResponseControl) {
                            cookie = ((PagedResultsResponseControl) c).getCookie();
                        }
                    }
                }
            } while (cookie != null && cookie.length > 0);

            List<LdapUser> list = new ArrayList<LdapUser>(uniq.values());
            Collections.sort(list, new Comparator<LdapUser>() {
                @Override
                public int compare(LdapUser a, LdapUser b) {
                    return a.username.compareToIgnoreCase(b.username);
                }
            });
            return list;
        } catch (javax.naming.AuthenticationException e) {
            throw new IllegalStateException("LDAP 查询账号认证失败，请核对系统设置里的查询账号与密码");
        } catch (Exception e) {
            log.error("LDAP 搜索异常 keyword={}, err={}", k, e.getMessage());
            throw new IllegalStateException("LDAP 搜索失败: " + e.getMessage(), e);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    // ---------------- 域组查询（RESTRICTED 空间 ACL 用） ----------------

    /** 用户 -> AD 组列表，5 分钟缓存（与 SpaceService 的 ACL 缓存同 TTL） */
    private final Cache<String, List<String>> groupCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES).maximumSize(1000).build();

    @Override
    public List<String> fetchGroups(String username) {
        if (StrUtil.isBlank(username)) {
            return Collections.emptyList();
        }
        String key = username.trim().toLowerCase();
        List<String> cached = groupCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        List<String> groups = doFetchGroups(username.trim());
        groupCache.put(key, groups);
        return groups;
    }

    /**
     * 查询账号 bind 后按 sAMAccountName 精确搜该用户，读 memberOf（多值）。
     * 每个 DN 同时收「完整 DN」和「CN」两种形式，ACL 里填哪种都命中。
     * 任何异常（未配查询账号 / LDAP 不可用 / 用户无 memberOf）都返回空列表，
     * 绝不向上抛——权限链上域组授权失效时还有 USER 直授兜底。
     */
    private List<String> doFetchGroups(String username) {
        LdapConfig cfg = sysConfigService.getLdapConfig();
        if (!cfg.configured || StrUtil.isBlank(cfg.bindUser) || StrUtil.isBlank(cfg.bindPassword)) {
            return Collections.emptyList();
        }
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, cfg.url);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, cfg.bindUser);
        env.put(Context.SECURITY_CREDENTIALS, cfg.bindPassword);
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");
        env.put(Context.REFERRAL, "follow");

        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setTimeLimit(10000);
            sc.setReturningAttributes(new String[]{"memberOf"});
            NamingEnumeration<SearchResult> rs = ctx.search(cfg.baseDn,
                    "(sAMAccountName=" + ldapFilterEscape(username) + ")", sc);
            List<String> out = new ArrayList<String>();
            try {
                while (rs.hasMore()) {
                    SearchResult r = rs.next();
                    javax.naming.directory.Attribute a = r.getAttributes().get("memberOf");
                    if (a == null) {
                        continue;
                    }
                    for (int i = 0; i < a.size(); i++) {
                        Object v = a.get(i);
                        if (v == null) {
                            continue;
                        }
                        String dn = String.valueOf(v).trim();
                        if (dn.isEmpty() || out.contains(dn)) {
                            continue;
                        }
                        out.add(dn);
                        String cn = cnOfDn(dn);
                        if (cn != null && !out.contains(cn)) {
                            out.add(cn);
                        }
                    }
                }
            } catch (javax.naming.PartialResultException pre) {
                log.warn("LDAP 组查询遇到续传引用，返回部分结果：{}", pre.getMessage());
            }
            return out;
        } catch (Exception e) {
            // fail-closed：域组授权静默失效（USER 直授不受影响），5 分钟后自动重试
            log.warn("LDAP 组查询失败（域组授权暂不生效）username={}, err={}", username, e.getMessage());
            return Collections.emptyList();
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /** 从 "CN=IT-Admins,OU=Groups,DC=corp,DC=com" 提取 "IT-Admins"；不是 DN 或无 CN 段返回 null */
    private String cnOfDn(String dn) {
        if (!dn.regionMatches(true, 0, "CN=", 0, 3)) {
            return null;
        }
        int end = dn.indexOf(',');
        if (end <= 3) {
            return null;
        }
        String cn = dn.substring(3, end).trim();
        // 转义值（CN=IT\,Admins）还原
        return cn.replace("\\,", ",").replace("\\\\", "\\");
    }

    private String attr(SearchResult r, String name) {        try {
            javax.naming.directory.Attribute a = r.getAttributes().get(name);
            return a == null ? null : String.valueOf(a.get());
        } catch (Exception e) {
            return null;
        }
    }

    /** LDAP filter 元字符转义：* ( ) \ NUL */
    private String ldapFilterEscape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\5c"); break;
                case '*':  sb.append("\\2a"); break;
                case '(':  sb.append("\\28"); break;
                case ')':  sb.append("\\29"); break;
                case '\u0000': sb.append("\\00"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

}
