import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.io.File
import java.util.*

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2021.1"


project {
    description = "DevSecOps-SysMonTS"
    val services = File(DslContext.baseDir, "services.conf")

    var projectList = arrayListOf<Project>()

    var confList=arrayListOf<ConfigurationTS>()  //готовый к итерированию объект с конфигурацией
    var microServicesList=arrayListOf<String>()  //временная переменная со строками параметров микросервисов проекта

    var subTS=""

    //пройдем по строкам файла и создадим объект
    //формат -TSSXX начало конфигапроекта  # - окончание конфига проекта
    services.forEachLine {
        if (it[0] == '-') {
            subTS=it.replace("-","")
            microServicesList.clear()
        } else if (it[0] == '#') {
            confList.add(ConfigurationTS(subTS, microServicesList))
        } else {
            microServicesList.add(it)
        }
    }

    confList.forEach{ c->

        var subProjectList = arrayListOf<Project>()
        c.microServices.forEach{ m->

            var confLine = m.split(';').toTypedArray()
            var microService=confLine[0]
            var vcsRootUrl=confLine[1]
            //var projectLang=confLine[2]
            var vcsUsername=confLine[3]
            var vcsPassword=confLine[4]

            subProjectList.add(Project {
                name = microService
                id(c.projectName.plus("_").plus(microService))

                // subservice1
                var developProject = Project{
                    name = "develop"
                    var parentJobId=c.projectName.plus("_").plus(microService).plus(this.name)
                    id(parentJobId)

                    object subproj_vcs : GitVcsRoot({
                        url = vcsRootUrl
                        authMethod = password {
                            userName = vcsUsername
                            password = vcsPassword
                        }
                    })

                    vcsRoot(subproj_vcs)

                    var bts = sequential {
                        parallel {
                            buildType(ScriptBuildType("Этап 1.1", parentJobId.plus("stage1_1"), "Сканирование кода в sonarqube", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.2", parentJobId.plus("stage1_2"), "Сканирование кода в fortify", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.3", parentJobId.plus("stage1_3"),"Линтеры", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.4", parentJobId.plus("stage1_4"),"unit тесты", "echo 1"))
                        }
                        buildType(ScriptBuildType("Этап 2", parentJobId.plus("stage2"), "Сборка", "echo 1"))
                        buildType(ScriptBuildType("Этап 3", parentJobId.plus("stage3"),"Деплой на dev st", "echo 1"))
                        buildType(ScriptBuildType("Этап 4", parentJobId.plus("stage4"),"Системные тесты", "echo 1"))
                    }.buildTypes()

                    bts.forEach { bt -> buildType(bt) }
                }

                // subservice2
                var pullRequestProject = Project{
                    name = "pullrequest"
                    var parentJobId=c.projectName.plus("_").plus(microService).plus(this.name)
                    id(parentJobId)

                    var bts = sequential {
                        parallel {
                            buildType(ScriptBuildType("Этап 1.1", parentJobId.plus("stage1_1"), "Сканирование кода в sonarqube", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.2", parentJobId.plus("stage1_2"),"Сканирование кода в fortify", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.3", parentJobId.plus("stage1_3"),"Линтеры", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.4", parentJobId.plus("stage1_4"),"unit тесты", "echo 1"))
                        }
                        buildType(ScriptBuildType("Этап 2", parentJobId.plus("stage2"),"Сборка", "echo 1"))
                        buildType(ScriptBuildType("Этап 3", parentJobId.plus("stage3"),"Деплой на dev", "echo 1"))
                        buildType(ScriptBuildType("Этап 4", parentJobId.plus("stage4"),"Системные тесты", "echo 1"))
                    }.buildTypes()

                    bts.forEach { bt -> buildType(bt) }
                }

                //subservice3
                var releaseProject = Project{
                    name = "release"
                    var parentJobId=c.projectName.plus("_").plus(microService).plus(this.name)
                    id(parentJobId)

                    var bts = sequential {
                        parallel {
                            buildType(ScriptBuildType("Этап 1.1", parentJobId.plus("stage1_1"), "Сканирование кода в sonarqube", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.2", parentJobId.plus("stage1_2"),"Сканирование кода в fortify", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.3", parentJobId.plus("stage1_3"),"Линтеры", "echo 1"))
                            buildType(ScriptBuildType("Этап 1.4", parentJobId.plus("stage1_4"),"unit тесты", "echo 1"))
                        }
                        buildType(ScriptBuildType("Этап 2", parentJobId.plus("stage2"),"Сборка", "echo 1"))
                        buildType(ScriptBuildType("Этап 3", parentJobId.plus("stage3"),"Деплой на нфт", "echo 1"))
                        buildType(ScriptBuildType("Этап 4", parentJobId.plus("stage4"),"Нагрузочные тесты", "echo 1"))
                        buildType(ScriptBuildType("Этап 5", parentJobId.plus("stage5"),"Деплой на ПСИ", "echo 1"))
                        buildType(ScriptBuildType("Этап 6", parentJobId.plus("stage6"),"Системные тесты", "echo 1"))
                    }.buildTypes()

                    bts.forEach { bt -> buildType(bt) }
                }

                subProject(developProject)
                subProject(pullRequestProject)
                subProject(releaseProject)


            })
        }

        projectList.add(Project {
            name = c.projectName
            id(c.projectName)

            subProjectList.forEach { sp -> subProject(sp) }
        })
    }

    projectList.forEach{subProject(it)}

}

class ConfigurationTS(projectName: String, microServices: ArrayList<String>, vscRoot: ){
    val projectName=projectName
    val microServices=microServices
}

class MavenBuildType(name: String, jobId: String, goals: String, runneArgs: String? = null): BuildType({
    this.name=name
    id(jobId)

    vcs{
    }

    steps{
        maven{
            this.goals=goals
            this.runnerArgs=runnerArgs
        }
    }
})

class ScriptBuildType(name: String, jobId: String, scriptName: String, scriptContent: String): BuildType({
    this.name=name
    id(jobId)

    vcs{
    }

    steps{
        script{
            this.name=scriptName
            this.scriptContent=scriptContent
        }
    }
})

//class JavaBuildConfiguration() {
//    //companion object pipeline {
//        fun createDevelopBuildConfiguration(): List<BuildType> {
//            var bts = sequential {
//                parallel {
//                    buildType(ScriptBuildType("Первый этап", "Сканирование кода в sonarqube", "echo 1"))
//                    buildType(ScriptBuildType("Первый этап", "Сканирование кода в fortify", "echo 1"))
//                    buildType(ScriptBuildType("Первый этап", "Линтеры", "echo 1"))
//                    buildType(ScriptBuildType("Первый этап", "unit тесты", "echo 1"))
//                }
//                buildType(ScriptBuildType("Второй этап", "Сборка", "echo 1"))
//                buildType(ScriptBuildType("Третий этап", "Деплой на dev st", "echo 1"))
//                buildType(ScriptBuildType("stage 4", "system test", "echo 1"))
//            }.buildTypes()
//        return bts
//        }
//   // }
//}



object Testtsubproj_Testtvcsrootname : GitVcsRoot({
    name = "testtvcsrootname"
    url = "https://bitbucket.region.vtb.ru/scm/pfom/omni9.git"
    branch = "refs/heads/develop"
    authMethod = password {
        userName = "vtb4052236@region.vtb.ru"
        password = "credentialsJSON:1794872e-b876-43ed-8891-d6e1e989a9e6"
    }
})


object Testtsubproj_Buildconf : BuildType({
    name = "develop"

    vcs {
        root(Testtsubproj_Testtvcsrootname)
    }

    steps {
        script {
            name = "custom scriptt"
            scriptContent = "env"
        }
    }
})


