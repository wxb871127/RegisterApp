package com.register;

public class RegisterInfo{
    String scanAnnotationClass //扫描注解的类
    String registerIntoClass //自动注册到这个Class
    String registerMethod //自动注册方法
    String extendClass //继承的子类

    File registerFile //自动注册的类文件


    public RegisterInfo(){}

    public void init(){
        registerIntoClass = registerIntoClass.replaceAll("\\.","/")
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append('scanAnnotationClass='+scanAnnotationClass)
        .append(',registerIntoClass='+registerIntoClass)
        .append(',registerMethod='+registerMethod)
        .append(',extendClass='+extendClass).append('\n')
        return stringBuilder.toString()
    }
}