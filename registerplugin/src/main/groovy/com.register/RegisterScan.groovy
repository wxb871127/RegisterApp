package com.register

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern

public class RegisterScan {
    RegisterConfig registerConfig

    RegisterScan(RegisterConfig registerConfig){
        this.registerConfig = registerConfig
    }

    boolean scanJar(File jarFile, File destFile){
        if (!jarFile)
            return false

        def srcFilePath = jarFile.absolutePath
        def file = new JarFile(jarFile)
        Enumeration enumeration = file.entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement()
            String entryName = jarEntry.getName()
            //support包不扫描
            if (entryName.startsWith("android/support"))
                break
            checkRegisterClass(entryName, destFile, srcFilePath)
        }
        if (null != file) {
            file.close()
        }
        return true
    }

    //记录需要注册的文件
    boolean checkRegisterClass(String entryName, File destFile, String srcFilePath) {
        if (entryName == null || !entryName.endsWith(".class"))
            return
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))

        def found = false
        registerConfig.registerInfoList.each {
            RegisterInfo registerInfo ->
                if(entryName == registerInfo.registerIntoClass){
                    registerInfo.registerInfoFile = destFile
                    if (destFile.name.endsWith(".jar")) {
                        found = true
                    }
                }
        }
        return found
    }

    //过滤文件来筛选符合条件的文件
    boolean filterClass(File file, String rootPath){
        def fileName = file.absolutePath.replace(rootPath, '')

        if (fileName == null || !fileName.endsWith(".class"))
            return false
        fileName = fileName.substring(0, fileName.lastIndexOf('.'))
        //过滤掉R资源文件
        Pattern pattern = Pattern.compile('.*/R(\\$[^/]*)?')
        Matcher matcher1 = pattern.matcher(fileName)
        //过滤掉BuikdConfig文件
        pattern = Pattern.compile('.*/BuildConfig$')
        Matcher matcher2 = pattern.matcher(fileName)

        if(matcher1.matches() || matcher2.matches()){
            return true
        }

        RegisterClassVisitor classVisitor = new RegisterClassVisitor(registerConfig)
        classVisitor.scanClass(file)
    }

}
