package util

class JobSharedUtils {

    public static final String DEFAULT_SCHEDULE = 'H/10 * * * *'

    static defaultBuildDiscarder(def job) {
        job.with {
            logRotator {
                daysToKeep(30)
                numToKeep(10)
                artifactDaysToKeep(60)
                artifactNumToKeep(5)
            }
        }
    }

    static gitParameters(def params, def gitRepositoryUrl, def branch) {
        params.with {
            stringParam {
                name ("GIT_REPOSITORY_URL")
                defaultValue(gitRepositoryUrl)
            }
            stringParam {
                name ("GIT_REPOSITORY_BRANCH")
                defaultValue(branch)
            }
        }
    }

    static mavenParameters(Map args) {
        if (args.mavenSettingsXml == null) {
            args.mavenSettingsXml = '/opt/tools/settings.xml'
        }

        if (args.javaHome == null) {
            args.javaHome = '/opt/oracle/java'
        }

        args.params.with {
            stringParam {
                name ("MAVEN_HOME")
                defaultValue("/opt/apache/maven")
            }
            stringParam {
                name ("JAVA_HOME")
                defaultValue(args.javaHome)
            }
            stringParam {
                name ("MAVEN_SETTINGS_XML")
                defaultValue(args.mavenSettingsXml)
            }
            stringParam {
                name ("MAVEN_OPTS")
                defaultValue("-Dmaven.wagon.http.ssl.insecure=true -Dhttps.protocols=TLSv1.2")
            }
        }
    }

    static doDisableConcurrentBuilds(def properties) {
        properties.with {
            disableConcurrentBuilds {
                abortPrevious(false)
            }
        }
    }

    static defaultPublishers(def job) {
        job.with {
            archiveJunit('**/target/surefire-reports/*.xml') {
                allowEmptyResults()
                retainLongStdout()
                healthScaleFactor(1.0)
            }
            extendedEmail()
            wsCleanup()
        }
    }

    static defaultSettings(def job) {
        job.with {
            preBuildCleanup()
            timeout {
                absolute(600)
            }
        }
    }

    static defaultMaven(def job) {
        job.with {
            env('MAVEN_HOME', '/opt/apache-maven-3.6.3')
            env('PATH', '$PATH:$MAVEN_HOME/bin')
        }
    }

    static customParams(def params, def customParams) {
        customParams.delegate = params
        customParams.resolveStrategy = Closure.DELEGATE_FIRST
        customParams.call()
    }
}
