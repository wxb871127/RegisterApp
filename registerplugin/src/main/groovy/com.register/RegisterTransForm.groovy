package com.register

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.file.Directory

public class RegisterTransForm extends Transform{

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
                transformInput.directoryInputs.each {
                    DirectoryInput directory ->
                        File dest = outputProvider.getContentLocation(directory.name,
                                directory.contentTypes, directory.scopes, Format.DIRECTORY)
//                        println 'dest = '+ dest.absolutePath
//                        String root = directory.file.absolutePath
//                        if (!root.endsWith(File.separator))
//                            root += File.separator

                        directory.file.eachFileRecurse {
                            File file ->
//                                def path = file.absolutePath.replace(root, '')
                                def entryName = file.absolutePath
                                if(file.isFile()){
                                    entryName = entryName.replaceAll("\\\\", "/")
//                                    Class cls = Class.forName(entryName.substring(0, entryName.lastIndexOf('.')))
//                                    Class t = Class.forName('register.com.registerapp.Template')
//                                    println 'Template = ' + t.toString()

//                                    println 'annotation Class = ' + cls.getAnnotation(t)

//                                    println 'entryName = ' + entryName
//                                    println 'dest = '+ dest.absolutePath + File.separator + path

                                }
                        }
                }
        }
    }
}