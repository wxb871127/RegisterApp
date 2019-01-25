package com.register

import org.gradle.api.Plugin
import org.gradle.api.Project

public  class RegisterPlugin implements Plugin<Project>{

    @Override
    public void apply(Project project){
        println "Hello gradle plugin"

        project.extensions.create('pluginExt', ExtensionParams)

        ExtensionParams params = project.extensions.findByName('pluginExt') as ExtensionParams

        println 'params is ' + params.toString()
        println 'templateStateClass is ' + params.templateStateClass + ',templateTagClass is '+ params.templateTagClass
    }

}