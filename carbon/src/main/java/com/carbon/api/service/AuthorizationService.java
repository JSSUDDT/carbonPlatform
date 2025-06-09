package com.carbon.api.service;

/**
 * 授权服务接口
 * 用于处理权限相关的逻辑
 */
public interface AuthorizationService {
    
    /**
     * 检查当前用户是否有访问指定厂商资源的权限
     * 
     * @param vendorId 厂商ID
     * @return 是否有权限
     */
    boolean hasAccessToVendor(String vendorId);
    
    /**
     * 检查当前用户是否拥有管理员权限
     * 
     * @return 是否为管理员
     */
    boolean isCurrentUserAdmin();
    
    /**
     * 获取当前登录用户的厂商ID
     * 
     * @return 当前用户的厂商ID
     */
    String getCurrentVendorId();
} 