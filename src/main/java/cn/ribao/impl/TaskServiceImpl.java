package cn.ribao.impl;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import cn.ribao.dao.RiBaoMapper;
import cn.ribao.po.RiBao;
import cn.ribao.service.RiBaoService;
import cn.ribao.service.TaskService;
import cn.util.FileUtils;
import cn.util.SVNKit;

@Component
@Service
public class TaskServiceImpl implements TaskService {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    //从配置文件中读取项目日报存储路径
    @Value("${params.ribao.updatePath}")
    private String updatePath;
    
    //从配置文件中读取项目日报存储路径
    @Value("${params.ribao.checkOutPath}")
    private String checkOutPath;
    
    //从配置文件中读取项目日报SVN存储位置
    @Value("${params.ribao.svnUrl}")
    private String svnUrl;
    
    //从配置文件中读取项目日报自动校验开关，1为校验
    @Value("${params.ribao.checkswitch}")
    private String checkswitch;
    
    //从配置文件中读取项目日报SVN用户密码
    @Value("${params.svn.password}")
    private String password;
    
    //从配置文件中读取项目日报SVN用户用户名
    @Value("${params.svn.username}")
    private String username;
    
    //从配置文件中读取项目日报检查邮件发送地址
    @Value("${params.email.sendemail}")
    private String sendemail;
    
    //从配置文件中读取项目日报检查邮件发送邮箱密码
    @Value("${params.email.password}")
    private String epassword;
    
    //从配置文件中读取项目日报检查邮件接收邮箱地址，多个地址用英文分号分隔
    @Value("${params.email.receiveemail}")
    private String receiveemail;
    
    //从配置文件中读取项目日报检查人员，多个人员用英文分号分隔
    @Value("${params.ribao.checkname}")
    private String checkname;
    
    //从配置文件中读取特殊工作日配置
    @Value("${params.ribao.specialworkday}")
    private String specialworkday;
    
    @Autowired
    RiBaoService riBaoService;
    @Autowired
    RiBaoMapper riBaoMapper;
    
    
    /**
     *日报检查入口方法，定时任务为每天凌晨1点执行
     */
    @Override
    @Scheduled(cron = "0 0 1 * * ?") 
    public void checkRibao() {
        //检出日报
//        if(true) {
        if(this.checkoutSvn()) {
            if("1".equals(checkswitch)) {
                //将日报读取到数据为库中
                riBaoService.readExcel(this.checkOutPath);
                FileUtils.delDir(new File(this.checkOutPath));
                Calendar calendar = Calendar.getInstance();
                //处理检查日期为当前日期前一天最近工作日，去掉时分秒
                calendar.setTime(new Date());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.clear(Calendar.MINUTE);
                calendar.clear(Calendar.SECOND);
                calendar.clear(Calendar.MILLISECOND);
                calendar.add(Calendar.DATE, -1);
                Date checkDate = calendar.getTime();
                if(this.isWorkDay(checkDate)) {
                    //生成未按时提交日报的结果集
                    Collection<RiBao> list =  this.chekcDetail(checkDate);
                    //生成当天日报不满8小时的结果集
                    Collection<RiBao> list1 =  riBaoMapper.selectCheck8(checkDate);
                    //生成提前填写日报的结果集
                    Collection<RiBao> list2 =  riBaoMapper.selectOverTime(checkDate);
                    //组织邮件正文
                    String eMailContent = this.genEMailContent(list,list1,list2,checkDate);
                    //发送日报检查邮件
                    this.sendEmail(eMailContent,checkDate);
                }
            }
        }
    }
    
