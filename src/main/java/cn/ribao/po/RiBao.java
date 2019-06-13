package cn.ribao.po;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 日报对象PO类
 * @author ZhaoZhigang
 *
 */
public class RiBao {
    /** 主键ID     */
    private Integer id;
    /** 姓名     */
    private String name;
    /** 工作时间     */
    private Date workDate;
    /** 任务类型     */
    private String taskType;
    /** 任务号     */
    private String taskNo;
    /** 工作内容     */
    private String workContent;
    /** 投入工时     */
    private BigDecimal workHour;
    /** 核算工时     */
    private BigDecimal realHour;
    /** 项目名称     */
    private String projectName;
    /** 备注     */
    private String remark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public Date getWorkDate() {
        return workDate;
    }

    public void setWorkDate(Date workDate) {
        this.workDate = workDate;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType == null ? null : taskType.trim();
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo == null ? null : taskNo.trim();
    }

    public String getWorkContent() {
        return workContent;
    }

    public void setWorkContent(String workContent) {
        this.workContent = workContent == null ? null : workContent.trim();
    }

    public BigDecimal getWorkHour() {
        return workHour;
    }

    public void setWorkHour(BigDecimal workHour) {
        this.workHour = workHour;
    }

    public BigDecimal getRealHour() {
        return realHour;
    }

    public void setRealHour(BigDecimal realHour) {
        this.realHour = realHour;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName == null ? null : projectName.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }
}