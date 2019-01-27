package com.register;

public class RegisterConfig{
    public List<Map<String, Object>> registerInfo = []
    List<RegisterInfo> registerInfoList = new ArrayList<>()
    public def enabledCache = true
    String registerMap


    public RegisterConfig(){

    }

    void parseParams(){

        println 'enabledCache is ' + enabledCache

        println 'registerMap is ' + registerMap


        registerInfo.each {
            map ->
                RegisterInfo registerInfo = new RegisterInfo()
                registerInfo.scanAnnotationClass = map.get('scanAnnotationClass')
                registerInfo.registerMethod = map.get('registerMethod')
                registerInfoList.add(registerInfoList)
                println 'scanAnnotationClass is ' + registerInfo.scanAnnotationClass
                println 'registerMethod is ' +  registerInfo.registerMethod
        }
    }

    @Override
    String toString() {
        StringBuilder stringBuilder = new StringBuilder()
        for(int i=0; i<registerInfoList.size(); i++){
            stringBuilder.append('scanAnnotationClass is ')
                    .append(registerInfoList.get(i).scanAnnotationClass)
            .append(',registerMethod is ')
            .append(registerInfoList.get(i).registerMethod)
            .append('\n')
        }
        return stringBuilder.toString()
    }
}
