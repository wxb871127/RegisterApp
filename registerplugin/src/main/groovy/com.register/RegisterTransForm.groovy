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
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

import java.util.regex.Matcher
import java.util.regex.Pattern

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


        def root

        inputs.each {
                //分别扫描出 build文件夹下的debug和release文件
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
                    root = directory.file.absolutePath
                    if (!root.endsWith(File.separator))
                        root += File.separator

                    directory.file.eachFileRecurse {
                        File file ->
                            def fileName = file.absolutePath.replace(root, '')
                            fileName = fileName.replaceAll("\\\\", "/")
                            if (file.isFile()) {
                                findNeedRegisterClass(fileName, file)
                            }
                    }
                    FileUtils.copyDirectory(directory.file, dest)
            }
        }
        registerClass(root)
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

    boolean is(int access, int flag) {
        return (access & flag) == flag
    }

    void findNeedRegisterClass(String fileName, File file){
        if (fileName == null || !fileName.endsWith(".class"))
            return
        fileName = fileName.substring(0, fileName.lastIndexOf('.'))
        Pattern pattern = Pattern.compile('.*/R(\\$[^/]*)?')
        Matcher matcher1 = pattern.matcher(fileName)
        pattern = Pattern.compile('.*/BuildConfig$')
        Matcher matcher2 = pattern.matcher(fileName)

        if(matcher1.matches() || matcher2.matches()){
            return
        }

        InputStream inputStream = file.newInputStream()
        ClassReader classReader = new ClassReader(inputStream)
        ClassWriter classWriter = new ClassWriter(classReader, 0)
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM6, classWriter) {
            @Override
            void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces)
                //抽象类、接口、非public等类无法调用其无参构造方法
                if (is(access, Opcodes.ACC_ABSTRACT)
                        || is(access, Opcodes.ACC_INTERFACE)
                        || !is(access, Opcodes.ACC_PUBLIC)
                ) {
                    return
                }
                if(superName != 'java/lang/Object'){
                    registerConfig.registerInfoList.each {
                        RegisterInfo registerInfo ->
                            if(registerInfo.superClass == superName) {
                                registerInfo.needRegisterClass.add(file.absolutePath)
//                                println '========needRegisterClass ' + file.absolutePath
                            }
                    }
                }
            }
        }
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        inputStream.close()
    }

    void registerClass(String root){
        registerConfig.registerInfoList.each {
            RegisterInfo registerInfo ->
                registerInfo.registerIntoClass = registerInfo.registerIntoClass.replaceAll('/', '\\\\')
                registerInfo.registerIntoClass = root + registerInfo.registerIntoClass + ".class"
                registerInfo.needRegisterClass.each {
                    String needRegisterClass ->
                        println 'needRegisterClass = ' + needRegisterClass
                        visitorClass(registerInfo.superClass, registerInfo.registerIntoClass,
                                registerInfo.registerMethod, needRegisterClass)
                }
        }
    }

    void visitorClass(String superClass, String registerIntoClass, String registerMethod, String needRegisterClass){
        println '============= registerInfo.registerIntoClass = ' + registerIntoClass
        File file2 = new File(registerIntoClass)
        InputStream inputStream = file2.newInputStream()
        ClassReader classReader = new ClassReader(inputStream)
        ClassWriter classWriter = new ClassWriter(classReader, 0)
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM6, classWriter) {
            @Override
            MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions)
                if(name == registerMethod){
                    println 'find registerMethod = ' + name
                    methodVisitor = new MethodVisitor(Opcodes.ASM6, methodVisitor){
                        @Override
                        void visitInsn(int opcode) {
                            if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)) {
                                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0)//非static方法，加载this指针
                                methodVisitor.visitTypeInsn(Opcodes.NEW, needRegisterClass)
                                methodVisitor.visitInsn(Opcodes.DUP)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, needRegisterClass, '<init>', '()V', false)
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL,registerIntoClass,
                                        registerMethod, "(L${superClass};)V" , false)
                            }
                            super.visitInsn(opcode)
                        }

                        @Override
                        void visitMaxs(int maxStack, int maxLocals) {
                            super.visitMaxs(maxStack + 4, maxLocals)
                        }
                    };
                }
                return methodVisitor
            }
        }
        classReader.accept(classVisitor, 0)
        byte[] bytes = classWriter.toByteArray()
        String tmp = registerIntoClass + ".test.class"

        println 'tmp = '+ tmp

//        File file1 = new File(tmp)
//        if(!file1.exists())
//            file1.createNewFile()
//        FileOutputStream fileOutputStream = new FileOutputStream(file1)
//        fileOutputStream.write(bytes)
//        fileOutputStream.flush()
        inputStream.close()
        fileOutputStream.close()
    }
}