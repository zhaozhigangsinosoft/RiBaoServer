package cn.ribao.dao;

import java.util.List;

import cn.ribao.po.RiBao;

public interface RiBaoMapper {
    /**
     * 插入单个日报记录
     * @param record
     * @return
     */
    int insert(RiBao record);

    /**
     * 插入单个日报记录，仅选择非字字段进行插入
     * @param record
     * @return
     */
    int insertSelective(RiBao record);

    /**
     * 删除所有日报记录
     */
    void deleteAll();

    /**
     * 一次性插入集合中所有日报记录
     * @param riBaoList
     */
    void insertAll(List<RiBao> riBaoList);
}