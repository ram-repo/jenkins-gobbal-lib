multibranchPipelineJob('devOps9') {
    branchSources {
		displayName('devOps9')
		description('Hi Testing mvn')
        github {
            id('91179757') // IMPORTANT: use a constant and unique identifier
            scanCredentialsId('usergithub')
            repoOwner('ram-repo') // https://github.com/ram-repo/maven-sample.git
            repository('maven-sample')
            includes("master feature/* bugfix/* hotfix/* release/*")
            excludes("donotbuild/*")
        }
    }
  	factory {
        workflowBranchProjectFactory {
            scriptPath("Jenkinsfile")
        }
    }
    triggers {
        periodicFolderTrigger {
            interval("2m")
        }
    }
    orphanedItemStrategy {
		primaryView('jobdsl')
        discardOldItems {
            numToKeep(10)
        }
    }
}
