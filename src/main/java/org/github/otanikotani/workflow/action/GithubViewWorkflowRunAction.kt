package org.github.otanikotani.workflow.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.github.otanikotani.workflow.GitHubWorkflowRunManager
import org.jetbrains.plugins.github.AbstractGithubUrlGroupingAction
import org.jetbrains.plugins.github.util.GitRemoteUrlCoordinates

class GithubViewWorkflowRunAction :
  AbstractGithubUrlGroupingAction("View Workflows", null, AllIcons.Vcs.Vendors.Github) {
  override fun actionPerformed(e: AnActionEvent, project: Project, repository: GitRepository, remote: GitRemote, remoteUrl: String) {
    val remoteCoordinates = GitRemoteUrlCoordinates(remoteUrl, remote, repository)
    project.service<GitHubWorkflowRunManager>().showTab(remoteCoordinates)
  }
}