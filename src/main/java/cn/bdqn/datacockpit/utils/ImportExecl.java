package cn.bdqn.datacockpit.utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import cn.bdqn.datacockpit.entity.Tablecolumninfo;

public class ImportExecl {

    /** 总行数 */

    private int totalRows = 0;

    /** 总列数 */

    private int totalCells = 0;

    /** 错误信息 */

    private String errorInfo;

    /** 构造方法 */

    public ImportExecl() {

    }

    public int getTotalRows() {

        return totalRows;

    }

    public int getTotalCells() {

        return totalCells;

    }

    public String getErrorInfo() {

        return errorInfo;

    }

    ChineseToPinYin ctpy = new ChineseToPinYin();

    /**
     * 
     * Description:验证是否是excel文件 <br/>
     *
     * @author huMZ
     * @param file
     * @return
     */

    public boolean validateExcel(MultipartFile file) {

        /** 检查文件名是否为空或者是否是Excel格式的文件 */

        if (file != null || WDWUtil.isExcel2003(file) || WDWUtil.isExcel2007(file)) {

            return true;

        }
        errorInfo = "文件名不是excel格式";
        return false;

    }

    /**
     * 
     * Description:获取workbook对象 <br/>
     *
     * @author huMZ
     * @param file
     * @return
     * @throws Exception
     */
    public Workbook getWorkbook(MultipartFile file) throws Exception {
        Workbook workbook = null;
        if (WDWUtil.isExcel2003(file)) {
            workbook = new HSSFWorkbook(file.getInputStream());
        } else if (WDWUtil.isExcel2007(file)) {
            workbook = new XSSFWorkbook(file.getInputStream());
        }
        return workbook;
    }

    /**
     * 
     * Description: 将excel表中数据转成List<Map<String, Object>>进行存储<br/>
     *
     * @author huMZ
     * @param workbook
     * @return
     */
    public List<Map<String, Object>> getExceList(Workbook workbook) {
        // 用户存储除表中第一行的所有数据
        List<Map<String, Object>> excelList = new ArrayList<Map<String, Object>>();
        // // 用于存储除第一行的单行的数据
        // Map<String, Object> excelMap = new HashMap<String, Object>();
        // 获取工作表
        Sheet sheet = workbook.getSheetAt(0);
        Row row = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            sheet = workbook.getSheetAt(i);
            // 遍历该表除第一行外所有的行,j表示行数,getPhysicalNumberOfRows行的总数
            for (int j = 1; j < sheet.getPhysicalNumberOfRows(); j++) {
                // 用于存储除第一行的单行的数据
                Map<String, Object> excelMap1 = new HashMap<String, Object>();
                row = sheet.getRow(j);
                // 获取第一行数据作为键
                List<String> columns = getColumns(workbook);
                // 遍历该行所有的列,k表示列数 getPhysicalNumberOfCells行的总数
                for (int k = 0; k < row.getPhysicalNumberOfCells(); k++) {
                    Cell cell = row.getCell(k);
                    Object object = getCellData(cell);
                    excelMap1.put(columns.get(k), object);
                }
                excelList.add(excelMap1);
            }
        }

