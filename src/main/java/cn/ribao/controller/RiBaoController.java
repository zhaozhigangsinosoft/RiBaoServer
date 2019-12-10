package cn.ribao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    //从配置文件中读取项目日报更新存储路径
    @Value("${params.ribao.updatePath}")
    private String updatePath;
    
    //从配置文件中读取项目日报检出存储路径
    @Value("${params.ribao.checkOutPath}")
    private String checkOutPath;
    
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
        return riBaoService.readExcel(this.updatePath);
    }
    @RequestMapping(value = "/check")
    public void check() {
        taskService.checkRibao();
    }
}
