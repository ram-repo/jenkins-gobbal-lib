properties([
    parameters([
        choice(name: 'projectsview', description: 'Please pick one', choices: ['dev', 'qa', 'test', 'prod']),
        string(name: 'repoName', description: 'id github ', defaultValue: 'test')
    ])])

pipeline {
    agent any
    stages{
        stage('New MutiBranch pipline'){
            steps{
                script{
                Mutibranchpipline.createNewJenkinsJobWithMultiBranch("$params.projectsview", "$params.repoName")
                }
            }
        }
    }
}
