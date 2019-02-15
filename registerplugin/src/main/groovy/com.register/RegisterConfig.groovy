package com.register;

public class RegisterConfig{
    public List<Map<String, Object>> registerInfo = []  //buil.gradle的注册信息
    List<RegisterInfo> registerInfoList = new ArrayList<>() //存放每项注册信息
    public def enabledCache = true  //是否需要缓存


    public RegisterConfig(){

    }

    void parseParams(){
        registerInfo.each {
            map ->
                RegisterInfo registerInfo = new RegisterInfo()
                registerInfo.scanAnnotationClass = map.get('scanAnnotationClass')
                registerInfo.registerIntoClass = map.get('registerIntoClass')
                registerInfo.registerMethod = map.get('registerMethod')
                registerInfo.superClass = map.get('superClass')
                registerInfo.init()
                registerInfoList.add(registerInfo)
        }
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder()
        stringBuilder.append('enabledCache='+enabledCache).append('\n')

        for(int i=0; i<registerInfoList.size(); i++){
            stringBuilder.append(registerInfoList.get(i).toString())
        }
        return stringBuilder.toString()
    }
}
