package com.yu.usercenter.utils.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.support.ExcelTypeEnum;

import java.util.LinkedList;
import java.util.List;

/**
 * 工具类
 */
public class ExcelUtil {

    /**
     * 封装Excel中的数据到指定的实体类中
     *
     * @param typeClass 指定的实体类的字节码类别
     * @param readPath  Excel的文件路径
     * @return 指定的实体类对象的集合（每个对象代表每一条数据）
     */
    public static <T> List<T> getDataFromExcel(Class<T> typeClass, String readPath) {
        List<T> list = new LinkedList<>();
        EasyExcel.read(readPath)
                .head(typeClass)
                .sheet()
                .registerReadListener(new AnalysisEventListener<T>() {

                    /**
                     * 每一条数据的监听回调
                     */
                    @Override
                    public void invoke(T excelData, AnalysisContext analysisContext) {
                        list.add(excelData);
                    }

                    /**
                     * 完成所有的数据读取后调用此方法
                     */
                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        System.out.println("数据读取完毕");
                    }
                }).doRead();
        return list;
    }

    /**
     * 将封装好的数据写入Excel中
     *
     * @param list      写入的数据集合
     * @param writePath 写入的Excel文件的路径
     * @param sheet     excel表中生成的sheet表名
     * @param excelType 插入的excel的类别，有xls、xlsx两种
     */
    public static <T> void saveDataToExcel(List<T> list, String writePath, String sheet, ExcelTypeEnum excelType, Class<T> clazz) {
        // 写入Excel文件
        EasyExcel.write(writePath)
                .head(clazz)
                .excelType(excelType)
                .sheet(sheet)
                .doWrite(list);
    }

}