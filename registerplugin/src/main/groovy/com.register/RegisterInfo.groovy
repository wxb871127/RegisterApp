package com.register;

public class RegisterInfo{
    String scanAnnotationClass //扫描注解的类
    String registerIntoClass //自动注册到这个Class
    File registerInfoFile //自动注册到这个文件
    String registerMethod //自动注册方法
    String superClass //父类
    List<String> needRegisterClass = new ArrayList<>() //待注册的类集合


    public RegisterInfo(){}

    public void init(){
        if(registerIntoClass != null)
            registerIntoClass = registerIntoClass.replaceAll("\\.","/")
        if(superClass != null)
            superClass = superClass.replaceAll('\\.','/')
        if(scanAnnotationClass != null)
            scanAnnotationClass = scanAnnotationClass.replaceAll('\\.','/')
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append('scanAnnotationClass='+scanAnnotationClass)
        .append(',registerIntoClass='+registerIntoClass)
        .append(',registerMethod='+registerMethod)
        .append(',superClass='+superClass).append('\n')
        return stringBuilder.toString()
    }
}