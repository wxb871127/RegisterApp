package com.register

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

public class RegisterClassVisitor {
    RegisterConfig registerConfig

    RegisterClassVisitor(RegisterConfig registerConfig){
        this.registerConfig = registerConfig
    }

    boolean is(int access, int flag) {
        return (access & flag) == flag
    }

    //过滤符合条件的class文件并记录到registerInfo
    void scanClass(File file){
        InputStream inputStream = file.newInputStream()
        ClassReader classReader = new ClassReader(inputStream)
        ClassWriter classWriter = new ClassWriter(classReader, 0)
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
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

                registerConfig.registerInfoList.each {
                    RegisterInfo registerInfo ->
                        if(superName != 'java/lang/Object') {
                            if (registerInfo.superClass == superName) {
//                                println 'find class ' + file.absolutePath
                                registerInfo.needRegisterClass.add(name)
                            }
                        }
                        if(name == registerInfo.registerIntoClass) {
                            registerInfo.registerInfoFile = file
//                            println 'find registerInfoFile = ' + file.absolutePath
                        }
                }

            }
        }
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        inputStream.close()
    }

    void registerClass(){
        registerConfig.registerInfoList.each {
            RegisterInfo registerInfo ->
                registerInfo.needRegisterClass.each {
                    String registerClass ->
                    visitRegisterClass(registerInfo.registerInfoFile, registerInfo.registerIntoClass,
                            registerInfo.registerMethod, registerInfo.superClass, registerClass)
                }
        }
    }

    //将needRegisterClass 注册到 file文件中
    void visitRegisterClass(File file, String className, String method, String superClassName, String needRegisterClass){
        println '============= start visitRegisterClass'
        InputStream inputStream = new FileInputStream(file);
        ClassReader classReader = new ClassReader(inputStream);
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, classWriter) {
            @Override
            public void visit(int i, int i1, String s, String s1, String s2, String[] strings) {
                super.visit(i, i1, s, s1, s2, strings);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                if(name.equals("<init>")) {//只修改构造方法  此处在构造方法中创建needRegisterClass对象并调用注册方法进行注册
                    methodVisitor = new MethodVisitor(Opcodes.ASM5, methodVisitor) {
                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {

                                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);//非静态方法加载this指针

                                methodVisitor.visitTypeInsn(Opcodes.NEW, needRegisterClass);
                                //new指令 创建对象
                                methodVisitor.visitInsn(Opcodes.DUP);//复制栈顶指针的值
                                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, needRegisterClass,//调用构造方法
                                        "<init>", "()V", false);

                                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className,//调用指定register方法进行注册
                                        method, "(L" + superClassName + ";)V", false);
                            }
                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitMaxs(int i, int i1) {
                            super.visitMaxs(i + 4, i1);
                        }
                    };
                }
                return methodVisitor;
            }
        };
        classReader.accept(classVisitor, 0);
        byte[] bytes = classWriter.toByteArray();

        def optClass = new File(file.getParent(), file.name + ".opt")
        FileOutputStream outputStream = new FileOutputStream(optClass)
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
            println 'xxxxx delete file = ' + file.absolutePath
        }
        optClass.renameTo(file)
    }

}
