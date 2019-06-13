package cn.ribao.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.ribao.dao.RiBaoMapper;
import cn.ribao.po.RiBao;
import cn.ribao.service.RiBaoService;
import cn.util.FileUtils;
import cn.util.RegTest;

/**
 * 项目日报导入服务接口实现类
 * @author ZhaoZhigang
 *
 */
@Service
public class RiBaoServiceImpl implements RiBaoService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    //从配置文件中读取项目日报存储路径
    @Value("${params.ribao.filepath}")
    private String filePath;

    @Autowired
    private RiBaoMapper riBaoMapper;

    /**
     * 项目日报导入数据库方法
     * @return success/failed
     */
    @Override
    public String readExcel() {
        List<RiBao> riBaoList = null;
        String result = "success";
        try {
            //读取工作日报文件，转换为日报对象的集合
            riBaoList = this.readFile();
            if (riBaoList != null && !riBaoList.isEmpty()) {
                //删除数据库中的所有数据
                riBaoMapper.deleteAll();
                //全量插入最新读取的数据
                riBaoMapper.insertAll(riBaoList);
            }
        } catch (Exception e) {
            //如果发生异常，则返回失败
            result = "failed";
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 读取工作日报文件，转换为日报对象的集合
     * @return List<RiBao>
     * @throws Exception
     */
    private List<RiBao> readFile() throws Exception {
        List<RiBao> riBaoList = new ArrayList<RiBao>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            //使用非递归方式获取日报存储目录下的所有文件
            ArrayList<File> fileList = FileUtils.getFiles(this.filePath,false);
            //遍历所有文件
            for (Iterator<File> iterator = fileList.iterator(); 
                    iterator.hasNext();) {
                File file = iterator.next();
                //如果文件不是xlsx格式的项目日报文件，则扫描下一个文件
                if(!RegTest.match(file.getName(), "^2019项目日报表-.*\\.xlsx$")) {
                    continue;
                }
                
                logger.info("正在处理文件：" + file.getName());
                
                InputStream is = new FileInputStream(file);
                try {
                    ZipSecureFile.setMinInflateRatio(-1.0d);
                    @SuppressWarnings("resource")
                    XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);
                    //读取第一个sheet页
                    XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(0);
                    //从第三行开始遍历每一个单元格，将值存储到对象中
                    for (int i = 2; i <= xssfSheet.getLastRowNum(); i++) {
                        XSSFRow xssfRow = xssfSheet.getRow(i);
                        int index = 0;
                        XSSFCell name = xssfRow.getCell(index++);
                        XSSFCell workDate = xssfRow.getCell(index++);
                        XSSFCell taskType = xssfRow.getCell(index++);
                        XSSFCell taskNo = xssfRow.getCell(index++);
                        XSSFCell workContent = xssfRow.getCell(index++);
                        XSSFCell workHour = xssfRow.getCell(index++);
                        XSSFCell realHour = xssfRow.getCell(index++);
                        XSSFCell projectName = xssfRow.getCell(index++);
                        XSSFCell remark = xssfRow.getCell(index++);

                        RiBao riBao = new RiBao();
                        if (name == null) {
                            continue;
                        }
                        riBao.setName(name.toString());
                        if (workDate != null) {
                            double value = workDate.getNumericCellValue();
                            Date date = DateUtil.getJavaDate(value);
                            riBao.setWorkDate(sdf.parse(sdf.format(date)));
                        }
                        if (taskType != null) {
                            riBao.setTaskType(taskType.toString());
                        }
                        if (taskNo != null) {
                            riBao.setTaskNo(taskNo.toString());
                        }
                        if (workContent != null) {
                            riBao.setWorkContent(workContent.toString());
                        }
                        try {
                            riBao.setWorkHour(BigDecimal.valueOf(
                                    Double.parseDouble(workHour.toString())));
                        } catch (Exception e) {
                            riBao.setWorkHour(BigDecimal.valueOf(0));
                        }
                        try {
                            riBao.setRealHour(BigDecimal.valueOf(
                                    Double.parseDouble(realHour.toString())));
                        } catch (Exception e) {
                            riBao.setRealHour(BigDecimal.valueOf(0));
                        }
                        if (projectName != null) {
                            riBao.setProjectName(projectName.toString());
                        }
                        if (remark != null) {
                            riBao.setRemark(remark.toString());
                        }
                        //将本条日报数据存放到集合中
                        if(riBao.getWorkHour().intValue() != 0) {
                            riBaoList.add(riBao);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                } finally {
                    is.close();
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(),e);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
        return riBaoList;
    }
}
