package cn.ribao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.ribao.service.RiBaoService;
import cn.ribao.service.TaskService;

/**
 * 项目日报导入数据库控制器类
 * @author ZhaoZhigang
 *
 */
@RestController
@RequestMapping("/ribao")
public class RiBaoController {
    
    @Autowired
    RiBaoService riBaoService;
    
    @Autowired
    TaskService taskService;
    
    /**
     * 项目日报导入数据库请求方法
     * @return
     */
    @RequestMapping(value = "/read")
    public String readExcel() {
        return riBaoService.readExcel();
    }
    @RequestMapping(value = "/check")
    public void check() {
        taskService.checkRibao();
    }
}
