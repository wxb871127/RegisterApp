package com.register

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

public class RegisterTransForm extends Transform{
    RegisterConfig registerConfig

    public RegisterTransForm(){}

    @Override
    String getName() {
        return "registerplugin"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否支持增量编译
     * @return
     */
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs
                   , Collection<TransformInput> referencedInputs
                   , TransformOutputProvider outputProvider
                   , boolean isIncremental) throws IOException, TransformException, InterruptedException {
        def clearCache = !isIncremental
        // clean build cache
        if (clearCache) {
            outputProvider.deleteAll()
        }

        RegisterScan registerScan = new RegisterScan(registerConfig)

        // 遍历输入文件
        inputs.each { TransformInput input ->
            // 遍历jar
            input.jarInputs.each { JarInput jarInput ->
                File src = jarInput.file
                File dest = getDestFile(jarInput, outputProvider)
//                println 'xxxxxxxxxx src jar = ' + src.absolutePath
//                println 'xxxxxxxxxx dest jar = ' + dest.absolutePath
                FileUtils.copyFile(src, dest)
            }

            //遍历文件夹
            input.directoryInputs.each {
                DirectoryInput directory ->
                    File dest = outputProvider.getContentLocation(directory.name,
                            directory.contentTypes, directory.scopes, Format.DIRECTORY)
                    def root = directory.file.absolutePath
                    println 'xxxxxxxx dest directory = ' + dest
                    println 'xxxxxxxx src directory =  ' + root
                    if (!root.endsWith(File.separator))
                        root += File.separator

                    directory.file.eachFileRecurse {
                        File file ->
                            def fileName = file.absolutePath.replace(root, '')
                            fileName = fileName.replaceAll("\\\\", "/")
                            if (file.isFile()) {
                                registerScan.filterClass(file, root)

                                registerScan.filterRegisterIntoClass(new File(dest.absolutePath + File.separator + fileName), fileName)
                            }
                    }
                    FileUtils.copyDirectory(directory.file, dest)
            }
        }

        RegisterClassVisitor classVisitor = new RegisterClassVisitor(registerConfig)
        classVisitor.registerClass()
    }

    static File getDestFile(JarInput jarInput, TransformOutputProvider outputProvider) {
        def destName = jarInput.name
        // 重名名输出文件,因为可能同名,会覆盖
        def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath)
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4)
        }
        // 获得输出文件
        File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
        return dest
    }
}