    /**
     * 组织邮件内容
     * @param list
     * @param list1 
     * @param list2
     * @param checkDate
     * @return
     */
    private String genEMailContent(Collection<RiBao> list, Collection<RiBao> list1,Collection<RiBao> list2,Date checkDate) {
        StringBuffer stringBufferHead = new StringBuffer();
        StringBuffer stringBufferBody = new StringBuffer();
        boolean errorFlag = false;
        if(list!=null&&!list.isEmpty()) {
            stringBufferHead.append("日报检查日期："+new SimpleDateFormat("yyyy-MM-dd").format(checkDate)+"\n");
            for (Iterator<RiBao> iterator = list.iterator(); iterator.hasNext();) {
                RiBao riBao = (RiBao) iterator.next();
                long diff = checkDate.getTime() - riBao.getWorkDate().getTime();//这样得到的差值是毫秒级别  
                long days = diff / (1000 * 60 * 60 * 24); 
                if(days>0) {
                    errorFlag = true;
                    stringBufferBody.append("  姓名："+riBao.getName());
                    if(riBao.getName().length()<3) {
                        stringBufferBody.append("    ");
                    }
                    stringBufferBody.append("  最后一次提交日报日期："+new SimpleDateFormat("yyyy-MM-dd").format(riBao.getWorkDate()));
                    stringBufferBody.append("  已延期：");
                    stringBufferBody.append(days+"天\n");
                }
            }
            if(!errorFlag) {
                stringBufferHead.append("所有人均已按时提交日报。");
            }else {
                stringBufferHead.append("未按时提交日报人员如下：\n");
            }
            stringBufferHead.append(stringBufferBody.toString());
        }
        if(list1!=null&&!list1.isEmpty()) {
            stringBufferHead.append("\n\n\n\n\n\n\n\n\n\n\n当天日报未填满8小时的人员如下：\n");
            for (Iterator<RiBao> iterator = list1.iterator(); iterator.hasNext();) {
                RiBao riBao = (RiBao) iterator.next();
                stringBufferHead.append("  姓名："+riBao.getName());
                if(riBao.getName().length()<3) {
                    stringBufferHead.append("    ");
                }
                stringBufferHead.append("  累计工时："+riBao.getWorkHour()+"小时\n");
            }
        }
        
        if(list2!=null&&!list2.isEmpty()) {
            stringBufferHead.append("\n\n提前填写日报的人员如下：\n");
            for (Iterator<RiBao> iterator = list2.iterator(); iterator.hasNext();) {
                RiBao riBao = (RiBao) iterator.next();
                stringBufferHead.append("  姓名："+riBao.getName());
                if(riBao.getName().length()<3) {
                    stringBufferHead.append("    ");
                }
                stringBufferHead.append("  工作时间："+new SimpleDateFormat("yyyy-MM-dd").format(riBao.getWorkDate()));
                stringBufferHead.append("  工作内容："+riBao.getWorkContent());
                stringBufferHead.append("  工时："+riBao.getWorkHour()+"小时\n");
            }
        }
        
        logger.debug(stringBufferHead.toString());
        return stringBufferHead.toString();
    }