        return excelList;
    }

    /**
     * 
     * Description:将cell的数据类型转化成java数据类型 <br/>
     *
     * @author huMZ
     * @param cell
     * @param formula
     * @return
     */
    private Object getCellData(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
        case Cell.CELL_TYPE_STRING:// 返回字符串类型
            return cell.getRichStringCellValue().getString();
        case Cell.CELL_TYPE_NUMERIC:// 返回时间类型以及double类型
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                return (Double) cell.getNumericCellValue();
            }
        case Cell.CELL_TYPE_BOOLEAN:// 返回boolean类型
            return cell.getBooleanCellValue();
        default:
            return null;
        }
    }

    /**
     * 
     * Description: 获取表第一行的数据作为表字段使用<br/>
     *
     * @author huMZ
     * @param workbook
     * @return
     */
    public List<String> getColumns(Workbook workbook) {
        // 存储表中第一行的数据
        List<String> columnList = new ArrayList<String>();
        // 获取第一张工作表
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            // 将首行数据存储,以作为表字段使用
            Row row = sheet.getRow(0);
            if (row != null) {
                for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                    Cell cell = row.getCell(j);
                    String column = cell.getStringCellValue();
                    columnList.add(column);
                }
            }

        }
        return columnList;
    }

    /**
     * 
     * Description:判断上传excel表首行信息是否与表字段名匹配 <br/>
     *
     * @author huMZ
     * @return
     */
    public boolean columnIsMatches(List<String> listCells, List<Tablecolumninfo> listColumnsName) {
        StringBuffer colunmBuffer = new StringBuffer();
        boolean flag = true;
        for (Tablecolumninfo map : listColumnsName) {
            colunmBuffer.append(map.getColumnname());
        }
        String columnName = colunmBuffer.toString();
        for (String string1 : listCells) {
            if (!columnName.contains(string1)) {
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 
     * Description:将数据库数据除主键外的数据类型转换成java数据类型的反射类名已供比较 <br/>
     *
     * @author huMZ
     * @param columnList
     * @return
     */
    public List<String> changeSqlColumnTypeToJava(List<String> columnList) {

        for (int i = 0; i < columnList.size(); i++) {
            String column = columnList.get(i);
            if (column.contains("varchar")) {
                column = "class java.lang.String";
            } else if (column.contains("float")) {
                column = "class java.lang.Double";
            } else if (column.contains("date")) {
                column = "class java.util.Date";
            }
            columnList.add(i, column);
        }
        columnList.remove(0);
        return columnList;

    }

    /**
     * 
     * Description:对excel表中内容进行匹配,有错误时返回错误信息 <br/>
     *
     * @author huMZ
     */
    public List<String> checkExcel(List<Map<String, Object>> excelList, List<Tablecolumninfo> listColumnsType,
            List<Tablecolumninfo> listColumnsName) {
        List<String> message = new ArrayList<String>();
        // 将数据库中表字段类型取出存入list<String>
        List<String> columnTypes = new ArrayList<String>();
        for (Tablecolumninfo tablecolumninfo : listColumnsType) {
            columnTypes.add(tablecolumninfo.getColumntype());
        }
        for (int i = 0; i < excelList.size(); i++) {

            Map<String, Object> map = excelList.get(i);
            for (int j = 0; j < map.size(); j++) {
                String columnType = columnTypes.get(j);
                String columnName = listColumnsName.get(j).getColumnname();
                if (columnType.equals("INTEGER")) {
                    try {
                        Integer integer = Integer.parseInt(((map.get(columnName)).toString().substring(0,
                                map.get(columnName).toString().lastIndexOf(".")))
                                + "");
                    } catch (Exception e) {
                        message.add("您表中第" + (i + 2) + "行数据有误.请检查.");

                    }
                } else if (columnType.equals("FLOAT")) {
                    try {
                        Double double1 = Double.parseDouble(map.get(columnName) + "");
                    } catch (Exception e) {
                        message.add("您表中第" + (i + 2) + "行数据有误.请检查.");
                    }
                }
            }

        }
        return message;
    }

    /**
     * 
     * Description:将用户上传原始文件进行保存<br/>
     *
     * @author huMZ
     * @param file
     * @throws Exception
     */
    public void uploadExcel(HttpServletRequest request, MultipartFile file, String companyName) throws Exception {

        if (ServletFileUpload.isMultipartContent(request)) {
            // 获取文件名
            String excelName = file.getOriginalFilename();
            // 获取服务器上传文件夹路径
            String realpath = request.getSession().getServletContext().getRealPath("upload");
            // String excelType =
            // excelName.substring(excelName.lastIndexOf("."));
            // 根据日期以及公司名创建文件夹
            String newName = new SimpleDateFormat("yyyyMMDD").format(new Date());
            String path = realpath + "/" + companyName + "/" + newName;
            File file1 = new File(path);
            file1.mkdirs();
            try {
                file.transferTo(new File(path + "//" + excelName));
            } catch (IllegalStateException | IOException e) {

                e.printStackTrace();

            }
        }

    }

}

class WDWUtil {
    /**
     * 
     * Description:判断是否是excel2003 <br/>
     *
     * @author huMZ
     * @param file
     * @return
     */
    public static boolean isExcel2003(MultipartFile file) {

        return file.getOriginalFilename().matches("^.+\\.(?i)(xls)$");

    }

    /**
     * 
     * Description:判断是否是excel2007<br/>
     *
     * @author huMZ
     * @param file
     * @return
     */
    public static boolean isExcel2007(MultipartFile file) {

        return file.getOriginalFilename().matches("^.+\\.(?i)(xlsx)$");

    }

}
