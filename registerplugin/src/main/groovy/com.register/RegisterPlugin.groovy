package com.register

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppPlugin


public  class RegisterPlugin implements Plugin<Project>{

    public static final String ExtenName = 'registerplugin'

    @Override
    public void apply(Project project){
        def isApp = project.plugins.hasPlugin(AppPlugin)

        if(isApp) {
            project.extensions.create(ExtenName, RegisterConfig)
            def android = project.extensions.getByType(AppExtension)
//            def android = project.extensions.getByType(BaseExtension.class)
            RegisterTransForm registerTransForm = new RegisterTransForm()
            android.registerTransform(registerTransForm)

            project.afterEvaluate {
                RegisterConfig params = project.extensions.findByName(ExtenName) as RegisterConfig
                params.parseParams()
                println params.toString()
                registerTransForm.registerConfig = params
            }
        }

    }

}