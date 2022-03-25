multibranchPipelineJob("mvn-branch21") {
	branchSources {
        github {                
			id('91179757') // IMPORTANT: use a constant and unique identifier
            scanCredentialsId('usergithub')
            repoOwner('ram-repo') // https://github.com/ram-repo/maven-sample.git
          	// repository_url": "https://api.github.com/repos/ram-repo/maven-sample"
            repository('maven-sample')
            includes("master feature/* bugfix/* hotfix/* release/*")
            excludes("donotbuild/*")
        }
      	displayName('MVN-project')
      	description('description of Project')
      	configure {
          def traits = it / navigators / 'org.jenkinsci.plugins.github__branch__source.GitHubSCMNavigator' / traits
          traits << 'org.jenkinsci.plugins.github_branch_source.BranchDiscoveryTrait' {
            strategyId (3)
          }
    	  traits << 'org.jenkinsci.plugins.github__branch__source.OriginPullRequestDiscoveryTrait' {
        	strategyId (1)
    	  }
		  traits << 'org.jenkinsci.plugins.github__branch__source.TagDiscoveryTrait' {
        	strategyId (1)
    	  }
		}
    }
	factory {
        workflowBranchProjectFactory {
            scriptPath("Jenkinsfile")
            }
    }
    triggers {
        periodicFolderTrigger {
            interval("20m")
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(10)
        }
    }
}
