package com.register;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;


class ScanClassVisitor extends ClassVisitor {
    private String filePath
    def found = false

    ScanClassVisitor(int api, ClassVisitor cv, String filePath) {
        super(api, cv)
        this.filePath = filePath
    }

    boolean is(int access, int flag) {
        return (access & flag) == flag
    }

    public boolean isFound() {
        return found
    }

    void visit(int version, int access, String name, String signature,
               String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces)
        //抽象类、接口、非public等类无法调用其无参构造方法
        if (is(access, Opcodes.ACC_ABSTRACT)
                || is(access, Opcodes.ACC_INTERFACE)
                || !is(access, Opcodes.ACC_PUBLIC)
                ) {
            return
        }
        infoList.each { ext ->
            if (shouldProcessThisClassForRegister(ext, name)) {
                if (superName != 'java/lang/Object' && !ext.superClassNames.isEmpty()) {
                    for (int i = 0; i < ext.superClassNames.size(); i++) {
                        if (ext.superClassNames.get(i) == superName) {
                            //  println("superClassNames--------"+name)
                            ext.classList.add(name) //需要把对象注入到管理类 就是fileContainsInitClass
                            println '=========name = ' + name
                            found = true
                            addToCacheMap(superName, name, filePath)
                            return
                        }
                    }
                }
                if (ext.interfaceName && interfaces != null) {
                    interfaces.each { itName ->
                        if (itName == ext.interfaceName) {
                            ext.classList.add(name)//需要把对象注入到管理类  就是fileContainsInitClass
                            println '============ name='+name
                            addToCacheMap(itName, name, filePath)
                            found = true
                        }
                    }
                }
            }
        }
    }
}
