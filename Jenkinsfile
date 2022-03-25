@Library('jenkinslib') _

pipeline {
    agent any
    parameters {
        string(name: 'projectname', defaultValue: 'bre', description: 'projectname' )
        // choice(name: 'GOAL', choices: ['package', 'clean package', 'install'], description: 'maven goals')
        string (name: 'destGit', defaultValue: 'reponame', description: 'reponame', trim: true)
        string (name: 'projectsFolder', defaultValue: 'dev', description: 'foldername', trim: true)
        string (name: 'destProject', defaultValue: 'destProject', description: 'destProject', trim: true)
        string (name: 'githubid', defaultValue: 'githubid', description: 'githubid', trim: true)
        string (name: 'destGit', defaultValue: 'destGit', description: 'destGit', trim: true)
        // booleanParam name: 'test', description: 'true'
        }
    stages{
        stage('create multibranch'){
            steps{
                scripts{
                repobuilder.createNewJenkinsJobWithMultiBranch("${params.projectsFolder}", "${params.projectName}", "${params.destProject}", "${params.destGit}", "${params.githubid}")
                }
            }
        }
    }
}