    private boolean isWorkDay(Date checkDate) {
        boolean result = true;
        //解析配置文件中的特殊工作日配置为map,value为1为工作日，0为非工作日
        HashMap<String, String> wordDayMap = new HashMap<>();
        String workday[] = this.specialworkday.split(";");
        for (int i = 0; i < workday.length; i++) {
            String str[] = workday[i].split(":");
            wordDayMap.put(str[0], str[1]);
        }
        //定义日历，将校验日期设置进去，以方便后面操作
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(checkDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        String specialResult = wordDayMap.get(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        //先判断校验日期是否为特殊工作日,如果有,直接返回结果.
        if(specialResult!=null) {
            if("0".equals(specialResult)) {
                result = false;
            }else {
                result = true;
            }
            return result;
        }
        //如果不是特殊工作日，则按星期判断
        if(week == 1 || week == 7) {
            result = false;
        }else {
            result = true;
        }
        return result;
    }
    
    /**
     * 按参数日期进行未提交日报校验
     * @param nowDate
     * @return
     */
    private Collection<RiBao> chekcDetail(Date nowDate){
        //将配置的姓名解析为map
        HashMap<String, String> checkNameMap = new HashMap<>();
        String[] names = this.checkname.split(";");
        for (int i = 0; i < names.length; i++) {
            checkNameMap.put(names[i],"");
        }
        
        Collection<RiBao> result = new ArrayList<RiBao>();
        Collection<String> ribaoNameList = new ArrayList<>();
        //查询出所有人的最晚提交日报的日期
        Collection<RiBao> list = riBaoMapper.selectCheckResult(nowDate);
        for (Iterator<RiBao> iterator = list.iterator(); iterator.hasNext();) {
            RiBao riBao = (RiBao) iterator.next();
            ribaoNameList.add(riBao.getName());
            if(riBao.getName() != null && checkNameMap.containsKey(riBao.getName())) {
                result.add(riBao);
            }
        }
        
        //对未找到日报文件的人员也要检查，并将日期设置为2019-01-01
        Set<String> set = checkNameMap.keySet();
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if(!ribaoNameList.contains(name)) {
                RiBao riBao = new RiBao();
                riBao.setName(name);
                try {
                    riBao.setWorkDate(new SimpleDateFormat("yyyy-MM-dd").parse("2019-01-01"));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                result.add(riBao);
            }
        }
        
        return result;
    }
    
    /**
     * 获取截止入参时间最后一个工作日的日期
     * @param nowDate
     * @return
     */
    private Date getWorkDay(Date nowDate) {
        //解析配置文件中的特殊工作日配置为map,value为1为工作日，0为非工作日
        HashMap<String, String> wordDayMap = new HashMap<>();
        String workday[] = this.specialworkday.split(";");
        for (int i = 0; i < workday.length; i++) {
            String str[] = workday[i].split(":");
            wordDayMap.put(str[0], str[1]);
        }
        //定义日历，将校验日期设置进去，以方便后面操作
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(nowDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        while(true) {
            int week = calendar.get(Calendar.DAY_OF_WEEK);
            String specialResult = wordDayMap.get(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
            //先判断校验日期是否为特殊工作日
            if(specialResult!=null) {
                if("0".equals(specialResult)) {//如果是非工作日，则日期减一天
                    calendar.add(Calendar.DATE,-1);
                }else {//如果是工作日，返回此日期
                    return calendar.getTime();
                }
            }
            //如果不是特殊工作日，则按星期判断
            if(week == 1 || week == 7) {//如果是周六或周日，则日期减一天
                calendar.add(Calendar.DATE,-1);
            }else {//如果是周一到周五，则返回此日期
                return calendar.getTime();
            }
        }
    }
    
    /**
     * 发送邮件
     * @param eMailContent
     * @param checkDate
     */
    private void sendEmail(String eMailContent,Date checkDate) {
        try {
            if(this.receiveemail!=null && this.receiveemail.length()>0) {
                Properties properties = new Properties();
                properties.put("mail.transport.protocol", "smtp");// 连接协议，即：邮件协议
                properties.put("mail.smtp.host", "smtp.exmail.qq.com");// 主机名
                properties.put("mail.smtp.port", 465);// 端口号
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.ssl.enable", "true");// 设置是否使用ssl安全连接 ---一般都使用
                properties.put("mail.debug", "true");// 设置是否显示debug信息 true 会在控制台显示相关信息
                // 得到回话对象
                Session session = Session.getInstance(properties);
                // 获取邮件对象
                Message message = new MimeMessage(session);
                // 设置发件人邮箱地址
                message.setFrom(new InternetAddress(this.sendemail));
                // 设置收件人邮箱地址
                String[] emails = this.receiveemail.split(";");
                //解析配置文件中的多个电子邮件
                InternetAddress[] addresses = new InternetAddress[emails.length];
                for (int i = 0; i < emails.length; i++) {
                    addresses[i] = new InternetAddress(emails[i]);
                }
                message.setRecipients(Message.RecipientType.TO, addresses);
                //message.setRecipient(Message.RecipientType.TO, new InternetAddress("xxx@qq.com"));//一个收件人
                // 设置邮件标题
                message.setSubject(new SimpleDateFormat("yyyy-MM-dd").format(checkDate)+"日报检查结果通知");
                // 设置邮件内容
                message.setText(eMailContent);
                // 得到邮差对象
                Transport transport = session.getTransport();
                // 连接自己的邮箱账户
                transport.connect(this.sendemail, this.epassword);// 密码为QQ邮箱开通的stmp服务后得到的客户端授权码（你可以进入你的邮箱的设置里面查看）
                // 发送邮件
                transport.sendMessage(message, message.getAllRecipients());
                transport.close();
            }
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }        
    }
    
    /**
     * 检出日报，并返回结果，true为成功，false为失败
     * @return
     */
    private boolean checkoutSvn() {
        boolean result ;
        FileUtils.delDir(new File(this.checkOutPath));
        try {
            result = SVNKit.checkOut(this.username, this.password, this.svnUrl, this.checkOutPath);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            result = false;
        }
        return result;
    }

}
