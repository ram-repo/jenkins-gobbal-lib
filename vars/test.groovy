def call() {
    pipeline {
        agent any

        stages {
            stage('Hello') {
                steps {
                    script {
                        echo 'Hello World'
                        printfunc('murali', 'devops', 'jenkinspipeline')
                    }
                }
            }
        }
    }
}
