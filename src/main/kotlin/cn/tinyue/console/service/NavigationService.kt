package cn.tinyue.console.service

import cn.tinyue.console.model.Comment

/**
 * 注释结构导航服务接口
 */
interface NavigationService {
    /**
     * 导航到指定注释
     * @param comment 目标注释
     */
    fun navigateToComment(comment: Comment)

    /**
     * 获取所有注释
     * @return 注释列表
     */
    fun getComments(): List<Comment>
} 