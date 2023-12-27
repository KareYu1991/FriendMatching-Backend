package com.yu.usercenter.utils.excel;

import java.util.List;

import com.alibaba.excel.EasyExcel;
import com.yu.usercenter.model.domain.ImportUserDto;

public class EasyExcelImport {

    public static void main(String[] args) {

        String filname = "E:\\IDEdownload\\testExcel.xlsx";

        readByListener(filname);

    }

    /**
     * 监听器读取
     */
    public static void readByListener(String fileName) {
        List<ImportUserDto> dataFromExcel = ExcelUtil.getDataFromExcel(ImportUserDto.class, fileName);
        for (ImportUserDto user : dataFromExcel) {
            System.out.println(user);
        }
    }

    /**
     * 同步读
     * 同步的返回，如果数据量大会把数据放到内存里面,但是可以直接返回所有数据，不需要自定义监听器
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取sheet 同步读取会自动finish
        List<ImportUserDto> userList = EasyExcel.read(fileName).head(ImportUserDto.class).sheet().doReadSync();
        // 可以拿到所有数据
    }

}
