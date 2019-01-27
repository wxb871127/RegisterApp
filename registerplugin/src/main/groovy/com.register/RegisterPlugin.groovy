package com.register

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppPlugin


public  class RegisterPlugin implements Plugin<Project>{

    public static final String ExtenName = 'registerplugin'

    @Override
    public void apply(Project project){
        println "Hello gradle plugin"
        def isApp = project.plugins.hasPlugin(AppPlugin)
//        println 'plugins is :'
//        project.plugins.forEach(){
//            Plugin plugin ->
//                println plugin.toString()
//        }
//        println '\n'
//
        if(isApp){
            project.extensions.create(ExtenName, RegisterConfig)
            RegisterConfig params = project.extensions.findByName(ExtenName) as RegisterConfig
            params.parseParams()
//            println params.toString()
        }





    }

}