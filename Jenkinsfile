@Library('jenkinslib') _

pipeline {
    agent any
    parameters {
        string(name: 'name', defaultValue: 'bre', description: 'projectname' )
        // choice(name: 'GOAL', choices: ['package', 'clean package', 'install'], description: 'maven goals')
        string (name: 'Git', defaultValue: 'reponame', description: 'reponame', trim: true)
        string (name: 'Folder', defaultValue: 'dev', description: 'foldername', trim: true)
        string (name: 'dest', defaultValue: 'destProject', description: 'destProject', trim: true)
        string (name: 'id', defaultValue: 'githubid', description: 'githubid', trim: true)
        string (name: 'dest', defaultValue: 'destGit', description: 'destGit', trim: true)
        // booleanParam name: 'test', description: 'true'
        }
    stages{
        stage('create multibranch'){
            steps{
                scripts{
                repobuilder.createNewJenkinsJobWithMultiBranch("${params.Folder}","${params.Name}","${params.dest}","${params.Git}","${params.id}")
                }
            }
        }
    }
}
