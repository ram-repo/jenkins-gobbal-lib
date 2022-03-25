
package com.org

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials
import com.cloudbees.plugins.credentials.CredentialsProvider
import hudson.util.Secret
import hudson.plugins.git.*
import hudson.model.BuildAuthorizationToken
import org.apache.commons.lang.RandomStringUtils
import hudson.tasks.Mailer
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.scm.SCM
import hudson.model.ListView
import hudson.views.ListViewColumn
import hudson.triggers.SCMTrigger
import hudson.*
import hudson.security.*
import java.util.*
import com.michelin.cio.hudson.plugins.rolestrategy.*
import java.lang.reflect.*

def createNewJenkinsJobWithMultiBranch(String  projectView, String repoName ){

    jobDsl additionalParameters: [
        projectView: projectView,
        repoName: repoName,
        scmCredsID: scmCredsID
    ], scriptText: '''
    multibranchPipelineJob("${projectView}/${repoName}") {
    branchSources {
        github {
            id('34343434') // IMPORTANT: use a constant and unique identifier
            scanCredentialsId('BREP-GitHubApp')
            repoOwner('bre-org')
            repository("${repoName}")
            includes("master feature/* bugfix/* hotfix/* release/*")
            excludes("donotbuild/*")
        }
    }
  	factory {
        workflowBranchProjectFactory {
            scriptPath("jenkinsFile.groovy")
        }
    }
    triggers {
        periodicFolderTrigger {
            interval("2m")
        }
    }
    orphanedItemStrategy {
        discardOldItems {
            numToKeep(10)
        }
    }
}
'''
}

return this

