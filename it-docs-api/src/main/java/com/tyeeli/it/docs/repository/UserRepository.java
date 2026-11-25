package com.tyeeli.it.docs.repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tyeeli.it.docs.repository.entity.User;

import java.util.Date;

/**
 * 用户数据访问。it_user 同时承载域账号（LDAP）与本地账号，用户名全局唯一。
 *
 * <p>只做数据访问，不含业务判断：不校验权限、不抛业务异常、不写审计。
 * 查询条件（QueryWrapper / SQL）全部内聚在实现类里，上层只表达意图。</p>
 */
public interface UserRepository {
    /**
     * 按登录名精确查询。登录判定、首登建档、重名校验都走这里，不做模糊匹配。
     */
    User findByUsername(String username);

    /**
     * 按主键查询，不判断状态（状态校验属于业务规则，留给 Service）。
     */
    User findById(Long id);

    /**
     * 新增用户。主键自增，回填到入参对象上。
     */
    void insert(User user);

    /**
     * 按主键全字段更新，调用方负责先查后改。
     */
    void update(User user);

    /**
     * 分页查询。keyword 同时模糊匹配 username 与 display_name；authSource 精确过滤 LOCAL/LDAP。按创建时间倒序。
     */
    Page<User> page(String keyword, String authSource, int pageNo, int pageSize);

    /**
     * 登录成功：只更新最后登录时间，避免整行覆盖并发修改的角色/状态。
     */
    void touchLoginSuccess(String username, Date now);
}
