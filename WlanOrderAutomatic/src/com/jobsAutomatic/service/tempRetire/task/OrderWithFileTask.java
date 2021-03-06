package com.jobsAutomatic.service.tempRetire.task;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.jobsAutomatic.dao.OperateWorkOrder;
import com.jobsAutomatic.service.Sender.Receipt;
import com.jobsAutomatic.service.check.CheckWorkJob;
import com.jobsAutomatic.service.modle.WorkJob;
import com.jobsAutomatic.service.readExcle.ReadWorkJob;
import com.jobsAutomatic.service.util.ftp.FileOperate;
import com.jobsAutomatic.service.util.ftp.OrderTypeTransfer;

public class OrderWithFileTask extends ATask {
//	@Autowired
//	OperateWorkOrder operateWorkOrder;
//	@Autowired
//	CheckWorkJob checkWorkJob;
	private String xmlText;

	@Override
	public void HandleTask() {
		// TODO Auto-generated method stub
		OperateWorkOrder operateWorkOrder = new OperateWorkOrder();
		operateWorkOrder.setJdbcTemplate(jdbcTemplate);
		CheckWorkJob checkWorkJob = new CheckWorkJob();
		Document dom = null;
		try {
			dom = DocumentHelper.parseText(xmlText);
			Element root = dom.getRootElement();
			Element ftpInfo = root.element("ftpInfo");
			String connectionString = ftpInfo.element("ConnectionString").getText().replace("ftp://", "");
			String path = ftpInfo.element("Path").getText();
			String userName = ftpInfo.element("userName").getText();
			String passWord = ftpInfo.element("password").getText();
			String url = "ftp://" + userName + ":" + passWord + "@" + connectionString + path;
			Element files = ftpInfo.element("files");
			@SuppressWarnings("unchecked")
			List<Element> filess = files.elements();
			for (Element file : filess) {
				String ftpUrl = url + file.element("fileName").getText();
				System.out.println(ftpUrl);		
				FileOperate fo = new FileOperate(ftpUrl);
				String filePath = fo.getDownloadFile();
				ReadWorkJob readWorkJob = new ReadWorkJob();
				WorkJob workJob = readWorkJob.readXlsx(filePath);
				checkWorkJob.setWorkJob(workJob);
				if (!checkWorkJob.CheckWork().equals("")) {
					new Receipt().SendReceipt(workOrder.getWorkjob_id(), "失败", checkWorkJob.CheckWork());
					updateWorkOrder.Update("校验失败", 2, checkWorkJob.CheckWork(), workOrder.getWorkjob_id());
				} else {
					OrderTypeTransfer orderTypeTransfer = new OrderTypeTransfer();
					String type = orderTypeTransfer.Transfer(readWorkJob.getSheetName(filePath));
					operateWorkOrder.Insert(workJob, type, filePath,ftpUrl);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage() + "xmlText -->" + xmlText);
			e.printStackTrace();
//			updateWorkOrder.Update("校验失败", 2, e.getMessage(), workOrder.getWorkjob_id());		
		}
	}

	public String getXmlText() {
		return xmlText;
	}

	public void setXmlText(String xmlText) {
		this.xmlText = xmlText;
	}

	public static void main(String[] args) {
		OrderWithFileTask orderWithFileTask = new OrderWithFileTask();
		orderWithFileTask.setXmlText(
				"<fileInfo><ftpInfo><Type></Type><DataCatalog></DataCatalog><WorkMode></WorkMode><SystemID></SystemID><SessionID></SessionID><MsgSerial></MsgSerial><DeliveryTime></DeliveryTime><ReadyStatusCode></ReadyStatusCode><ReadyStatusDescription></ReadyStatusDescription><ConnectionString>ftp://10.221.18.29:21</ConnectionString><Path>//home/inas/fast-clt-pro/test/</Path><userName>inas</userName><password>1Na512#$</password><FileList>SH-201-170322-00005_WLAN.xls</FileList><files><file><fileName>AP导入.xlsx</fileName><FileFormat>xlsx</FileFormat><FileSize>文件大小</FileSize><IsEncryption>是否加密</IsEncryption><CipherKey>密钥</CipherKey><CipherFile>密钥文件</CipherFile><IsCompressed>是否压缩</IsCompressed><CompressKey>压缩密码</CompressKey><DataInfo>数据信息</DataInfo><FieldSeparator>字段分割符</FieldSeparator><LineSeparator>行分割符</LineSeparator><XmlSchema>XmlSchema</XmlSchema><CharSet>字符集</CharSet><FileCheckInfo>文件检验信息</FileCheckInfo></file></files></ftpInfo></fileInfo>");
		orderWithFileTask.HandleTask();
	}
}
