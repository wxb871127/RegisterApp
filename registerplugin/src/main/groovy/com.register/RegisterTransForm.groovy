package com.register

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import com.sun.org.apache.bcel.internal.util.ClassVector
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.internal.impldep.org.apache.commons.lang.ClassUtils
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.gradle.api.file.Directory
import org.objectweb.asm.Opcodes

public class RegisterTransForm extends Transform{

    RegisterConfig registerConfig

    public RegisterTransForm(){}

    @Override
    String getName() {
        return 'registerplugin'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {

        inputs.each {//分别扫描出 build文件夹下的debug和release文件
            TransformInput transformInput ->

                // 遍历jar  拷贝编译的源jar包到目标路径
                transformInput.jarInputs.each { JarInput jarInput ->
                    scanJar(jarInput, outputProvider)
                }

                //遍历文件夹
                transformInput.directoryInputs.each {
                    DirectoryInput directory ->
                        File dest = outputProvider.getContentLocation(directory.name,
                                directory.contentTypes, directory.scopes, Format.DIRECTORY)
                        String root = directory.file.absolutePath
                        if (!root.endsWith(File.separator))
                            root += File.separator

                        directory.file.eachFileRecurse {
                            File file ->
                                def path = file.absolutePath.replace(root, '')
                                def entryName = path
                                if(file.isFile()){
                                    entryName = entryName.replaceAll("\\\\", "/")
                                    if(scanFile(entryName, dest)){//找到需要注册到的类
                                        visitClass(file)
                                    }
                                }
                        }
                }
        }
    }

    boolean scanFile(String entryName, File destFile){
        if (entryName == null || !entryName.endsWith(".class"))
            return
        entryName = entryName.substring(0, entryName.lastIndexOf('.'))

        def found = false
        registerConfig.registerInfoList.each { ext ->
            if (ext.registerIntoClass == entryName) {
                println '===========find class ' + entryName
                ext.registerFile = destFile
                if (destFile.name.endsWith(".jar")) {
                    found = true
                    println '===========find jar initClassName = '+ext.initClassName
                }
            }
        }
        return found
    }

    void scanJar(JarInput jarInput, TransformOutputProvider outputProvider) {
        // 获得输入文件
        File src = jarInput.file
        //遍历jar的字节码类文件，找到需要自动注册的类
        File dest = getDestFile(jarInput, outputProvider)
        //复制jar文件到transform目录：build/transforms/registerplugin/
        FileUtils.copyFile(src, dest)
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

    void visitClass(File file){
        visitClass(new FileInputStream(file), file.absolutePath)
    }

    /**
     * ASM5 修改class文件
     * @param inputStream
     * @param filePath
     */
    void visitClass(InputStream inputStream, String filePath){
        ClassReader cr = new ClassReader(inputStream)
        ClassWriter cw = new ClassWriter(cr, 0)
        ScanClassVisitor cv = new ScanClassVisitor(Opcodes.ASM5, cw, filePath)
        cr.accept(cv, ClassReader.EXPAND_FRAMES)
        inputStream.close()

        return cv.isFound
    }
}