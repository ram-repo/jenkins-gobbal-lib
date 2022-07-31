def call() {

  def jobNameParts = env.JOB_NAME.tokenize('/') as String[] // [TEST Projects, test-pawel-test-1-docker, master]
  jobNameParts.length < 2 ? env.JOB_NAME : jobNameParts[jobNameParts.length - 2]
  def buildType = jobNameParts[1].replaceAll('.*-', '')
  def jenkinsSlaveRoleArn = false

  if (params.configFile != null){
    jenkinsSlaveRoleArn = "arn:aws:iam::" + params.configFile.split("/")[1] + ":role/aaaaa-jenkins-slave"
  } else {
    // Printing message that first time run and params are not taken into effect
    // https://issues.jenkins-ci.org/browse/JENKINS-40574 - it is marked resolved but the last comment says it doesn't work for declaritive pipelines
    jenkinsSlaveRoleArn = "First time build, please rerun to use AWS IAM role"
  }

  pipeline {
    agent {
      kubernetes {
        yaml """
          apiVersion: v1
          kind: Pod
          metadata:
            labels:
              name: terraform-pipeline
            annotations:
              iam.amazonaws.com/role: ${jenkinsSlaveRoleArn}
          spec:
            imagePullSecrets:
              - name: artifactory
            containers:
              - name: build-tools
                image: artifactory.aaaaa.aaaaacompany.com/docker/aaaa-build-tools:${buildToolsImageVersion}
                command:
                  - cat
                tty: true
              - name: lambda-app-builder
                image: artifactory.aaaaa.aaaaacompany.com/docker/aaaaa-lambda-app-builder:${buildToolsLambdaAppImageVersion}
                command:
                  - cat
                tty: true
        """
      }
    }

    parameters {
      string(defaultValue: "path/to/tf/config/file", description: 'Please specify path to config file', name: 'configFile')
      choice(name: 'Terraform Action', choices: ['none', 'plan', 'apply', 'destroy'], description: 'Pick plan, apply, destroy or none')
    }

    stages {
      stage('GIT Checkout'){
          steps {
            script {
                def scmVars = checkout scm
                // extract git information
                env.GIT_COMMIT = scmVars.GIT_COMMIT
                env.GIT_BRANCH = scmVars.GIT_BRANCH
                GIT_COMMIT = "${scmVars.GIT_COMMIT}"
                GIT_BRANCH = "${scmVars.GIT_BRANCH}"
              }
          }
      }

      stage('build lambda app') {
        when { expression { params['configFile'] != "path/to/tf/config/file" }}
        steps {
          container('lambda-app-builder'){
            ansiColor('xterm') {
              script {
                if (buildType == "lambda") {
                  if (fileExists("gradle")){
                    sh "gradle build"
                  } else if (fileExists("install_deps.sh")) {
                    sh "./install_deps.sh"
                  }
                }
              }
            }
          }
        }
      }

      stage('SonarQube Test & Coverage') {
        when { expression { params['configFile'] != "path/to/tf/config/file" }}
        steps {
          container('lambda-app-builder'){
            ansiColor('xterm') {
              script {
                if (buildType == "lambda") {
                  if (fileExists("gradle")) {
                    sh "chmod +x gradlew"
                    sh "./gradlew test"
                  }
                  else if (fileExists("install_deps.sh")) {
                    sh "./install_deps.sh"
                  }
                  else {
                    echo 'No gradle file or POM file found for test coverage, this is a python project proceeding further'
                  }
                }
              }
            }
          }
        }
      }

      stage('set up git to be able to download modules'){
        steps {
          withCredentials([usernamePassword(credentialsId: '6532e3ff-1c9c-46f6-9d3d-5a1505c41cf9', passwordVariable: 'Password', usernameVariable: 'Username')]) { //added git cred's ID 
            container('build-tools'){
              ansiColor('xterm') {
                sh("""
                  git config --global credential.username ${Username}
                  git config --global credential.helper "!echo password=${Password}; echo"
                """)
              }
            }
          }
        }
      }
      
      stage('terraform init and validate') {
        steps{
          withCredentials([usernamePassword(credentialsId: 'artifactory', passwordVariable: 'Password', usernameVariable: 'Username')]){
            script {
              container ('build-tools') {
                if (buildType == "terraform" && params['configFile'] != "path/to/tf/config/file") {
                  sh """
                    curl -u "${Username}":"${Password}" -s -L ${terraformInitScriptUrl} -o tf_init.sh
                    chmod +x tf_init.sh
                    ./tf_init.sh "./${params.configFile}"
                    terraform validate
                  """
                } else if (buildType == "lambda" && params['configFile'] != "path/to/tf/config/file") {
                  if (fileExists("terraform")){
                    dir("terraform"){
                      sh """
                        curl -u "${Username}":"${Password}" -s -L ${terraformInitScriptUrl} -o tf_init.sh
                        chmod +x tf_init.sh
                        ./tf_init.sh "./${params.configFile}"
                        terraform validate
                      """
                    }
                  } else {
                      sh """
                        set +x
                        RED='\033[0;31m'
                        BOLD='\033[1m'
                        NC='\033[0m'
                        echo -e "\${RED}+==================================[NOTICE]===================================+\${NC}"
                        echo -e "\t\${BOLD}terraform\${NC} folder was not detected in your repo.\n\tThis folder with terraform code is required to run lambda pipeline."
                        echo -e "\${RED}+=============================================================================+\${NC}"
                      """
                    error("terraform directory does not exist in your git repository")
                  }
                } else if (buildType == "module"){
                  sh """
                    terraform init
                    AWS_REGION=us-east-1 terraform validate
                  """
                }
              }
            }
          }
        }
      }

      stage('terraform plan') {
        steps {
          container('build-tools'){
            ansiColor('xterm') {
              script {
                if (params['Terraform Action'] == "plan" && params['configFile'] != "path/to/tf/config/file") {
                  if (buildType == "terraform") {
                    sh "terraform plan -var-file=${params.configFile}"
                  } else if (buildType == "lambda") {
                    dir("terraform"){
                      sh "terraform plan -var-file=${params.configFile}"
                    }
                  }
                }
              }
            }
          }
        }
      }

      stage('terraform apply') {
        steps {
          container('build-tools'){
            ansiColor('xterm') {
              script {
                if (params['Terraform Action'] == "apply" && params['configFile'] != "path/to/tf/config/file") {
                  if (buildType == "terraform") {
                    sh "terraform plan -var-file=${params.configFile}"
                    sh "terraform apply -var-file=${params.configFile} --auto-approve"
                  } else if (buildType == "lambda") {
                    dir("terraform"){
                      sh "terraform plan -var-file=${params.configFile}"
                      sh "terraform apply -var-file=${params.configFile} --auto-approve"
                    }
                  }
                }
              }
            }
          }
        }
      }

      stage('Sonarqube EnterPrise Scan') {
        /* groovylint-disable-next-line SpaceAfterClosingBrace */
        steps {
          withSonarQubeEnv('Sonar Ent Tooling') {
            container('lambda-app-builder') {
                script {
                  if (fileExists("build.gradle")){
                  def projectName = env.JOB_NAME.split("/")[1]
                  def orgName = projectName.split("-")[0,1].join()
                  sh """cat << EOF > gradle.properties
sonar.projectKey=com.aaaaacompany.${orgName}:${projectName}
sonar.projectName=${projectName}
EOF"""
                  sh "chmod +x gradlew"
                  sh  "./gradlew sonarqube -Dsonar.projectKey=com.aaaaacompany.${orgName}:${projectName} -Dsonar.projectName=${projectName}"
                } else if (fileExists("pom.xml")) {
                  sh 'mvn -Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.http.retryHandler.count=10 clean install sonar:sonar -X'
                } else if (fileExists("package.json")){
                  def scannerHome = tool 'Sonar Scanner';
                  def projectName = env.JOB_NAME.split("/")[1]
                  def orgName = projectName.split("-")[0,1].join()
                  def test-report-path = test-report.xml
                  sh """cat << EOF > sonar-project.properties
sonar.projectKey=com.aaaaacompany.${orgName}:${projectName}
sonar.projectName=${projectName}
sonar.testExecutionReportPaths=${test-report-path}
sonar.tests=src
sonar.javascript.lcov.reportPaths=test-coverage/lcov.info
EOF"""
                  sh "npm install"
                  sh "npm run coverage-lcov"
                  sh "${scannerHome}/bin/sonar-scanner \
                  -Dsonar.projectKey=com.aaaaacompany.${orgName}:${projectName} \
                  -Dsonar.projectName=${projectName} -Dsonar.testExecutionReportPaths=${test-report-path} \
                  -Dsonar.tests=src \
                  -Dsonar.javascript.lcov.reportPaths=test-coverage/lcov.info"
                } else if (fileExists("*.py")){
                  def scannerHome = tool 'Sonar Scanner';
                  def projectName = env.JOB_NAME.split("/")[1]
                  def orgName = projectName.split("-")[0,1].join()
                  sh """cat << EOF > sonar-project.properties
sonar.projectKey=com.aaaaacompany.${orgName}:${projectName}
sonar.projectName=${projectName}
sonar.sourceEncoding=UTF-8
sonar.python.coverage.reportPaths=Coverage.xml
EOF"""            
                  sh "{scannerHome}/bin/sonar-scanner -X \
                  -Dsonar.projectKey=com.aaaaacompany.${orgName}:${projectName} \
                  -Dsonar.projectName=${projectName} \
                  -Dsonar.sourceEncoding=UTF-8 \
                  -Dsonar.python.coverage.reportPaths=Coverage.xml"
                } else {
                  def scannerHome = tool 'Sonar Scanner';
                  def projectName = env.JOB_NAME.split("/")[1]
                  def orgName = projectName.split("-")[0,1].join()
                  sh """cat << EOF > sonar-project.properties
sonar.projectKey=com.aaaaacompany.${orgName}:${projectName}
sonar.projectName=${projectName}
EOF"""
                sh "${scannerHome}/bin/sonar-scanner"
              }
            }
          }
        }
      }
    }


    stage('Sonar Quality Gate') {
      steps {
        script {
          timeout(time: 15, unit: 'MINUTES') { //Entire Pipeline will fail just in case if something goes wrong
          def qg = waitForQualityGate() //task ID to reused from Sonar Background tasks
          if (qg.status != 'OK') {
            // error "Pipeline aborted due to quality gate failure: ${qg.status}" // uncomment this when we want to fail pipeline if sonar fails(Not recommended)
            echo 'Sonar Quality Gate Failed'
          }
        }
      }
    }
  }

      stage('terraform destroy'){
        steps {
          container('build-tools'){
            ansiColor('xterm') {
              script {
                if (params['Terraform Action'] == "destroy" && params['configFile'] != "path/to/tf/config/file") {
                  if (buildType == "terraform") {
                    sh "terraform plan -destroy -var-file=${params.configFile}"
                    sh "terraform destroy -var-file=${params.configFile} --auto-approve"
                  } else if (buildType == "lambda") {
                    dir("terraform"){
                      sh "terraform plan -destroy -var-file=${params.configFile}"
                      sh "terraform destroy -var-file=${params.configFile} --auto-approve"
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

